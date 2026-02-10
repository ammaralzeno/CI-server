package ci.server;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for BuildHistoryServlet.
 *
 * <p>Tests complete HTTP request/response flow using BuildStore.
 * Uses actual HTTP connections to verify servlet behavior.
 *
 * <p>Test coverage:
 * <ul>
 * <li>Build list returns valid JSON
 * <li>404 for missing builds
 * <li>400 for invalid build IDs
 * <li>405 for non-GET methods
 * <li>Content type verification
 * </ul>
 */
public class BuildHistoryServletTest {

    private WebhookServer server;
    private static final int TEST_PORT = 8889;

    /**
     * Sets up test server before each test.
     */
    @BeforeEach
    public void setUp() throws Exception {
        server = new WebhookServer(TEST_PORT);
        server.start();
        Thread.sleep(200); // Wait for server startup
    }

    /**
     * Tears down test server after each test.
     */
    @AfterEach
    public void tearDown() throws Exception {
        if (server != null && server.isRunning()) {
            server.stop();
        }
    }

    /**
     * Tests GET /builds returns valid JSON with builds array and count.
     */
    @Test
    public void testGetAllBuildsReturnsValidJson() throws Exception {
        URL url = new URL("http://localhost:" + TEST_PORT + "/builds");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        int responseCode = conn.getResponseCode();
        assertEquals(200, responseCode, "Should return 200 for builds list");

        String response = readResponse(conn);
        JSONObject json = new JSONObject(response);

        assertTrue(json.has("builds"), "Response should have builds array");
        assertTrue(json.has("count"), "Response should have count field");
        assertEquals(json.getJSONArray("builds").length(), json.getInt("count"),
                    "Count should match builds array length");

        conn.disconnect();
    }

    /**
     * Tests GET /builds/ (with trailing slash) works correctly.
     */
    @Test
    public void testGetAllBuildsWithTrailingSlash() throws Exception {
        URL url = new URL("http://localhost:" + TEST_PORT + "/builds/");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        int responseCode = conn.getResponseCode();
        assertEquals(200, responseCode, "Should return 200 for /builds/ with trailing slash");

        String response = readResponse(conn);
        JSONObject json = new JSONObject(response);

        assertTrue(json.has("builds"), "Response should have builds array");
        assertTrue(json.has("count"), "Response should have count field");

        conn.disconnect();
    }

    /**
     * Tests GET /builds/{id} returns 404 when build doesn't exist.
     */
    @Test
    public void testGetBuildByIdNotFound() throws Exception {
        URL url = new URL("http://localhost:" + TEST_PORT + "/builds/nonexistent");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        int responseCode = conn.getResponseCode();
        assertEquals(404, responseCode, "Should return 404 for nonexistent build");

        String response = readResponse(conn);
        JSONObject json = new JSONObject(response);

        assertTrue(json.has("error"), "Error response should have error field");
        assertEquals("Build not found", json.getString("error"));
        assertEquals("nonexistent", json.getString("buildId"));

        conn.disconnect();
    }

    /**
     * Tests GET /builds/{id} returns 400 for invalid build ID format.
     * Build IDs cannot contain slashes.
     */
    @Test
    public void testGetBuildByIdInvalidFormat() throws Exception {
        URL url = new URL("http://localhost:" + TEST_PORT + "/builds/invalid/id/format");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        int responseCode = conn.getResponseCode();
        assertEquals(400, responseCode, "Should return 400 for invalid build ID");

        String response = readResponse(conn);
        JSONObject json = new JSONObject(response);

        assertTrue(json.has("error"), "Error response should have error field");
        assertTrue(json.getString("error").contains("Invalid"),
                  "Error should indicate invalid format");

        conn.disconnect();
    }

    /**
     * Tests POST /builds returns 405 Method Not Allowed.
     */
    @Test
    public void testBuildEndpointPostMethodNotAllowed() throws Exception {
        URL url = new URL("http://localhost:" + TEST_PORT + "/builds");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);

        int responseCode = conn.getResponseCode();
        assertEquals(405, responseCode, "POST should return 405 Method Not Allowed");

        String allowHeader = conn.getHeaderField("Allow");
        assertEquals("GET", allowHeader, "Allow header should indicate GET is allowed");

        String response = readResponse(conn);
        JSONObject json = new JSONObject(response);

        assertTrue(json.has("error"), "Error response should have error field");
        assertTrue(json.getString("error").contains("POST"),
                  "Error should mention POST method");
        assertTrue(json.getString("error").contains("not allowed"),
                  "Error should indicate method not allowed");

        conn.disconnect();
    }

    /**
     * Tests PUT /builds returns 405 Method Not Allowed.
     */
    @Test
    public void testBuildEndpointPutMethodNotAllowed() throws Exception {
        URL url = new URL("http://localhost:" + TEST_PORT + "/builds");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("PUT");
        conn.setDoOutput(true);

        int responseCode = conn.getResponseCode();
        assertEquals(405, responseCode, "PUT should return 405 Method Not Allowed");

        String allowHeader = conn.getHeaderField("Allow");
        assertEquals("GET", allowHeader, "Allow header should indicate GET is allowed");

        conn.disconnect();
    }

    /**
     * Tests DELETE /builds returns 405 Method Not Allowed.
     */
    @Test
    public void testBuildEndpointDeleteMethodNotAllowed() throws Exception {
        URL url = new URL("http://localhost:" + TEST_PORT + "/builds");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("DELETE");

        int responseCode = conn.getResponseCode();
        assertEquals(405, responseCode, "DELETE should return 405 Method Not Allowed");

        String allowHeader = conn.getHeaderField("Allow");
        assertEquals("GET", allowHeader, "Allow header should indicate GET is allowed");

        conn.disconnect();
    }

    /**
     * Tests response has correct content type (application/json).
     */
    @Test
    public void testBuildEndpointContentType() throws Exception {
        URL url = new URL("http://localhost:" + TEST_PORT + "/builds");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        int responseCode = conn.getResponseCode();
        assertEquals(200, responseCode, "Should return 200 OK");

        String contentType = conn.getContentType();
        assertTrue(contentType.contains("application/json"),
                  "Content type should be application/json, got: " + contentType);

        conn.disconnect();
    }

    /**
     * Helper to read full response body from connection.
     * Handles both success (InputStream) and error (ErrorStream) responses.
     *
     * @param conn the HTTP connection
     * @return the complete response body as a string
     * @throws Exception if reading fails
     */
    private String readResponse(HttpURLConnection conn) throws Exception {
        BufferedReader reader;
        if (conn.getResponseCode() >= 400) {
            reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
        } else {
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        }

        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        return response.toString();
    }
}
