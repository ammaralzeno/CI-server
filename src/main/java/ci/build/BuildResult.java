package ci.build;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Result of a CI pipeline execution.
 */
public class BuildResult {

    public enum Status { SUCCESS, FAILURE, ERROR }

    public final String buildId;
    public final String date;
    public final Status status;
    public final String logs;
    public final List<StepResult> steps;

    /**
     * Constructor.
     * Automatically generates buildId and sets build date.
     * @param status success, failure or error
     * @param logs console output
     * @param steps results of build steps
     */
    public BuildResult(Status status, String logs, List<StepResult> steps) {
        this.buildId = UUID.randomUUID().toString();
        this.date = LocalDate.now().toString();
        this.status = status;
        this.logs = logs;
        this.steps = steps;
    }

    /**
     * Constructor for loading builds from persistence.
     */
    public BuildResult(String buildId, String date, Status status, String logs, List<StepResult> steps) {
        this.buildId = buildId;
        this.date = date;
        this.status = status;
        this.logs = logs;
        this.steps = steps;
    }


    /**
     * Checks for successful build.
     * @return true only if status is SUCCESS
     */
    public boolean isSuccessful() {
        return this.status == Status.SUCCESS;
    }
}
