package ci.build;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for BuildExecutor.
 * Tests logic for test and compile methods.
 */

public class BuildExecutorTest {

    /**
     * Tests compile method for process returning 0.
     * @result The compile method returns a result where success is true and logs are the same as the dummy logs specified in the process result.
     */
    @Test
    void compileReturnsSuccessWhenExitCodeIsZero() {
        String dummyLogs = "fake compile logs";
        ProcessRunner fakeRunner = (dir, cmd) -> new ProcessResult(0, dummyLogs);
        BuildExecutor exec = new DefaultBuildExecutor(fakeRunner);
        StepResult result = exec.compile(Path.of("/tmp"));
        assertTrue(result.success);
        assertEquals(dummyLogs, result.logs);
    }

    /**
     * Tests compile method for process returning 1.
     * @result The compile method returns a result where success is false and logs are the same as the dummy logs specified in the process result.
     */
    @Test
    void compileReturnsFailureWhenExitCodeIsNonZero() {
        String dummyLogs = "fake compile logs";
        ProcessRunner fakeRunner = (dir, cmd) -> new ProcessResult(1, dummyLogs);
        BuildExecutor exec = new DefaultBuildExecutor(fakeRunner);
        StepResult result = exec.compile(Path.of("/tmp"));
        assertFalse(result.success);
        assertEquals(dummyLogs, result.logs);
    }

}
