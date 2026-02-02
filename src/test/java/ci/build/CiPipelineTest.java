package ci.build;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CiPipeline}.
 * Verifies SUCCESS/FAILURE/ERROR outcomes.
 */
class CiPipelineTest {

    /** Compile fails -> pipeline returns FAILURE. */
    @Test
    void compileFailure_returnsFailure() {
        CiPipeline pipeline = new CiPipeline() {
            @Override protected boolean runCompile(CiTrigger t) { return false; }
        };

        BuildResult res = pipeline.run(new CiTrigger("repo", "assessment", "abc123"));

        assertEquals(BuildResult.Status.FAILURE, res.status);
        assertTrue(res.logs.contains("Compile failed"));
    }

    /** Tests fail -> pipeline returns FAILURE. */
    @Test
    void testFailure_returnsFailure() {
        CiPipeline pipeline = new CiPipeline() {
            @Override protected boolean runCompile(CiTrigger t) { return true; }
            @Override protected boolean runTests(CiTrigger t) { return false; }
        };

        BuildResult res = pipeline.run(new CiTrigger("repo", "assessment", "abc123"));

        assertEquals(BuildResult.Status.FAILURE, res.status);
        assertTrue(res.logs.contains("Tests failed"));
    }

    /** Compile + tests succeed -> pipeline returns SUCCESS. */
    @Test
    void success_returnsSuccess() {
        CiPipeline pipeline = new CiPipeline();
        BuildResult res = pipeline.run(new CiTrigger("repo", "assessment", "abc123"));

        assertEquals(BuildResult.Status.SUCCESS, res.status);
    }

     /** Exception during execution -> pipeline returns ERROR. */
    @Test
    void exception_returnsError() {
        CiPipeline pipeline = new CiPipeline() {
            @Override protected boolean runCompile(CiTrigger t) { throw new RuntimeException("boom"); }
        };

        BuildResult res = pipeline.run(new CiTrigger("repo", "assessment", "abc123"));

        assertEquals(BuildResult.Status.ERROR, res.status);
        assertTrue(res.logs.contains("boom"));
    }
}
