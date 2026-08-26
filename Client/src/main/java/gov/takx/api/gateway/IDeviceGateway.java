package gov.takx.api.gateway;

import gov.takx.api.plugin.connection.ICommServiceDelegate;
import gov.takx.api.plugin.gateway.IGatewayController;
import gov.takx.api.plugin.gateway.IGatewayFramer;
import gov.takx.commons.constants.CommServiceType;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 */
public interface IDeviceGateway
{
    /**
     * Configuration Log Reporting.
     *
     * @return Set of parser keys that are actively in use within the device gateway
     */
    Set<String> getParserKeys();

    /**
     * Configuration Log Reporting.
     *
     * @return Set of framer keys that are actively in use within the device gateway
     */
    Set<String> getFramerKeys();

    /**
     * Get the IGatewayController that corresponds to the family, type, and unitId
     *
     * @param family family of plugin
     * @param type   type of plugin
     * @param unitId unitId of plugin instance
     * @return IDevicePluginController created by the type, and unitId, {@code null} if it can't be created
     */
    IGatewayController getController(String family, String type, String unitId);

    /**
     * Get the IGatewayController that corresponds to the unique key and version.  The controller must already
     * exist.
     *
     * @param uniqueKey Unique key of plugin instance (family, type, unit Id)
     * @return IDevicePluginController located by the key, {@code null} if it doesn't exist
     * @since 2.2
     */
    IGatewayController getController(String uniqueKey);

    /**
     * Get the IGatewayController that corresponds to the unique key and version.  If {@code createIfNotFound}
     * is {@code true}, then the controller will be created if it doesn't already exist.
     *
     * @param uniqueKey        Unique key of plugin instance (family, type, unit Id)
     * @param createIfNotFound Pass {@code true} to create a controller if one doesn't already exist
     * @return IDevicePluginController created by the type, ID and version, {@code null} if it can't be created
     * @since 2.2
     */
    IGatewayController getController(String uniqueKey, boolean createIfNotFound);

    /**
     * Get the IGatewayFramer that corresponds to the type and source name.
     *
     * @param sourceType string type of port "ETHERNET", "SERIAL"
     * @param sourceName String representation of the source name (ie "COM2")
     * @param family     plugin family, i.e. "Acme"
     * @param type       type of plugin, i.e. "AcmeDevice"
     * @return gateway framer, or {@code null} if none exist.
     * @see CommServiceType
     */
    @Nullable
    IGatewayFramer getFramer(CommServiceType sourceType, String sourceName, String family, String type);

    /**
     * Get map of all comm paths with framers for this device's family/type, by comm path type
     * and list of names
     *
     * @param family Device's family
     * @param type   Device's type
     * @return Map containing a list of string comm path names, by comm path type key values
     */
    Map<CommServiceType, Collection<String>> getFramedCommPaths(String family, String type);

    ICommServiceDelegate getCommServiceDelegate();

    /**
     * Bring a device online. Ensure the MO is created and processed.
     *
     * @since 2.29, moved here from IDeviceOnlineMessenger
     */
    void putDeviceOnline(String family, String deviceType, String unitID);
}
