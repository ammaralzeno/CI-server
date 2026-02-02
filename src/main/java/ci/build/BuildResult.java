package ci.build;

import java.util.List;

/**
 * Result of a CI pipeline execution.
 */
public class BuildResult {

    public enum Status { SUCCESS, FAILURE, ERROR }

    public final Status status;
    public final String logs;
    public final List<StepResult> steps;

    public BuildResult(Status status, String logs, List<StepResult> steps) {
        this.status = status;
        this.logs = logs;
        this.steps = steps;
    }
}
