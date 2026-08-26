package gov.takx.api.layer.builder;

import com.atakmap.map.layer.Layer;
import com.google.auto.value.AutoBuilder;
import gov.takx.api.layer.LayerConfiguration;
import gov.takx.api.layer.LayerConfiguration.LayerClassType;
import gov.takx.api.layer.LayerGroup;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * A builder to construct a {@link LayerConfiguration} for adding a preconstructed TAK {@link Layer} to the globe.
 *
 * @since 5.4
 */
@AutoBuilder(ofClass = GenericLayerBuilder.class, callMethod = "build")
public abstract class GenericLayerBuilder extends LayerBuilder<GenericLayerBuilder>
{
    /**
     * Construct a {@link GenericLayerBuilder}.
     * @return the builder
     */
    public static GenericLayerBuilder builder()
    {
        return LayerBuilder.setOptionals(new AutoBuilder_GenericLayerBuilder());
    }

    static LayerConfiguration build(@Nullable String name, @Nullable String displayName, boolean visible, @Nonnull Layer layer, @Nonnull LayerGroup layerGroup)
    {
        String layerName = name != null ? name : layer.getName();
        return new LayerConfiguration(layerName, displayName, LayerClassType.GenericLayer, visible, false, "resources/globe.png",
                layerGroup, null, null, false, layer);
    }

    /**
     * Set the unique layer name for layer identification or null to use the preconstructed TAK {@link Layer#getName() layer name}.
     * @param name the unique layer name, or {@code null} to use the precontructed TAK layer's name.
     * @return the builder for chaining
     */
    @Override
    public abstract GenericLayerBuilder setName(@Nullable String name);

    /**
     * Set the preconstructed TAK {@link Layer} to add to the globe.
     * @param layer the TAK layer to add
     * @return the builder for chaining
     */
    public abstract GenericLayerBuilder setLayer(@Nonnull Layer layer);

    /**
     * Set the {@link LayerGroup} to add the layer to.
     * @param layerGroup the layer group
     * @return the builder for chaining
     */
    public abstract GenericLayerBuilder setLayerGroup(@Nonnull LayerGroup layerGroup);

    /**
     * Build the layer configuration.
     * @return the layer configuration
     */
    public abstract LayerConfiguration build();
}
