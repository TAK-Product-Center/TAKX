package gov.takx.commons.swing.actionlist;

import gov.tak.api.annotation.IncubatingApi;
import gov.takx.commons.swing.SimpleAction;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.Icon;

import java.util.function.Consumer;

/**
 * Action class that holds extended information related to actions within {@link ActionListTableCellRenderer}. While any
 * Action instance will work, using this class, or subclasses, allows further configuration of the buttons and interactions
 * with them.
 * <p>
 * @apiNote This class and subclasses are admittedly flawed. They mix model information (the action instance and
 * listener) with rendering information (the icons, text, button properties, etc.), which is usually defined separately for
 * swing tables and models. However, given the overhead introduced by separating them, and the current lack of a critical
 * reason to do so (e.g. column value sorting), I struggle to find justification for separating their configurations. I can't
 * rule it out completely and more thought should be given to it before declaring this API stable.
 *
 * @param <T> the value type that this action expects (should match the value type in {@link ActionList})
 * @since 5.7
 */
@IncubatingApi(since = "5.7")
public class ActionListAction<T> extends SimpleAction
{
    protected ActionListButtonConfigurer<T> buttonConfigurer;

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
     * @return the configurer used for configuring buttons in this action list
     */
    @Nullable
    public ActionListButtonConfigurer<T> getButtonConfigurer()
    {
        return buttonConfigurer;
    }

    /**
     * Specifies a configurer to process buttons created for actions in an action list.  Note this is meant for
     * one-time use when the buttons are initially created.  It is not intended for use with dynamic state that may
     * change after the buttons are created.
     * <p/>
     * For handling dynamic state, make appropriate modifications to this Action, such as calling {@link #setEnabled(boolean)},
     * prior to returning the list from {@code CommonEntityDefinition#getActions(Object)}.  You will also need to
     * add a property change listener to force a recalculation of the action list, something like this within your
     * {@link gov.takx.commons.swing.entity.EntityDefinition} constructor:
     * <pre>{@code
     * addPropertyChangeMapping(ShapeProperties.EDITABLE, getActionsColumn());
     * }</pre>
     *
     * @param buttonConfigurer the configurer to use
     * @return this (for chaining)
     */
    @Nonnull
    public ActionListAction<T> withButtonConfigurer(@Nonnull ActionListButtonConfigurer<T> buttonConfigurer)
    {
        this.buttonConfigurer = buttonConfigurer;
        return this;
    }

    /**
     * @param valueConsumer the consumer that accepts the action list value field when the action is triggered
     * @return this (for chaining)
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    public ActionListAction<T> withValueConsumer(@Nonnull Consumer<T> valueConsumer)
    {
        setActionPerformedListener(event -> valueConsumer.accept(((ActionListActionEvent<T>) event).getValue()));
        return this;
    }

    /**
     * @param eventConsumer the consumer that accepts the action event when the action is triggered
     * @return this (for chaining)
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    public ActionListAction<T> withEventConsumer(@Nonnull Consumer<ActionListActionEvent<T>> eventConsumer)
    {
        setActionPerformedListener(event -> eventConsumer.accept((ActionListActionEvent<T>) event));
        return this;
    }
}
