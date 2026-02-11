package ci.notify;

import ci.build.BuildResult;
import ci.build.CiTrigger;

/**
 * Abstraction for reporting CI results.
 *
 * <p>The pipeline depends on this interface so that different notification mechanisms
 * (e.g. GitHub commit statuses, email) can be plugged in without changing pipeline logic.</p>
 */
public interface notify {
    /**
     * Notifies about the result of a CI run triggered by {@code trigger}.
     *
     * @param trigger information about what triggered the build (branch/commit/repo)
     * @param result CI build result
     */
    void notify(CiTrigger trigger, BuildResult result);
}