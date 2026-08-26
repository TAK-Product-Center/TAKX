package gov.takx.core.kml;

import com.atakmap.map.layer.feature.geometry.Envelope;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Wrapper to abstract different KML "visual" elements, like geographic features (placemarks and ground overlays)
 * and screen overlays.
 *
 * @since 5.5
 */
public class KmlVisual
{
    private final Supplier<Boolean> visibilityGetter;
    private final Consumer<Boolean> visibilitySetter;
    private final Envelope envelope;

    /**
     * @param visibilityGetter method to get the current visibility of the visual
     * @param visibilitySetter method to set the visibility of the visual
     * @param envelope Bounding envelope for the visual, to support Go To operations, may be {@code null}
     */
    public KmlVisual(@Nonnull Supplier<Boolean> visibilityGetter, @Nonnull Consumer<Boolean> visibilitySetter, @Nullable Envelope envelope)
    {
        this.visibilityGetter = visibilityGetter;
        this.visibilitySetter = visibilitySetter;
        this.envelope = envelope;
    }

    /**
     * @return {@code true} if the visual is visible, {@code false} otherwise
     */
    public boolean isVisible()
    {
        return visibilityGetter.get();
    }

    /**
     * Set the visible state of the visual.
     */
    public void setVisible(boolean visible)
    {
        visibilitySetter.accept(visible);
    }

    /**
     * @return Bounding envelope of the visual (if it is geographic), {@code null} for non-geographic visuals.
     */
    @Nullable
    public Envelope getEnvelope()
    {
        return envelope;
    }
}
