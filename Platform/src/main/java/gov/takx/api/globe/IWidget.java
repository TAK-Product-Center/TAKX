package gov.takx.api.globe;

import gov.tak.api.widgets.IMapWidget;
import gov.takx.api.ui.IResourceCleaner;

/**
 * Contract for a "Widget", a screen overlay that can be added to a globe at specific screen locations via {@link IWidgetLayerManager}.
 *
 * @since 3.5
 */
public interface IWidget extends IResourceCleaner, IMapWidget
{
    /**
     * @return {@code true} if the widget should appear in the list of widgets in the widget group panel.
     * @since 5.2
     */
    default boolean showInWidgetList()
    {
        return true;
    }

    /**
     * Widgets are often compared by class to determine if they are the same kind of widget across multiple globes.
     * However, there are cases where this test is incorrect, such as our project labels, where we use one widget
     * class to represent the 8 distinct locations of project labels, so equality by class is incorrect.
     * <p>
     * Default implementation simply compares the widget class.
     *
     * @param other Other widget to compare against
     * @return {@code true} if the widgets should be considered the same kind, {@code false} if not
     * @since 5.5.1
     */
    default boolean sameKindAs(IWidget other)
    {
        return getClass() == other.getClass();
    }
}
