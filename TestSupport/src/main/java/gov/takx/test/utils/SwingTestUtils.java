package gov.takx.test.utils;

import gov.takx.api.annotation.InvokeAndWaitOnEDT;

import javax.swing.SwingUtilities;

import java.lang.reflect.InvocationTargetException;

/**
 * Test utilities to support testing Swing code.
 *
 * @since 3.4
 */
public class SwingTestUtils
{
    /**
     * Invoke a runnable on the EDT and wait for completion. Any exception thrown by the runnable will be unwrapped and
     * re-thrown by this method.
     *
     * @param runnable runnable to execute
     */
    public static void invokeAndWaitOnEDT(final Runnable runnable) throws Throwable
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            try
            {
                SwingUtilities.invokeAndWait(runnable);
            } catch (InvocationTargetException e)
            {
                // When an exception is thrown by a method called using invokeAndWait, it is wrapped in an
                // InvocationTargetException, so unwrap it and rethrow up to the calling test
                throw e.getCause();
            }
        } else
        {
            runnable.run();
        }
    }

    /**
     * Wait for EDT events to happen. This just queues and waits for an empty runnable. This ensures that all pending
     * UI events have been processed.
     */
    @InvokeAndWaitOnEDT
    public static void waitForEdtEvents()
    {
    }
}
