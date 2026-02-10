package ci.storage;

import ci.build.BuildResult;
import ci.build.CiTrigger;

/**
 * No-op implementation of build storage.
 *
 * TODO(Person E): Replace with real build history persistence.
 */
public class NoOpStore implements storage {

    @Override
    public void save(CiTrigger trigger, BuildResult result) {
        // do nothing
    }
}
