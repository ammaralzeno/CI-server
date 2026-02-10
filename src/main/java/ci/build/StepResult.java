package ci.build;

/**
 * Result of a single pipeline step (e.g., checkout, compile, test).
 */
public class StepResult {
    public final String name;
    public final boolean success;
    public final String logs;

    public StepResult(String name, boolean success, String logs) {
        this.name = name;
        this.success = success;
        this.logs = logs;
    }
}
