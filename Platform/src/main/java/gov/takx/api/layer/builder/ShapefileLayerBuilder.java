package gov.takx.api.layer.builder;

import com.google.auto.value.AutoBuilder;
import gov.takx.api.layer.LayerConfiguration;
import gov.takx.api.layer.LayerConfiguration.LayerClassType;
import gov.takx.api.layer.LayerGroup;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;

/**
 * A builder to construct a {@link LayerConfiguration} for adding shapefile layers to the globe.
 *
 * @since 5.4
 */
@AutoBuilder(ofClass = ShapefileLayerBuilder.class, callMethod = "build")
public abstract class ShapefileLayerBuilder extends LayerBuilder<ShapefileLayerBuilder>
{
    /**
     * Construct a {@link ShapefileLayerBuilder}.
     * @return the builder
     */
    public static ShapefileLayerBuilder builder()
    {
        return LayerBuilder.setOptionals(new AutoBuilder_ShapefileLayerBuilder());
    }

    static LayerConfiguration build(@Nonnull String name, @Nullable String displayName, boolean visible, @Nonnull File file)
    {
        return new LayerConfiguration(name, displayName, LayerClassType.GenericLayer, visible, false, null,
                LayerGroup.SHAPE_FILES, null, file.getAbsolutePath(), false, null);
    }

    /**
     * Set a {@link File} that points to the shapefile {@code .shp} file.
     * @param file the path to the shapefile {@code .shp} file
     * @return the builder for chaining
     */
    public abstract ShapefileLayerBuilder setFile(@Nonnull File file);

    /**
     * Build the layer configuration.
     * @return the layer configuration
     */
    public abstract LayerConfiguration build();
}
