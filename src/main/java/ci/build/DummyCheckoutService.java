package ci.build;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Temporary checkout stub.
 *
 * TODO(Person C): Replace with real git checkout.
 */
public class DummyCheckoutService implements CheckoutService {
    @Override
    public Path checkout(CiTrigger trigger) throws Exception {
        return Files.createTempDirectory("ci-workspace-");
    }
}
