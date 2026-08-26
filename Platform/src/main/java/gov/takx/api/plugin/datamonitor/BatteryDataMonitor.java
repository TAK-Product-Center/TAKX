package gov.takx.api.plugin.datamonitor;

import gov.takx.api.mapobject.FieldType;
import gov.takx.commons.constants.State;
import gov.takx.commons.utils.Threshold;
import gov.takx.platform.ui.UIResources;

/**
 * A pre-defined battery data monitor. The display name and icon cannot be customized by the user.
 * The field name to analyze defaults to {@code Battery} and it is expected to be of type {@link FieldType#DOUBLE}.
 * <p>
 * Note: as constructed this data monitor has no thresholds defined.  You must add appropriate thresholds for your
 * field values and alarm states.
 * <p>
 * Here's an example using a builder to construct and configure an instance:
 * <pre>
 * {@code
 * IDataMonitor monitor = new DataMonitorBuilder(family, type, StandardDataMonitorType.Battery)
 *     .setFieldName("Voltage")
 *     .addThreshold(7.0, State.Critical, IDataMonitor.EXCLUSIVE)
 *     .addThreshold(8.0, State.Warning, IDataMonitor.EXCLUSIVE)
 *     .addThreshold(Double.MAX_VALUE, State.Active, IDataMonitor.INCLUSIVE)
 *     .build();
 * }
 * </pre>
 *
 * @since 1.11
 * @see #addThreshold(Threshold)
 * @see #addThreshold(Comparable, State, boolean)
 */
final class BatteryDataMonitor extends AStockDataMonitor
{
    /**
     * Constructor.
     *
     * @param family The data monitor's family.
     * @param type   The data monitor's type.
     */
    public BatteryDataMonitor(String family, String type)
    {
        super(family, type, "Battery", UIResources.BATTERY_BLACK_ICON_URL);

        setFieldName("Battery");
        setFieldType(FieldType.DOUBLE);
        setClearable(true);
        setShowValueOnDisplay(true);
        setDefaultState(State.Inactive);
    }
}
