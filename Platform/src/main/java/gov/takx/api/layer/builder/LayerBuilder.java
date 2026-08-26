package gov.takx.api.layer.builder;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

abstract class LayerBuilder<T>
{
    static <T> T setOptionals(LayerBuilder<T> layerBuilder)
    {
        return layerBuilder.setVisible(true);
    }

    /**
     * Set the unique layer name for layer identification.
     * @param name the unique layer name
     * @return the builder for chaining
     */
    public abstract T setName(@Nonnull String name);

    /**
     * Set the layer's display name for presenting in the UI, or null to use the layer name.
     * @param displayName the layer's display name, or null to use the layer name
     * @return the builder for chaining
     */
    public abstract T setDisplayName(@Nullable String displayName);

    /**
     * Set whether the layer is visible.
     * @param visible {@code true} if the layer is visible, {@code false} if not
     * @return the builder for chaining
     */
    public abstract T setVisible(boolean visible);
}
