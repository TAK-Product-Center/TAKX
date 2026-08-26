package gov.takx.processmanager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Simple launcher for the process manager.  Required to disconnect the classes needed in the PM from
 * TAKX to avoid class loading issues (since TAKX doesn't have access to tools.jar).
 *
 * @since 1.49
 */
public class ProcessManagerLauncher
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * @return {@code true} if the given {@code fileName} appears to be one of our log files
     * @since 5.4
     */
    public static boolean isProcessManagerLogFile(String fileName)
    {
        return fileName.startsWith(LOG_FILE_PREFIX);
    }

    private static final String LOG_FILE_PREFIX = "process_manager_";
    private static final String JAVA_CMD = Paths.get(System.getProperty("java.home"), "bin", "java").toString();

    // We can't directly reference RemoteProcessManager without pulling in the class, so construct its canonical name by hand.
    private static final String RPM_CLASS_NAME = ProcessManagerLauncher.class.getPackage().getName() + ".RemoteProcessManager";

    private static final AtomicBoolean processCreated = new AtomicBoolean(false);
    private static Process process;

    // Generated stub for sending commands to the remote process manager.
    private static IRemoteProcessManager manager;

    /**
     * Set up the singleton instance of the manager.
     *
     * @param applicationName The name of the primary application.
     * @param logFileDirectory Directory in which to place our log file
     * @param headless Pass {@code true} if running headless
     * @throws IOException Thrown on exceptions starting the process.
     */
    public static IRemoteProcessManager getProcessManager(String applicationName, String logFileDirectory, boolean headless)
            throws IOException
    {
        if (processCreated.compareAndSet(false, true))
        {
            final String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());
            final String logFileName = LOG_FILE_PREFIX + timestamp + ".log";
            final File logFile = new File(logFileDirectory, logFileName);

            if (!logFile.exists()) Files.createFile(logFile.toPath());
            logFile.deleteOnExit();

            // Start new process manager.  We will send it commands via the process' input stream.

            final String classPath = findJarPath();
            final long pid = ProcessHandle.current().pid();

            logger.debug("LAUNCH: {} cp={} pid={} log={}", RPM_CLASS_NAME, classPath, pid, logFile);

            final ProcessBuilder builder = new ProcessBuilder(
                    JAVA_CMD,
                    "-cp",
                    classPath,
                    RPM_CLASS_NAME,
                    String.valueOf(pid),
                    applicationName,
                    String.valueOf(headless));

            // Send all output to our configured log file
            builder.redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.to(logFile));

            process = builder.start();

            manager = new ProcessManagerStub();
        }

        return manager;
    }

    /**
     * @return The path to the jar containing this class.
     */
    private static String findJarPath()
    {
        final URL location = ProcessManagerLauncher.class.getProtectionDomain().getCodeSource().getLocation();
        final String protocol = location.getProtocol();
        final String path = location.getPath();

        logger.debug("jar location: {}", location);

        if ("jar".equals(protocol) && path.startsWith("file:") && path.endsWith("!/"))
        {
            // Need to decode the path since it came from a URL (-2 to chop off the trailing "!/")
            return URLDecoder.decode(path.substring("file:".length(), path.length() - 2), StandardCharsets.UTF_8);
        } else
        {
            logger.error("Unable to locate process manager jar from: {}", location);
            return "";
        }
    }

    /**
     * Stub to communicate with the process manager.  Converts local method calls to data written to the process'
     * input stream.
     *
     * @since 3.0
     */
    private static final class ProcessManagerStub implements IRemoteProcessManager
    {
        private static final byte[] LINE_SEP_BYTES = System.lineSeparator().getBytes(StandardCharsets.UTF_8);
        private static final byte[] EXIT_BYTES = ("EXIT" + System.lineSeparator()).getBytes(StandardCharsets.UTF_8);

        private final OutputStream outputStream;

        ProcessManagerStub()
        {
            outputStream = process.getOutputStream();
        }

        @Override
        public void addPID(long pid)
        {
            try
            {
                outputStream.write(Long.toString(pid).getBytes(StandardCharsets.UTF_8));
                outputStream.write(LINE_SEP_BYTES);
                outputStream.flush();
            } catch (IOException e)
            {
                logger.error("Failed to send PID to process manager", e);
            }
        }

        @Override
        public void shutdown()
        {
            try
            {
                outputStream.write(EXIT_BYTES);
                outputStream.flush();

                // Now wait for the process to exit, but don't hang indefinitely
                if (!process.waitFor(10, TimeUnit.SECONDS))
                {
                    logger.warn("Process manager has not exited; killing it.");
                    process.destroy();
                }
            } catch (IOException e)
            {
                logger.error("Failed to send shutdown command to process manager", e);
                process.destroy(); // Try to directly kill it
            } catch (InterruptedException ignore)
            {
            }
        }
    }
}
