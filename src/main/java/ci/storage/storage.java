package ci.storage;

import ci.build.BuildResult;
import ci.build.CiTrigger;

/**
 * Stores CI build results.
 *
 * TODO(Person E): Implement persistent build history storage.
 */
public interface storage {

    /**
     * Persist a CI build result.
     */
    void save(CiTrigger trigger, BuildResult result);
}
