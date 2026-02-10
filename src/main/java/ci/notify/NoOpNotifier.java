package ci.notify;

import ci.build.BuildResult;
import ci.build.CiTrigger;

/**
 * No-op notifier stub.
 *
 * TODO(Person D): Replace with real notifier.
 */
public class NoOpNotifier implements notify {
    @Override
    public void notify(CiTrigger trigger, BuildResult result) {
        // do nothing
    }
}
