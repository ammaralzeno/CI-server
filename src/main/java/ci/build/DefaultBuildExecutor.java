package ci.build;

import java.nio.file.Path;
import java.util.List;

/**
 * Implements build executor for compiling and testing with Maven.
 */
public class DefaultBuildExecutor implements BuildExecutor {

    private final ProcessRunner runner;

    /**
     * Single-argument constructor.
     * 
     * @param runner ProcessRunner which will be used to run commands
     */
    public DefaultBuildExecutor(ProcessRunner runner) {
        this.runner = runner;
    }

    /**
     * No-argument constructor.
     */
    public DefaultBuildExecutor() {
        this.runner = new DefaultProcessRunner();
    }

    /**
     * Calls runner with given workspace and command "mvn -B compile", 
     * returning a StepResult with logs and a Boolean representing success/failure.
     * 
     * @param workspace path to the directory where the command is to be run
     * @return name "compile", success/failure, compile logs
     */
    @Override
    public StepResult compile(Path workspace) {
        ProcessResult result = runner.run(workspace, List.of("mvn", "-B", "compile"));
        return new StepResult("compile", result.exitCode() == 0, result.logs());
    }

    @Override
    public StepResult test(Path workspace) {
        return null;
    }

}
