package gov.takx.api.globe;

import gov.tak.platform.graphics.PointF;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;

/**
 * Contract for an instance of a {@link gov.takx.api.globe.IWidget widget} on a specific
 * {@link gov.takx.api.globe.IGlobeDelegate globe}.
 *
 * @since 5.4
 */
@NoPluginImplementations
public interface IWidgetInstance
{
    /**
     * @return The globe on which the widget is registered
     */
    @Nonnull
    IGlobeDelegate globe();

    /**
     * @return The name of the widget
     */
    @Nonnull
    String getName();

    /**
     * @return {@code true} if the widget is visible
     */
    boolean isVisible();

    /**
     * Set the visibility of the widget.
     *
     * @param visible Pass {@code true} to show the widget, {@code false} to hide it
     */
    void setVisible(boolean visible);

    /**
     * @return the absolute position of this widget on the screen
     */
    @Nonnull
    PointF getAbsoluteWidgetPosition();

    /**
     * Get the size of the widget (in pixels), optionally including the padding and margins.
     *
     * @param incPadding Pass {@code true} to include any padding
     * @param incMargin  Pass {@code true} to include any margin
     * @return Array of {@code {width, height}}
     */
    @Nonnull
    float[] getSize(boolean incPadding, boolean incMargin);

    /**
     * @return {@code true} if the widget is configured to respond to touch events
     */
    boolean isTouchable();
}
