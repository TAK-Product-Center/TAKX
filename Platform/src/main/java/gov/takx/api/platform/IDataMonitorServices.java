package gov.takx.api.platform;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.mapobject.IDataMonitor;
import gov.takx.api.plugin.IRaptorObject;
import gov.takx.api.plugin.datamonitor.DataMonitorBuilder;
import gov.takx.api.plugin.datamonitor.DataMonitorEvent;
import gov.takx.api.plugin.datamonitor.StandardDataMonitorType;
import gov.takx.commons.constants.State;
import gov.takx.commons.lang.IPropertyChangeListenerSupport;
import jakarta.annotation.Nonnull;

import java.awt.image.BufferedImage;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * @implNote Adding a PCL on this services instance will also provide change notifications for all individual data monitors managed by this services
 * instance. This minimizes the burden of watching for all/any DM related changes.
 * @since 1.4
 */
@NoPluginImplementations
public interface IDataMonitorServices extends IPropertyChangeListenerSupport
{
    /**
     * Event name used to notify listeners that a data monitor has been added.
     * @since 1.11
     */
    String DATA_MONITOR_ADDED = "DATA_MONITOR_ADDED";

    /**
     * Event name used to notify listeners that a data monitor has been removed.
     * @since 1.11
     */
    String DATA_MONITOR_REMOVED = "DATA_MONITOR_REMOVED";

    /**
     * Event name used to notify listeners that the sort order of the data monitors has changed.
     * The old and new values will be of type {@link Comparator Comparator&lt;IDataMonitor&gt;}.
     * The old and new values can be the same in the case that a plugin is requesting a "re-sort" of the data monitors.
     *
     * @since 2.37
     */
    String DATA_MONITOR_SORT_ORDER_CHANGED = "DATA_MONITOR_SORT_ORDER_CHANGED";

    /**
     * Add a custom data monitor.  Can optionally extend the ADataMonitor class, or create a custom
     * data monitor implementing IDataMonitor.
     *
     * @param dm Data monitor implementing IDataMonitor
     * @see IDataMonitor
     */
    void addDataMonitor(@Nonnull IDataMonitor dm);

    /**
     * Removes a data monitor at runtime. Also removes the data monitor from the ui.
     *
     * @param dm Data monitor to be removed.
     */
    void removeDataMonitor(@Nonnull IDataMonitor dm);

    /**
     * Call this method when your data monitor ordering needs to be updated. This will sort the data monitors depending
     * on their compareTo values;
     */
    void sortDataMonitors();

    /**
     * Return list of data monitors for this map object
     *
     * @return list of data monitors
     */
    List<IDataMonitor> getDataMonitors();

    /**
     * Return a data monitor builder capable of generating new data monitor.
     *
     * @return A data monitor builder capable of generating new data monitor.
     * @since 1.10
     */
    DataMonitorBuilder createDataMonitorBuilder();

    /**
     * Create one of the stock data monitors listed in {@link StandardDataMonitorType}.
     *
     * @param type The type of stock data monitor builder to return.
     * @return The builder required to build a monitor of the type specified in {@code type}.
     * @since 1.10
     */
    DataMonitorBuilder createStandardDataMonitorBuilder(StandardDataMonitorType type);

    /**
     * Send a command to a data monitor.
     *
     * @param target     The target data monitor.
     * @param command    The command to send.
     * @param properties A set of properties to update.
     * @return The event generated to command the data monitor.
     * @since 1.10
     */
    DataMonitorEvent sendDataMonitorCommand(IRaptorObject target, String command, Map<String, Object> properties);

    /**
     * Inject a custom {@link Comparator} that will be used to sort the data monitors managed by this
     * {@link IDataMonitorServices} object.
     *
     * @param comparator The comparator containing the desired sorting logic that should be used instead of the
     *                   default sort order.
     * @since 1.10
     */
    void delegateSort(@Nonnull Comparator<IDataMonitor> comparator);

    /**
     * @return The highest state reached for the set of data monitors managed by this service.
     * @since 2.16
     */
    State getHighestState();

    /**
     * @return The owner's family.
     * @since 2.16
     */
    String getFamily();

    /**
     * @return The owner's type.
     * @since 2.16
     */
    String getType();

    /**
     * @return The owner's display name.
     * @since 2.16
     */
    String getDisplayName();

    /**
     * @return The owner's unique key.
     * @since 2.16
     */
    String getKey();

    /**
     * The icon image associated with the data monitor owner. May be {@code null} if the owner doesn't have an icon.
     *
     * @return The owner's icon image or {@code null} if one does not exist.
     * @since 2.16
     */
    BufferedImage getIconImage();
}
