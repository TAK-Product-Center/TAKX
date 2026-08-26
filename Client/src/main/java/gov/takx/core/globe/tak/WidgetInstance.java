package gov.takx.core.globe.tak;

import gov.tak.platform.graphics.PointF;
import gov.takx.api.globe.IGlobeDelegate;
import gov.takx.api.globe.IWidget;
import gov.takx.api.globe.IWidgetInstance;
import jakarta.annotation.Nonnull;

/**
 * Concrete implementation of {@link IWidgetInstance} providing plugins with access to a controlled set of operations
 * on the underlying widget.
 *
 * @param widget The widget (not visible in the public API)
 * @param globe The globe on which the widget is registered
 * @since 5.4
 */
public record WidgetInstance(@Nonnull IWidget widget, @Nonnull IGlobeDelegate globe) implements IWidgetInstance
{
    @Override
    @Nonnull
    public String getName()
    {
        return widget.getName();
    }

    @Override
    public void setVisible(boolean visible)
    {
        widget.setVisible(visible);
    }

    @Override
    public boolean isVisible()
    {
        return widget.isVisible();
    }

    @Override
    @Nonnull
    public PointF getAbsoluteWidgetPosition()
    {
        return widget.getAbsoluteWidgetPosition();
    }

    @Override
    @Nonnull
    public float[] getSize(boolean incPadding, boolean incMargin)
    {
        return widget.getSize(incPadding, incMargin);
    }

    @Override
    public boolean isTouchable()
    {
        return widget.isTouchable();
    }
}
