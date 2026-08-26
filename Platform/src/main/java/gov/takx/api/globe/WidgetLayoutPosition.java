package gov.takx.api.globe;

import gov.tak.api.widgets.IMapWidget;
import gov.tak.platform.widgets.RootLayoutWidget;

/**
 * Defined locations for an {@link IWidget IWidgets} to be placed.
 * <p>
 * {@link #ABSOLUTE} requires the calling {@link IMapWidget#setPoint(float, float)} to place the widget, and the
 * creator is responsible for relocating the widget on any window changes.
 * @since 5.3
 */
public enum WidgetLayoutPosition
{
    /**
     * Using the {@code ABSOLUTE} position requires calling {@link IMapWidget#setPoint(float, float)} to place the widget,
     * and the creator is responsible for relocating the widget on any window changes.
     * <p>
     * This method does <i>not</i> constrain the placement to be visible on the screen, and {@code (0, 0)} is the top-left of the screen.
     */
    ABSOLUTE(-1),
    TOP_LEFT(RootLayoutWidget.TOP_LEFT),
    TOP_EDGE(RootLayoutWidget.TOP_EDGE),
    TOP_RIGHT(RootLayoutWidget.TOP_RIGHT),
    LEFT_EDGE(RootLayoutWidget.LEFT_EDGE),
    RIGHT_EDGE(RootLayoutWidget.RIGHT_EDGE),
    BOTTOM_LEFT(RootLayoutWidget.BOTTOM_LEFT),
    BOTTOM_RIGHT(RootLayoutWidget.BOTTOM_RIGHT),
    BOTTOM_EDGE(RootLayoutWidget.BOTTOM_EDGE);

    public final int layoutValue;

    WidgetLayoutPosition(int layoutValue)
    {
        this.layoutValue = layoutValue;
    }
}
