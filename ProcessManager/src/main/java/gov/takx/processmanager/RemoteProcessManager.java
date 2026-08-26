package gov.takx.processmanager;

import javax.swing.JOptionPane;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Stops orphaned processes if the main application crashes. This manager is intended to run as a sub-process of the
 * main application. If the main application shuts down, this sub-process will be shut down as well. If the main
 * application crashes, this sub-process will continue to run as an orphan and will
 * detect that the main application's PID is missing and shut down other orphaned processes and finally, itself.
 * <p>
 * Note, all our output has been redirected to a log file, so the use of {@code System.out/System.err} instead
 * of a logger is intentional and appropriate.
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class RemoteProcessManager extends Thread
{
    /**
     * The time period to wait between tests to see if the main app is still alive.
     */
    private static final long TIMEOUT = TimeUnit.SECONDS.toMillis(1);

    private static final String QUERY = "<html><center>It appears that %s" +
            " has stopped unexpectedly." + "<br>Would you like to stop any associated processes?</html>";

    private final String applicationName; // name of the application that is being monitored.
    private final long applicationPid; // The PID of the main application.
    private final boolean headless; // Running headless?

    private final CommandHandler commandHandler = new CommandHandler();

    /**
     * The PIDs that will be killed if the parent application stops unexpectedly.
     */
    private final List<Long> childPIDs = new ArrayList<>();

    /**
     * Main entry point, constructs and starts the process manager.
     *
     * @param args main application pid, main application name, headless (true/false)
     */
    public static void main(String[] args)
    {
        new RemoteProcessManager(Long.parseLong(args[0]), args[1], Boolean.parseBoolean(args[2])).start();
    }

    /**
     * Constructor.
     *
     * @param applicationPid  PID of main application
     * @param applicationName The name of the application that is being monitored.
     * @param headless        Pass {@code true} if running headless
     */
    public RemoteProcessManager(long applicationPid, String applicationName, boolean headless)
    {
        this.applicationPid = applicationPid;
        this.applicationName = applicationName;
        this.headless = headless;

        System.out.printf("STARTED: name=%s pid=%s headless=%s%n", applicationName, applicationPid, headless);
    }

    /**
     * Shutdown operations by interrupting this thread.
     *
     * @since 3.0
     */
    private void shutdown()
    {
        System.out.println("Shutting down process manager.");
        interrupt();
    }

    @Override
    public void run()
    {
        System.out.println("Watching main application: " + applicationName);

        commandHandler.start();

        waitForMainAppExit();

        if (!isInterrupted() && !childPIDs.isEmpty()) handleMainAppExit();

        commandHandler.interrupt();

        System.exit(0);
    }

    /**
     * Monitor the main application, waiting until it is no longer alive, or we are interrupted.
     *
     * @since 3.0
     */
    private void waitForMainAppExit()
    {
        while (!isInterrupted() && isMainAppAlive())
        {
            try
            {
                //noinspection BusyWait
                sleep(TIMEOUT);
            } catch (InterruptedException ignore)
            {
                // Interrupt flag is reset by sleep.
                currentThread().interrupt();
            }
        }
    }

    /**
     * @return {@code true} if the main app is alive, {@code false} if not.
     * @since 3.0
     */
    private boolean isMainAppAlive()
    {
        return ProcessHandle.of(applicationPid).map(ProcessHandle::isAlive).orElse(false);
    }

    /**
     * The main application has exited without properly telling us to stop, so we need to terminate any
     * remaining registered sub-processes.  If not headless, we query the user before doing so.
     *
     * @since 3.0
     */
    private void handleMainAppExit()
    {
        System.out.println(applicationName + " has apparently died. Querying user about killing processes.");

        if (queryUserForKill())
        {
            System.out.println("User chose to kill orphaned processes.");

            for (long pid : childPIDs)
            {
                System.out.println("Killing process [" + pid + "]");
                ProcessHandle.of(pid).ifPresent(ProcessHandle::destroy);
            }
        } else
        {
            System.out.println("User chose not to kill orphaned processes.");
        }
    }

    /**
     * Determine if the user wants to kill background processes.  Default to true if running headless (no user to ask).
     *
     * @return {@code true} if headless or user said kill
     */
    private boolean queryUserForKill()
    {
        return headless
                || showOptionDialog(String.format(QUERY, applicationName), applicationName + " Stopped Unexpectedly") == JOptionPane.YES_OPTION;
    }

    /**
     * Show an option dialog and get the user's selection.
     *
     * @param message the message to display
     * @param title   the title string for the dialog
     * @return an integer indicating the option chosen by the user, or {@code CLOSED_OPTION} if the user closed the dialog
     * @see JOptionPane#showOptionDialog(java.awt.Component, Object, String, int, int, javax.swing.Icon, Object[], Object)
     */
    private static int showOptionDialog(String message, String title)
    {
        final var pane = new JOptionPane(message, JOptionPane.WARNING_MESSAGE, JOptionPane.YES_NO_OPTION);
        final var dialog = pane.createDialog(title);
        dialog.setAlwaysOnTop(true);
        pane.selectInitialValue();
        dialog.setVisible(true);
        dialog.dispose();

        final var selectedValue = pane.getValue();

        return selectedValue instanceof Integer ? (Integer) selectedValue : JOptionPane.CLOSED_OPTION;
    }

    /**
     * Thread to read incoming commands from {@link System#in}, and dispatch them.
     *
     * @since 3.0
     */
    @SuppressWarnings("CallToPrintStackTrace")
    private final class CommandHandler extends Thread
    {
        @Override
        public void run()
        {
            try (var reader = new BufferedReader(new InputStreamReader(System.in)))
            {
                String line;
                while ((line = reader.readLine()) != null)
                {
                    final String command = line.trim();

                    if ("EXIT".equalsIgnoreCase(command))
                    {
                        shutdown();
                        break;
                    } else
                    {
                        // should be a numeric pid to monitor
                        try
                        {
                            final long pid = Long.parseLong(command);

                            System.out.println("Adding process [" + pid + "].");
                            childPIDs.add(pid);
                        } catch (NumberFormatException e)
                        {
                            // Anything else is an unknown command
                            System.err.format("Unknown command: '%s'%n", command);
                        }
                    }
                }
            } catch (IOException e)
            {
                System.err.println("Failed reading commands: " + e.getMessage());
                e.printStackTrace();
                shutdown();
            }
        }
    }
}
