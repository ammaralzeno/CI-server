package ci.build;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;

/**
 * Implements process runner for running a command in a specified directory.
 */
public class DefaultProcessRunner implements ProcessRunner {

    /**
     * Runs the provided command in the given working directory.
     * 
     * @param directory the path to the working directory
     * @param command   a list containing an operating system program and its arguments
     * @return          exit code and captured logs
     */
    @Override
    public ProcessResult run(Path directory, List<String> command) {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(directory.toFile());
        pb.redirectErrorStream(true);

        StringBuilder logs = new StringBuilder();

        try {
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    logs.append(line).append(System.lineSeparator());
                }
            }

            int exitCode = process.waitFor();
            return new ProcessResult(exitCode, logs.toString());

        } catch (Exception e) {
            return new ProcessResult(
                    1,
                    "Process execution failed: " + e.getMessage()
            );
        }
    }
}
