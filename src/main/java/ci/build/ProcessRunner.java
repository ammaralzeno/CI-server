package ci.build;

import java.nio.file.Path;
import java.util.List;

/**
 * Abstract class for process runner.
 * Runs commands passed from a BuildExecutor method in the given working directory.
 */
@FunctionalInterface
public interface ProcessRunner {
    ProcessResult run(Path directory, List<String> command);
}

