package gov.takx.commons.swing.actionlist;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.Icon;

import java.awt.Color;
import java.util.function.Consumer;

/**
 * A builder for {@link ActionListAction} instances. See that class for more details.
 *
 * @param <T> the action list value type
 * @implNote Unlike many builders this class constructs a single mutable instance of {@link ActionListAction} and the builder
 * methods operate directly on it.
 * @since 5.8
 */
public class ActionListActionBuilder<T>
{
    private final ActionListAction<T> actionListAction;

    ActionListActionBuilder()
    {
        actionListAction = new ActionListAction<>(null, null);
    }

    /**
     * @param name the action name
     * @param icon the action icon
     * @param valueConsumer a consumer that accepts the action value
     * @return this (for chaining)
     */
    @Nonnull
    public ActionListActionBuilder<T> nameIconValueConsumer(@Nullable String name, @Nullable Icon icon, @Nonnull Consumer<T> valueConsumer)
    {
        name(name);
        icon(icon);
        valueConsumer(valueConsumer);
        return this;
    }

    /**
     * @param name the action name
     * @param icon the action icon
     * @param eventConsumer a consumer that accepts the action event
     * @return this (for chaining)
     */
    @Nonnull
    public ActionListActionBuilder<T> nameIconEventConsumer(@Nullable String name, @Nullable Icon icon, @Nonnull Consumer<ActionListActionEvent<T>> eventConsumer)
    {
        name(name);
        icon(icon);
        eventConsumer(eventConsumer);
        return this;
    }

    /**
     * @param name the action name when selected is {@code true}
     * @param icon the action icon when selected is {@code true}
     * @param color the background color when selected is {@code true}
     * @return this (for chaining)
     */
    @Nonnull
    public ActionListActionBuilder<T> selectedNameIconBackground(@Nullable String name, @Nullable Icon icon, @Nullable Color color)
    {
        selectedName(name);
        selectedIcon(icon);
        selectedBackground(color);
        return this;
    }

    /**
     * @param name the action name
     * @return this (for chaining)
     */
    @Nonnull
    public ActionListActionBuilder<T> name(@Nullable String name)
    {
        actionListAction.setName(name);
        return this;
    }

    /**
     * @param icon the action icon
     * @return this (for chaining)
     */
    @Nonnull
    public ActionListActionBuilder<T> icon(@Nullable Icon icon)
    {
        actionListAction.setIcon(icon);
        return this;
    }

    /**
     * @param name the action name when selected is {@code true}
     * @return this (for chaining)
     */
    @Nonnull
    public ActionListActionBuilder<T> selectedName(@Nullable String name)
    {
        actionListAction.setSelectedName(name);
        return this;
    }

    /**
     * @param icon the action icon when selected is {@code true}
     * @return this (for chaining)
     */
    @Nonnull
    public ActionListActionBuilder<T> selectedIcon(@Nullable Icon icon)
    {
        actionListAction.setSelectedIcon(icon);
        return this;
    }

    /**
     * @param background the background color when selected is {@code true}
     * @return this (for chaining)
     */
    @Nonnull
    public ActionListActionBuilder<T> selectedBackground(@Nullable Color background)
    {
        actionListAction.setSelectedBackground(background);
        return this;
    }

    /**
     * @param configurer the button configurer
     * @return this (for chaining)
     */
    @Nonnull
    public ActionListActionBuilder<T> buttonConfigurer(@Nonnull ActionListButtonConfigurer<T> configurer)
    {
        actionListAction.setButtonConfigurer(configurer);
        return this;
    }

    /**
     * @param valueConsumer a consumer that accepts the action list value
     * @return this (for chaining)
     */
    @Nonnull
    public ActionListActionBuilder<T> valueConsumer(@Nonnull Consumer<T> valueConsumer)
    {
        //noinspection unchecked
        actionListAction.setActionPerformedListener(event -> valueConsumer.accept(((ActionListActionEvent<T>) event).getValue()));
        return this;
    }

    /**
     * @param eventConsumer the consumer that accepts the action event when the action is triggered
     * @return this (for chaining)
     */
    @Nonnull
    public ActionListActionBuilder<T> eventConsumer(@Nonnull Consumer<ActionListActionEvent<T>> eventConsumer)
    {
        //noinspection unchecked
        actionListAction.setActionPerformedListener(event -> eventConsumer.accept((ActionListActionEvent<T>) event));
        return this;
    }

    /**
     * @return the built instance
     */
    @Nonnull
    public ActionListAction<T> build()
    {
        return actionListAction;
    }
}
