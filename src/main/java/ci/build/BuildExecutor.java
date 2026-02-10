package ci.build;

import java.nio.file.Path;

/**
 * Abstract class for build executor.
 * Executes compilation and tests in a given workspace.
 */
public interface BuildExecutor {
    StepResult compile(Path workspace);
    StepResult test(Path workspace);
}
