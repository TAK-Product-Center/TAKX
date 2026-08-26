package gov.takx.api.aspecttests;

import gov.takx.api.annotation.InvokeAndWaitOnEDT;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.test.utils.SwingTestUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validate handling of various aspects related to the EDT.
 *
 * @since 5.8
 */
public class DispatchEDTMethodsTest
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Test
    void invokeAndWaitOnEDT_runsOnEDT_whenCalledOffEDT()
    {
        assertThat(invokeAndWaitOnEDT()).withFailMessage("invokeAndWaitOnEDT was not called on the EDT").isEqualTo(42);
    }

    @Test
    void invokeOnEDT_runsOnEDT_whenCalledOffEDT()
    {
        final var calledOnEdt = new AtomicBoolean(false);
        invokeOnEDT(calledOnEdt);
        SwingTestUtils.waitForEdtEvents();

        assertThat(calledOnEdt.get()).withFailMessage("invokeOnEDT was not called on the EDT").isTrue();
    }

    @InvokeAndWaitOnEDT
    private int invokeAndWaitOnEDT()
    {
        logger.warn("invokeAndWaitOnEDT called from {}", Thread.currentThread().getName());
        return SwingUtilities.isEventDispatchThread() ? 42 : -1;
    }

    @InvokeOnEDT
    private void invokeOnEDT(AtomicBoolean calledOnEdt)
    {
        logger.warn("invokeOnEDT called from {}", Thread.currentThread().getName());
        calledOnEdt.set(SwingUtilities.isEventDispatchThread());
    }
}
