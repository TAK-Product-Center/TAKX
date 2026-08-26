package gov.takx.api.layer;

import gov.tak.api.annotation.DeprecatedApi;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.io.InputStream;

/**
 * Contract for the MOBAC layer manager, supporting the creation of layers from MOBAC configuration XML.
 *
 * @since 3.1
 * @deprecated Use {@link gov.takx.api.layer.builder.OnlineImageryLayerBuilder} and {@link ILayerService}
 */
@InjectableService
@NoPluginImplementations
@Deprecated(since = "5.4", forRemoval = true)
@DeprecatedApi(since = "5.4", forRemoval = true, removeAt = "5.7")
public interface IMobacLayerManager
{
    /**
     * Test if a file appears to be a map source XML config file.
     *
     * @param file File to test
     * @return {@code true} if the file contains the custom map source XML element
     */
    boolean isMapSourceXml(@Nonnull File file);

    /**
     * Test if a string appears to be a map source XML configuration.
     *
     * @param string String to test
     * @return {@code true} if the string contains the custom map source XML element
     * @since 5.0
     */
    boolean isMapSourceXml(@Nonnull String string);

    /**
     * Test if the content of an input stream appears to be a map source XML configuration.
     *
     * @param inputStream Input stream to test
     * @return {@code true} if the stream contains the custom map source XML element
     */
    boolean isMapSourceXml(@Nonnull InputStream inputStream);

    /**
     * Construct a layer from the given XML configuration file.  The name of the layer constructed will be the
     * base name of the given {@code configFile}.
     *
     * @param configFile XML configuration file to process
     */
    default void makeLayerFromFile(@Nonnull File configFile)
    {
        createLayerFromFile(configFile);
    }

    /**
     * Construct a layer from the given XML configuration.  By default, the layers created with this method will
     * not be initially visible.
     *
     * @param layerName Name to use for layer
     * @param configXml XML configuration to process
     */
    default void makeLayerFromConfigXml(@Nonnull String layerName, @Nonnull String configXml)
    {
        createLayerFromConfigXml(layerName, configXml);
    }

    /**
     * Construct a layer from the given XML configuration file.  The name of the layer constructed will be the
     * base name of the given {@code configFile}.
     *
     * @param configFile XML configuration file to process
     * @return the raptor layer, or {@code null} if it could not be created
     * @since 5.3
     */
    @Nullable
    IRaptorLayer createLayerFromFile(@Nonnull File configFile);

    /**
     * Construct a layer from the given XML configuration.  By default, the layers created with this method will
     * not be initially visible.
     *
     * @param layerName Name to use for layer
     * @param configXml XML configuration to process
     * @return the raptor layer, or {@code null} if it could not be created
     * @since 5.3
     */
    @Nullable
    IRaptorLayer createLayerFromConfigXml(@Nonnull String layerName, @Nonnull String configXml);
}
