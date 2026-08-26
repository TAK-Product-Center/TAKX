package gov.takx.commons.swing;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.swing.JPanel;

import java.awt.AWTException;
import java.awt.Toolkit;
import java.awt.event.AWTEventListenerProxy;
import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

/**
 * @since 5.8
 */
public class MouseRolloverListenerTest
{
    @Test
    public void test() throws InterruptedException, InvocationTargetException, AWTException
    {
        var panel = new JPanel();
        var entered = new MutableBoolean();
        var exited = new MutableBoolean();
        var listener = (MouseEnteredExitedListener) e -> {
            if (e.getID() == MouseEvent.MOUSE_ENTERED)
                entered.setTrue();
            else if (e.getID() == MouseEvent.MOUSE_EXITED) exited.setTrue();
        };
        var rolloverListener = MouseRolloverListener.addRolloverListener(panel, listener);

        // We cant simulate motion events so all we can really test is that the proper listeners get added and removed
        Assertions.assertThat(panel.getHierarchyListeners()).contains(rolloverListener);
        // AWT event listeners get wrapped in a proxy so we have to unwrap them before checking
        var unwrapped = Arrays.stream(Toolkit.getDefaultToolkit().getAWTEventListeners()).map(l -> l instanceof AWTEventListenerProxy p ? p.getListener() : l).toList();
        Assertions.assertThat(unwrapped).contains(rolloverListener);

        MouseRolloverListener.removeRolloverListener(panel, listener);
        Assertions.assertThat(panel.getHierarchyListeners()).doesNotContain(rolloverListener);
        unwrapped = Arrays.stream(Toolkit.getDefaultToolkit().getAWTEventListeners()).map(l -> l instanceof AWTEventListenerProxy p ? p.getListener() : l).toList();
        Assertions.assertThat(unwrapped).doesNotContain(rolloverListener);
    }
}
