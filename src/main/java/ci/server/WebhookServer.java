package ci.server;

import ci.storage.BuildStore;
import ci.storage.Storage;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;

/**
 * Receives GitHub webhook notifications via HTTP and triggers CI builds.
 * 
 * <p>Exposed endpoints:
 * <ul>
 * <li>GET / - health check
 * <li>POST /webhook - receives GitHub push events
 * </ul>
 * 
 * <p>Usage:
 * <pre>
 * WebhookServer server = new WebhookServer(8080);
 * server.start();  // blocks until server is ready
 * // ... server runs ...
 * server.stop();   // graceful shutdown
 * </pre>
 * 
 * <p>Thread-safety: This class is not thread-safe. Do not call start() or stop() 
 * from multiple threads concurrently.
 */
public class WebhookServer {
    
    private static final int DEFAULT_PORT = 8080;
    private final int port;
    private Server server;
    
    public WebhookServer() {
        this(DEFAULT_PORT);
    }
    
    /**
     * @param port must be in range 1-65535
     * @throws IllegalArgumentException if port is outside valid range
     */
    public WebhookServer(int port) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535");
        }
        this.port = port;
    }
    
    /**
     * Binds to the configured port and begins accepting HTTP connections.
     * Creates internal threads for request handling.
     * Must be called exactly once before stop().
     * 
     * @throws Exception if port is already in use or network error occurs
     */
    public void start() throws Exception {
        server = new Server();
        
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        server.addConnector(connector);
        
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);
        
        context.addServlet(new ServletHolder(new HealthCheckServlet()), "/");
        context.addServlet(new ServletHolder(new WebhookServlet()), "/webhook");

        Storage buildStorage = new BuildStore();
        context.addServlet(new ServletHolder(new BuildHistoryServlet(buildStorage)), "/builds/*");

        server.start();
        System.out.println("CI server started on port " + port);
        System.out.println("Health check: http://localhost:" + port + "/");
        System.out.println("Webhook endpoint: http://localhost:" + port + "/webhook");
        System.out.println("Build history: http://localhost:" + port + "/builds");
    }
    
    /**
     * Closes all connections and releases the port. Stops background threads.
     * Safe to call multiple times (subsequent calls are no-ops).
     * 
     * @throws Exception if shutdown encounters errors
     */
    public void stop() throws Exception {
        if (server != null) {
            server.stop();
            System.out.println("CI server stopped");
        }
    }
    
    public int getPort() {
        return port;
    }
    
    /**
     * @return true if start() has completed and stop() has not been called
     */
    public boolean isRunning() {
        return server != null && server.isRunning();
    }
    
    /**
     * Runs the server indefinitely (blocks until process is killed).
     * Port defaults to 8080 unless PORT environment variable is set.
     * 
     * @param args unused
     */
    public static void main(String[] args) {
        try {
            String portEnv = System.getenv("PORT");
            int port = (portEnv != null) ? Integer.parseInt(portEnv) : DEFAULT_PORT;
            
            WebhookServer server = new WebhookServer(port);
            server.start();
            
            server.server.join();
            
        } catch (Exception e) {
            System.err.println("Failed to start CI server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
