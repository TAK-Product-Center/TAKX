package gov.takx.commons.swing.actionlist;

import gov.tak.api.annotation.IncubatingApi;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.Icon;

import java.awt.Color;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Extension of {@link ActionListAction} that triggers the renderer and editor to treat the button as a toggle. It can use
 * separate icons and text when selected vs unselected. A custom function is used to determine whether a given cell value
 * is selected or unselected at render time.
 * 
 * @param <T> the value type that this action expects (should match the value type in {@link ActionList})
 * @since 5.7
 */
@IncubatingApi(since = "5.7")
public class ActionListToggleAction<T> extends ActionListAction<T>
{
    protected Icon selectedIcon;
    protected String selectedName;
    protected Color selectedBackground;
    protected Function<T, Boolean> selectedFunction;

    /**
     * 
     * @param name the action name text
     * @param unselectedIcon the icon to use when unselected
     * @param selectedFunction the function to determine the selected state
     */
    public ActionListToggleAction(@Nullable String name, @Nullable Icon unselectedIcon, @Nonnull Function<T, Boolean> selectedFunction)
    {
        super(name, unselectedIcon);
        this.selectedFunction = selectedFunction;
    }

    /**
     * @return the icon used when selected
     */
    @Nullable
    public Icon getSelectedIcon()
    {
        return selectedIcon;
    }

    /**
     * @param selectedIcon the icon to use when selected
     * @return this  (for chaining)
     */
    @SuppressWarnings("unused")
    @Nonnull
    public ActionListToggleAction<T> withSelectedIcon(@Nullable Icon selectedIcon)
    {
        this.selectedIcon = selectedIcon;
        return this;
    }

    /**
     * @return the name used when selected
     */
    @Nullable
    public String getSelectedName()
    {
        return selectedName;
    }

    /**
     * @param selectedName the name to use when selected
     * @return this (for chaining)
     */
    @Nonnull
    public ActionListToggleAction<T> withSelectedName(@Nullable String selectedName)
    {
        this.selectedName = selectedName;
        return this;
    }

    /**
     * @return the function used to determine selected state
     */
    @Nonnull
    public Function<T, Boolean> getSelectedFunction()
    {
        return selectedFunction;
    }

    /**
     * @return the background color to use when the toggle button is selected
     */
    @Nullable
    public Color getSelectedBackground()
    {
        return selectedBackground;
    }

    /**
     * @param selectedBackground the background color to use when selected
     * @return this (for chaining)
     */
    @SuppressWarnings("unused")
    @Nonnull
    public ActionListToggleAction<T> withSelectedBackground(@Nullable Color selectedBackground)
    {
        this.selectedBackground = selectedBackground;
        return this;
    }

    /**
     * @param selectedIcon the icon to use when selected
     * @param selectedBackground the background color to use when selected
     * @return this (for chaining)
     */
    @Nonnull
    public ActionListToggleAction<T> withSelectedIconAndBackground(@Nullable Icon selectedIcon, @Nullable Color selectedBackground)
    {
        this.selectedIcon = selectedIcon;
        this.selectedBackground = selectedBackground;
        return this;
    }

    @Nonnull
    @Override
    public ActionListToggleAction<T> withButtonConfigurer(@Nonnull ActionListButtonConfigurer<T> buttonConfigurer)
    {
        return (ActionListToggleAction<T>) super.withButtonConfigurer(buttonConfigurer);
    }

    @Nonnull
    @Override
    public ActionListToggleAction<T> withValueConsumer(@Nonnull Consumer<T> valueConsumer)
    {
        return (ActionListToggleAction<T>) super.withValueConsumer(valueConsumer);
    }

    @Nonnull
    @Override
    public ActionListToggleAction<T> withEventConsumer(@Nonnull Consumer<ActionListActionEvent<T>> eventConsumer)
    {
        return (ActionListToggleAction<T>) super.withEventConsumer(eventConsumer);
    }
}
