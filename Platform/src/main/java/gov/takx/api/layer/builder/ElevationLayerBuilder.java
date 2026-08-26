package gov.takx.api.layer.builder;

import com.google.auto.value.AutoBuilder;
import gov.takx.api.layer.LayerConfiguration;
import gov.takx.api.layer.LayerConfiguration.LayerClassType;
import gov.takx.api.layer.LayerGroup;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;

/**
 * A builder to construct a {@link LayerConfiguration} for adding local elevation layers to the globe. Supported data types include:
 * <ul>
 *     <li>DTED - point to the root directory of the DTED path structure.</li>
 *     <li>SRTM - point to the root directory of the SRTM path structure.</li>
 *     <li>GeoTIFF heightmap - point to the individual {@code .tiff} file.</li>
 * </ul>
 *
 * @since 5.4
 */
@AutoBuilder(ofClass = ElevationLayerBuilder.class, callMethod = "build")
public abstract class ElevationLayerBuilder extends LayerBuilder<ElevationLayerBuilder>
{
    /**
     * Construct a {@link ElevationLayerBuilder}.
     * @return the builder
     */
    public static ElevationLayerBuilder builder()
    {
        return LayerBuilder.setOptionals(new AutoBuilder_ElevationLayerBuilder());
    }

    static LayerConfiguration build(@Nonnull String name, @Nullable String displayName, boolean visible, @Nonnull File file)
    {
        return new LayerConfiguration(name, displayName, LayerClassType.Elevation, visible, false, null,
                LayerGroup.ELEVATION, null, file.getAbsolutePath(), false, null);
    }

    /**
     * Set a {@link File} that points to the elevation data. This can be either a file or a directory.
     * @param file the path to the elevation data
     * @return the builder for chaining
     */
    public abstract ElevationLayerBuilder setFile(@Nonnull File file);

    /**
     * Build the layer configuration.
     * @return the layer configuration
     */
    public abstract LayerConfiguration build();
}
