package gov.takx.core.uimodel;

import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.platform.preferences.UserPreferenceEntry;

import javax.swing.AbstractListModel;

/**
 * Abstract base class providing a list model that responds to preference change events (on the
 * coordinate system) by firing a contents changed event on all list elements.
 *
 */
public abstract class ACoordinateSystemListModel<T> extends AbstractListModel<T>
{
    private final IPreferenceMonitor<CoordinateSystem> coordinateSystemMonitor = UserPreferenceEntry.COORDINATE_SYSTEM.newMonitor(CoordinateSystem.class, this::coordinateSystemChanged);

    /**
     * @return The current coordinate system
     * @since 2.35
     */
    public CoordinateSystem getCoordinateSystem()
    {
        return coordinateSystemMonitor.getCurrentValue();
    }

    @InvokeOnEDT
    protected void coordinateSystemChanged(CoordinateSystem coordinateSystem)
    {
        fireContentsChanged(this, 0, getSize() - 1);
    }

    /**
     * Release resources.
     *
     * @since 2.35
     */
    public void dispose()
    {
        coordinateSystemMonitor.dispose();
    }
}
