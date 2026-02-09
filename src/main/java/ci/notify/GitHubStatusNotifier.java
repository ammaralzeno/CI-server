package ci.notify;

import ci.build.BuildResult;
import ci.build.CiTrigger;
import ci.notify.http.JavaHttpSender;
import ci.server.WebhookPayload;

import java.io.IOException;
import java.util.Objects;

/**
 * Sends CI notifications to GitHub as commit statuses (REST API).
 *
 * Env vars:
 * - GITHUB_TOKEN (required)
 * - GITHUB_API_BASE (optional, default "https://api.github.com")
 * - CI_STATUS_CONTEXT (optional, default "kth-ci/build")
 * - CI_PUBLIC_BUILD_URL (optional, used as target_url if provided)
 */
public final class GitHubStatusNotifier implements notify {

    private final GitHubStatusClient client;
    private final String context;
    private final String publicBuildUrl;

    public static GitHubStatusNotifier fromEnv() {
        String token = System.getenv("GITHUB_TOKEN");
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Missing env var GITHUB_TOKEN");
        }
        String apiBase = getenvOrDefault("GITHUB_API_BASE", "https://api.github.com");
        String ctx = getenvOrDefault("CI_STATUS_CONTEXT", "kth-ci/build");
        String target = getenvOrDefault("CI_PUBLIC_BUILD_URL", "");

        return new GitHubStatusNotifier(
                new GitHubStatusClient(new JavaHttpSender(), apiBase, token),
                ctx,
                target
        );
    }

    public GitHubStatusNotifier(GitHubStatusClient client, String context, String publicBuildUrl) {
        this.client = Objects.requireNonNull(client, "client");
        this.context = requireNonBlank(context, "context");
        this.publicBuildUrl = publicBuildUrl == null ? "" : publicBuildUrl.trim();
    }

    @Override
    public void notify(CiTrigger trigger, BuildResult result) {
        Objects.requireNonNull(trigger, "trigger");
        Objects.requireNonNull(result, "result");

        // This assumes CiTrigger can provide WebhookPayload (very likely in your pipeline).
        WebhookPayload payload = extractPayload(trigger);
        notifyFromPayload(payload, result);
    }

    /**
     * Convenience method that avoids needing a CiTrigger in unit tests.
     */
    public void notifyFromPayload(WebhookPayload payload, BuildResult result) {
        Objects.requireNonNull(payload, "payload");
        Objects.requireNonNull(result, "result");

        String repoFullName = payload.getRepositoryFullName();
        String sha = payload.getCommitSha();

        String state = mapState(result.status);
        String description = mapDescription(result.status);

        try {
            int code = client.createStatus(repoFullName, sha, state, context, description, publicBuildUrl);

            if (code < 200 || code >= 300) {
                System.err.println("[notify] GitHub status rejected: HTTP " + code
                        + " repo=" + repoFullName + " sha=" + sha);
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("[notify] GitHub status failed: " + e.getMessage()
                    + " repo=" + repoFullName + " sha=" + sha);

        }
    }

    private static String mapState(BuildResult.Status status) {
        if (status == null) return "error";
        switch (status) {
            case SUCCESS: return "success";
            case FAILURE: return "failure";
            case ERROR:
            default: return "error";
        }
    }

    private static String mapDescription(BuildResult.Status status) {
        if (status == null) return "CI error";
        switch (status) {
            case SUCCESS: return "CI passed";
            case FAILURE: return "CI failed";
            case ERROR:
            default: return "CI error";
        }
    }

    private static WebhookPayload extractPayload(CiTrigger trigger) {
        // Try common getter names via reflection so we don't have to edit CiTrigger.
        try {
            Object o = trigger.getClass().getMethod("getPayload").invoke(trigger);
            return (WebhookPayload) o;
        } catch (Exception ignored) { }

        try {
            Object o = trigger.getClass().getMethod("payload").invoke(trigger);
            return (WebhookPayload) o;
        } catch (Exception ignored) { }

        throw new IllegalStateException("CiTrigger does not expose WebhookPayload (expected getPayload() or payload())");
    }

    private static String getenvOrDefault(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? def : v;
    }

    private static String requireNonBlank(String v, String field) {
        if (v == null || v.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must be non-blank");
        }
        return v.trim();
    }
}
