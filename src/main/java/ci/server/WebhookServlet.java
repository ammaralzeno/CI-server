package ci.server;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONException;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Handles GitHub webhook POST requests at /webhook endpoint.
 * Parses push events and extracts build information (branch, commit SHA, repo).
 * 
 * <p>Registered by {@link WebhookServer}. Will be integrated with the build pipeline
 * in the future.
 * 
 * <p>Only accepts POST requests with X-GitHub-Event: push header.
 */
public class WebhookServlet extends HttpServlet {
    
    private static final Logger logger = Logger.getLogger(WebhookServlet.class.getName());
    private static final String GITHUB_EVENT_HEADER = "X-GitHub-Event";
    private static final String PUSH_EVENT = "push";
    
    /**
     * Processes GitHub push webhook events.
     * Validates X-GitHub-Event header, JSON payload, and extracts branch, commit, 
     * and repository information.
     * Currently logs the payload (will trigger builds in the future).
     * 
     * <p>Returns:
     * <ul>
     * <li>200 OK - webhook received and parsed successfully
     * <li>400 Bad Request - invalid event type, JSON, or missing required fields
     * <li>500 Internal Server Error - unexpected processing error
     * </ul>
     * 
     * <p>No side effects beyond logging (build triggering will be added in the future).
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        logRequest(request, "POST");
        
        String eventType = request.getHeader(GITHUB_EVENT_HEADER);
        logger.info("Received event type: " + eventType);
        
        if (eventType == null || eventType.trim().isEmpty()) {
            logger.warning("Missing X-GitHub-Event header");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("text/plain");
            response.getWriter().write("Missing X-GitHub-Event header");
            return;
        }
        
        if (!PUSH_EVENT.equals(eventType)) {
            logger.info("Ignoring non-push event: " + eventType);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("text/plain");
            response.getWriter().write("Only 'push' events are supported, received: " + eventType);
            return;
        }
        
        StringBuilder jsonBuilder = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error reading request body", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("text/plain");
            response.getWriter().write("Error reading request body: " + e.getMessage());
            return;
        }
        
        String json = jsonBuilder.toString();
        logger.fine("Received payload length: " + json.length() + " bytes");
        
        try {
            WebhookPayload payload = WebhookPayload.fromJson(json);
            
            logger.info("Successfully parsed webhook: " + payload);
            logger.info("- Branch: " + payload.getBranchName());
            logger.info("- Commit: " + payload.getCommitSha());
            logger.info("- Repository: " + payload.getRepositoryFullName());
            
            // TODO: Call CiPipeline.triggerBuild(payload) - Person B task
            
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":\"received\",\"commit\":\"" + 
                                      payload.getCommitSha() + "\"}");
            
        } catch (JSONException e) {
            logger.log(Level.WARNING, "Invalid JSON in webhook payload", e);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("text/plain");
            response.getWriter().write("Invalid JSON: " + e.getMessage());
            
        } catch (IllegalArgumentException e) {
            logger.log(Level.WARNING, "Invalid webhook payload", e);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("text/plain");
            response.getWriter().write("Invalid payload: " + e.getMessage());
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Internal error processing webhook", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("text/plain");
            response.getWriter().write("Internal error: " + e.getMessage());
        }
    }
    
    /**
     * Rejects GET requests with 405 Method Not Allowed.
     * Webhook endpoint only accepts POST requests.
     * 
     * @return 405 Method Not Allowed with explanatory message
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        logRequest(request, "GET");
        handleMethodNotAllowed(response, "GET");
    }
    
    /**
     * Rejects PUT requests with 405 Method Not Allowed.
     * Webhook endpoint only accepts POST requests.
     * 
     * @return 405 Method Not Allowed with explanatory message
     */
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        logRequest(request, "PUT");
        handleMethodNotAllowed(response, "PUT");
    }
    
    /**
     * Rejects DELETE requests with 405 Method Not Allowed.
     * Webhook endpoint only accepts POST requests.
     * 
     * @return 405 Method Not Allowed with explanatory message
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        logRequest(request, "DELETE");
        handleMethodNotAllowed(response, "DELETE");
    }
    
    /**
     * Logs incoming request details including method, URI, and remote address.
     * Captures all requests for monitoring and debugging.
     * 
     * @param request the HTTP request
     * @param method the HTTP method (GET, POST, etc.)
     */
    private void logRequest(HttpServletRequest request, String method) {
        String remoteAddr = request.getRemoteAddr();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        
        String fullUri = uri + (queryString != null ? "?" + queryString : "");
        
        logger.info(String.format("Incoming %s request from %s to %s", 
                                 method, remoteAddr, fullUri));
    }
    
    /**
     * Sends 405 Method Not Allowed response for unsupported HTTP methods.
     * Sets Allow header to indicate only POST is accepted.
     * 
     * @param response the HTTP response
     * @param method the unsupported HTTP method that was attempted
     */
    private void handleMethodNotAllowed(HttpServletResponse response, String method) 
            throws IOException {
        logger.warning("Method not allowed: " + method);
        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        response.setHeader("Allow", "POST");
        response.setContentType("text/plain");
        response.getWriter().write("Method " + method + " not allowed. Only POST requests are accepted.");
    }
}
