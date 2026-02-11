package ci.notify;

import ci.build.BuildResult;
import ci.build.CiTrigger;

/**
 * No-op notifier.
 *
 * <p>Used as a fallback when notifications are disabled or not configured
 * (e.g., missing {@code GITHUB_TOKEN}).</p>
 */
public class NoOpNotifier implements notify {
    @Override
    public void notify(CiTrigger trigger, BuildResult result) {
        // intentionally empty
    }
}