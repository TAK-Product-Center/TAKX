package gov.takx.commons.swing;

import gov.tak.api.annotation.IncubatingApi;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.AbstractAction;
import javax.swing.Icon;

import java.awt.event.ActionEvent;

/**
 * Implementation of swing {@link javax.swing.Action} that can be used without needing to create anonymous/inner classes.
 * <br/>
 * Usage:
 * <pre>
 *     {@code
 *     JPopupMenu menu = new JPopupMenu();
 *     menu.add(new SimpleAction("Edit", actionEvent -> ... ));
 *     }
 * </pre>
 * @since 5.3
 */
@IncubatingApi(since = "5.3")
public class SimpleAction extends AbstractAction
{
    private ActionPerformedListener actionPerformedListener;

    /**
     * Construct a new Simple Action with the specified listener and name.
     *
     * @param name the action name
     * @param actionPerformedListener the action listener
     */
    public SimpleAction(@Nullable String name, @Nullable ActionPerformedListener actionPerformedListener)
    {
        this(name, null, actionPerformedListener);
    }

    /**
     * Construct a new Simple Action with the specified name, icon, and listener.
     *
     * @param name the action name
     * @param icon the action icon
     * @param actionPerformedListener the action listener
     */
    public SimpleAction(@Nullable String name, @Nullable Icon icon, @Nullable ActionPerformedListener actionPerformedListener)
    {
        super(name, icon);
        this.actionPerformedListener = actionPerformedListener;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        if (actionPerformedListener != null) actionPerformedListener.actionPerformed(e);
    }

    /**
     * @return the listener that is invoked when an action is performed
     * @since 5.7
     */
    @Nullable
    public ActionPerformedListener getActionPerformedListener()
    {
        return actionPerformedListener;
    }

    /**
     * @param actionPerformedListener the listener that should be invoked when an action is performed
     * @since 5.7
     */
    public void setActionPerformedListener(@Nullable ActionPerformedListener actionPerformedListener)
    {
        this.actionPerformedListener = actionPerformedListener;
    }

    /**
     * @return the icon (See {@link javax.swing.Action#SMALL_ICON})
     * @since 5.7
     */
    @Nullable
    public Icon getIcon()
    {
        return (Icon) getValue(SMALL_ICON);
    }

    /**
     * @param icon the icon (See {@link javax.swing.Action#SMALL_ICON})
     * @since 5.8
     */
    public void setIcon(@Nullable Icon icon)
    {
        putValue(SMALL_ICON, icon);
    }

    /**
     * @return the name (See {@link javax.swing.Action#NAME})
     * @since 5.7
     */
    @Nullable
    public String getName()
    {
        return (String) getValue(NAME);
    }

    /**
     * @param name the name (See {@link javax.swing.Action#NAME})
     * @since 5.8
     */
    public void setName(@Nullable String name)
    {
        putValue(NAME, name);
    }

    /**
     * @param tooltipText Tooltip text to display, {@code null} for none
     * @since 5.7
     */
    public void setTooltipText(@Nullable String tooltipText)
    {
        putValue(SHORT_DESCRIPTION, tooltipText);
    }

    /**
     * @return Tooltip text, or {@code null}
     * @since 5.7
     */
    @Nullable
    public String getTooltipText()
    {
        return (String) getValue(SHORT_DESCRIPTION);
    }

    /**
     * A functional interface that allows users to pass method references as the action listener.
     */
    @FunctionalInterface
    public interface ActionPerformedListener
    {
        /**
         * Called when the action is performed.
         *
         * @param event the action event
         */
        void actionPerformed(@Nonnull ActionEvent event);
    }
}
