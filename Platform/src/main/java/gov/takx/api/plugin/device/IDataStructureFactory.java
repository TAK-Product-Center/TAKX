package gov.takx.api.plugin.device;

import gov.takx.api.messages.IRaptorCommandMessage;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.messages.NoSuchStructureException;
import gov.takx.api.plugin.annotations.DevicePlugin;
import gov.takx.api.ui.IDeviceSwingUIOperations;

import java.util.List;

/**
 * Concrete implementations of this factory are solely responsible for managing the {@link IRaptorDataMessage}
 * definitions used by a plugin. Management responsibilities include structures that are persisted, non-persisted
 * (internal plugin structures only), and non-persisted {@link IRaptorCommandMessage} data structures.
 * <p/>
 * Developers are encouraged to extend {@link ADataStructureFactory} which provides a number
 * of protected helper methods for managing these data structure lists.
 *
 */
public interface IDataStructureFactory
{
    /**
     * Generate a list of persistable {@link IRaptorDataStructure} instances. Persistable data structure definitions are
     * physically stored in the database. If a plugin is using any {@link IRaptorDataStructure} definitions for internal
     * communications only, it is not necessary to include those data structures inside of this list. If a data
     * structure is shared between both {@link IRaptorDataMessage} and {@link IRaptorCommandMessage} instances and the
     * {@link IRaptorDataMessage} is persistable, then the structure should be included in this list.
     *
     * @return List of persistable {@link IRaptorDataStructure} instances.
     */
    List<IRaptorDataStructure> getPersistableDataStructures();

    /**
     * Return a list of structures for {@link IRaptorDataMessage} instances (persisted and non-persisted).
     *
     * @return List of persisted and non-persisted {@link IRaptorDataStructure} definitions used to build
     * {@link IRaptorDataMessage} instances
     */
    List<IRaptorDataStructure> getMessageDataStructures();

    /**
     * Return a list of structures for {@link IRaptorCommandMessage} instances (non-persisted).
     *
     * @return List of non-persisted IRaptorDataStructure definitions used as {@link IRaptorCommandMessage} instances
     */
    List<IRaptorDataStructure> getCommandDataStructures();

    /**
     * Locate a specific {@link IRaptorDataStructure} by the name arbitrarily assigned by the plugin developer.
     *
     * @param structureName Plugin developer assigned arbitrary structure name.
     * @return {@link IRaptorDataStructure} if a case-sensitive name match is found, or {@code null} if not found
     * @throws NoSuchStructureException When the structure name cannot be found
     */
    IRaptorDataStructure getDataStructure(String structureName) throws NoSuchStructureException;

    /**
     * Recreates all persistable {@link IRaptorDataStructure} instances. This method is invoked when the application is
     * shutdown and when the user initiates a new session via File > New. It is imperative that every persistable data
     * structure instance be recreated in order to assure proper persistence in the database. This method must be
     * invoked by the TAKX core automatically and is called immediately prior to the invocation of the plugin's
     * {@link IDevicePlugin#shutdownPlugin()} method.
     */
    void resetStructures();

    /**
     * The TAKX Device Log is a panel that is accessible by clicking on a Map Object and choosing "Device Log" from the
     * popup menu. This UI panel must be populated with a set of {@link IRaptorDataStructure} instances that are used as
     * the data source for the structure combobox, and it may define a default {@link IRaptorDataStructure} that would
     * be initially selected the first time the window is opened. This method is automatically invoked whenever the
     * device log is accessed via this popup menu to determine which data structures to omit from the device log.
     * <p/>
     * Developers looking to provide additional or a more specialized Device Log should refer to the methods
     * available in the {@link IDeviceSwingUIOperations}.
     * <p/>
     * Another resource is the {@link DevicePlugin} annotation where device log structure exclusions can be applied.
     *
     * @return List of {@link IRaptorDataStructure} instances that are explicitly hidden from the user in the Device
     * Log structure list.
     * @see IDeviceSwingUIOperations
     * @see DevicePlugin
     * @since 2.21
     */
    List<IRaptorDataStructure> getExcludedDeviceLogStructures();

    /**
     * The TAKX Device Log is a panel that is accessible by clicking on a Map Object and choosing "Device Log" from the
     * popup menu. This UI panel is populated with a set of {@link IRaptorDataStructure} instances that are used as
     * the data source for the structure combobox, and it may define a default {@link IRaptorDataStructure} that would
     * be initially selected the first time the window is opened. This method is automatically invoked to request a
     * {@link IRaptorDataStructure} that should be selected in the structure combobox by default. If no default is
     * desired, this method may return {@code null}. When a {@code null} value is returned, the first
     * {@link IRaptorDataMessage} in the list is automatically set as the default.
     *
     * @return {@link IRaptorDataStructure} to initially select in the structure combobox when the "Device Log" dialog
     * is displayed, or {@code null} if no default is to be specified.
     */
    IRaptorDataStructure getDefaultDeviceLogDataStructure();
}
