package gov.takx.core.database.provider.postgres;

import jakarta.annotation.Nonnull;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

// TODO: these probably need a better home
final class ProcessUtils
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private ProcessUtils()
    {
    }

    /**
     * Run a process until it exits, throwing an IOException if there is any output on stderr or if the process exits with a non-zero exit code.
     *
     * @param tag Descriptive tag for debug/error output
     * @param cmd the command to execute
     * @param env Map of environment variables to set in the sub-process, may be empty.
     * @throws IOException if the command outputs an error message or returns a nonzero exit code
     * @since 5.4
     */
    public static void runProcess(@Nonnull String tag, @Nonnull Map<String, String> env, @Nonnull String... cmd) throws IOException
    {
        runProcess(tag, env, cmd, () -> null);
    }

    /**
     * Run a process until it exits, throwing an IOException if there is any output on stderr or if the process exits with a non-zero exit code.
     *
     * @param tag                 Descriptive tag for debug/error output
     * @param env                 Map of environment variables to set in the sub-process, may be empty.
     * @param cmd                 the command to execute
     * @param successHandler      Handler to invoke after if the process runs successfully.  It must return {@code null}
     *                            or an exception, which will be rethrown.
     * @param innocuousIndicators Strings that, if included in the error messages, indicate that the error was innocuous.
     * @throws IOException if the command outputs an error message or returns a nonzero exit code
     * @since 5.1 (signature change)
     */
    public static void runProcess(@Nonnull String tag, @Nonnull Map<String, String> env, @Nonnull String[] cmd,
                                  @Nonnull Supplier<IOException> successHandler, String... innocuousIndicators)
            throws IOException
    {
        final long start = System.currentTimeMillis();

        logger.debug("CMD: {}; ENV {}", cmd, env);

        final ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.environment().putAll(env);

        final ProcessResult processResult = runProcessAndGetOutput(pb);

        processResult.processErrorMessages(innocuousIndicators);

        if (processResult.throwException())
        {
            handleErrorResult(processResult, tag);
        } else
        {
            if (logger.isDebugEnabled())
            {
                if (processResult.hasMessages()) logger.debug("{}", processResult.getMessages());
                logger.debug("{} took {} ms", tag, System.currentTimeMillis() - start);
            }

            final IOException exception = successHandler.get();
            if (exception != null) throw exception;
        }
    }

    /**
     * Run a process and collect any error output that it produces.  Return {@link ProcessResult}.
     *
     * @param pb The process builder
     * @return Process result
     */
    private static ProcessResult runProcessAndGetOutput(ProcessBuilder pb) throws IOException
    {
        final File procStdout = Files.createTempFile("proc_stdout_", null).toFile();
        procStdout.deleteOnExit();
        pb.redirectOutput(procStdout);

        final File procStderr = Files.createTempFile("proc_stderr_", null).toFile();
        procStderr.deleteOnExit();
        pb.redirectError(procStderr);

        final Process proc = pb.start();

        final int stat = waitForProc(proc);
        logger.debug("Done, proc exit stat: {}", stat);

        // Get the output
        final List<String> standardOutput = Files.readAllLines(procStdout.toPath());
        if (logger.isDebugEnabled()) logger.debug("stdout:\n{}", String.join("\n   ", standardOutput));

        final List<String> errorOutput = Files.readAllLines(procStderr.toPath());
        if (logger.isDebugEnabled()) logger.debug("stderr:\n{}", String.join("\n   ", errorOutput));

        return new ProcessResult(pb, stat, standardOutput, errorOutput);
    }

    private static int waitForProc(Process proc)
    {
        boolean interrupted = false;
        try
        {
            while (true)
            {
                try
                {
                    return proc.waitFor();
                } catch (InterruptedException e)
                {
                    interrupted = true;
                }
            }
        } finally
        {
            if (interrupted) Thread.currentThread().interrupt();
        }
    }

    /**
     * Execute a process and return the output.
     *
     * @param env Map of environment variables to set in the sub-process, may be empty.
     * @param cmd Command to execute
     * @return List of lines of output produced to {@code stdout}
     */
    public static List<String> readProcess(@Nonnull Map<String, String> env, @Nonnull String... cmd) throws IOException
    {
        if (logger.isDebugEnabled()) logger.debug("CMD: {}", Arrays.toString(cmd));

        final ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.environment().putAll(env);
        final Process proc = pb.start();
        final List<String> output = IOUtils.readLines(proc.getInputStream(), Charset.defaultCharset());

        waitForProc(proc);

        if (logger.isDebugEnabled()) logger.debug("stdout:\n{}", String.join("\n   ", output));
        return output;
    }

    private static void handleErrorResult(ProcessResult processResult, String action) throws IOException
    {
        final String initialMessage = processResult.processFailed()
                ? "Postgres exited the " + action + " process with code " + processResult.getExitStatus()
                : "Errors " + action + " Postgres.";

        final ProcessBuilder processBuilder = processResult.processBuilder;

        final String message = "Database operations failed\n" +
                initialMessage +
                "\nCommand was: " + processBuilder.command() +
                "\n\nOutput was:\n" + String.join("\n", processResult.stdoutMessages) +
                "\n\nError output was:\n" + processResult.getMessages();

        throw new IOException(message);
    }

    /**
     * Processes the error stream results returned from an external process, looking for errors and building a composite
     * message as necessary. We do some... interesting checks to check to see if we need to actually care about the
     * error messages returned from Postgres and they're reflected in the {@code innocuousIndicators}.
     */
    private static class ProcessResult
    {
        /**
         * The exit status returned from the external Postgres process.
         */
        private final int exitStatus;

        private final List<String> stdoutMessages;
        private final List<String> stderrMessages;

        /**
         * Flag that tracks whether the error messages themselves indicate that errors were encountered. This even
         * supersedes a non-zero error code.
         */
        private boolean hasErrors;

        /**
         * Flag that indicates that an error is ignorable, even if the error code indicates that an error occurred.
         */
        private boolean failureIsIgnorable = false;

        /**
         * All the messages returned on the error stream that may or may not indicate that there were actual errors.
         */
        private String messages = null;

        /**
         * Process builder used to construct the process/operation that failed. Typically used to access the command
         * and the environment settings.
         *
         * @since 2.28
         */
        private final ProcessBuilder processBuilder;

        /**
         * Constructor.
         *
         * @param processBuilder ProcessBuilder used to construct process that failed
         * @param exitStatus     The exit status returned from the process
         * @param stderrMessages The list of messages returned by the external process on the error stream.
         */
        public ProcessResult(ProcessBuilder processBuilder, int exitStatus, List<String> stdoutMessages, List<String> stderrMessages)
        {
            this.processBuilder = processBuilder;
            this.exitStatus = exitStatus;
            this.stdoutMessages = stdoutMessages;
            this.stderrMessages = stderrMessages;

            hasErrors = exitStatus != 0;
        }

        /**
         * Process the recorded error messages.
         *
         * @param innocuousIndicators Strings that, if included in the error messages, indicate that the error was innocuous.
         */
        void processErrorMessages(String... innocuousIndicators)
        {
            messages = String.join("\n", stderrMessages);

            if (!stderrMessages.isEmpty())
            {
                for (String line : stderrMessages)
                {
                    // Once we've determined that the error is innocuous, we can stop checking.
                    if (!failureIsIgnorable)
                    {
                        if (innocuousIndicators.length > 0)
                        {
                            for (String innocuousIndicator : innocuousIndicators)
                            {
                                if (line.contains(innocuousIndicator))
                                {
                                    failureIsIgnorable = true;
                                    hasErrors = false;
                                    break;
                                }
                            }
                        } else
                        {
                            if (line.toLowerCase().contains("error")) hasErrors = true;
                        }
                    }
                }
            }
        }

        public boolean processFailed()
        {
            return exitStatus != 0;
        }

        public boolean throwException()
        {
            return (processFailed() && !failureIsIgnorable) || hasErrors;
        }

        public int getExitStatus()
        {
            return exitStatus;
        }

        public boolean hasMessages()
        {
            return messages != null && !messages.isEmpty();
        }

        public String getMessages()
        {
            return messages;
        }
    }
}
