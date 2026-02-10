package ci.build;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for CheckService.
 * Tests logic for resolving checkout target.
 */

public class CheckoutServiceTest {

    /**
     * Tests checkout target resolution for trigger where SHA == null.
     * @result The returned target is "refs/heads/" followed by the branch name.
     */
    @Test
    void returnsBranchWhenShaIsNull() {
        var trigger = new CiTrigger("url", "branch", null);
        var service = new DefaultCheckoutService();
        var target = service.resolveCheckoutTarget(trigger);
        assertEquals(target, "refs/heads/" + trigger.branch);
    }

    /**
     * Tests checkout target resolution for trigger where SHA is blank.
     * @result The returned target is "refs/heads/" followed by the branch name.
     */
    @Test
    void returnsBranchWhenShaIsBlank() {
        var trigger = new CiTrigger("url", "branch", "");
        var service = new DefaultCheckoutService();
        var target = service.resolveCheckoutTarget(trigger);
        assertEquals(target, "refs/heads/" + trigger.branch);
    }

    /**
     * Tests checkout target resolution for trigger where SHA is neither null nor blank.
     * @result The returned target is the same as the commit SHA.
     */
    @Test
    void returnsShaWhenNotNullAndNotBlank() {
        var trigger = new CiTrigger("url", "branch", "sha");
        var service = new DefaultCheckoutService();
        var target = service.resolveCheckoutTarget(trigger);
        assertEquals(target, trigger.commitSha);
    }
}
