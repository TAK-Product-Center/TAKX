package gov.takx.api.globe;

import gov.takx.api.annotation.Experimental;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.event.PlatformEvent;
import gov.takx.api.mapobject.IMapObject;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * A manager for adding and removing TME widgets and their respective factories
 * @since 5.0
 */
@InjectableService
@NoPluginImplementations
public interface IWidgetLayerManager
{
    /**
     * Event fired when a widget instance is added to a globe.
     *
     * @param widgetInstance The instance that was added
     * @param layoutPosition The position at which it was added
     * @since 5.4
     */
    record WidgetInstanceAdded(@Nonnull IWidgetInstance widgetInstance, @Nonnull WidgetLayoutPosition layoutPosition) implements PlatformEvent
    {
    }

    /**
     * Event fired when a widget instance is removed from a globe.
     * @since 5.4
     */
    record WidgetInstanceRemoved(@Nonnull IWidgetInstance widgetInstance) implements PlatformEvent
    {
    }

    /**
     * Event fired when the visibility of a widget has changed.
     * @since 5.4
     */
    record WidgetVisibilityChanged(@Nonnull IWidgetInstance widgetInstance) implements PlatformEvent
    {
    }

    /**
     * Add an {@link IWidgetFactory} that adds widgets to all globes.
     * @param widgetFactory The widget factory to add
     * @param layoutPosition the {@link WidgetLayoutPosition} position to add the widget to
     * @since 5.3
     */
    void addWidgetFactory(@Nonnull IWidgetFactory widgetFactory, @Nonnull WidgetLayoutPosition layoutPosition);

    /**
     * Remove a widget factory.
     * @param widgetFactory The widget factory to remove
     */
    void removeWidgetFactory(@Nonnull IWidgetFactory widgetFactory);

    /**
     * Remove a widget from all globes.
     * @param widget The widget to remove
     */
    void removeWidget(@Nonnull IWidget widget);

    /**
     * Remove all widgets of the specified class from all globes.
     * @param clazz the widget class to remove
     * @since 5.1
     */
    void removeWidgets(@Nonnull Class<? extends IWidget> clazz);

    /**
     * Remove all widgets with the specified name from all globes.
     * @param name Name of widget to remove
     * @since 5.5
     */
    void removeWidgetsByName(@Nonnull String name);

    /**
     * @return a list of all widget instances across all globes
     * @since 5.4
     */
    @Nonnull
    List<IWidgetInstance> getAllWidgetInstances();

    /**
     * Get a list of all widget instances with the given {@code name}, across all globes.
     *
     * @param name Name of widgets to find
     * @return List of widgets with that name, may be empty
     * @see CoreWidgetNames
     * @since 5.4
     */
    @Nonnull
    List<IWidgetInstance> getWidgetInstancesByName(@Nonnull String name);

    /**
     * Get a list of all widget instances on the given {@code globe}.
     *
     * @param globe The globe of interest
     * @return List of widgets on that globe, may be empty
     * @since 5.4
     */
    @Nonnull
    List<IWidgetInstance> getWidgetInstancesByGlobe(@Nonnull IGlobeDelegate globe);

    /**
     * Add a listener to receive notifications on open/close of the radial menu.
     *
     * @param listener Listener to add
     * @since 5.6
     */
    @Experimental
    void addRadialMenuListener(@Nonnull IRadialMenuListener listener);

    /**
     * Remove a previously registered listener.
     *
     * @param listener Listener to remove
     * @since 5.6
     */
    @Experimental
    void removeRadialMenuListener(@Nonnull IRadialMenuListener listener);

    /**
     * Returns all widgets at a {@link WidgetLayoutPosition position} on the specified {@link IGlobeDelegate globe}.
     *
     * @param globe The globe to check
     * @param layoutPosition The position on the globe to check
     * @return The widgets at the specified position
     * @since 5.8
     */
    @Experimental
    @Nonnull
    List<IWidgetInstance> getWidgetsAtPosition(@Nonnull IGlobeDelegate globe, @Nonnull WidgetLayoutPosition layoutPosition);

    /**
     * Contract for notifications of radial menu open/close/
     * @since 5.6
     */
    @Experimental
    interface IRadialMenuListener
    {
        /**
         * Called after the radial menu is opened.
         * @param mapObject The map object for which the menu was opened, {@code null} if none (when the terrain popup
         *                  menu is shown)
         */
        void onMenuOpened(@Nullable IMapObject mapObject);

        /**
         * Called after the radial menu is closed.
         */
        void onMenuClosed();
    }

    /**
     * A factory for creating widgets
     */
    @FunctionalInterface
    interface IWidgetFactory
    {
        /**
         * Create a widget for the provided {@link IGlobeDelegate} or return {@code null} to decline adding a widget to the globe.
         * @param globeDelegate the globe delegate
         * @return a widget to be added to the globe or {@code null}
         */
        @Nullable
        IWidget create(IGlobeDelegate globeDelegate);
    }
}
