package ci.build;

import java.nio.file.Path;

/**
 * Temporary build executor stub.
 *
 * TODO(Person C): Replace with mvn compile/test execution.
 */
public class DummyBuildExecutor implements BuildExecutor {
    @Override
    public StepResult compile(Path workspace) {
        return new StepResult("compile", true, "compile stub OK");
    }

    @Override
    public StepResult test(Path workspace) {
        return new StepResult("test", true, "test stub OK");
    }
}
