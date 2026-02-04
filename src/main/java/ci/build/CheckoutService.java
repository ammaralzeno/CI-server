package ci.build;

import java.nio.file.Path;

/**
 * Checks out the repository/branch to a local workspace.
 *
 * TODO(Person C): Implement using git clone/fetch + checkout.
 */
public interface CheckoutService {
    Path checkout(CiTrigger trigger) throws Exception;
}
