package ci.storage;

import ci.build.BuildResult;
import ci.build.CiTrigger;

import java.util.ArrayList;
import java.util.List;

/**
 * No-op implementation of build storage.
 *
 * <p>This implementation does not persist any build data. It is used as a placeholder
 * for tests that don't need real storage.
 */
public class NoOpStore implements Storage {

    @Override
    public void save(CiTrigger trigger, BuildResult result) {
        // do nothing
    }

    public List<BuildResult> loadAll() {
        return new ArrayList<>();
    }

    public BuildResult load(String buildId) {
        return null;
    }
}
