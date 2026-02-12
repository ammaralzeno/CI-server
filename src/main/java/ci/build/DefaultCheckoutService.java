package ci.build;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.RefNotFoundException;

/**
 * Implements checkout service with git clone and checkout.
 * 
 */
public class DefaultCheckoutService implements CheckoutService {

    /**
     * Clones the given repository and checks out the given branch, 
     * returning path to created directory.
     * 
     * @param trigger   CiTrigger containing Git information
     * @return         path to the temporary workspace containing the cloned repository
     * @exception IOException               If temporary directory cannot be created 
     *                                      or closing of repo fails
     * @exception GitAPIException           If an error occurs relating to JGit
     * @exception InvalidRemoteException    If remote repo is invalid or not found
     * @exception RefNotFoundException      If the branch or commit SHA does not exist
     * @exception InvalidRefNameException   If the commit SHA is invalid
     */
    @Override
    public Path checkout(CiTrigger trigger) throws Exception {
        Path workspace = Files.createTempDirectory("ci-workspace-");

        String checkoutTarget = resolveCheckoutTarget(trigger);
        try (Git git = Git.cloneRepository()
                .setURI(trigger.repoUrl)
                .setDirectory(workspace.toFile())
                .setBranch("refs/heads/" + trigger.branch)
                .call()) {

            if (!checkoutTarget.equals("refs/heads/" + trigger.branch)) {
                git.checkout()
                    .setName(checkoutTarget)
                    .call();
            }
        }

        return workspace;
    }

    /**
     * Isolates checkout logic for unit testing.
     * 
     * @param trigger CiTrigger containing Git information
     * @return the ref or commit to check out
     */
    String resolveCheckoutTarget(CiTrigger trigger) {
        if (trigger.commitSha != null && !trigger.commitSha.isBlank()) {
            return trigger.commitSha;
        }
        return "refs/heads/" + trigger.branch;
    }

}
