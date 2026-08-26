package gov.takx.api.layer;

import gov.tak.api.annotation.DeprecatedApi;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Contract for the WCS elevation layer manager, supporting the creation of elevation layers from WCS configuration XML.
 *
 * @since 5.3
 * @deprecated Use {@link gov.takx.api.layer.builder.WcsElevationLayerBuilder} and {@link ILayerService}
 */
@InjectableService
@NoPluginImplementations
@Deprecated(since = "5.4", forRemoval = true)
@DeprecatedApi(since = "5.4", forRemoval = true, removeAt = "5.7")
public interface IWcsElevationLayerManager
{
    /**
     * Test if a string appears to be a WCS elevation source XML configuration.
     *
     * @param string string to test
     * @return {@code true} if the string contains the custom elevation source XML element
     * @since 5.0
     */
    boolean isMapSourceXml(@Nonnull String string);

    /**
     * Construct a layer from the given WCS elevation source XML configuration.
     *
     * @param layerName name to use for layer
     * @param configXml xml configuration to process
     * @return the raptor layer, or {@code null} if it could not be created
     */
    @Nullable
    IRaptorLayer addLayerFromConfigXml(@Nonnull String layerName, @Nonnull String configXml);

    /**
     * Remove a WCS elevation source layer with the given layer name.
     *
     * @param layerName the layer name
     */
    void removeLayer(@Nonnull String layerName);
}
