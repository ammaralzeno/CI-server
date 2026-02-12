package ci.build;

import ci.server.WebhookPayload;

/**
 * Data passed to the CI pipeline from a webhook event.
 */
public class CiTrigger {

    public final String repoUrl;
    public final String branch;
    public final String commitSha;
    public final WebhookPayload payload;

    /**
     * Creates a CI trigger.
     * 
     * @param repoUrl repository clone URL
     * @param branch branch name
     * @param commitSha commit SHA
     * @param payload original webhook payload (may be null in tests)
     */
    public CiTrigger(String repoUrl, String branch, String commitSha, WebhookPayload payload) {
        this.repoUrl = repoUrl;
        this.branch = branch;
        this.commitSha = commitSha;
        this.payload = payload;
    }
    
    /**
     * Returns the webhook payload.
     * Used by {@link ci.notify.GitHubStatusNotifier} to extract repository information.
     * 
     * @return webhook payload, or null if not available
     */
    public WebhookPayload getPayload() {
        return payload;
    }
}
