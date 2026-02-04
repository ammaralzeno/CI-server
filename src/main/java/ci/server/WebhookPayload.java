package ci.server;

import org.json.JSONObject;
import org.json.JSONException;

/**
 * Represents parsed data from a GitHub push webhook event.
 * Contains branch name, commit SHA, and repository information needed for the CI builds.
 * 
 * <p>Usage:
 * <pre>
 * String json = "{...}";  // GitHub webhook JSON
 * WebhookPayload payload = WebhookPayload.fromJson(json);
 * String branch = payload.getBranchName();
 * </pre>
 * 
 * <p>Immutable after construction.
 */
public class WebhookPayload {
    
    private final String branchName;
    private final String commitSha;
    private final String repositoryName;
    private final String repositoryFullName;
    private final String cloneUrl;
    
    /**
     * @param branchName branch being pushed (e.g., "main", "assessment")
     * @param commitSha full 40-character commit SHA
     * @param repositoryName repo name only (e.g., "CI-server")
     * @param repositoryFullName owner/repo format (e.g., "username/CI-server")
     * @param cloneUrl HTTPS clone URL for git operations
     */
    public WebhookPayload(String branchName, String commitSha, String repositoryName, 
                          String repositoryFullName, String cloneUrl) {
        this.branchName = branchName;
        this.commitSha = commitSha;
        this.repositoryName = repositoryName;
        this.repositoryFullName = repositoryFullName;
        this.cloneUrl = cloneUrl;
    }
    
    /**
     * Parses GitHub push event JSON into a WebhookPayload.
     * Extracts branch from "ref" field (refs/heads/branch-name).
     * 
     * @param json GitHub webhook JSON payload
     * @return parsed payload object
     * @throws JSONException if JSON is malformed or missing required fields
     * @throws IllegalArgumentException if required fields are null or empty
     */
    public static WebhookPayload fromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            throw new IllegalArgumentException("JSON payload cannot be null or empty");
        }
        
        JSONObject obj = new JSONObject(json);
        
        String ref = obj.optString("ref", null);
        String after = obj.optString("after", null);
        JSONObject repository = obj.optJSONObject("repository");
        
        if (ref == null || ref.isEmpty()) {
            throw new IllegalArgumentException("Missing required field: ref");
        }
        if (after == null || after.isEmpty()) {
            throw new IllegalArgumentException("Missing required field: after");
        }
        if (repository == null) {
            throw new IllegalArgumentException("Missing required field: repository");
        }
        
        String branchName = ref;
        if (ref.startsWith("refs/heads/")) {
            branchName = ref.substring("refs/heads/".length());
        }
        
        String repoName = repository.optString("name", null);
        String repoFullName = repository.optString("full_name", null);
        String cloneUrl = repository.optString("clone_url", null);
        
        if (repoName == null || repoName.isEmpty()) {
            throw new IllegalArgumentException("Missing required field: repository.name");
        }
        if (repoFullName == null || repoFullName.isEmpty()) {
            throw new IllegalArgumentException("Missing required field: repository.full_name");
        }
        if (cloneUrl == null || cloneUrl.isEmpty()) {
            throw new IllegalArgumentException("Missing required field: repository.clone_url");
        }
        
        return new WebhookPayload(branchName, after, repoName, repoFullName, cloneUrl);
    }
    
    public String getBranchName() {
        return branchName;
    }
    
    public String getCommitSha() {
        return commitSha;
    }
    
    public String getRepositoryName() {
        return repositoryName;
    }
    
    public String getRepositoryFullName() {
        return repositoryFullName;
    }
    
    public String getCloneUrl() {
        return cloneUrl;
    }
    
    @Override
    public String toString() {
        return "WebhookPayload{" +
                "branch='" + branchName + '\'' +
                ", commit='" + commitSha + '\'' +
                ", repo='" + repositoryFullName + '\'' +
                '}';
    }
}
