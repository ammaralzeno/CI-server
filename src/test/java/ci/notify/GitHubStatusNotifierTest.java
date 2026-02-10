package ci.notify;

import ci.build.BuildResult;
import ci.notify.http.HttpResponseData;
import ci.notify.http.HttpSender;
import ci.server.WebhookPayload;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link GitHubStatusNotifier}.
 *
 * <p>These tests use fake {@link HttpSender} implementations (or {@link RecordingHttpSender})
 * to avoid real network calls. They verify that {@link GitHubStatusNotifier#notifyFromPayload(WebhookPayload, BuildResult)}
 * maps {@link BuildResult.Status} to the correct GitHub commit status state and produces the expected JSON payload.</p>
 */
public final class GitHubStatusNotifierTest {

    /**
     * Verifies that a successful CI result is mapped to GitHub status state {@code "success"}
     * and that the configured status context is included.
     */
    @Test
    void notifyFromPayload_mapsSuccessToGitHubSuccess() {
        AtomicReference<String> capturedBody = new AtomicReference<>();

        HttpSender fakeHttp = (url, headers, jsonBody) -> {
            capturedBody.set(jsonBody);
            return new HttpResponseData(201, "{}");
        };

        GitHubStatusClient client = new GitHubStatusClient(fakeHttp, "https://api.github.com", "TOKEN");
        GitHubStatusNotifier notifier = new GitHubStatusNotifier(client, "kth-ci/build", "");

        WebhookPayload payload = new WebhookPayload(
                "assessment",
                "abc123",
                "CI-server",
                "octocat/CI-server",
                "https://github.com/octocat/CI-server.git"
        );

        BuildResult res = new BuildResult(BuildResult.Status.SUCCESS, "ok", List.of());
        notifier.notifyFromPayload(payload, res);

        JSONObject body = new JSONObject(capturedBody.get());
        assertEquals("success", body.getString("state"));
        assertEquals("kth-ci/build", body.getString("context"));
    }

    /**
     * Verifies that a failing CI result is mapped to GitHub status state {@code "failure"}.
     */
    @Test
    void notifyFromPayload_mapsFailureToGitHubFailure() {
        AtomicReference<String> capturedBody = new AtomicReference<>();

        HttpSender fakeHttp = (url, headers, jsonBody) -> {
            capturedBody.set(jsonBody);
            return new HttpResponseData(201, "{}");
        };

        GitHubStatusClient client = new GitHubStatusClient(fakeHttp, "https://api.github.com", "TOKEN");
        GitHubStatusNotifier notifier = new GitHubStatusNotifier(client, "kth-ci/build", "");

        WebhookPayload payload = new WebhookPayload(
                "assessment",
                "deadbeef",
                "CI-server",
                "octocat/CI-server",
                "https://github.com/octocat/CI-server.git"
        );

        BuildResult res = new BuildResult(BuildResult.Status.FAILURE, "fail", List.of());
        notifier.notifyFromPayload(payload, res);

        JSONObject body = new JSONObject(capturedBody.get());
        assertEquals("failure", body.getString("state"));
    }

    /**
     * Verifies that an internal CI error is mapped to GitHub status state {@code "error"}.
     */
    @Test
    void notifyFromPayload_mapsErrorToGitHubError() {
        AtomicReference<String> capturedBody = new AtomicReference<>();

        HttpSender fakeHttp = (url, headers, jsonBody) -> {
            capturedBody.set(jsonBody);
            return new HttpResponseData(201, "{}");
        };

        GitHubStatusClient client = new GitHubStatusClient(fakeHttp, "https://api.github.com", "TOKEN");
        GitHubStatusNotifier notifier = new GitHubStatusNotifier(client, "kth-ci/build", "");

        WebhookPayload payload = new WebhookPayload(
                "assessment",
                "ffff",
                "CI-server",
                "octocat/CI-server",
                "https://github.com/octocat/CI-server.git"
        );

        BuildResult res = new BuildResult(BuildResult.Status.ERROR, "err", List.of());
        notifier.notifyFromPayload(payload, res);

        JSONObject body = new JSONObject(capturedBody.get());
        assertEquals("error", body.getString("state"));
    }

    /**
     * Verifies that notification failures (e.g., network errors) do not throw and therefore
     * do not fail the CI pipeline.
     */
    @Test
    void notifyFromPayload_doesNotThrowWhenHttpFails() {
        HttpSender throwing = (url, headers, jsonBody) -> {
            throw new java.io.IOException("network down");
        };

        GitHubStatusClient client = new GitHubStatusClient(throwing, "https://api.github.com", "TOKEN");
        GitHubStatusNotifier notifier = new GitHubStatusNotifier(client, "kth-ci/build", "");

        WebhookPayload payload = new WebhookPayload(
                "assessment",
                "abc123",
                "CI-server",
                "octocat/CI-server",
                "https://github.com/octocat/CI-server.git"
        );

        BuildResult res = new BuildResult(BuildResult.Status.SUCCESS, "ok", List.of());

        assertDoesNotThrow(() -> notifier.notifyFromPayload(payload, res));
    }

    /**
     * Verifies that the {@code target_url} field is included in the GitHub status payload
     * when a public build URL is configured.
     */
    @Test
    void notifyFromPayload_includesTargetUrlWhenProvided() {
        RecordingHttpSender rec = new RecordingHttpSender(201);
        GitHubStatusClient client = new GitHubStatusClient(rec, "https://api.github.com", "TOKEN");
        GitHubStatusNotifier notifier = new GitHubStatusNotifier(client, "kth-ci/build", "https://x.ngrok.io/builds/7");

        WebhookPayload payload = new WebhookPayload(
                "assessment",
                "abc123",
                "CI-server",
                "octocat/CI-server",
                "https://github.com/octocat/CI-server.git"
        );

        BuildResult res = new BuildResult(BuildResult.Status.SUCCESS, "ok", List.of());
        notifier.notifyFromPayload(payload, res);

        JSONObject body = new JSONObject(rec.body.get());
        assertEquals("https://x.ngrok.io/builds/7", body.getString("target_url"));
    }
}
