package ci.notify;

import ci.notify.http.HttpResponseData;
import ci.notify.http.HttpSender;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Low-level GitHub REST client for creating commit statuses.
 *
 * <p>This class wraps the GitHub REST endpoint used to attach build results to a specific commit,
 * which then appears on commits and pull requests as a status check.</p>
 *
 * <p>Endpoint used:
 * <pre>
 * POST {apiBase}/repos/{owner}/{repo}/statuses/{sha}
 * </pre>
 *
 * <p>Authentication is performed via a token sent in the {@code Authorization: Bearer ...} header.</p>
 */
public final class GitHubStatusClient {

    private final HttpSender http;
    private final String apiBase;
    private final String token;

    /**
     * Creates a new GitHubStatusClient.
     *
     * @param http HTTP sender used to execute requests (injectable for unit testing)
     * @param apiBase GitHub API base URL (e.g. {@code https://api.github.com})
     * @param token GitHub token used for authentication
     * @throws NullPointerException if {@code http} is null
     * @throws IllegalArgumentException if {@code apiBase} or {@code token} is blank
     */
    public GitHubStatusClient(HttpSender http, String apiBase, String token) {
        this.http = Objects.requireNonNull(http, "http");
        this.apiBase = requireNonBlank(apiBase, "apiBase");
        this.token = requireNonBlank(token, "token");
    }

    /**
     * Creates a commit status on GitHub for the given repository and commit SHA.
     *
     * <p>Typical usage is to call this after running the CI pipeline, mapping the build result to
     * one of GitHub's supported status states: {@code pending}, {@code success}, {@code failure}, {@code error}.</p>
     *
     * @param repositoryFullName repository in {@code owner/repo} format
     * @param sha commit SHA to attach the status to
     * @param state GitHub status state: {@code pending}, {@code success}, {@code failure}, or {@code error}
     * @param context status context label shown on GitHub (e.g. {@code kth-ci/build})
     * @param description short human-readable text (optional; truncated to 140 chars)
     * @param targetUrl optional URL for "Details" (e.g. link to build logs/history)
     * @return HTTP status code returned by GitHub (typically 201 on success)
     * @throws IOException if the HTTP request fails at transport level
     * @throws InterruptedException if the calling thread is interrupted during the request
     * @throws IllegalArgumentException if required parameters are blank
     */
    public int createStatus(
            String repositoryFullName,
            String sha,
            String state,
            String context,
            String description,
            String targetUrl
    ) throws IOException, InterruptedException {

        String repo = requireNonBlank(repositoryFullName, "repositoryFullName");
        String commit = requireNonBlank(sha, "sha");
        String st = requireNonBlank(state, "state");
        String ctx = requireNonBlank(context, "context");

        String url = apiBase + "/repos/" + repo + "/statuses/" + commit;

        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/vnd.github+json");
        headers.put("Authorization", "Bearer " + token);
        headers.put("X-GitHub-Api-Version", "2022-11-28");
        headers.put("User-Agent", "kth-dd2480-ci");

        JSONObject body = new JSONObject();
        body.put("state", st);
        body.put("context", ctx);

        if (description != null && !description.isBlank()) {
            body.put("description", truncate(description.trim(), 140));
        }
        if (targetUrl != null && !targetUrl.isBlank()) {
            body.put("target_url", targetUrl.trim());
        }

        HttpResponseData resp = http.postJson(url, headers, body.toString());
        return resp.statusCode();
    }

    private static String truncate(String s, int maxLen) {
        if (s.length() <= maxLen) return s;
        return s.substring(0, Math.max(0, maxLen - 1)) + "…";
    }

    private static String requireNonBlank(String v, String field) {
        if (v == null || v.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must be non-blank");
        }
        return v.trim();
    }
}
