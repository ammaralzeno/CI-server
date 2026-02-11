package ci.server;

import ci.build.BuildResult;
import ci.build.StepResult;
import ci.storage.Storage;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles build history HTTP requests for the CI server.
 *
 * <p>This servlet provides two endpoints:
 * <ul>
 * <li>GET /builds - Returns a list of all builds with summary information (no logs or steps)
 * <li>GET /builds/{id} - Returns detailed information for a specific build (includes logs and steps)
 * </ul>
 *
 * <p>Response format is JSON. Errors return appropriate HTTP status codes:
 * <ul>
 * <li>200 OK - successful retrieval
 * <li>404 Not Found - build ID doesn't exist
 * <li>400 Bad Request - invalid build ID format
 * <li>405 Method Not Allowed - non-GET methods
 * <li>500 Internal Server Error - storage layer failures
 * </ul>
 *
 * @see Storage
 * @see BuildResult
 */
public class BuildHistoryServlet extends HttpServlet {

    private static final Logger logger = Logger.getLogger(BuildHistoryServlet.class.getName());
    private final Storage buildStorage;

    /**
     * Creates servlet with specified storage implementation.
     *
     * @param buildStorage storage layer for retrieving build results
     * @throws IllegalArgumentException if buildStorage is null
     */
    public BuildHistoryServlet(Storage buildStorage) {
        if (buildStorage == null) {
            throw new IllegalArgumentException("buildStorage cannot be null");
        }
        this.buildStorage = buildStorage;
    }

    /**
     * Handles GET requests for build history endpoints.
     *
     * <p>Routes requests to the appropriate handler based on URI path:
     * <ul>
     * <li>/builds or /builds/ → list all builds
     * <li>/builds/{id} → get specific build details
     * </ul>
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @throws IOException if writing response fails
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        logRequest(request);

        String path = request.getRequestURI();

        try {
            if (path.equals("/builds") || path.equals("/builds/")) {
                handleGetAllBuilds(response);
                return;
            }

            if (path.startsWith("/builds/")) {
                String buildId = path.substring("/builds/".length());

                if (buildId.isEmpty() || buildId.contains("/")) {
                    sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                            "Invalid build ID format", null);
                    return;
                }

                handleGetBuildById(response, buildId);
                return;
            }

            sendError(response, HttpServletResponse.SC_NOT_FOUND,
                    "Endpoint not found", null);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error processing request", e);
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Internal server error", e.getMessage());
        }
    }

    /**
     * Handles GET /builds - returns list of all builds.
     *
     * <p>Returns JSON with summary information for each build (no logs or steps).
     * Builds are ordered by date, newest first.
     *
     * @param response the HTTP response to write to
     * @throws IOException if writing response fails
     */
    private void handleGetAllBuilds(HttpServletResponse response) throws IOException {
        logger.info("Fetching all builds");

        List<BuildResult> builds = buildStorage.loadAll();

        JSONObject responseJson = new JSONObject();
        JSONArray buildsArray = new JSONArray();

        for (BuildResult build : builds) {
            buildsArray.put(buildToSummaryJson(build));
        }

        responseJson.put("builds", buildsArray);
        responseJson.put("count", builds.size());

        logger.info("Returning " + builds.size() + " builds");

        sendJsonResponse(response, HttpServletResponse.SC_OK, responseJson);
    }

    /**
     * Handles GET /builds/{id} - returns specific build details.
     *
     * <p>Returns complete build information including logs and individual step results.
     * If build ID is not found, returns 404.
     *
     * @param response the HTTP response to write to
     * @param buildId the unique build identifier
     * @throws IOException if writing response fails
     */
    private void handleGetBuildById(HttpServletResponse response, String buildId)
            throws IOException {

        logger.info("Fetching build: " + buildId);

        BuildResult build = buildStorage.load(buildId);

        if (build == null) {
            logger.warning("Build not found: " + buildId);
            JSONObject error = new JSONObject();
            error.put("error", "Build not found");
            error.put("buildId", buildId);
            sendJsonResponse(response, HttpServletResponse.SC_NOT_FOUND, error);
            return;
        }

        JSONObject buildJson = buildToDetailedJson(build);

        logger.info("Returning build: " + buildId);

        sendJsonResponse(response, HttpServletResponse.SC_OK, buildJson);
    }

    /**
     * Converts BuildResult to summary JSON (no logs or steps).
     *
     * <p>Used by the /builds endpoint to return lightweight build listings.
     *
     * @param build the build result to convert
     * @return JSON object with summary fields
     */
    private JSONObject buildToSummaryJson(BuildResult build) {
        JSONObject json = new JSONObject();
        json.put("id", build.buildId);
        json.put("date", build.date);
        json.put("status", build.status.toString());
        return json;
    }

    /**
     * Converts BuildResult to detailed JSON (includes logs and steps).
     *
     * <p>Used by the /builds/{id} endpoint to return complete build information.
     *
     * @param build the build result to convert
     * @return JSON object with all build details
     */
    private JSONObject buildToDetailedJson(BuildResult build) {
        JSONObject json = buildToSummaryJson(build);

        json.put("logs", build.logs);

        JSONArray stepsArray = new JSONArray();
        for (StepResult step : build.steps) {
            JSONObject stepJson = new JSONObject();
            stepJson.put("name", step.name);
            stepJson.put("success", step.success);
            stepJson.put("logs", step.logs);
            stepsArray.put(stepJson);
        }
        json.put("steps", stepsArray);

        return json;
    }

    /**
     * Sends JSON response with specified status code.
     *
     * @param response the HTTP response
     * @param statusCode HTTP status code
     * @param json JSON object to send
     * @throws IOException if writing fails
     */
    private void sendJsonResponse(HttpServletResponse response, int statusCode,
                                   JSONObject json) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json");
        response.getWriter().write(json.toString());
    }

    /**
     * Sends JSON error response.
     *
     * @param response the HTTP response
     * @param statusCode HTTP status code
     * @param message error message
     * @param details optional error details (can be null)
     * @throws IOException if writing fails
     */
    private void sendError(HttpServletResponse response, int statusCode,
                          String message, String details) throws IOException {
        JSONObject error = new JSONObject();
        error.put("error", message);
        if (details != null) {
            error.put("details", details);
        }
        sendJsonResponse(response, statusCode, error);
    }

    /**
     * Logs incoming request details.
     *
     * @param request the HTTP request
     */
    private void logRequest(HttpServletRequest request) {
        logger.info(String.format("Incoming GET request from %s to %s",
                                 request.getRemoteAddr(),
                                 request.getRequestURI()));
    }

    /**
     * Rejects POST requests with 405 Method Not Allowed.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @throws IOException if writing response fails
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        handleMethodNotAllowed(response, "POST");
    }

    /**
     * Rejects PUT requests with 405 Method Not Allowed.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @throws IOException if writing response fails
     */
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        handleMethodNotAllowed(response, "PUT");
    }

    /**
     * Rejects DELETE requests with 405 Method Not Allowed.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @throws IOException if writing response fails
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        handleMethodNotAllowed(response, "DELETE");
    }

    /**
     * Sends 405 Method Not Allowed response.
     *
     * @param response the HTTP response
     * @param method the HTTP method that was attempted
     * @throws IOException if writing response fails
     */
    private void handleMethodNotAllowed(HttpServletResponse response, String method)
            throws IOException {
        logger.warning("Method not allowed: " + method);
        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        response.setHeader("Allow", "GET");

        JSONObject error = new JSONObject();
        error.put("error", "Method " + method + " not allowed");
        error.put("allowedMethods", new JSONArray().put("GET"));

        response.setContentType("application/json");
        response.getWriter().write(error.toString());
    }
}
