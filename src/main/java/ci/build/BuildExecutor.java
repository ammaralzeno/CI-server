package ci.build;

import java.nio.file.Path;

/**
 * Executes compilation and tests in a given workspace.
 *
 * TODO(Person C): Implement by running mvn compile and mvn test and capturing logs.
 */
public interface BuildExecutor {
    StepResult compile(Path workspace);
    StepResult test(Path workspace);
}
