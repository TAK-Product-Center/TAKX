package gov.takx.core.gateway;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.commservices.ICommServiceManager;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.filesystem.IVirtualFileSystem;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.filesystem.VfsConstants;
import gov.takx.api.gateway.IDeviceGateway;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.api.plugin.gateway.IGatewayController;
import gov.takx.api.plugin.gateway.IGatewayParserDelegate;
import gov.takx.api.registry.IPluginInspector;
import gov.takx.api.tracks.ISmartTrackManager;
import gov.takx.commons.file.FilePath;
import gov.takx.commons.security.IParserEncryptionKey;
import gov.takx.core.pluginclient.EncryptionKeyMap;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.core.registry.PluginInspectorManager;
import gov.takx.platform.KeyUtils;
import gov.takx.platform.cdi.InjectableContainerSupport;
import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

/**
 * Delegate used by plugin developer injected into IGatewayParser to allow plugin developer
 * to send RDMs upstream.  Also has a utility method to restart a serial port comm service.
 *
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class ParserDelegate extends InjectableContainerSupport implements IGatewayParserDelegate
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private PluginInspectorManager pluginInspectorManager;

    @Inject
    private IDeviceGateway deviceGateway;

    @Inject
    @VFSManagerScope(VFSScope.Project)
    private IVirtualFileSystemManager projectVFSManager;

    private final Map<String, IVirtualFileSystem> cachedVFS = new HashMap<>();

    private final String family;
    private final String sourceName;
    private final String deviceType;
    private IVirtualFileSystem fileSystem;

    protected ParserDelegate(String family, String deviceType, String sourceName)
    {
        this.family = family;
        this.deviceType = deviceType;
        this.sourceName = sourceName;
    }

    @Override
    public String getDeviceUniqueKey()
    {
        return KeyUtils.getUniqueKey(family, deviceType);
    }

    @Override
    public void sendToController(IPrePersistRaptorDataMessage message)
    {
        if (!deviceType.equals(message.getType()) && !family.equals(message.getFamily()))
        {
            throw new IllegalStateException("IGatewayParser must send IPrePersistRaptorDataMessage instances only to its same family and device type. (either " + family + " != " + message.getFamily() + ", or" + deviceType + " != " + message.getType() + ")");
        }

        String deviceID = message.getUnitID();

        if (logger.isDebugEnabled()) logger.debug("Requesting controller for {}:{}", deviceType, deviceID);

        IGatewayController controller = deviceGateway.getController(message.getFamily(), deviceType, deviceID);
        if (controller != null)
        {
            controller.processParserMessage(message);
        } else
        {
            logger.error("ParserDelegate error trying to find IGatewayController for deviceType = {}:{}", deviceType, deviceID);
        }
    }

    @Override
    public ISmartTrackManager getSmartTrackManager()
    {
        return getBean(ISmartTrackManager.class);
    }

    @Override
    public IVirtualFileSystem getVirtualFileSystem(String unitID)
    {
        final IVirtualFileSystem virtualFileSystem = cachedVFS.get(unitID);
        // Cache the VFS if we can't find it
        if (virtualFileSystem == null)
        {
            fileSystem = projectVFSManager.getVirtualFileSystemForDevice(family, deviceType, unitID);
        }
        return fileSystem;
    }

    @Override
    public FilePath createFilePath(String unitID)
    {
        return new FilePath(VfsConstants.DEVICE_NAMESPACE, family, deviceType, unitID);
    }

    @Override
    public void resetCommPort()
    {
        getBean(ICoreExecutorService.class).execute(() -> {
            Thread.currentThread().setName("Reset Serial Port: " + sourceName);
            getBean(ICommServiceManager.class).resetSerialPort(sourceName);
        });
    }

    @Override
    public SortedSet<IParserEncryptionKey> getKeys(String unitID)
    {
        String key = KeyUtils.getUniqueKey(family, deviceType, unitID);
        return EncryptionKeyMap.getParserKeys(key);
    }

    @Override
    public IPluginInspector getPluginInspector()
    {
        return pluginInspectorManager.getPluginInspector();
    }

    private void stopService(@SuppressWarnings("unused") @Observes @Priority(ServiceOrdering.STOP_DeviceGateway + 1) StopProjectServices event)
    {
        cachedVFS.clear();
    }
}
