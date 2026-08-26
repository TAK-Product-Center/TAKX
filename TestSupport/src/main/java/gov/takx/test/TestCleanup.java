package gov.takx.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

/**
 * Performs registered actions when the JVM shuts down.  Used to support final cleanup after all tests have been run.
 *
 * @since 5.3
 */
public class TestCleanup
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    static
    {
        Runtime.getRuntime().addShutdownHook(new Thread(TestCleanup::runActions));
    }

    private static final List<Runnable> CLEANUP_ACTIONS = new ArrayList<>();

    /**
     * Add an action to be performed when the JVM shuts down.
     */
    public static void addCleanupAction(Runnable action)
    {
        CLEANUP_ACTIONS.add(action);
    }

    /**
     * Run all registered actions.
     */
    private static void runActions()
    {
        CLEANUP_ACTIONS.forEach(action -> {
            try
            {
                action.run();
            } catch (Exception e)
            {
                logger.error("Failed to execute cleanup action", e);
            }
        });
    }
}
