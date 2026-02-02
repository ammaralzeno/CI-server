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
     * Submits a CI trigger for execution.
     */
    public void submit(CiTrigger trigger) {
        executor.submit(() -> pipeline.run(trigger));
    }
}
