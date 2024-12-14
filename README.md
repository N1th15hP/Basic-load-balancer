# Basic-load-balancer
This project is a simple load balancer implemented in Java using plain sockets. It distributes incoming requests across multiple backend servers and includes health checks to ensure backend server availability. The project uses Log4j for logging.

Features

Round-robin Load Balancing: Distributes requests evenly across registered backend servers.

Health Checks: Periodically checks the health of backend servers and removes unhealthy servers from the pool.

Simple HTTP Response Handling: Processes HTTP requests and returns appropriate responses.

Logging with Log4j: Provides detailed logs for debugging and monitoring.

How to Run

Start Backend Servers: The project includes a PlainSocketServers class to simulate backend servers. Run this class to start servers on ports 8081, 8082, and 8083.

Start the Load Balancer: Run the Server class. By default, it listens on port 8080 and forwards requests to backend servers using round-robin.

Send Requests:

Use a browser, Postman, or curl to send requests to the load balancer.

Example:

curl http://localhost:8080

Health Check Endpoint: The load balancer periodically checks the /healthCheck endpoint of backend servers to ensure availability.
