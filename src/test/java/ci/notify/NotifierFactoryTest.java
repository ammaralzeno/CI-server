package ci.notify;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link NotifierFactory}.
 *
 * <p>Verifies that the factory selects the correct {@link notify} implementation
 * based on presence/absence of the {@code GITHUB_TOKEN} environment variable.</p>
 */
public final class NotifierFactoryTest {

    /**
     * If no token is provided, the factory must fall back to {@link NoOpNotifier}
     * so the CI pipeline can run without external notifications.
     */
    @Test
    void create_returnsNoOpNotifierWhenTokenMissing() {
        Map<String, String> env = new HashMap<>();
        NotifierFactory f = new NotifierFactory(env);

        notify n = f.create();
        assertTrue(n instanceof NoOpNotifier);
    }

    /**
     * If a token is provided, the factory should create a {@link GitHubStatusNotifier}
     * to post commit statuses to GitHub via the REST API.
     */
    @Test
    void create_returnsGitHubNotifierWhenTokenPresent() {
        Map<String, String> env = new HashMap<>();
        env.put("GITHUB_TOKEN", "TOKEN");
        NotifierFactory f = new NotifierFactory(env);

        notify n = f.create();
        assertTrue(n instanceof GitHubStatusNotifier);
    }
}
