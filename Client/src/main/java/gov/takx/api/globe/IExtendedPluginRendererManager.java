package gov.takx.api.globe;

import java.util.List;

/**
 * Contract for extended renderer manager. Used to create/manage renderer instances for the globe.
 *
 * @since 2.29
 */
public interface IExtendedPluginRendererManager extends IPluginRendererManager
{
    /**
     * Construct all the registered plugin renderers for the given globe.
     *
     * @param globe Globe for which to create renderers
     * @return List of renderer instances
     */
    List<IRenderingEngine> constructRenderers(IApplicationGlobe globe);

    /**
     * Shutdown all the renderers constructed for the given globe.
     *
     * @param globe Globe for which to shutdown renderers
     */
    void shutdownRenderers(IApplicationGlobe globe);
}
