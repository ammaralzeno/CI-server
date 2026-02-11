package ci.notify;

import ci.notify.http.JavaHttpSender;

import java.util.Map;

/**
 * Creates a {@link notify} implementation based on environment configuration.
 */
public final class NotifierFactory {

    private final Map<String, String> env;

    /**
     * Uses the real process environment.
     */
    public NotifierFactory() {
        this(System.getenv());
    }

    /**
     * Injectable environment map (for unit tests).
     */
    public NotifierFactory(Map<String, String> env) {
        this.env = env;
    }

    /**
     * Creates a notifier. If GITHUB_TOKEN is missing, returns {@link NoOpNotifier}.
     */
    public notify create() {
        String token = env.get("GITHUB_TOKEN");
        if (token == null || token.isBlank()) {
            return new NoOpNotifier();
        }

        String apiBase = env.getOrDefault("GITHUB_API_BASE", "https://api.github.com").trim();
        String context = env.getOrDefault("CI_STATUS_CONTEXT", "kth-ci/build").trim();
        String targetUrl = env.getOrDefault("CI_PUBLIC_BUILD_URL", "").trim();

        GitHubStatusClient client = new GitHubStatusClient(new JavaHttpSender(), apiBase, token.trim());
        return new GitHubStatusNotifier(client, context, targetUrl);
    }
}
