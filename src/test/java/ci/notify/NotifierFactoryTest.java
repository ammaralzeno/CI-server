package ci.notify;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public final class NotifierFactoryTest {

    @Test
    void create_returnsNoOpNotifierWhenTokenMissing() {
        Map<String, String> env = new HashMap<>();
        NotifierFactory f = new NotifierFactory(env);

        notify n = f.create();
        assertTrue(n instanceof NoOpNotifier);
    }

    @Test
    void create_returnsGitHubNotifierWhenTokenPresent() {
        Map<String, String> env = new HashMap<>();
        env.put("GITHUB_TOKEN", "TOKEN");
        NotifierFactory f = new NotifierFactory(env);

        notify n = f.create();
        assertTrue(n instanceof GitHubStatusNotifier);
    }
}
