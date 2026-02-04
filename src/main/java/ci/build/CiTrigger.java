package ci.build;

/**
 * Data passed to the CI pipeline from a webhook event.
 */
public class CiTrigger {

    public final String repoUrl;
    public final String branch;
    public final String commitSha;

    /**
     * Creates a CI trigger.
     */
    public CiTrigger(String repoUrl, String branch, String commitSha) {
        this.repoUrl = repoUrl;
        this.branch = branch;
        this.commitSha = commitSha;
    }
}
