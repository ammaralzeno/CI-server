package ci.build;

import java.nio.file.Path;

/**
 * Abstract class for checkout service.
 * Checks out the repository/branch to a local workspace.
 */
public interface CheckoutService {
    Path checkout(CiTrigger trigger) throws Exception;
}
