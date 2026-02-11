package ci.storage;

import ci.build.BuildResult;
import ci.build.CiTrigger;

import java.util.List;

/**
 * Stores CI build results.
 */
public interface Storage {

    /**
     * Persist a CI build result.
     *
     * @param trigger the CI trigger containing repo, branch, and commit information
     * @param result the build result containing status, logs, and step results
     */
    void save(CiTrigger trigger, BuildResult result);

    /**
     * Load all stored builds, ordered by date (newest first).
     *
     * @return list of all builds, or empty list if none exist
     */
    List<BuildResult> loadAll();

    /**
     * Load a specific build by its unique ID.
     *
     * @param buildId the unique identifier for the build
     * @return the build result, or null if not found
     */
    BuildResult load(String buildId);
}
