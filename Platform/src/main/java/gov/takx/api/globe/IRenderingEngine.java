package gov.takx.api.globe;

import gov.nasa.worldwind.render.DrawContext;
import gov.takx.api.globe.render.RendererConstants;
import gov.takx.api.mapobject.IMapObject;
import jakarta.annotation.Nonnull;

/**
 * Contract for objects that can serve as a rendering engine for the TAKX core.
 *
 * @since 2.29
 */
public interface IRenderingEngine
{
    /**
     * Set state of the view.  If {@code stale} is {@code true}, then the next rendering cycle should invoke
     * {@link #updateView(DrawContext)}.
     *
     * @param viewStale Pass {@code true} if view is stale
     */
    void setViewStale(boolean viewStale);

    /**
     * @return If the view is stale
     */
    boolean isViewStale();

    /**
     * Update the rendering state to match the current view.
     *
     * @param dc The WW draw context
     */
    void updateView(DrawContext dc);

    /**
     * Set the Z Order for rendering the track points of the given map object.  All map objects start out with a
     * default Z ordering of -1 ({@link RendererConstants#NO_Z_ORDER}).  All map objects with NO Z order are rendered first
     * with no guaranteed ordering among them.  All other map objects will have their track points rendered in order of
     * increasing Z order.  The highest Z order is rendered last and ensures that it "draws on top" of the others.
     *
     * @param mapObject The map object to affect
     * @param zorder    The new Z order for the map object, use {@link RendererConstants#NO_Z_ORDER} to clear the Z ordering
     */
    void setZOrder(@Nonnull IMapObject mapObject, int zorder);

    /**
     * Pre-render any specific scene elements (other than map point markers).
     *
     * @param dc WW draw context
     */
    void preRender(DrawContext dc);

    /**
     * Render any specific scene elements (other than map point markers).
     *
     * @param dc WW draw context
     */
    void render(DrawContext dc);

    /**
     * @return {@code true} if the rendering engine supports elevations (true 3D rendering)
     */
    boolean supportsElevation();

    /**
     * Determine if is a map object can have map data (track points).  Each rendering engine may have different
     * ideas on what constitutes data it can render, so this method embodies that decision.
     *
     * @param mapObject Map object to test
     * @return {@code true} if the map object can have map data, {@code false} otherwise
     * @since 2.29
     */
    boolean canHaveMapData(@Nonnull IMapObject mapObject);

    /**
     * Deactivate the renderer, it can discard internal state, but should not flush any primary model components.
     * This is called when the rendering engine is being switched dynamically.
     */
    void deactivate();

    /**
     * Shutdown this rendering engine as part of deleting a globe.  If this is the primary globe being shutdown,
     * then we scan clear any associated, shared rendering model since the project is closing.
     *
     * @param isPrimary {@code true} if this is the primary globe shutting down
     * @since 1.49
     */
    void shutdown(boolean isPrimary);
}
