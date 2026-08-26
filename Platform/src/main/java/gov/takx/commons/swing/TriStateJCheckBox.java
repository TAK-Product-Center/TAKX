package gov.takx.commons.swing;

import com.formdev.flatlaf.ui.FlatCheckBoxUI;
import gov.tak.api.annotation.IncubatingApi;
import gov.tak.api.annotation.Nullable;
import gov.takx.platform.ui.UIResources;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ActionMapUIResource;

import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;

import static com.formdev.flatlaf.FlatClientProperties.SELECTED_STATE;
import static com.formdev.flatlaf.FlatClientProperties.SELECTED_STATE_INDETERMINATE;
import static gov.takx.commons.swing.TriStateJCheckBox.State.INDETERMINATE;
import static gov.takx.commons.swing.TriStateJCheckBox.State.NOT_SELECTED;
import static gov.takx.commons.swing.TriStateJCheckBox.State.SELECTED;
import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * A {@link JCheckBox} that can have three states: selected, unselected, and indeterminate.<br>
 * Unless specified in one of the constructors, this field will operate in two-state mode, which means that users will only
 * be able to iterate between the {@link State#SELECTED} and {@link State#NOT_SELECTED} states. In two-state mode the field value can still
 * be set to {@link State#INDETERMINATE} and will display it accordingly, but once the user changes it they will not be able to change back to it.
 * When operating in three-state mode the user will be able to iterate between any of the {@link State} values.
 * <p>
 * Maintenance tip - There were some tricks to getting this code working:
 * <ol>
 * <li>You have to add a mouse event on mousePressed by calling super.addMouseListener() </li>
 * <li>You have to replace the UIActionMap for the keyboard event "pressed" with your own one. </li>
 * <li>You have to remove the UIActionMap for the keyboard event "released". </li>
 * <li>You have to grab focus when the next state is entered, otherwise clicking on the component won't get the focus. </li>
 * <li>You have to make a TriStateButtonModel as a button model that wraps the original button model and does state management.</li>
 * </ol>
 * Based on <a href="https://github.com/kabutz/TheJavaSpecialistsNewsletterSamples/tree/master/issue082/src/main/java/eu/javaspecialists/tjsn/issue082">this</a>.
 * @since 5.3
 */
@IncubatingApi(since = "5.3")
public class TriStateJCheckBox extends JCheckBox
{
    private static final Icon SELECTED_UNDEFINED_ICON = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "selected_undefined.svg");

    /**
     * The tree possible states.
     *
     * @since 5.3
     */
    public enum State
    {
        /**
         * The fully selected state.
         *
         * @since 5.3
         */
        SELECTED,
        /**
         * The fully UN-selected state.
         *
         * @since 5.3
         */
        NOT_SELECTED,
        /**
         * The indeterminate selected state.
         *
         * @since 5.3 (Renamed from PARTIALLY_SELECTED in 5.7)
         */
        INDETERMINATE
    }

    private final TriStateButtonModel model;
    private final boolean triState;
    private final Icon indeterminateIcon;

    /**
     * Construct a new instance with the given parameters.
     *
     * @param text the display text
     * @param icon the icon
     * @param initial the initial state
     * @param triState {@code true} if the user can iterate through all three states, otherwise {@code false}
     * @param indeterminateUndefined {@code true} if the "indeterminate" state should use the special "undefined" icon and
     * style, or {@code false} to use the standard "indeterminate" icon and style.
     * @since 5.7
     */
    public TriStateJCheckBox(@Nullable String text, @Nullable Icon icon, @Nullable State initial, boolean triState, boolean indeterminateUndefined)
    {
        super(text, icon);
        setUI(new TriStateJCheckBoxUi());
        this.triState = triState;
        indeterminateIcon = indeterminateUndefined ? SELECTED_UNDEFINED_ICON : null;
        // Add a listener for when the mouse is pressed
        addMouseListener((MousePressedListener) this::mousePressed);
        // Reset the keyboard action map
        ActionMap map = new ActionMapUIResource();
        map.put("pressed", new AbstractAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                grabFocus();
                model.nextState();
            }
        });
        map.put("released", null);
        SwingUtilities.replaceUIActionMap(this, map);
        // set the model to the adapted model
        model = new TriStateButtonModel(getModel());
        setModel(model);
        setState(initial);
    }

    /**
     * Construct a new instance with display text, initial state, and a {@code null} icon.
     *
     * @param text the display text
     * @param initial the initial state
     * @see #TriStateJCheckBox(String, Icon, State, boolean, boolean)
     */
    public TriStateJCheckBox(@Nullable String text, @Nullable State initial)
    {
        this(text, null, initial, false, false);
    }

    /**
     * Construct a new instance with the given text and the {@link State#INDETERMINATE} initial state.
     *
     * @param text the display text
     * @see #TriStateJCheckBox(String, State)
     */
    public TriStateJCheckBox(String text)
    {
        this(text, INDETERMINATE);
    }

    /**
     * Construct a new instance with {@code null} text and icon, and {@link State#INDETERMINATE} state.
     *
     * @param triState {@code true} if the component should allow the user to iterate through the indeterminate state,
     * otherwise {@code false}
     * @param indeterminateUndefined {@code true} if the "indeterminate" state should use the special "undefined" icon and
     * style, or {@code false} to use the standard "indeterminate" icon and style.
     * @since 5.7
     */
    public TriStateJCheckBox(boolean triState, boolean indeterminateUndefined)
    {
        this(null, null, INDETERMINATE, triState, indeterminateUndefined);
    }

    /**
     * Construct a new instance with {@code null} text.
     *
     * @see #TriStateJCheckBox(String)
     */
    public TriStateJCheckBox()
    {
        this(null);
    }

    /**
     * @param state the new state
     */
    public void setState(State state)
    {
        model.setState(state);
    }

    /**
     * @return the current state
     */
    public State getState()
    {
        return model.getState();
    }

    private void mousePressed(MouseEvent event)
    {
        grabFocus();
        model.nextState();
        fireActionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
                getActionCommand(),
                EventQueue.getMostRecentEventTime(),
                0));
    }

    @Override
    public void setSelected(boolean b)
    {
        if (b)
        {
            setState(SELECTED);
        } else
        {
            setState(NOT_SELECTED);
        }
    }

    @Override
    public void paint(Graphics g)
    {
        super.paint(g);
        if (getState() == INDETERMINATE && indeterminateIcon != null)
        {
            var x = getInsets().left;
            var y = getHeight() / 2 - indeterminateIcon.getIconHeight() / 2;
            indeterminateIcon.paintIcon(this, g, x, y);
        }
    }

    private class TriStateButtonModel implements ButtonModel
    {
        private final ButtonModel other;
        private State state;
        private boolean selected;

        private TriStateButtonModel(ButtonModel other)
        {
            this.other = other;
        }

        private void setState(State state)
        {
            this.state = state;
            switch (state)
            {
                case SELECTED ->
                {
                    putClientProperty(SELECTED_STATE, null);
                    setSelected(true);
                }
                case NOT_SELECTED ->
                {
                    putClientProperty(SELECTED_STATE, null);
                    setSelected(false);
                }
                default ->
                {
                    if (indeterminateIcon == null)
                    {
                        setSelected(false);
                        putClientProperty(SELECTED_STATE, SELECTED_STATE_INDETERMINATE);
                    } else
                    {
                        putClientProperty(SELECTED_STATE, null);
                        setSelected(false);
                    }
                }
            }
        }

        private State getState()
        {
            return state;
        }

        /**
         * By default, we rotate: {@link State#INDETERMINATE} -> {@link State#SELECTED}
         * Because the {@link CombiningTreeSelectionModel} (which is used in combination with this class) does not properly
         * remove partially selected paths (It needs to remove all descendant selections). So instead we move from indeterminate
         * to selected, then to not selected. If the {@link #triState} property is enabled then the user can iterate through
         * all three states. This is likely only useful when the indeterminate state represents "undefined".
         */
        private void nextState()
        {
            setState(switch (getState())
            {
                case SELECTED -> NOT_SELECTED;
                case NOT_SELECTED -> triState ? INDETERMINATE : SELECTED;
                case INDETERMINATE -> SELECTED;
            });
            repaint();
        }

        /**
         * We disable focusing on the component when it is not
         * enabled.
         */
        @Override
        public void setEnabled(boolean b)
        {
            setFocusable(b);
            other.setEnabled(b);
        }

        @Override
        public boolean isSelected()
        {
            return selected;
        }

        @Override
        public void setSelected(boolean b)
        {
            other.setSelected(b);
            selected = b;
        }

        @Override
        public boolean isArmed()
        {
            return other.isArmed();
        }

        @Override
        public void setArmed(boolean b)
        {
            other.setArmed(b);
        }

        @Override
        public boolean isEnabled()
        {
            return other.isEnabled();
        }

        @Override
        public boolean isPressed()
        {
            return other.isPressed();
        }

        @Override
        public boolean isRollover()
        {
            return other.isRollover();
        }

        @Override
        public void setPressed(boolean b)
        {
            other.setPressed(b);
        }

        @Override
        public void setRollover(boolean b)
        {
            other.setRollover(b);
        }

        @Override
        public void setMnemonic(int key)
        {
            other.setMnemonic(key);
        }

        @Override
        public int getMnemonic()
        {
            return other.getMnemonic();
        }

        @Override
        public void setActionCommand(String s)
        {
            other.setActionCommand(s);
        }

        @Override
        public String getActionCommand()
        {
            return other.getActionCommand();
        }

        @Override
        public void setGroup(ButtonGroup group)
        {
            other.setGroup(group);
        }

        @Override
        public void addActionListener(ActionListener l)
        {
            other.addActionListener(l);
        }

        @Override
        public void removeActionListener(ActionListener l)
        {
            other.removeActionListener(l);
        }

        @Override
        public void addItemListener(ItemListener l)
        {
            other.addItemListener(l);
        }

        @Override
        public void removeItemListener(ItemListener l)
        {
            other.removeItemListener(l);
        }

        @Override
        public void addChangeListener(ChangeListener l)
        {
            other.addChangeListener(l);
        }

        @Override
        public void removeChangeListener(ChangeListener l)
        {
            other.removeChangeListener(l);
        }

        @Override
        public Object[] getSelectedObjects()
        {
            return other.getSelectedObjects();
        }
    }

    /**
     * @since 5.7
     */
    private static class TriStateJCheckBoxUi extends FlatCheckBoxUI
    {
        protected TriStateJCheckBoxUi()
        {
            super(false);
        }
    }
}
