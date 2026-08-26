package gov.takx.api.plugin.datamonitor;

import gov.takx.api.mapobject.FieldType;
import gov.takx.commons.constants.State;
import gov.takx.platform.ui.UIResources;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Standard pre-defined power data monitor. The display name and icon cannot be customized by the user.
 * The field name to analyze defaults to {@code Power} and it must be of type {@link FieldType#BOOLEAN}.
 * If the value of the configured field is {@code true}, the state will be set to {@link State#Active},
 * otherwise it is set to {@link State#Inactive}.
 *
 */
final class PowerDataMonitor extends AStockDataMonitor
{
    public PowerDataMonitor(String family, String type)
    {
        super(family, type, "Power", UIResources.getUrl(PLATFORM_ICONS, "power_black.png"));

        setFieldName("Power");
        setFieldType(FieldType.BOOLEAN);
        setClearable(true);
        setShowValueOnDisplay(false);
        setDefaultState(State.Inactive);

        addThreshold(true, State.Active, true);
    }
}
