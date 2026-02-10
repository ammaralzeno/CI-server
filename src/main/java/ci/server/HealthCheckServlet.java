package ci.server;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Returns "CI server running" for GET / requests.
 * Instantiated and registered by {@link WebhookServer}.
 */
public class HealthCheckServlet extends HttpServlet {
    
    private static final String HEALTH_CHECK_MESSAGE = "CI server running";
    
    /**
     * Always returns 200 OK with "CI server running" as text/plain.
     * No side effects.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/plain");
        response.getWriter().write(HEALTH_CHECK_MESSAGE);
    }
}
