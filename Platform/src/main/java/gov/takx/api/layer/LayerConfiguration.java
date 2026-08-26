package gov.takx.api.layer;

import com.atakmap.map.layer.Layer;
import gov.tak.api.annotation.DeprecatedApi;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * A record that describes the configuration for a layer.
 * @param name the layer name
 * @param displayName the layer display name, or {@code null}
 * @param layerClassType the layer class type enum, or {@code null}
 * @param visible {@code true} if the layer is visible, {@code false} if not
 * @param pickable {@code true} if the layer is pickable, {@code false} if not
 * @param iconPath the path for an icon to visually represent the layer, or {@code null}
 * @param layerGroup the layer group
 * @param type the layer type description, or {@code null}
 * @param restorableXMLString the restorable data string for the layer, or {@code null}
 * @param permanent {@code true} if the layer is permanent, {@code false} if not
 * @param layer a preconstructed TAK {@link Layer}, or {@code null}
 *
 * @since 5.4
 */
public record LayerConfiguration(
        @Nonnull String name, @Nullable String displayName, @Nonnull LayerClassType layerClassType, boolean visible, boolean pickable, @Nullable String iconPath,
        @Nonnull LayerGroup layerGroup, @Nullable String type, @Nullable String restorableXMLString, boolean permanent, Layer layer)
{
    /**
     * Describes the class type of the layer being configured
     */
    public enum LayerClassType
    {
        Elevation,
        GenericLayer,
        /**
         * @deprecated use {@link #Model}
         */
        @Deprecated(since = "5.7", forRemoval = true)
        @DeprecatedApi(since = "5.7", forRemoval = true, removeAt = "5.10")
        LegacyModel,
        LocalImagery,
        OnlineImagery,
        Model,
        Wcs
    }
}
