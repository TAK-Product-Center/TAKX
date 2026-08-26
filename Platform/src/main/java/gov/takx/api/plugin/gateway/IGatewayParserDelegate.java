package gov.takx.api.plugin.gateway;

import gov.takx.api.filesystem.IVirtualFileSystem;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.plugin.IRaptorPlugin;
import gov.takx.api.registry.IPluginInspector;
import gov.takx.api.tracks.ISmartTrackManager;
import gov.takx.commons.file.FilePath;
import gov.takx.commons.security.IParserEncryptionKey;

import java.util.SortedSet;

/**
 * Delegate that is injected into the plugin implementation of IGatewayParser
 *
 * @see IGatewayParser
 */
@SuppressWarnings("unused") // Called from plugins
public interface IGatewayParserDelegate
{
    /**
     * Send the RDM to the plugin implementation of IGatewayController
     *
     * @param message RDM to be sent to IGatewayController implementation
     * @throws IllegalStateException if the device type does not match the delegate's device type. Simply stated, an
     * Acme Device parser cannot send an {@link IRaptorDataMessage} that targets a Foo Device. Only the gateway
     * controller has this ability via {@link IGatewayController#processDeviceMessage}.
     * @see IGatewayController
     */
    void sendToController(IPrePersistRaptorDataMessage message);

    /**
     * Get a reference to the virtual file system for a specific unit ID within the parser delegate's established
     * device family and type. Use the VFS to write binary content directly to the VFS without being forced to send it
     * through a formal {@link IRaptorDataMessage} construct. Instead, after writing the file to the VFS, send a thin
     * {@link IRaptorDataMessage} with a field that points to the file's relative path in the VFS.
     *
     * @param unitID Unit ID.  The delegate already knows its device family and type, but a parser may target multiple
     *               unit IDs.
     * @return a writeable virtual file system object with a subset of
     * {@link IVirtualFileSystem} functionality.
     * @since 1.11
     */
    IVirtualFileSystem getVirtualFileSystem(String unitID);

    /**
     * Get a reference to the SmartTracks manager for use in registering new and locating existing track style
     * definitions. An {@link IPrePersistRaptorDataMessage} may be assigned an explicit
     * track style definition.
     *
     * @return SmartTrack Manager
     */
    ISmartTrackManager getSmartTrackManager();

    /**
     * Utility method for creating a FilePath to the root VFS directory of the given Unit ID. The relative VFS path will
     * automatically be based on the parser delegate's established device family and type.
     *
     * @param unitID Unit ID.  The delegate already knows its device family and type, but a parser may target multiple
     *               unit IDs.
     * @return FilePath instance, suitable for {@link FilePath#append(String)} calls to more
     * specific directory
     */
    FilePath createFilePath(String unitID);

    /**
     * Closes and reopens a comm port, though callers are advised to use with extreme caution.
     * Currently only implemented for Serial Comm Ports.
     */
    void resetCommPort();

    /**
     * Fetches a list of known keys based on the device type and unit ID.
     *
     * @param unitID Unit ID. The delegate already knows its device type.
     * @return Sorted set of keys, or {@code null} if no keys exists
     * @since 1.2
     */
    SortedSet<IParserEncryptionKey> getKeys(String unitID);

    /**
     * Access read-only metadata about installed TAKX plugins. This service does not provide any means of editing
     * any existing plugin data, nor does it provide direct access to the plugin. Similarly, it is not possible to
     * dynamically install or uninstall plugins via the plugin manager.
     * <p/>
     * Every {@link IRaptorPlugin} data returned from the {@link IPluginInspector} relies on composition, wrapping and
     * protecting the actual plugin to ensure these contractual obligations are enforced.
     *
     * @return Plugin Manager
     * @since 1.49
     */
    IPluginInspector getPluginInspector();

    /**
     * @return The unique key of the device (plugin) that created the parser
     * @since 2.0.0
     */
    String getDeviceUniqueKey();
}
