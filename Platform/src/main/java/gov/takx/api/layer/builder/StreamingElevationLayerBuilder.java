package gov.takx.api.layer.builder;

import com.google.auto.value.AutoBuilder;
import gov.takx.api.layer.LayerConfiguration;
import gov.takx.api.layer.LayerConfiguration.LayerClassType;
import gov.takx.api.layer.LayerGroup;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;

/**
 * A builder to construct a {@link LayerConfiguration} for adding streaming elevation layers to the globe. The configuration JSON should
 * conform to the TAK streaming configuration schema.
 *
 * @since 5.4
 */
@AutoBuilder(ofClass = StreamingElevationLayerBuilder.class, callMethod = "build")
public abstract class StreamingElevationLayerBuilder extends LayerBuilder<StreamingElevationLayerBuilder>
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Construct a {@link StreamingElevationLayerBuilder}.
     * @return the builder
     */
    public static StreamingElevationLayerBuilder builder()
    {
        return LayerBuilder.setOptionals(new AutoBuilder_StreamingElevationLayerBuilder());
    }

    static LayerConfiguration build(@Nonnull String name, @Nullable String displayName, boolean visible, @Nonnull String configurationJson)
    {
        return new LayerConfiguration(name, displayName, LayerClassType.Elevation, visible, false, null,
                LayerGroup.ELEVATION, null, configurationJson, false, null);
    }

    /**
     * Load the configuration JSON for the streaming elevation layer from a {@link File}.
     * @param configurationFile the configuration file
     * @return the builder for chaining
     */
    public StreamingElevationLayerBuilder setConfigurationFile(@Nonnull File configurationFile)
    {
        try
        {
            String configurationJson = Files.readString(configurationFile.toPath());
            setConfigurationJson(configurationJson);
        } catch (IOException e)
        {
            logger.error("Error loading MOBAC configuration from {}", configurationFile.getAbsolutePath(), e);
        }
        return this;
    }

    /**
     * Set the configuration JSON for the streaming elevation layer.
     * @param configurationJson the configuration json
     * @return the builder for chaining
     */
    public abstract StreamingElevationLayerBuilder setConfigurationJson(@Nonnull String configurationJson);

    /**
     * Build the layer configuration.
     * @return the layer configuration
     */
    public abstract LayerConfiguration build();
}
