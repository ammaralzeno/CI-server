package ci.notify;

import ci.notify.http.HttpResponseData;
import ci.notify.http.HttpSender;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public final class GitHubStatusClientTest {

    @Test
    void createStatus_buildsCorrectUrlHeadersAndJson() throws Exception {
        AtomicReference<String> capturedUrl = new AtomicReference<>();
        AtomicReference<Map<String, String>> capturedHeaders = new AtomicReference<>();
        AtomicReference<String> capturedBody = new AtomicReference<>();

        HttpSender fake = (url, headers, jsonBody) -> {
            capturedUrl.set(url);
            capturedHeaders.set(headers);
            capturedBody.set(jsonBody);
            return new HttpResponseData(201, "{}");
        };

        GitHubStatusClient client = new GitHubStatusClient(fake, "https://api.github.com", "TOKEN123");

        int code = client.createStatus(
                "octocat/CI-server",
                "abc123",
                "success",
                "kth-ci/build",
                "CI passed",
                "https://example.ngrok.io/builds/1"
        );

        assertEquals(201, code);
        assertEquals("https://api.github.com/repos/octocat/CI-server/statuses/abc123", capturedUrl.get());

        Map<String, String> headers = capturedHeaders.get();
        assertEquals("Bearer TOKEN123", headers.get("Authorization"));
        assertEquals("application/vnd.github+json", headers.get("Accept"));
        assertNotNull(headers.get("User-Agent"));

        JSONObject body = new JSONObject(capturedBody.get());
        assertEquals("success", body.getString("state"));
        assertEquals("kth-ci/build", body.getString("context"));
        assertEquals("CI passed", body.getString("description"));
        assertEquals("https://example.ngrok.io/builds/1", body.getString("target_url"));
    }

    @Test
    void createStatus_truncatesDescriptionToMax140() throws Exception {
        AtomicReference<String> capturedBody = new AtomicReference<>();

        HttpSender fake = (url, headers, jsonBody) -> {
            capturedBody.set(jsonBody);
            return new HttpResponseData(201, "{}");
        };

        GitHubStatusClient client = new GitHubStatusClient(fake, "https://api.github.com", "TOKEN123");

        String longDesc = "x".repeat(300);

        client.createStatus(
                "octocat/CI-server",
                "abc123",
                "failure",
                "kth-ci/build",
                longDesc,
                null
        );

        JSONObject body = new JSONObject(capturedBody.get());
        assertTrue(body.getString("description").length() <= 140);
    }

    @Test
    void createStatus_setsAuthHeaderAndCorrectEndpoint() throws Exception {
        RecordingHttpSender rec = new RecordingHttpSender(201);
        GitHubStatusClient client = new GitHubStatusClient(rec, "https://api.github.com", "TOKEN123");

        client.createStatus("octocat/CI-server", "abc123", "success", "kth-ci/build", "ok", null);

        org.junit.jupiter.api.Assertions.assertEquals(
                "https://api.github.com/repos/octocat/CI-server/statuses/abc123",
                rec.url.get()
        );
        org.junit.jupiter.api.Assertions.assertEquals("Bearer TOKEN123", rec.headers.get().get("Authorization"));
        org.junit.jupiter.api.Assertions.assertEquals("application/vnd.github+json", rec.headers.get().get("Accept"));
    }

    @Test
    void createStatus_omitsOptionalFieldsWhenBlank() throws Exception {
        RecordingHttpSender rec = new RecordingHttpSender(201);
        GitHubStatusClient client = new GitHubStatusClient(rec, "https://api.github.com", "TOKEN123");

        client.createStatus("octocat/CI-server", "abc123", "success", "kth-ci/build", "  ", " ");

        org.json.JSONObject body = new org.json.JSONObject(rec.body.get());
        org.junit.jupiter.api.Assertions.assertEquals("success", body.getString("state"));
        org.junit.jupiter.api.Assertions.assertEquals("kth-ci/build", body.getString("context"));
        org.junit.jupiter.api.Assertions.assertFalse(body.has("description"));
        org.junit.jupiter.api.Assertions.assertFalse(body.has("target_url"));
    }

}
