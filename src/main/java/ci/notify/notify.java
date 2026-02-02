package ci.notify;

import ci.build.BuildResult;
import ci.build.CiTrigger;

/**
 * Sends notifications about CI results.
 *
 * TODO(Person D): Implement GitHub commit status or email notification.
 */
public interface notify {

    /**
     * Notify users about a CI build result.
     */
    void notify(CiTrigger trigger, BuildResult result);
}
