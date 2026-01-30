package ci.server;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * Handles GitHub webhook POST requests at /webhook endpoint.
 * Receives webhook events and logs them.
 * 
 * <p>Registered by {@link WebhookServer}.
 */
public class WebhookServlet extends HttpServlet {
    
    /**
     * Receives GitHub webhook events.
     * Currently just logs the raw payload.
     * 
     * <p>Returns 200 OK for any POST request with a body.
     * No side effects beyond logging.
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
            response.getWriter().write("Error reading request body");
            return;
        }
        
        String json = jsonBuilder.toString();
        
        System.out.println("Received webhook payload (" + json.length() + " bytes)");
        
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/plain");
        response.getWriter().write("Webhook received");
    }
}
