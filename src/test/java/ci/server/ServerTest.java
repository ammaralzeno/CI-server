package ci.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Unit tests for WebhookServer and HealthCheckServlet.
 * Tests server startup, health check endpoint, and shutdown.
 */
public class ServerTest {
    
    private WebhookServer server;
    private static final int TEST_PORT = 8888;
    
    @BeforeEach
    public void setUp() {
        server = null;
    }
    
    @AfterEach
    public void tearDown() throws Exception {
        if (server != null && server.isRunning()) {
            server.stop();
        }
    }
    
    /**
     * Reads test JSON file from resources.
     */
    private String readTestFile(String filename) throws IOException {
        return Files.readString(Paths.get("src/test/resources/webhook-payloads/" + filename));
    }
    
    /**
     * Tests that the server can be created with default port.
     */
    @Test
    public void testServerCreationWithDefaultPort() {
        server = new WebhookServer();
        assertNotNull(server, "Server should be created");
        assertEquals(8080, server.getPort(), "Default port should be 8080");
    }
    
    /**
     * Tests that the server can be created with a custom port.
     */
    @Test
    public void testServerCreationWithCustomPort() {
        server = new WebhookServer(TEST_PORT);
        assertNotNull(server, "Server should be created");
        assertEquals(TEST_PORT, server.getPort(), "Port should match custom port");
    }
    
    /**
     * Tests that invalid port numbers throw IllegalArgumentException.
     */
    @Test
    public void testInvalidPortNumbers() {
        assertThrows(IllegalArgumentException.class, () -> {
            new WebhookServer(0);
        }, "Port 0 should throw IllegalArgumentException");
        
        assertThrows(IllegalArgumentException.class, () -> {
            new WebhookServer(-1);
        }, "Negative port should throw IllegalArgumentException");
        
        assertThrows(IllegalArgumentException.class, () -> {
            new WebhookServer(65536);
        }, "Port > 65535 should throw IllegalArgumentException");
    }
    
    /**
     * Tests that the server can start successfully.
     */
    @Test
    public void testServerStart() throws Exception {
        server = new WebhookServer(TEST_PORT);
        server.start();
        
        assertTrue(server.isRunning(), "Server should be running after start");
        
        Thread.sleep(100);
    }
    
    /**
     * Tests that the server can stop successfully.
     */
    @Test
    public void testServerStop() throws Exception {
        server = new WebhookServer(TEST_PORT);
        server.start();
        Thread.sleep(100);
        
        assertTrue(server.isRunning(), "Server should be running");
        
        server.stop();
        assertFalse(server.isRunning(), "Server should not be running after stop");
    }
    
    /**
     * Tests that the health check endpoint returns "CI server running".
     */
    @Test
    public void testHealthCheckEndpoint() throws Exception {
        server = new WebhookServer(TEST_PORT);
        server.start();
        
        Thread.sleep(200);
        
        URL url = new URL("http://localhost:" + TEST_PORT + "/");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        
        int responseCode = connection.getResponseCode();
        assertEquals(200, responseCode, "Health check should return 200 OK");
        
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(connection.getInputStream())
        );
        String response = reader.readLine();
        reader.close();
        connection.disconnect();
        
        assertEquals("CI server running", response, 
            "Health check should return 'CI server running'");
    }
    
    /**
     * Tests that GET request returns correct content type.
     */
    @Test
    public void testHealthCheckContentType() throws Exception {
        server = new WebhookServer(TEST_PORT);
        server.start();
        Thread.sleep(200);
        
        URL url = new URL("http://localhost:" + TEST_PORT + "/");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        
        String contentType = connection.getContentType();
        connection.disconnect();
        
        assertTrue(contentType.contains("text/plain"), 
            "Content type should be text/plain, got: " + contentType);
    }
    
    /**
     * Tests that isRunning returns false before server starts.
     */
    @Test
    public void testIsRunningBeforeStart() {
        server = new WebhookServer(TEST_PORT);
        assertFalse(server.isRunning(), 
            "Server should not be running before start() is called");
    }
    
    /**
     * Tests that POST request to /webhook with valid payload returns 200 OK.
     */
    @Test
    public void testWebhookEndpointValidPayload() throws Exception {
        server = new WebhookServer(TEST_PORT);
        server.start();
        Thread.sleep(200);
        
        String validPayload = readTestFile("valid-push.json");
        
        URL url = new URL("http://localhost:" + TEST_PORT + "/webhook");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("X-GitHub-Event", "push");
        connection.setDoOutput(true);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        
        connection.getOutputStream().write(validPayload.getBytes());
        connection.getOutputStream().flush();
        
        int responseCode = connection.getResponseCode();
        assertEquals(200, responseCode, "Valid webhook should return 200 OK");
        
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(connection.getInputStream())
        );
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        connection.disconnect();
        
        String responseBody = response.toString();
        assertTrue(responseBody.contains("received"), 
                  "Response should indicate webhook was received");
        assertTrue(responseBody.contains("a1b2c3d4e5f6789012345678901234567890abcd"), 
                  "Response should contain commit SHA");
    }
    
    /**
     * Tests that POST request with invalid JSON returns 400 Bad Request.
     */
    @Test
    public void testWebhookEndpointInvalidJson() throws Exception {
        server = new WebhookServer(TEST_PORT);
        server.start();
        Thread.sleep(200);
        
        String invalidPayload = readTestFile("malformed.json");
        
        URL url = new URL("http://localhost:" + TEST_PORT + "/webhook");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("X-GitHub-Event", "push");
        connection.setDoOutput(true);
        
        connection.getOutputStream().write(invalidPayload.getBytes());
        connection.getOutputStream().flush();
        
        int responseCode = connection.getResponseCode();
        assertEquals(400, responseCode, "Invalid JSON should return 400 Bad Request");
        
        connection.disconnect();
    }
    
    /**
     * Tests that POST request with missing required fields returns 400.
     */
    @Test
    public void testWebhookEndpointMissingFields() throws Exception {
        server = new WebhookServer(TEST_PORT);
        server.start();
        Thread.sleep(200);
        
        String incompletePayload = readTestFile("missing-ref.json");
        
        URL url = new URL("http://localhost:" + TEST_PORT + "/webhook");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("X-GitHub-Event", "push");
        connection.setDoOutput(true);
        
        connection.getOutputStream().write(incompletePayload.getBytes());
        connection.getOutputStream().flush();
        
        int responseCode = connection.getResponseCode();
        assertEquals(400, responseCode, 
                    "Payload with missing fields should return 400 Bad Request");
        
        connection.disconnect();
    }
    
    /**
     * Tests that POST request without X-GitHub-Event header returns 400.
     */
    @Test
    public void testWebhookEndpointMissingEventHeader() throws Exception {
        server = new WebhookServer(TEST_PORT);
        server.start();
        Thread.sleep(200);
        
        String validPayload = readTestFile("valid-push.json");
        
        URL url = new URL("http://localhost:" + TEST_PORT + "/webhook");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        
        connection.getOutputStream().write(validPayload.getBytes());
        connection.getOutputStream().flush();
        
        int responseCode = connection.getResponseCode();
        assertEquals(400, responseCode, 
                    "Request without X-GitHub-Event header should return 400 Bad Request");
        
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(connection.getErrorStream())
        );
        String response = reader.readLine();
        reader.close();
        connection.disconnect();
        
        assertTrue(response.contains("Missing X-GitHub-Event header"), 
                  "Response should indicate missing header");
    }
    
    /**
     * Tests that POST request with non-push event type returns 400.
     */
    @Test
    public void testWebhookEndpointNonPushEvent() throws Exception {
        server = new WebhookServer(TEST_PORT);
        server.start();
        Thread.sleep(200);
        
        String validPayload = readTestFile("valid-push.json");
        
        URL url = new URL("http://localhost:" + TEST_PORT + "/webhook");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("X-GitHub-Event", "pull_request");
        connection.setDoOutput(true);
        
        connection.getOutputStream().write(validPayload.getBytes());
        connection.getOutputStream().flush();
        
        int responseCode = connection.getResponseCode();
        assertEquals(400, responseCode, 
                    "Non-push event should return 400 Bad Request");
        
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(connection.getErrorStream())
        );
        String response = reader.readLine();
        reader.close();
        connection.disconnect();
        
        assertTrue(response.contains("push"), 
                  "Response should indicate only push events are supported");
    }
    
    /**
     * Tests that POST request with X-GitHub-Event: push succeeds.
     */
    @Test
    public void testWebhookEndpointWithValidEventHeader() throws Exception {
        server = new WebhookServer(TEST_PORT);
        server.start();
        Thread.sleep(200);
        
        String validPayload = readTestFile("valid-push.json");
        
        URL url = new URL("http://localhost:" + TEST_PORT + "/webhook");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("X-GitHub-Event", "push");
        connection.setDoOutput(true);
        
        connection.getOutputStream().write(validPayload.getBytes());
        connection.getOutputStream().flush();
        
        int responseCode = connection.getResponseCode();
        assertEquals(200, responseCode, 
                    "Valid push event should return 200 OK");
        
        connection.disconnect();
    }
    
    /**
     * Tests that GET request to /webhook returns 405 Method Not Allowed.
     */
    @Test
    public void testWebhookEndpointGetMethodNotAllowed() throws Exception {
        server = new WebhookServer(TEST_PORT);
        server.start();
        Thread.sleep(200);
        
        URL url = new URL("http://localhost:" + TEST_PORT + "/webhook");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        
        int responseCode = connection.getResponseCode();
        assertEquals(405, responseCode, 
                    "GET request should return 405 Method Not Allowed");
        
        String allowHeader = connection.getHeaderField("Allow");
        assertEquals("POST", allowHeader, 
                    "Allow header should indicate POST is the only accepted method");
        
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(connection.getErrorStream())
        );
        String response = reader.readLine();
        reader.close();
        connection.disconnect();
        
        assertTrue(response.contains("GET") && response.contains("not allowed"), 
                  "Response should indicate GET is not allowed");
    }
    
    
    /**
     * Tests that empty X-GitHub-Event header returns 400.
     */
    @Test
    public void testWebhookEndpointEmptyEventHeader() throws Exception {
        server = new WebhookServer(TEST_PORT);
        server.start();
        Thread.sleep(200);
        
        String validPayload = readTestFile("valid-push.json");
        
        URL url = new URL("http://localhost:" + TEST_PORT + "/webhook");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("X-GitHub-Event", "");
        connection.setDoOutput(true);
        
        connection.getOutputStream().write(validPayload.getBytes());
        connection.getOutputStream().flush();
        
        int responseCode = connection.getResponseCode();
        assertEquals(400, responseCode, 
                    "Empty X-GitHub-Event header should return 400 Bad Request");
        
        connection.disconnect();
    }
}
