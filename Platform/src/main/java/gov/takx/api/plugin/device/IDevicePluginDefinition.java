package gov.takx.api.plugin.device;

import gov.takx.api.mapobject.MapObjectOptions;
import gov.takx.api.plugin.IPluginDefinition;

/**
 * Contract for Device Plugin definitions.  This includes those attributes that are unique to device plugins.
 *
 * @see IPluginDefinition
 * @since 2.0.0
 */
public interface IDevicePluginDefinition extends IPluginDefinition<IDevicePlugin>
{
    /**
     * @return {@code true} If a user is allowed to create an instance of this plugin via UI interaction
     */
    boolean isManualCreationAllowed();

    /**
     * @return {@code true} iff the device map object can be dragged into a container.
     * @since 2.7
     */
    boolean canBeContained();

    /**
     * @return {@code true} If a unit Id should be automatically generated on manually created map objects
     */
    boolean isAutoGenerateUnitId();

    /**
     * @return Name of map object tab to initially select when displaying the device preferences dialog
     */
    String getDefaultPreferencesTab();

    /**
     * @return XML resource path to the XML file to use to construct the data structures (for RDMs).
     */
    String getDataStructureXml();

    /**
     * @return XML resource path to the XML file to use to construct the command structures (for RCMs).  May be empty.
     */
    String getCommandStructureXml();

    /**
     * @return Array of message types to be excluded from the Device Log
     * @since 2.21
     */
    String[] getExcludeFromDeviceLog();

    /**
     * @return message type to use as the default in the Device Log.
     * @since 2.21
     */
    String getDefaultDeviceLogMessage();

    /**
     * @return the Map Object Options to apply to all Map Objects created from this device
     * @since 2.29
     */
    MapObjectOptions getMapObjectOptions();

    /**
     * @return Key of associated file importer to use for dropped files
     * @since 3.0
     */
    String getFileImporterKey();
}
