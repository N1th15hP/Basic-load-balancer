package com.codingchallenges.loadbalancer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class TestServer {
    public static void main(String[] args) throws IOException {
        // Start the first server on port 8081
        startServer(8081, "<html><body><h1>Hello from Server 1!</h1></body></html>");

        // Start the second server on port 8082
        startServer(8082, "<html><body><h1>Hello from Server 2!</h1></body></html>");
    }

    private static void startServer(int port, String htmlContent) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
            	System.out.println("Recieved request from " + exchange.getRemoteAddress());
                // Set the response headers to indicate HTML content
                exchange.getResponseHeaders().set("Content-Type", "text/html");

                // Send the response headers (200 OK) and the length of the HTML content
                exchange.sendResponseHeaders(200, htmlContent.getBytes().length);


                // Write the HTML content to the response body
                long startTime = System.currentTimeMillis();
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(htmlContent.getBytes());
                    os.flush();
                }
                
                exchange.close();
                long endTime = System.currentTimeMillis();
                System.out.println("Writing response took: " + (endTime - startTime) + " ms");
            }
        });

        server.setExecutor(Executors.newFixedThreadPool(10)); // Default executor
        System.out.println("Server started on port " + port);
        server.start();
    }
}
