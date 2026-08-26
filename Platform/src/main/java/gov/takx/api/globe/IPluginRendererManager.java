package gov.takx.api.globe;

import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.mapobject.IMapObject;

import java.util.Set;

/**
 * Contract for service responsible for managing all renderers defined within plugin deployments.
 * <p>
 * Plugin supplied renderers can supply a list/pattern of Map Object keys and message types that are to be
 * exclusively handled by that renderer, meaning that all <em>other</em> renderers should avoid processing any
 * data from those Map Object types or message types.
 * <p>
 * Renderers can use {@link #isRenderingExcluded(Class, String)} to determine if they should ignore a given RDM by
 * message type, or {@link #isRenderingExcluded(Class, IMapObject)} to determine if they should ignore a given
 * Map Object.
 *
 * @since 2.29
 */
@InjectableService
@NoPluginImplementations
public interface IPluginRendererManager
{
    /**
     * Get the renderer of the given type on the given globe.
     *
     * @param globeDelegate Globe to search for renderer
     * @param rendererClass Class of renderer to locate
     * @param <T>           Inferred renderer type
     * @return Renderer or {@code null} if not found
     */
    <T extends IRenderingEngine> T getRendererForGlobe(IGlobeDelegate globeDelegate, Class<T> rendererClass);

    /**
     * Determine if the given message type has been marked as "exclusive" to a renderer <em>other</em>
     * than the requester.
     *
     * @param rendererClass The class indicating which renderer is making the request
     * @param messageType   The message type of interest
     * @return {@code true} this the given message type is marked as exclusive to another renderer
     */
    boolean isRenderingExcluded(Class<? extends IRenderingEngine> rendererClass, String messageType);

    /**
     * Determine if the given map object has been marked as "exclusive" to a renderer <em>other</em>
     * than the requester.
     *
     * @param rendererClass The class indicating which renderer is making the request
     * @param mapObject     The map object of interest
     * @return {@code true} this the given map object is marked as exclusive to another renderer
     */
    boolean isRenderingExcluded(Class<? extends IRenderingEngine> rendererClass, IMapObject mapObject);

    /**
     * Get the set of message types that are excluded from the given renderer type.
     *
     * @param rendererClass The class indicating which renderer is making the request
     * @return set of message types that are marked as excluded
     */
    Set<String> getExcludedMessageTypes(Class<? extends IRenderingEngine> rendererClass);
}
