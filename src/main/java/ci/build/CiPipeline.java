package ci.build;

/**
 * Runs the CI pipeline.
 */
public class CiPipeline {

    /**
     * Executes compilation and tests and returns the result.
     */
    public BuildResult run(CiTrigger trigger) {
        try {
            boolean compileOk = runCompile(trigger);
            if (!compileOk) {
                return new BuildResult(BuildResult.Status.FAILURE, "Compile failed");
            }

            boolean testOk = runTests(trigger);
            if (!testOk) {
                return new BuildResult(BuildResult.Status.FAILURE, "Tests failed");
            }

            return new BuildResult(BuildResult.Status.SUCCESS, "OK");
        } catch (Exception e) {
            return new BuildResult(BuildResult.Status.ERROR, e.toString());
        }
    }

    /**
     * Runs compilation step.
     */
    protected boolean runCompile(CiTrigger trigger) {
        return true;
    }

    /**
     * Runs test step.
     */
    protected boolean runTests(CiTrigger trigger) {
        return true;
    }
}
