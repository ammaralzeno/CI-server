package ci.notify;

import ci.notify.http.HttpResponseData;
import ci.notify.http.HttpSender;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Low-level GitHub REST client for commit statuses.
 *
 * Endpoint:
 * POST {apiBase}/repos/{owner}/{repo}/statuses/{sha}
 *
 * Docs: "Create a commit status" (GitHub REST API).
 */
public final class GitHubStatusClient {

    private final HttpSender http;
    private final String apiBase;
    private final String token;

    public GitHubStatusClient(HttpSender http, String apiBase, String token) {
        this.http = Objects.requireNonNull(http, "http");
        this.apiBase = requireNonBlank(apiBase, "apiBase");
        this.token = requireNonBlank(token, "token");
    }

    /**
     * Creates a commit status on GitHub.
     *
     * @param repositoryFullName "owner/repo"
     * @param sha commit SHA
     * @param state "pending", "success", "failure", or "error"
     * @param context status context (e.g. "kth-ci/build")
     * @param description short text (optional)
     * @param targetUrl URL to logs/build page (optional)
     * @return HTTP status code returned by GitHub
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
