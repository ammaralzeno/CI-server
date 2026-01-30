package ci.server;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONException;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * Handles GitHub webhook POST requests at /webhook endpoint.
 * Parses push events and extracts build information (branch, commit SHA, repo).
 * 
 * <p>Registered by {@link WebhookServer}. Will be integrated with the build pipeline
 * in the future.
 */
public class WebhookServlet extends HttpServlet {
    
    /**
     * Processes GitHub push webhook events.
     * Validates JSON payload and extracts branch, commit, and repository information.
     * Currently logs the payload (will trigger builds in the future).
     * 
     * <p>Returns:
     * <ul>
     * <li>200 OK - webhook received and parsed successfully
     * <li>400 Bad Request - invalid JSON or missing required fields
     * <li>500 Internal Server Error - unexpected processing error
     * </ul>
     * 
     * <p>No side effects beyond logging (build triggering will be added in the future).
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        StringBuilder jsonBuilder = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("text/plain");
            response.getWriter().write("Error reading request body: " + e.getMessage());
            return;
        }
        
        String json = jsonBuilder.toString();
        
        try {
            WebhookPayload payload = WebhookPayload.fromJson(json);
            
            System.out.println("Received webhook: " + payload);
            System.out.println("- Branch: " + payload.getBranchName());
            System.out.println("- Commit: " + payload.getCommitSha());
            System.out.println("- Repository: " + payload.getRepositoryFullName());
            
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":\"received\",\"commit\":\"" + 
                                      payload.getCommitSha() + "\"}");
            
        } catch (JSONException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("text/plain");
            response.getWriter().write("Invalid JSON: " + e.getMessage());
            
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("text/plain");
            response.getWriter().write("Invalid payload: " + e.getMessage());
            
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("text/plain");
            response.getWriter().write("Internal error: " + e.getMessage());
        }
    }
}
