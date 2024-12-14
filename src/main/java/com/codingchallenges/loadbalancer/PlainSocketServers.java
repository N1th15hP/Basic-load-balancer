package com.codingchallenges.loadbalancer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class PlainSocketServers {
	public static void main(String[] args) throws IOException {
        startServer(8081, "Hello from Server 1!");
        startServer(8082, "Hello from Server 2!");
        startServer(8083, "Hello from Server 3!");
    }

    private static void startServer(int port, String message) {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("Plain socket server started on port " + port);
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Received request on port " + port + " from " + clientSocket.getInetAddress());
                    
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    String line = null;
                    List<String> request = new ArrayList<>();
                    String requestPath = null;
                    
        			while ((line = in.readLine()) != null && !line.isEmpty()) {
        				request.add(line);

        				if (line.startsWith("GET")) {
        					requestPath = line.split(" ")[1];
        				}
        			}
                    
                    String content = null;
                    
					if ("/healthCheck".equalsIgnoreCase(requestPath)) {
						content = "server online on port: " + port;
					} else {
						content = "<html><body><h1>" + message + "</h1></body></html>";
					}
					
                    try (PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
                        out.println("HTTP/1.1 200 OK");
                        out.println("Content-Type: text/html");
                        out.println("Content-Length: " + content.length());
                        out.println();
                        out.println(content);
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
