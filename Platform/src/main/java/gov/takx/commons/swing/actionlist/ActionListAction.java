package gov.takx.commons.swing.actionlist;

import gov.tak.api.annotation.IncubatingApi;
import gov.takx.commons.swing.SimpleAction;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.Icon;

import java.awt.Color;
import java.util.function.Consumer;

/**
 * Action class that holds extended information related to actions within {@link ActionListTableCellRenderer}. Using this
 * class allows further configuration of the buttons and interactions beyond what is available in {@link javax.swing.Action}.
 * This class is mutable and can be constructed directly, or via the {@link ActionListActionBuilder}. The builder exists
 * primarily to allow fluent construction via method chaining and reduce the need for telescoping constructors. However,
 * direct construction is still valid and preferred if an appropriate constructor exists.
 *
 * @param <T> the value type that this action expects (should match the value type in {@link ActionList})
 * @apiNote This class is admittedly flawed. It further mixes model information (the action instance and listener) with
 * rendering information (the icons, text, button properties, etc.), which is usually defined separately for swing tables and
 * models. However, given the overhead introduced by separating them, and the current lack of a critical reason to do so
 * (e.g. column value sorting), I struggle to find justification for separating their configurations. I can't rule it out
 * completely and more thought should be given to it before declaring this API stable.
 * @since 5.7
 */
@IncubatingApi(since = "5.7")
public class ActionListAction<T> extends SimpleAction
{
    private ActionListButtonConfigurer<T> buttonConfigurer;
    private Icon selectedIcon;
    private String selectedName;
    private Color selectedBackground;
    private boolean selected;

    /**
     * Construct a new instance using the given action name and icon.
     *
     * @param name the action name text
     * @param icon the icon
     */
    public ActionListAction(@Nullable String name, @Nullable Icon icon)
    {
        super(name, icon, null);
    }

    /**
     * Construct a new action with a name and icon that triggers the given event consumer when the action is performed.
     * 
     * @param name the action name text
     * @param icon the icon
     * @param eventConsumer the consumer that accepts the action event when the action is triggered
     * @since 5.8
     */
    public ActionListAction(@Nullable String name, @Nullable Icon icon, @Nonnull Consumer<ActionListActionEvent<T>> eventConsumer)
    {
        //noinspection unchecked
        super(name, icon, event -> eventConsumer.accept((ActionListActionEvent<T>) event));
    }

    /**
     * Construct a new action with a name and icon that triggers either the given value or event consumer when the action is
     * performed.
     *
     * @param name the action name text
     * @param icon the icon
     * @param valueConsumer the consumer that accepts the action list value field when the action is triggered
     * @param eventConsumer the consumer that accepts the action event when the action is triggered
     * @implNote The last parameter exists mostly to avoid a signature conflict.
     * @since 5.8
     */
    public ActionListAction(@Nullable String name, @Nullable Icon icon, @Nullable Consumer<T> valueConsumer, @Nullable Consumer<ActionListActionEvent<T>> eventConsumer)
    {
        //noinspection unchecked event is always an ActionListActionEvent
        super(name, icon,
                valueConsumer != null ? event -> valueConsumer.accept(((ActionListActionEvent<T>) event).requireValue())
                        : eventConsumer != null ? event -> eventConsumer.accept((ActionListActionEvent<T>) event)
                        : null);
    }

    /**
     * @return the configurer used for configuring buttons in this action list
     */
    @Nullable
    public ActionListButtonConfigurer<T> getButtonConfigurer()
    {
        return buttonConfigurer;
    }

    /**
     * Specifies a configurer to process buttons created for actions in an action list. Note this is meant for one-time use
     * when the buttons are initially created.  It is not intended for use with dynamic state that may change after the
     * buttons are created.
     * <p>
     * For handling dynamic state, make appropriate modifications to this Action, such as calling
     * {@link #setEnabled(boolean)}, prior to creating the {@link ActionList}.
     *
     * @param buttonConfigurer the configurer to use
     * @since 5.8
     */
    public void setButtonConfigurer(@Nullable ActionListButtonConfigurer<T> buttonConfigurer)
    {
        this.buttonConfigurer = buttonConfigurer;
    }

    /**
     * @return {@code true} if the action should be rendered in selected mode, otherwise {@code false}
     * @since 5.8
     */
    public boolean isSelected()
    {
        return selected;
    }

    /**
     * @param selected {@code true} if the action should be rendered in selected mode, otherwise {@code false}
     * @since 5.8
     */
    public void setSelected(boolean selected)
    {
        this.selected = selected;
    }

    /**
     * @return the name used when the button is selected
     * @since 5.8
     */
    @Nullable
    public String getSelectedName()
    {
        return selectedName;
    }

    /**
     * @param selectedName the name used when the button is selected
     * @since 5.8
     */
    public void setSelectedName(@Nullable String selectedName)
    {
        this.selectedName = selectedName;
    }

    /**
     * @return the background color to use when the button is selected
     * @since 5.8
     */
    @Nullable
    public Color getSelectedBackground()
    {
        return selectedBackground;
    }

    /**
     * @param selectedBackground the background color to use when the button is selected
     * @since 5.8
     */
    public void setSelectedBackground(@Nullable Color selectedBackground)
    {
        this.selectedBackground = selectedBackground;
    }

    /**
     * @return the icon used when the button is selected
     * @since 5.8
     */
    @Nullable
    public Icon getSelectedIcon()
    {
        return selectedIcon;
    }

    /**
     * @param selectedIcon the icon to use when the button is selected
     * @since 5.8
     */
    public void setSelectedIcon(@Nullable Icon selectedIcon)
    {
        this.selectedIcon = selectedIcon;
    }

    /**
     * @return a new builder
     * @param <T> the action list value type
     * @since 5.8
     */
    public static <T> ActionListActionBuilder<T> builder()
    {
        return new ActionListActionBuilder<>();
    }
}
