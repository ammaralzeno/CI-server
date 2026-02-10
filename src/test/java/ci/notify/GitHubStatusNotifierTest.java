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

public final class GitHubStatusNotifierTest {

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

        BuildResult res = new BuildResult(BuildResult.Status.SUCCESS, "ok", java.util.List.of());

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() ->
                notifier.notifyFromPayload(payload, res)
        );
    }

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

        BuildResult res = new BuildResult(BuildResult.Status.SUCCESS, "ok", java.util.List.of());
        notifier.notifyFromPayload(payload, res);

        org.json.JSONObject body = new org.json.JSONObject(rec.body.get());
        org.junit.jupiter.api.Assertions.assertEquals("https://x.ngrok.io/builds/7", body.getString("target_url"));
    }

}
