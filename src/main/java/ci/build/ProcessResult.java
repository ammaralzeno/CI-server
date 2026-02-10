package ci.build;

/**
 * Return type for ProcessRunner.
 */
public record ProcessResult(int exitCode, String logs) {}

