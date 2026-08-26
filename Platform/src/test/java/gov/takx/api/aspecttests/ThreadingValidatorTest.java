package gov.takx.api.aspecttests;

import gov.takx.api.annotation.InvokeAndWaitOnEDT;
import gov.takx.api.annotation.UiThread;
import gov.takx.api.annotation.WorkerThread;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Validate handling of threading aspects.
 *
 * @since 5.8
 */
public class ThreadingValidatorTest
{
    @Test
    void uiThread_onMethod_throws_whenNotOnEDT() throws InterruptedException, InvocationTargetException
    {
        assertThatIllegalStateException().isThrownBy(this::mustBeCalledOnEDT);

        SwingUtilities.invokeAndWait(() -> assertThatNoException().isThrownBy(this::mustBeCalledOnEDT));
    }

    @Test
    void uiThread_onClass_throws_whenNotOnEDT() throws InterruptedException, InvocationTargetException
    {
        assertThatIllegalStateException().isThrownBy(UiOnly::new);

        final var uiOnlyRef = new AtomicReference<UiOnly>();
        SwingUtilities.invokeAndWait(() -> uiOnlyRef.set(UiOnly.makeUiOnly()));
        final var uiOnly = uiOnlyRef.get();

        assertThatIllegalStateException().isThrownBy(uiOnly::methodA);
        assertThatIllegalStateException().isThrownBy(uiOnly::methodB);

        SwingUtilities.invokeAndWait(() -> {
            final var uiOnly2 = new UiOnly();
            assertThatNoException().isThrownBy(UiOnly::new);
            assertThatNoException().isThrownBy(uiOnly2::methodA);
            assertThatNoException().isThrownBy(uiOnly2::methodB);
        });
    }

    @Test
    void workerThread_onMethod_throws_whenOnEDT() throws InterruptedException, InvocationTargetException
    {
        assertThatNoException().isThrownBy(this::mustBeCalledOffEDT);

        SwingUtilities.invokeAndWait(() -> assertThatIllegalStateException().isThrownBy(this::mustBeCalledOffEDT));
    }

    @Test
    void workerThread_onClass_throws_whenOnEDT() throws InterruptedException, InvocationTargetException
    {
        SwingUtilities.invokeAndWait(() -> assertThatIllegalStateException().isThrownBy(WorkerOnly::new));

        final var workerOnly = new WorkerOnly();
        assertThatNoException().isThrownBy(workerOnly::methodA);
        assertThatNoException().isThrownBy(workerOnly::methodB);

        SwingUtilities.invokeAndWait(() -> assertThatIllegalStateException().isThrownBy(workerOnly::methodA));
        SwingUtilities.invokeAndWait(() -> assertThatIllegalStateException().isThrownBy(workerOnly::methodB));
    }

    @UiThread
    private void mustBeCalledOnEDT()
    {
    }

    @WorkerThread
    private void mustBeCalledOffEDT()
    {
    }

    @UiThread
    private static class UiOnly
    {
        @InvokeAndWaitOnEDT
        static UiOnly makeUiOnly()
        {
            return new UiOnly();
        }

        UiOnly()
        {
        }

        void methodA()
        {
        }

        void methodB()
        {
        }
    }

    @WorkerThread
    private static class WorkerOnly
    {
        WorkerOnly()
        {
        }

        void methodA()
        {
        }

        void methodB()
        {
        }
    }
}
