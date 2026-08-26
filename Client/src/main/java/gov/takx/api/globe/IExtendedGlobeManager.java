package gov.takx.api.globe;

import gov.nasa.worldwind.WorldWindow;
import gov.takx.api.mapobject.IMapObject;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.Cursor;
import java.util.List;

/**
 * Adds additional functionality to the {@link IGlobeManager} available in the public API.
 *
 * @since 1.49
 */
public interface IExtendedGlobeManager extends IGlobeManager
{
    /**
     * Set the globe that last had focus.
     *
     * @param globe The last globe to have focus.
     */
    void setLastFocusedGlobe(IApplicationGlobe globe);

    /**
     * Stop all tracking operations (center on and keep in view) on all globes.
     *
     * @param mapObject The map object in question
     */
    void stopTracking(IMapObject mapObject);

    /**
     * Set the cursor across all extant globes.
     *
     * @param cursor Cursor to install
     */
    void setCursorOnAllGlobes(Cursor cursor);

    /**
     * Get the globe with the specified id, or create it if it does not exist.
     *
     * @param globeId The globe id.
     * @return The new or existing globe associated with the specified globe id.
     * @throws IllegalStateException if another globe is being created before the primary globe is initialized, multiple
     * globes are disabled in the preferences, or a globe already exists with the provided id.
     * @see #isAdditionalGlobeCreationEnabled()
     */
    @Nonnull
    IApplicationGlobe getOrCreateGlobe(int globeId) throws IllegalStateException;

    /**
     * Overridden to return the more derived type.
     * <p/>
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    IApplicationGlobe createGlobe() throws IllegalStateException;

    /**
     * Overridden to return the more derived type.
     * <p/>
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    IApplicationGlobe createGlobe(int globeId) throws IllegalStateException;

    /**
     * Overridden to return the more derived type.
     * <p/>
     * {@inheritDoc}
     */
    @Nullable
    @Override
    IApplicationGlobe getGlobeForComponent(@Nonnull WorldWindow wwd);

    /**
     * Overridden to return the more derived type.
     * <p/>
     * {@inheritDoc}
     */
    @Nullable
    @Override
    IApplicationGlobe getGlobeDelegate(int globeId);

    /**
     * Overridden to return the more derived type.
     * <p/>
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    List<IApplicationGlobe> getAllGlobeDelegates();

    /**
     * Overridden to return the more derived type.
     * <p/>
     * {@inheritDoc}
     */
    @Nullable
    @Override
    IApplicationGlobe getLastFocusedGlobe();

    /**
     * Overridden to return the more derived type.
     * <p/>
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    default IApplicationGlobe requireLastFocusedGlobe()
    {
        return (IApplicationGlobe) IGlobeManager.super.requireLastFocusedGlobe();
    }

    /**
     * Overridden to return the more derived type.
     * <p/>
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    IApplicationGlobe getPrimaryGlobeDelegate();

    /**
     * Return the current set of globe lifecycle listeners.
     *
     * @return The current set of globe lifecycle listeners.
     */
    @Nonnull
    List<IGlobeLifeCycleListener> getGlobeLifeCycleListeners();

    /**
     * Request a repaint on all globes.
     *
     * @since 2.37
     */
    void repaintAllGlobes();

    /**
     * Activate or deactivate the default select listener on all globes.
     *
     * @param active New active state
     * @since 2.37
     */
    void setSelectListenersActive(boolean active);

    /**
     * Determine if the current mouse location is contained within any of our existing
     * globes.  We actually test against the globe's parent to allow for clicking on a border.
     * <p/>
     * The mouse location is obtained as {@code MouseInfo.getPointerInfo().getLocation()}
     *
     * @return {@code true} if the mouse location is within any globe, {@code false} otherwise
     * @since 2.37
     */
    boolean mouseLocationInAnyGlobe();
}
