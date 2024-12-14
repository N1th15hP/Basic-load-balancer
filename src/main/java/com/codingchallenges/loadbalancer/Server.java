package com.codingchallenges.loadbalancer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Server {
	
	private static final Logger logger = LogManager.getLogger(Server.class);

	private int port;
	private ExecutorService threadPool;
	private List<Integer> portsList;
	List<Integer> registeredServersList;
	private AtomicInteger currentPortIndex;

	private int getNextPort() {
		return portsList.get(currentPortIndex.getAndUpdate(i -> (i + 1) % portsList.size()));
	}

	private void handleRequest(Socket clientSocket) {
		String requestPath = null;		// Read and log the HTTP request
		try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				PrintWriter out = new PrintWriter(clientSocket.getOutputStream());) {
			String line;
			List<String> request = new ArrayList<>();
			while ((line = in.readLine()) != null && !line.isEmpty()) {
				request.add(line);
			}

			// Forward the request to the backend server (localhost:8081)
			try (Socket backendSocket = new Socket("localhost", getNextPort());
					PrintWriter backendOut = new PrintWriter(backendSocket.getOutputStream());
					BufferedReader backendIn = new BufferedReader(
							new InputStreamReader(backendSocket.getInputStream()))) {

				// Forward the HTTP request to the backend server
				for (String requestLine : request) {
					backendOut.println(requestLine);
				}
				backendOut.println(); // End of request headers
				backendOut.flush();

				// Read the response from the backend server
				String responseLine;
				while ((responseLine = backendIn.readLine()) != null) {
					// Forward the response back to the client
					out.println(responseLine);
				}
			} catch (IOException e) {
				System.err.println("Error forwarding request to backend: " + e.getMessage());
				out.println("HTTP/1.1 500 Internal Server Error");
				out.println();
			}

		} catch (IOException e) {
			System.err.println("Error reading request: " + e.getMessage());
		} finally {
			// Close the client socket
			try {
				clientSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void performHealthCheck() {
	    registeredServersList.forEach(port -> {
	        try (Socket backendSocket = new Socket("localhost", port)) {
	            // Set timeout to avoid blocking forever
	            backendSocket.setSoTimeout(2000); // 2 seconds timeout for health check

	            try (PrintWriter backendOut = new PrintWriter(backendSocket.getOutputStream());
	                 BufferedReader backendIn = new BufferedReader(new InputStreamReader(backendSocket.getInputStream()))) {

	                // Construct the HTTP GET request
	                backendOut.println("GET /healthCheck HTTP/1.1");
	                backendOut.println("Host: localhost:" + port);
	                backendOut.println("Connection: close");
	                backendOut.println(); // Blank line to end the headers
	                backendOut.flush();

	                // Read the response from the backend server
	                String responseFirstLine = backendIn.readLine();
	                if (responseFirstLine != null) {
	                    int statusCode = Integer.parseInt(responseFirstLine.split(" ")[1]);

	                    if (statusCode != 200) {
	                        // Handle unhealthy server
	                        int currentIndex = portsList.indexOf(port);

	                        if (currentIndex == currentPortIndex.get()) {
	                            getNextPort(); // Update the currentPortIndex
	                        }

	                        portsList.remove(Integer.valueOf(port)); // Remove the port from the list
	                    } else {
	                        logger.info("Server healthy on port: " + port);
	                        
	                        if(!portsList.contains(port)) {
	                        	portsList.add(port);
	                        }
	                    }
	                }

	            } catch (IOException e) {
	                logger.error("Error reading response or sending request: " + e.getMessage());
	                handleServerFailure(port);
	            }

	        } catch (IOException e) {
	            logger.error("Error connecting to server on port " + port + ": " + e.getMessage());
	            handleServerFailure(port);
	        }
	    });
	}

	private void handleServerFailure(int port) {
	    int currentIndex = portsList.indexOf(port);

	    if (currentIndex == currentPortIndex.get()) {
	        getNextPort(); // Update the currentPortIndex
	    }

	    portsList.remove(Integer.valueOf(port)); // Remove the port from the list
	}


	public Server(int port, int threadPoolSize) {
		this.port = port;
		this.threadPool = Executors.newFixedThreadPool(threadPoolSize);
		this.registeredServersList = Arrays.asList(8081, 8082, 8083);
		this.portsList = Arrays.asList(8081, 8082, 8083);
		this.currentPortIndex = new AtomicInteger(-1);

		ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

		scheduler.scheduleAtFixedRate(() -> {
		    try {
		        performHealthCheck(); // Perform health check
		    } catch (Exception e) {
		        // Log the error but do not terminate the health check schedule
		        logger.error("Error during health check: " + e.getMessage());
		    }
		}, 0, 5, TimeUnit.SECONDS); // Execute every 5 seconds

		try (ServerSocket server = new ServerSocket(port)) {

			while (true) {
				Socket clientSocket = server.accept();
				logger.info("Recieved request from " + clientSocket.getInetAddress() + "on port " + port);
				threadPool.submit(() -> handleRequest(clientSocket));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
