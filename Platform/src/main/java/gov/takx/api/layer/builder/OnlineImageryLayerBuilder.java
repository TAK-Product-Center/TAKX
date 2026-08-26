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
 * A builder to construct a {@link LayerConfiguration} for adding online imagery layers. Layer parameters are expressed via
 * configuration strings. Supported configurations include:
 * <ul>
 *     <li>MOBAC XML map sources
 *     <ul>
 *         <li>WCTS {@code customMapSource} </li>
 *         <li>WMS {@code customWmsMapSource} </li>
 *         <li>Multi-layer {@code customMultiLayerMapSource}</li>
 *     </ul>
 * </ul>
 *
 * @see <a href="https://mobac.sourceforge.io/wiki/index.php/Custom_XML_Map_Sources">MOBAC wiki</a>
 * @since 5.4
 */
@AutoBuilder(ofClass = OnlineImageryLayerBuilder.class, callMethod = "build")
public abstract class OnlineImageryLayerBuilder extends LayerBuilder<OnlineImageryLayerBuilder>
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Construct a {@link OnlineImageryLayerBuilder}.
     * @return the builder
     */
    public static OnlineImageryLayerBuilder builder()
    {
        return LayerBuilder.setOptionals(new AutoBuilder_OnlineImageryLayerBuilder());
    }

    static LayerConfiguration build(@Nonnull String name, @Nullable String displayName, boolean visible, @Nonnull String configurationXml)
    {
        return new LayerConfiguration(name, displayName, LayerClassType.OnlineImagery, visible, false, null,
                LayerGroup.IMAGERY, null, configurationXml, false, null);
    }

    /**
     * Load the configuration XML for the MOBAC layer from a {@link File}.
     * @param configurationFile the configuration file
     * @return the builder for chaining
     */
    public OnlineImageryLayerBuilder setConfigurationFile(@Nonnull File configurationFile)
    {
        try
        {
            String configurationXml = Files.readString(configurationFile.toPath());
            setConfigurationXml(configurationXml);
        } catch (IOException e)
        {
            logger.error("Error loading MOBAC configuration from {}", configurationFile.getAbsolutePath(), e);
        }
        return this;
    }

    /**
     * Set the configuration XML for the MOBAC layer.
     * @param configurationXml the configuration xml
     * @return the builder for chaining
     */
    public abstract OnlineImageryLayerBuilder setConfigurationXml(@Nonnull String configurationXml);

    /**
     * Build the layer configuration.
     * @return the layer configuration
     */
    public abstract LayerConfiguration build();
}
