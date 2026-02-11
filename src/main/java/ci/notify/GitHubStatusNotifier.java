package ci.notify;

import ci.build.BuildResult;
import ci.build.CiTrigger;
import ci.notify.http.JavaHttpSender;
import ci.server.WebhookPayload;

import java.io.IOException;
import java.util.Objects;

/**
 * {@link notify} implementation that reports CI results to GitHub using commit statuses (REST API).
 *
 * <p>This notifier posts a status to the commit SHA received in the webhook payload, so the result
 * appears on the commit and on pull requests. The status is posted using {@link GitHubStatusClient}.</p>
 *
 * <h2>Configuration</h2>
 * <p>The notifier can be created from environment variables via {@link #fromEnv()}:</p>
 * <ul>
 *   <li>{@code GITHUB_TOKEN} (required) - token used to authenticate to GitHub</li>
 *   <li>{@code GITHUB_API_BASE} (optional) - defaults to {@code https://api.github.com}</li>
 *   <li>{@code CI_STATUS_CONTEXT} (optional) - status label shown on GitHub; defaults to {@code kth-ci/build}</li>
 *   <li>{@code CI_PUBLIC_BUILD_URL} (optional) - link shown as "Details" on GitHub (e.g. ngrok URL)</li>
 * </ul>
 *
 * <h2>Error handling</h2>
 * <p>Failures to notify GitHub (network errors or non-2xx responses) are logged to {@code System.err}
 * and are intentionally not thrown further, so notification problems do not fail the CI pipeline.</p>
 */
public final class GitHubStatusNotifier implements notify {

    private final GitHubStatusClient client;
    private final String context;
    private final String publicBuildUrl;

    /**
     * Creates a notifier instance from environment variables.
     *
     * @return configured {@link GitHubStatusNotifier}
     * @throws IllegalStateException if {@code GITHUB_TOKEN} is missing or blank
     */
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

    /**
     * Creates a notifier with explicit dependencies (useful for tests).
     *
     * @param client GitHub REST client used to create commit statuses
     * @param context status context label shown on GitHub (e.g. {@code kth-ci/build})
     * @param publicBuildUrl optional URL attached as {@code target_url} ("Details" link) in GitHub UI
     * @throws NullPointerException if {@code client} is null
     * @throws IllegalArgumentException if {@code context} is blank
     */
    public GitHubStatusNotifier(GitHubStatusClient client, String context, String publicBuildUrl) {
        this.client = Objects.requireNonNull(client, "client");
        this.context = requireNonBlank(context, "context");
        this.publicBuildUrl = publicBuildUrl == null ? "" : publicBuildUrl.trim();
    }

    /**
     * Posts a commit status to GitHub for the commit referenced by the given trigger.
     *
     * <p>This method extracts a {@link WebhookPayload} from {@link CiTrigger} and delegates
     * to {@link #notifyFromPayload(WebhookPayload, BuildResult)}.</p>
     *
     * @param trigger CI trigger containing webhook information
     * @param result CI build result to report
     * @throws NullPointerException if {@code trigger} or {@code result} is null
     * @throws IllegalStateException if the trigger does not expose a {@link WebhookPayload}
     */
    @Override
    public void notify(CiTrigger trigger, BuildResult result) {
        Objects.requireNonNull(trigger, "trigger");
        Objects.requireNonNull(result, "result");

        WebhookPayload payload = extractPayload(trigger);
        notifyFromPayload(payload, result);
    }

    /**
     * Posts a commit status to GitHub using data directly from the webhook payload.
     *
     * <p>This method exists primarily to simplify unit testing, by avoiding any dependence
     * on the concrete {@link CiTrigger} implementation.</p>
     *
     * @param payload parsed webhook payload containing repository and commit SHA
     * @param result CI build result to report
     * @throws NullPointerException if {@code payload} or {@code result} is null
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

    /**
     * Attempts to extract a {@link WebhookPayload} from a {@link CiTrigger} by reflection.
     *
     * <p>Accepted trigger methods:
     * <ul>
     *   <li>{@code getPayload()}</li>
     *   <li>{@code payload()}</li>
     * </ul>
     *
     * @param trigger CI trigger
     * @return webhook payload
     * @throws IllegalStateException if no payload accessor method is available or if payload is null
     */
    private static WebhookPayload extractPayload(CiTrigger trigger) {
        try {
            Object o = trigger.getClass().getMethod("getPayload").invoke(trigger);
            if (o == null) {
                throw new IllegalStateException("CiTrigger.getPayload() returned null - webhook payload required for GitHub notification");
            }
            return (WebhookPayload) o;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception ignored) { }

        try {
            Object o = trigger.getClass().getMethod("payload").invoke(trigger);
            if (o == null) {
                throw new IllegalStateException("CiTrigger.payload() returned null - webhook payload required for GitHub notification");
            }
            return (WebhookPayload) o;
        } catch (IllegalStateException e) {
            throw e;
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
