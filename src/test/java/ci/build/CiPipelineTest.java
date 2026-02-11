package ci.build;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import ci.notify.notify;
import ci.storage.NoOpStore;
import ci.storage.Storage;

class CiPipelineTest {

    private static final CheckoutService CHECKOUT_OK = trigger -> Path.of("dummy-workspace");

    private static final notify NOOP_NOTIFY = (t, r) -> { };
    private static final Storage NOOP_STORE = new NoOpStore();

    /**
     * Verifies that a compile failure results in FAILURE
     * and that only the compile step is executed.
     */
    @Test
    void compileFailure_returnsFailure() {
        BuildExecutor executor = new BuildExecutor() {
            @Override public StepResult compile(Path ws) { return new StepResult("compile", false, "compile failed"); }
            @Override public StepResult test(Path ws) { return new StepResult("test", true, "ok"); }
        };

        CiPipeline pipeline = new CiPipeline(CHECKOUT_OK, executor, NOOP_NOTIFY, NOOP_STORE);
        BuildResult res = pipeline.run(new CiTrigger("repo", "assessment", "abc123"));

        assertEquals(BuildResult.Status.FAILURE, res.status);
        assertTrue(res.logs.contains("Compile failed"));
        assertEquals(1, res.steps.size());
        assertEquals("compile", res.steps.get(0).name);
        assertFalse(res.steps.get(0).success);
    }

    /**
     * Verifies that a test failure after a successful compile
     * results in FAILURE.
     */
    @Test
    void testFailure_returnsFailure() {
        BuildExecutor executor = new BuildExecutor() {
            @Override public StepResult compile(Path ws) { return new StepResult("compile", true, "ok"); }
            @Override public StepResult test(Path ws) { return new StepResult("test", false, "tests failed"); }
        };

        CiPipeline pipeline = new CiPipeline(CHECKOUT_OK, executor, NOOP_NOTIFY, NOOP_STORE);
        BuildResult res = pipeline.run(new CiTrigger("repo", "assessment", "abc123"));

        assertEquals(BuildResult.Status.FAILURE, res.status);
        assertTrue(res.logs.contains("Tests failed"));
        assertEquals(2, res.steps.size());
        assertEquals("test", res.steps.get(1).name);
        assertFalse(res.steps.get(1).success);
    }

    /**
     * Verifies that a successful compile and test
     * results in SUCCESS.
     */
    @Test
    void success_returnsSuccess() {
        BuildExecutor executor = new BuildExecutor() {
            @Override public StepResult compile(Path ws) { return new StepResult("compile", true, "ok"); }
            @Override public StepResult test(Path ws) { return new StepResult("test", true, "ok"); }
        };

        CiPipeline pipeline = new CiPipeline(CHECKOUT_OK, executor, NOOP_NOTIFY, NOOP_STORE);
        BuildResult res = pipeline.run(new CiTrigger("repo", "assessment", "abc123"));

        assertEquals(BuildResult.Status.SUCCESS, res.status);
        assertEquals(2, res.steps.size());
        assertTrue(res.steps.get(0).success);
        assertTrue(res.steps.get(1).success);
    }

    /**
     * Verifies that an exception during checkout
     * results in ERROR and no steps are executed.
     */
    @Test
    void exception_returnsError() {
        CheckoutService checkoutThrows = trigger -> { throw new RuntimeException("boom"); };

        BuildExecutor executor = new BuildExecutor() {
            @Override public StepResult compile(Path ws) { return new StepResult("compile", true, "ok"); }
            @Override public StepResult test(Path ws) { return new StepResult("test", true, "ok"); }
        };

        CiPipeline pipeline = new CiPipeline(checkoutThrows, executor, NOOP_NOTIFY, NOOP_STORE);
        BuildResult res = pipeline.run(new CiTrigger("repo", "assessment", "abc123"));

        assertEquals(BuildResult.Status.ERROR, res.status);
        assertTrue(res.logs.contains("boom"));
        assertEquals(0, res.steps.size());
    }
}
