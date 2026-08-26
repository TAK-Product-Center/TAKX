package gov.takx.core.view.gui.maptools;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.MapToolType;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.api.shapes.IMutableShape;
import gov.takx.commons.swing.IPanelStateListener;
import gov.takx.platform.KeyUtils;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.utils.UnitIdHelper;
import gov.takx.platform.utils.units.AltitudeUnits;
import gov.takx.platform.utils.units.RangeUnits;
import jakarta.inject.Inject;

/**
 * Abstract base for properties editor panels for shapes.  Handling for the "Units of measure" user preference is
 * provided.
 *
 * @since 3.3
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public abstract class AShapePropertiesEditorPanel<T extends IMutableShape> extends APropertiesEditorPanel<T>
{
    @Inject
    protected UnitIdHelper unitIdHelper;

    protected final IPreferenceMonitor<AltitudeUnits> altitudeUnitsMonitor =
            UserPreferenceEntry.DISPLAY_ALTITUDE_UNITS.newMonitor(AltitudeUnits.class, this::altitudeUnitsChanged);

    protected final IPreferenceMonitor<RangeUnits> rangeUnitsMonitor =
            UserPreferenceEntry.DISPLAY_RANGE_UNITS.newMonitor(RangeUnits.class, this::rangeUnitsChanged);

    protected AltitudeUnits altitudeUnits = altitudeUnitsMonitor.getCurrentValue();
    protected RangeUnits rangeUnits = rangeUnitsMonitor.getCurrentValue();
    protected RangeUnits priorRangeUnits = rangeUnits;

    /**
     * @param title Title of panel
     * @param target Target object (the one being edited), {@code null} if performing an initial object creation
     * @param directActionMode Pass {@code true} to immediately apply changes to UI controls
     * @param panelListener Listener for "panel dirty" events, {@code null} if not being used
     */
    protected AShapePropertiesEditorPanel(String title, T target, boolean directActionMode, IPanelStateListener panelListener)
    {
        super(title, target, directActionMode, panelListener);
    }

    /**
     * Generate the "next" name for the given tool type.
     */
    protected String nextFriendlyName(MapToolType toolType)
    {
        return toolType + " " + unitIdHelper.getNextFriendlyUnitIdNoUpdate(KeyUtils.TOOL_FAMILY, toolType.getShapeTypeName());
    }

    /**
     * @param rangeUnits The new units to use for range values
     */
    private void rangeUnitsChanged(RangeUnits rangeUnits)
    {
        priorRangeUnits = this.rangeUnits;
        this.rangeUnits = rangeUnits;

        updateGeometryControls();
    }

    /**
     * @param altitudeUnits The new units to use for altitude/height values
     */
    private void altitudeUnitsChanged(AltitudeUnits altitudeUnits)
    {
        this.altitudeUnits = altitudeUnits;

        updateGeometryControls();
    }

    /**
     * Update any controls that display values based on the current "units of measure".
     */
    protected abstract void updateGeometryControls();

    @Override
    public void releaseResources()
    {
        altitudeUnitsMonitor.dispose();
        rangeUnitsMonitor.dispose();
        super.releaseResources();
    }
}
