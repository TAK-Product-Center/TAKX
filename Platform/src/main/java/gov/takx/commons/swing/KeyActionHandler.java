package gov.takx.commons.swing;

import jakarta.annotation.Nonnull;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;

/**
 * Helper class to tie into the component {@link JComponent#getInputMap() input map} and {@link JComponent#getActionMap() action map}
 * to add handling for keystroke events.
 *
 * @see <a href="https://docs.oracle.com/javase%2Ftutorial%2F/uiswing/misc/keybinding.html">Key Bindings</a>
 * @since 5.2
 */
public class KeyActionHandler
{
    /**
     * {@link KeyStroke} for the Escape key.
     */
    public static final KeyStroke ESCAPE_KEY = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);

    private JComponent targetComponent;
    private KeyAction keyAction;
    private Object actionMapKey;
    private KeyStroke keyStroke;
    private int condition = JComponent.WHEN_IN_FOCUSED_WINDOW;

    /**
     * Default constructor.  Note that using this constructor you <em>must</em> call all the necessary setter methods
     * before using it.
     *
     * @see #setTargetComponent(JComponent)
     * @see #setActionMapKey(Object)
     * @see #setKeyStroke(KeyStroke)
     * @see #setAction(Runnable)
     * @see #setCondition(int) setCondition(int) *optional
     */
    public KeyActionHandler()
    {
    }

    /**
     * Create an action handler targeting the application root pane and using {@link JComponent#WHEN_IN_FOCUSED_WINDOW}
     * as the condition.
     *
     * @param actionMapKey The key to use for making the entry in the action map
     * @param keyStroke The keystroke to initiate the action
     * @param action Action to perform when the keystroke action is invoked
     */
    public KeyActionHandler(@Nonnull Object actionMapKey, @Nonnull KeyStroke keyStroke, @Nonnull Runnable action)
    {
        this(getRootPane(), actionMapKey, keyStroke, action);
    }

    /**
     * Create an action handler on the given component and using {@link JComponent#WHEN_IN_FOCUSED_WINDOW}
     * as the condition.
     *
     * @param targetComponent The component on which to install the action
     * @param actionMapKey The key to use for making the entry in the action map
     * @param keyStroke The keystroke to initiate the action
     * @param action Action to perform when the keystroke action is invoked
     */
    public KeyActionHandler(@Nonnull JComponent targetComponent, @Nonnull Object actionMapKey, @Nonnull KeyStroke keyStroke, @Nonnull Runnable action)
    {
        this(targetComponent, actionMapKey, keyStroke, JComponent.WHEN_IN_FOCUSED_WINDOW, action);
    }

    /**
     * Create an action handler on the given component and condition.
     *
     * @param targetComponent The component on which to install the action
     * @param actionMapKey The key to use for making the entry in the action map
     * @param keyStroke The keystroke to initiate the action
     * @param condition one of {@link JComponent#WHEN_IN_FOCUSED_WINDOW}, {@link JComponent#WHEN_FOCUSED},
     *        {@link JComponent#WHEN_ANCESTOR_OF_FOCUSED_COMPONENT}
     * @param action Action to perform when the keystroke action is invoked
     */
    public KeyActionHandler(@Nonnull JComponent targetComponent, @Nonnull Object actionMapKey, @Nonnull KeyStroke keyStroke, int condition, @Nonnull Runnable action)
    {
        setTargetComponent(targetComponent);
        setAction(action);
        setActionMapKey(actionMapKey);
        setKeyStroke(keyStroke);
        setCondition(condition);
    }

    /**
     * @param targetComponent The component on which to install the action
     */
    public void setTargetComponent(@Nonnull JComponent targetComponent)
    {
        this.targetComponent = targetComponent;
    }

    /**
     * @param action Action to perform when the keystroke is detected within the proper condition
     */
    public void setAction(@Nonnull Runnable action)
    {
        keyAction = new KeyAction(action);
    }

    /**
     * @param actionMapKey The key to use for making the entry in the action map
     */
    public void setActionMapKey(@Nonnull Object actionMapKey)
    {
        this.actionMapKey = actionMapKey;
    }

    /**
     * @param keyStroke The keystroke to initiate the action
     */
    public void setKeyStroke(@Nonnull KeyStroke keyStroke)
    {
        this.keyStroke = keyStroke;
    }

    /**
     * The condition for the input map.
     *
     * @param condition one of {@link JComponent#WHEN_IN_FOCUSED_WINDOW}, {@link JComponent#WHEN_FOCUSED},
     *        {@link JComponent#WHEN_ANCESTOR_OF_FOCUSED_COMPONENT}
     * @see JComponent#getInputMap(int)
     */
    public void setCondition(int condition)
    {
        this.condition = condition;
    }

    /**
     * Target the action at the application root pane.
     */
    public void useRootPane()
    {
        setTargetComponent(getRootPane());
    }

    /**
     * Install our keystroke action.
     */
    public void install()
    {
        Objects.requireNonNull(targetComponent, "'targetComponent' must not be null");
        Objects.requireNonNull(actionMapKey, "'actionMapKey' must not be null");
        Objects.requireNonNull(keyStroke, "'keyStroke' must not be null");
        Objects.requireNonNull(keyAction, "'action' must not be null");

        targetComponent.getInputMap(condition).put(keyStroke, actionMapKey);
        targetComponent.getActionMap().put(actionMapKey, keyAction);
    }

    /**
     * Uninstall our keystroke action.
     */
    public void uninstall()
    {
        targetComponent.getInputMap(condition).remove(keyStroke);
        targetComponent.getActionMap().remove(actionMapKey);
    }

    /**
     * @return The root pane for the application
     */
    private static JRootPane getRootPane()
    {
        return SwingUtilities.getRootPane(JFrame.getFrames()[0]);
    }

    /**
     * Simple {@link javax.swing.Action} to execute a given runnable.
     */
    private static class KeyAction extends AbstractAction
    {
        private final Runnable action;

        private KeyAction(Runnable action)
        {
            this.action = action;
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            action.run();
        }
    }
}
