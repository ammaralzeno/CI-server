package ci.build;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import ci.notify.notify;
import ci.storage.storage;
import ci.notify.NotifierFactory;

/**
 * Orchestrates the CI pipeline.
 */
public class CiPipeline {

    /**
     * Default constructor used by the servlet wiring.
     * Uses temporary stub implementations until real ones are provided.
     */
    public CiPipeline() {
      this(
          new DefaultCheckoutService(),
          new DefaultBuildExecutor(),
          new NotifierFactory().create(),
          new ci.storage.BuildStore()
      );
    }

    private final CheckoutService checkout;
    private final BuildExecutor executor;
    private final notify notifier;
    private final storage store;

    public CiPipeline(CheckoutService checkout,
                      BuildExecutor executor,
                      notify notifier,
                      storage store) {
        this.checkout = checkout;
        this.executor = executor;
        this.notifier = notifier;
        this.store = store;
    }

    /**
     * Runs the CI pipeline.
     */
    public BuildResult run(CiTrigger trigger) {
        List<StepResult> steps = new ArrayList<>();

        try {
            Path workspace = checkout.checkout(trigger);

            StepResult compile = executor.compile(workspace);
            steps.add(compile);
            if (!compile.success) {
                BuildResult res = new BuildResult(
                        BuildResult.Status.FAILURE,
                        "Compile failed",
                        steps
                );
                notifier.notify(trigger, res);
                store.save(trigger, res);
                return res;
            }

            StepResult test = executor.test(workspace);
            steps.add(test);
            if (!test.success) {
                BuildResult res = new BuildResult(
                        BuildResult.Status.FAILURE,
                        "Tests failed",
                        steps
                );
                notifier.notify(trigger, res);
                store.save(trigger, res);
                return res;
            }

            BuildResult res = new BuildResult(
                    BuildResult.Status.SUCCESS,
                    "OK",
                    steps
            );
            notifier.notify(trigger, res);
            store.save(trigger, res);
            return res;

        } catch (Exception e) {
            BuildResult res = new BuildResult(
                    BuildResult.Status.ERROR,
                    e.toString(),
                    steps
            );
            notifier.notify(trigger, res);
            store.save(trigger, res);
            return res;
        }
    }
}