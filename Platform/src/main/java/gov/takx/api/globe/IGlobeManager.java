package gov.takx.api.globe;

import gov.nasa.worldwind.WorldWindow;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.platform.preferences.PreferenceEntry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Manages the set of globe instances, including creation and deletion.
 *
 * @since 1.49
 */
@InjectableService
@NoPluginImplementations
public interface IGlobeManager
{
    /**
     * By default, TAKX will prevent any WorldWind object that is <i>not</i> an {@link IMapObject} from being
     * draggable. To override this behavior to allow an object to be draggable, add this key to the AVList's set of
     * values with a value of {@code true}:
     * <pre>
     *     BasicMarker marker = new BasicMarker(position, attributes);
     *     marker.setValue(IGlobeManager.IS_DRAGGABLE_KEY, true);
     * </pre>
     *
     * @since 1.16 (only exposed externally since 2.31)
     */
    String IS_DRAGGABLE_KEY = "avkey.isDraggable";

    /**
     * @return True if {@link PreferenceEntry#GLOBE_ENABLE_MULTIPLE_GLOBES} allows additional globes to be created. If
     * this is false, any calls to methods that create a globe or globe dockable will fail after the primary globe
     * has been created.
     * @since 3.4
     */
    boolean isAdditionalGlobeCreationEnabled();

    /**
     * Create a globe that isn't added to the UI by default using the next available globe id.
     *
     * @return A reference to the globe's delegate.
     * @throws IllegalStateException if another globe is being created before the primary globe is initialized or
     * multiple globes are disabled in the preferences.
     * @see #isAdditionalGlobeCreationEnabled()
     */
    @Nonnull
    IGlobeDelegate createGlobe() throws IllegalStateException;

    /**
     * Create a globe that isn't added to the UI by default.
     *
     * @param globeId The globe's id. If it's already used, an exception will be thrown.
     * @return A reference to the globe's delegate.
     * @throws IllegalStateException if another globe is being created before the primary globe is initialized, multiple
     * globes are disabled in the preferences, or a globe already exists with the provided id.
     * @see #isAdditionalGlobeCreationEnabled()
     */
    @Nonnull
    IGlobeDelegate createGlobe(int globeId) throws IllegalStateException;

    /**
     * Create a new globe that is added to the docking framework.
     *
     * @return A reference to the globe's delegate.
     * @throws IllegalStateException if another globe is being created before the primary globe is initialized or
     * multiple globes are disabled in the preferences.
     * @see #isAdditionalGlobeCreationEnabled()
     */
    @Nonnull
    IGlobeDelegate createGlobeDockable() throws IllegalStateException;

    /**
     * Create a new globe that is added to the docking framework.
     *
     * @param title The globe's name.
     * @return A reference to the globe's delegate.
     * @throws IllegalStateException if another globe is being created before the primary globe is initialized or
     * multiple globes are disabled in the preferences.
     * @see #isAdditionalGlobeCreationEnabled()
     */
    @Nonnull
    IGlobeDelegate createGlobeDockable(String title) throws IllegalStateException;

    /**
     * Create a new globe that is added to the docking framework.
     *
     * @param globeId The globe's id. If it's already used, an exception will be thrown.
     * @return A reference to the globe's delegate.
     * @throws IllegalStateException if another globe is being created before the primary globe is initialized, multiple
     * globes are disabled in the preferences, or a globe already exists with the provided id.
     * @see #isAdditionalGlobeCreationEnabled()
     */
    @Nonnull
    IGlobeDelegate createGlobeDockable(int globeId) throws IllegalStateException;

    /**
     * Create a new globe that is added to the docking framework.
     *
     * @param globeId The globe's id. If it's already used, an exception will be thrown.
     * @param title   The globe's name.
     * @return A reference to the globe's delegate.
     * @throws IllegalStateException if another globe is being created before the primary globe is initialized, multiple
     * globes are disabled in the preferences, or a globe already exists with the provided id.
     * @see #isAdditionalGlobeCreationEnabled()
     */
    @Nonnull
    IGlobeDelegate createGlobeDockable(int globeId, String title) throws IllegalStateException;

    /**
     * Get all globe ids, including that of the primary globe, sorted in numerical order.
     *
     * @return All globe ids.
     * @see #getPrimaryGlobeId()
     */
    @Nonnull
    Set<Integer> getGlobeIds();

    /**
     * Get the number of globes that have been created.  The globes will have Ids of 1 - N inclusive, where N is
     * the value returned by this method.  Note that not all globe ids in this range will refer to an extant
     * globe. Globes can be removed, see {@link #deleteGlobe(int)}.
     *
     * @return Number of globes in this project session
     * @see #getGlobeIds()
     */
    int getGlobeCount();

    /**
     * Get the id for the primary globe.
     *
     * @return The id for the primary globe.
     */
    int getPrimaryGlobeId();

    /**
     * Get the delegate for a specific globe.
     *
     * @param globeId The globe id.
     * @return The globe delegate if a globe with {@code globeId} exists, {@code null} otherwise.
     */
    @Nullable
    IGlobeDelegate getGlobeDelegate(int globeId);

    /**
     * Return a list of all available globe delegates.
     *
     * @return A list of all available globe delegates.
     */
    @Nonnull
    List<? extends IGlobeDelegate> getAllGlobeDelegates();

    /**
     * Return the primary globe's delegate.
     *
     * @return The primary globe's delegate.
     */
    @Nonnull
    IGlobeDelegate getPrimaryGlobeDelegate();

    /**
     * Given a WorldWind component, locate the globe that contains it.  This is useful for dealing with mouse events
     * that occur on the WW component, and you need to find the corresponding globe (say for a popup menu).
     *
     * @param wwd The WW component of interest
     * @return The globe containing the given WW component, {@code null} if not found
     */
    @Nullable
    IGlobeDelegate getGlobeForComponent(@Nonnull WorldWindow wwd);

    /**
     * Delete the globe with the specified id. If the id belongs to the primary globe or if no globe is associated with
     * the id, this will be a no-op.
     *
     * @param globeId The id of the globe that should be deleted.
     */
    void deleteGlobe(int globeId);

    /**
     * Get the last globe to have received the focus.  This is useful when you need to direct operations at
     * a specific globe (like navigation).
     *
     * @return Last globe to have received focus; will only be {@code null} if the globe service is not started.
     */
    @Nullable
    IGlobeDelegate getLastFocusedGlobe();

    /**
     * @return The last focused globe or throw an exception
     * @throws NullPointerException if the last focused globe is {@code null}
     * @since 5.7
     */
    @Nonnull
    default IGlobeDelegate requireLastFocusedGlobe()
    {
        return Objects.requireNonNull(getLastFocusedGlobe(), "last focused globe is null");
    }

    /**
     * Add a listener for globe lifecycle events.  If the given listener is already registered, it will not be added a
     * second time.
     *
     * @param listener Listener to register
     */
    void addGlobeLifeCycleListener(@Nonnull IGlobeLifeCycleListener listener);

    /**
     * Remove a listener for globe lifecycle events.
     *
     * @param listener Listener to unregister
     */
    void removeGlobeLifeCycleListener(@Nonnull IGlobeLifeCycleListener listener);

    /**
     * Specify a handler to be executed when each globe component is ready for use. As new globes are created,
     * and become ready for use, this handler will be invoked.  For all globes already in existence that are ready,
     * the handler will be invoked immediately.
     * <p>
     * Invocation of the handler is always made on the EDT.
     *
     * @param handler The handler to invoke if/when the globe component becomes ready
     * @since 2.23
     */
    void doWhenGlobeReady(@Nonnull Consumer<IGlobeDelegate> handler);

    /**
     * Request to redraw all the globes.  This request will be queued and coalesced with any other redraw requests
     * that come in within redraw time window (currently .3 seconds).
     *
     * @param reason Reason for requesting the redraw
     * @since 2.29
     */
    default void requestRedraw(@Nonnull String reason)
    {
        requestRedraw(reason, false);
    }

    /**
     * Request to redraw all the globes.
     * <p/>
     * Note: passing {@code true} for {@code immediate} can cause significant slowdown if called too frequently.
     * It should only be used for interactions where the user must see the visible changes immediately.
     * Use with caution.
     *
     * @param reason Reason for requesting the redraw
     * @param immediate Pass {@code true} to have the redraw happen immediately, {@code false} to queue the request
     * @since 5.3
     */
    void requestRedraw(@Nonnull String reason, boolean immediate);

    /**
     * Show the given message at the top of all globes.
     *
     * @param message Message to display
     * @since 5.3
     */
    default void showGlobeMessage(@Nonnull String message)
    {
        showGlobeMessage(message, null);
    }

    /**
     * Show the given message at the top of the specified globe.
     *
     * @param message Message to display
     * @param globe Globe on which to display, pass {@code null} to show the message on all globes
     * @since 5.3
     */
    void showGlobeMessage(@Nonnull String message, @Nullable IGlobeDelegate globe);

    /**
     * Hide any globe message being displayed on all globes.
     * @since 5.3
     */
    default void hideGlobeMessage()
    {
        hideGlobeMessage(null);
    }

    /**
     * Hide any globe message being displayed on the specified globe.
     *
     * @param globe Globe on which to hide a displayed message, pass {@code null} to hide on all globes
     * @since 5.3
     */
    void hideGlobeMessage(@Nullable IGlobeDelegate globe);
}
