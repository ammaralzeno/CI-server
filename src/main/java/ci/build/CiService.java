package ci.build;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Submits CI pipeline executions asynchronously.
 */
public class CiService {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final CiPipeline pipeline;

    /**
     * Creates a CI service.
     */
    public CiService(CiPipeline pipeline) {
        this.pipeline = pipeline;
    }

    /**
     * Triggers a CI build for the given webhook data.
     */
    public void submit(CiTrigger trigger) {
        executor.submit(() -> {
            System.out.println("CI: starting pipeline for " + trigger.branch +
                    " @ " + trigger.commitSha);

            BuildResult result = pipeline.run(trigger);

            System.out.println("CI: finished pipeline with status " + result.status +
                    " for " + trigger.branch + " @ " + trigger.commitSha);

            System.out.println("CI: logs: " + result.logs);

            for(var step : result.steps) {
                System.out.println("Step: " + step.name);
                System.out.println("Logs: " + step.logs);
                System.out.println("Success: " + step.success + "\n");
            }
        });
    }
}
