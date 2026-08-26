package gov.takx.api.ui;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.plugin.device.IDataStructureFactory;

import java.util.Collection;
import java.util.List;

/**
 * Contract for the UI operations provided by the delegate for a Device plugin.
 *
 * @since 2.0.0
 */
@SuppressWarnings("unused") // Called from plugins
@NoPluginImplementations
public interface IDeviceSwingUIOperations
{
    /**
     * Displays a dialog containing all the {@link IRaptorDataMessage}s of a singularly selectable
     * {@link IRaptorDataStructure} type. The list of data structures and the default structure is built from the
     * plugin's {@link IDataStructureFactory} implementation.
     */
    void showDeviceLog();

    /**
     * Displays a dialog containing all the {@link IRaptorDataMessage}s of a singularly selectable
     * {@link IRaptorDataStructure} type. The caller can use the defaultFilter to indicate a specific
     * IRaptorDataStructure should be used as the default filter when the dialog is opened.
     *
     * @param dataStructures Collection of IRaptorDataStructures that will be used to populate a combo box for the user to pick from
     * @param defaultFilter Default IRaptorDataStructure to select upon opening the dialog, or {@code null}. If
     *                      {@code null} is specified, the first IRaptorDataStructure will be selected from the list and
     *                      used as the filter.
     */
    void showDeviceLog(Collection<IRaptorDataStructure> dataStructures, IRaptorDataStructure defaultFilter);

    /**
     * Displays a dialog containing all the {@link IRaptorDataMessage}s of a singularly selectable
     * {@link IRaptorDataStructure} type. The caller can use the defaultFilter to indicate a specific
     * IRaptorDataStructure should be used as the default filter when the dialog is opened.
     *
     * @param dataStructures Collection of IRaptorDataStructures that will be used to populate a combo box for the user to pick from
     * @param minReceiveTime Establishes an absolute minimum receive time for all queries.  No messages prior to this timestamp will ever be retrieved.
     * @param defaultFilter Default IRaptorDataStructure to select upon opening the dialog, or {@code null}. If
     *                      {@code null} is specified, the first IRaptorDataStructure will be selected from the list and
     *                      used as the filter.
     */
    void showDeviceLog(Collection<IRaptorDataStructure> dataStructures, Long minReceiveTime, IRaptorDataStructure defaultFilter);

    /**
     * Display a dialog containing all the {@link IRaptorDataMessage}s associated with the tracks in the vicinity of a
     * user's double-click.
     *
     * @param rdms        The {@link IRaptorDataMessage}s associated with the tracks in the vicinity of a user's double-click.
     * @param minLocation The min corner of the location considered to be in the vicinity of the user's double-click.
     * @param maxLocation The max corner of the location considered to be in the vicinity of the user's double-click.
     * @since 1.17
     */
    void showTrackLog(List<IRaptorDataMessage> rdms, ILocation minLocation, ILocation maxLocation);

    /**
     * Display the configuration dialog for this device.  No user-defined tab page is pre-selected.
     */
    void showConfigurationDialog();

    /**
     * Display the configuration dialog for this device.  The first tab page that has a name that is a case-sensitive
     * match to the string provided will be pre-selected.
     *
     * @param title One of the user-defined preference tab page titles; case-sensitive
     */
    void showConfigurationDialog(String title);

    /**
     * Set the device default tab to select upon displaying the preferences dialog.  Any of the tabs may be selected,
     * including non-user-defined tabs.
     *
     * @param title one of preference tab page titles; case-sensitive.
     * @since 1.25
     */
    void setDefaultPreferencesTab(String title);
}
