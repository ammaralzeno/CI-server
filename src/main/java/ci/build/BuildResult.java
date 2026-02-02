package ci.build;

/**
 * Result of a CI pipeline execution.
 */
public class BuildResult {

    /**
     * Build outcome.
     */
    public enum Status {
        SUCCESS, FAILURE, ERROR
    }

    public final Status status;
    public final String logs;

    /**
     * Creates a build result.
     */
    public BuildResult(Status status, String logs) {
        this.status = status;
        this.logs = logs;
    }
}
