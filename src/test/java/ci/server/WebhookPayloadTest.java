package ci.server;

import org.junit.jupiter.api.Test;
import org.json.JSONException;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Unit tests for WebhookPayload parsing and validation.
 */
public class WebhookPayloadTest {
    
    /**
     * Reads test JSON file from resources.
     */
    private String readTestFile(String filename) throws IOException {
        return Files.readString(Paths.get("src/test/resources/webhook-payloads/" + filename));
    }
    
    /**
     * Tests parsing a valid GitHub push webhook payload.
     */
    @Test
    public void testParseValidPayload() throws IOException {
        String json = readTestFile("valid-push.json");
        
        WebhookPayload payload = WebhookPayload.fromJson(json);
        
        assertNotNull(payload, "Payload should not be null");
        assertEquals("assessment", payload.getBranchName(), "Branch name should be extracted from ref");
        assertEquals("a1b2c3d4e5f6789012345678901234567890abcd", payload.getCommitSha(), 
                    "Commit SHA should match");
        assertEquals("CI-server", payload.getRepositoryName(), "Repository name should match");
        assertEquals("username/CI-server", payload.getRepositoryFullName(), 
                    "Repository full name should match");
        assertEquals("https://github.com/username/CI-server.git", payload.getCloneUrl(), 
                    "Clone URL should match");
    }
    
    /**
     * Tests branch name extraction from refs/heads/ format.
     */
    @Test
    public void testBranchNameExtraction() {
        String json = "{\"ref\":\"refs/heads/main\"," +
                     "\"after\":\"abc123\"," +
                     "\"repository\":{\"name\":\"test\",\"full_name\":\"user/test\"," +
                     "\"clone_url\":\"https://github.com/user/test.git\"}}";
        
        WebhookPayload payload = WebhookPayload.fromJson(json);
        
        assertEquals("main", payload.getBranchName(), 
                    "Branch name should be extracted without refs/heads/ prefix");
    }
    
    /**
     * Tests handling of branch name without refs/heads/ prefix.
     */
    @Test
    public void testBranchNameWithoutPrefix() {
        String json = "{\"ref\":\"feature-branch\"," +
                     "\"after\":\"abc123\"," +
                     "\"repository\":{\"name\":\"test\",\"full_name\":\"user/test\"," +
                     "\"clone_url\":\"https://github.com/user/test.git\"}}";
        
        WebhookPayload payload = WebhookPayload.fromJson(json);
        
        assertEquals("feature-branch", payload.getBranchName(), 
                    "Branch name should remain unchanged if no prefix");
    }
    
    /**
     * Tests that null JSON throws IllegalArgumentException.
     */
    @Test
    public void testNullJson() {
        assertThrows(IllegalArgumentException.class, () -> {
            WebhookPayload.fromJson(null);
        }, "Null JSON should throw IllegalArgumentException");
    }
    
    /**
     * Tests that empty JSON throws IllegalArgumentException.
     */
    @Test
    public void testEmptyJson() {
        assertThrows(IllegalArgumentException.class, () -> {
            WebhookPayload.fromJson("");
        }, "Empty JSON should throw IllegalArgumentException");
        
        assertThrows(IllegalArgumentException.class, () -> {
            WebhookPayload.fromJson("   ");
        }, "Whitespace-only JSON should throw IllegalArgumentException");
    }
    
    /**
     * Tests that malformed JSON throws JSONException.
     */
    @Test
    public void testMalformedJson() throws IOException {
        String json = readTestFile("malformed.json");
        
        assertThrows(JSONException.class, () -> {
            WebhookPayload.fromJson(json);
        }, "Malformed JSON should throw JSONException");
    }
    
    /**
     * Tests that missing ref field throws IllegalArgumentException.
     */
    @Test
    public void testMissingRefField() throws IOException {
        String json = readTestFile("missing-ref.json");
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            WebhookPayload.fromJson(json);
        }, "Missing ref field should throw IllegalArgumentException");
        
        assertTrue(exception.getMessage().contains("ref"), 
                  "Exception message should mention missing ref field");
    }
    
    /**
     * Tests that missing after field throws IllegalArgumentException.
     */
    @Test
    public void testMissingAfterField() {
        String json = "{\"ref\":\"refs/heads/main\"," +
                     "\"repository\":{\"name\":\"test\",\"full_name\":\"user/test\"," +
                     "\"clone_url\":\"https://github.com/user/test.git\"}}";
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            WebhookPayload.fromJson(json);
        }, "Missing after field should throw IllegalArgumentException");
        
        assertTrue(exception.getMessage().contains("after"), 
                  "Exception message should mention missing after field");
    }
    
    /**
     * Tests that missing repository field throws IllegalArgumentException.
     */
    @Test
    public void testMissingRepositoryField() {
        String json = "{\"ref\":\"refs/heads/main\",\"after\":\"abc123\"}";
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            WebhookPayload.fromJson(json);
        }, "Missing repository field should throw IllegalArgumentException");
        
        assertTrue(exception.getMessage().contains("repository"), 
                  "Exception message should mention missing repository field");
    }
    
    /**
     * Tests that missing repository.name throws IllegalArgumentException.
     */
    @Test
    public void testMissingRepositoryName() {
        String json = "{\"ref\":\"refs/heads/main\"," +
                     "\"after\":\"abc123\"," +
                     "\"repository\":{\"full_name\":\"user/test\"," +
                     "\"clone_url\":\"https://github.com/user/test.git\"}}";
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            WebhookPayload.fromJson(json);
        }, "Missing repository.name should throw IllegalArgumentException");
        
        assertTrue(exception.getMessage().contains("repository.name"), 
                  "Exception message should mention missing repository.name field");
    }
    
    /**
     * Tests toString produces readable output.
     */
    @Test
    public void testToString() throws IOException {
        String json = readTestFile("valid-push.json");
        WebhookPayload payload = WebhookPayload.fromJson(json);
        
        String str = payload.toString();
        
        assertTrue(str.contains("assessment"), "toString should contain branch name");
        assertTrue(str.contains("a1b2c3d4e5f6789012345678901234567890abcd"), 
                  "toString should contain commit SHA");
        assertTrue(str.contains("username/CI-server"), 
                  "toString should contain repository full name");
    }
}
