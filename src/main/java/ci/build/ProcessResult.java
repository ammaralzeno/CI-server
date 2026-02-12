package ci.build;

/**
 * Return type for ProcessRunner.
 * @param exitCode the process exit code
 * @param logs the combined stdout and stderr output from the process
 */
public record ProcessResult(int exitCode, String logs) {}

