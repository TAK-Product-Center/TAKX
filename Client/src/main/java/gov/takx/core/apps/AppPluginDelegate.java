package gov.takx.core.apps;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.UiEventBus;
import gov.takx.api.apps.ICoreAppManager;
import gov.takx.api.commservices.ICommServiceManager;
import gov.takx.api.commservices.ISerialPortStatus;
import gov.takx.api.connection.IConnectionManager;
import gov.takx.api.dataportal.IRaptorDataListener;
import gov.takx.api.dataportal.IRaptorDataPortal;
import gov.takx.api.explorer.IExplorerManagerDelegate;
import gov.takx.api.filesystem.IVirtualFileSystem;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.gateway.IDeviceGateway;
import gov.takx.api.globe.IGlobeManager;
import gov.takx.api.globe.IProjectLabelService;
import gov.takx.api.io.IImportExportService;
import gov.takx.api.kml.IKmlLayerService;
import gov.takx.api.layer.ILayerManagerDelegate;
import gov.takx.api.layer.IRaptorLayerManager;
import gov.takx.api.mapobject.ICoreMapObjectManager;
import gov.takx.api.mapobject.IDataMonitor;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.mapobject.ITrackContextMenuListener;
import gov.takx.api.mapobject.ITrackSelectListener;
import gov.takx.api.pathsimulation.IPathSimulationService;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.api.persist.IPersistable;
import gov.takx.api.persist.IPersistableState;
import gov.takx.api.platform.IDataMonitorServices;
import gov.takx.api.playback.IPlaybackManager;
import gov.takx.api.playback.IPlaybackSystem;
import gov.takx.api.plugin.app.IAppPlugin;
import gov.takx.api.plugin.app.IAppPluginDelegate;
import gov.takx.api.plugin.app.IAppPluginManager;
import gov.takx.api.plugin.app.IImmutableAppInstance;
import gov.takx.api.plugin.connection.ConnectionState;
import gov.takx.api.plugin.connection.ICommServiceDelegate;
import gov.takx.api.project.IProjectDelegate;
import gov.takx.api.query.IExtendedQueryService;
import gov.takx.api.shapes.IShapeService;
import gov.takx.api.structures.ISurfaceStructureService;
import gov.takx.api.surfaces.IAnalyticSurfaceService;
import gov.takx.api.tracks.ISmartTrackManager;
import gov.takx.api.ui.IPreferenceSpaceManager;
import gov.takx.api.ui.ITerrainMenuSpaceManager;
import gov.takx.api.ui.IUISpaceManager;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.explorer.IExplorerManager;
import gov.takx.core.filesystem.VirtualFileSystemManagerProvider;
import gov.takx.core.ioservice.DataDelegate;
import gov.takx.core.messagequery.ExtendedQueryService;
import gov.takx.core.shapes.ShapeService;
import gov.takx.core.shapes.ShapeServiceFactory;
import gov.takx.core.surfaces.SurfaceService;
import gov.takx.core.view.gui.event.ProxyButtonEvent;
import gov.takx.platform.KeyUtils;
import jakarta.inject.Inject;
import org.greenrobot.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.Action;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.Collection;
import java.util.Optional;

/**
 * Implementation of the Application plugin delegate.  Note that this delegate will live across project boundaries,
 * so it can not cache references to objects that are project-dependent (like VFS for example).
 *
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class AppPluginDelegate extends DataDelegate implements IAppPluginDelegate
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final IAppPlugin plugin;

    private ShapeService shapeService;
    private final IAnalyticSurfaceService analyticSurfaceService;

    /**
     * Data monitor services used to add, remove and access app-owned data monitors.
     */
    private final AppDataMonitorServices dataMonitorServices;

    @Inject
    private ICoreMapObjectManager mapObjectManager;

    @Inject
    private IDeviceGateway deviceGateway;

    @Inject
    private ICommServiceManager commServiceManager;

    @Inject
    private ShapeServiceFactory shapeServiceFactory;

    @Inject
    private AppMenuSpaceManager menuSpaceManager;

    @Inject
    private IProjectDelegate projectDelegate;

    public AppPluginDelegate(IAppPlugin plugin)
    {
        super(plugin.getFamily(), plugin.getType());

        this.plugin = plugin;

        analyticSurfaceService = new SurfaceService(KeyUtils.getUniqueKey(plugin));
        dataMonitorServices = new AppDataMonitorServices(
                this,
                family,
                type,
                plugin.getName(),
                getAppIcon(plugin.getIconPath()));
    }

    /**
     * This delegate is being torn down, clean up.
     */
    public void dispose()
    {
        if (shapeService != null)
        {
            shapeServiceFactory.disposeShapeService(shapeService);
            shapeService = null;
        }

        dataMonitorServices.shutdownDataMonitors();
    }

    @Override
    public IImmutableAppInstance getMyAppInstance()
    {
        return getBean(ICoreAppManager.class).getAppInstance(plugin);
    }

    @Override
    public IAppPluginManager getAppPluginManager()
    {
        return getBean(ICoreAppManager.class);
    }

    @Override
    public Collection<ISerialPortStatus> listSerialPorts()
    {
        return commServiceManager.getSerialPortCommStatus();
    }

    @Override
    public IExtendedQueryService getQueryService()
    {
        return new ExtendedQueryService();
    }

    @Override
    public IImportExportService getImportExportService()
    {
        return getBean(IImportExportService.class);
    }

    @Override
    public IProjectLabelService getProjectLabelService()
    {
        return getBean(IProjectLabelService.class);
    }

    @Override
    public IPlaybackManager getPlaybackManager()
    {
        return getBean(IPlaybackSystem.class);
    }

    @Override
    public void addDataListener(IRaptorDataListener listener)
    {
        getBean(IRaptorDataPortal.class).addListener(listener);
    }

    @Override
    public void createProxyConnection(String id, String name, String iconPath, Action action)
    {
        // Publish the event so that the toolbar will add the button
        getBean(EventBus.class, UiEventBus.Literal.INSTANCE).post(new ProxyButtonEvent(id, name, iconPath, action, true, plugin));
    }

    @Override
    public void removeProxyConnection(String id)
    {
        // Publish the event so the connection will be removed from the toolbar and also notify the connection manager.
        getBean(EventBus.class, UiEventBus.Literal.INSTANCE).post(new ProxyButtonEvent(id, "", "", null, false, null));
        getBean(IConnectionManager.class).removeProxyConnection(id);
    }

    @Override
    public void displayProxyConnection(String id, ConnectionState connectionState)
    {
        getBean(IConnectionManager.class).displayProxyConnection(id, connectionState);
    }

    @Override
    public ICommServiceDelegate getCommServiceDelegate()
    {
        return deviceGateway.getCommServiceDelegate();
    }

    @Override
    public void removeDataListener(IRaptorDataListener listener)
    {
        getBean(IRaptorDataPortal.class).removeListener(listener);
    }

    @Override
    public IVirtualFileSystem getVirtualFileSystem(VFSScope scope)
    {
        return getBean(VirtualFileSystemManagerProvider.class)
                .getVirtualFileSystemManager(scope)
                .getVirtualFileSystemForApp(family, type);
    }

    @Override
    public IShapeService getShapeService()
    {
        if (shapeService == null)
        {
            shapeService = shapeServiceFactory.getOrCreateShapeService(this);
        }

        return shapeService;
    }

    @Override
    public IPathSimulationService getPathSimulationService()
    {
        return getBean(IPathSimulationService.class);
    }

    @Override
    public IAnalyticSurfaceService getAnalyticSurfaceService()
    {
        return analyticSurfaceService;
    }

    @Override
    public ISurfaceStructureService getSurfaceStructureService()
    {
        return getBean(ISurfaceStructureService.class);
    }

    @Override
    public IKmlLayerService getKmlLayerService()
    {
        return getBean(IKmlLayerService.class);
    }

    @Override
    public ISmartTrackManager getSmartTrackManager()
    {
        return getBean(ISmartTrackManager.class);
    }

    @Override
    public String getAppId()
    {
        return uniqueKey;
    }

    @Override
    public IGlobeManager getGlobeManager()
    {
        return getBean(IGlobeManager.class);
    }

    @Override
    public IUISpaceManager getUISpaceManager()
    {
        return getBean(IUISpaceManager.class);
    }

    @Override
    public IPreferenceSpaceManager getPreferenceSpaceManager()
    {
        return getBean(IPreferenceSpaceManager.class);
    }

    @Override
    public ITerrainMenuSpaceManager getTerrainMenuSpaceManager()
    {
        return menuSpaceManager;
    }

    @Override
    public IProjectDelegate getProjectDelegate()
    {
        return projectDelegate;
    }

    @Override
    public Collection<? extends IMapObjectController> getMapObjectControllers()
    {
        return mapObjectManager.getMapObjectControllers();
    }

    @Override
    public IMapObject getMapObject(IDataMonitor dataMonitor)
    {
        for (IMapObjectController moc : getMapObjectControllers())
        {
            for (IDataMonitor dm : moc.getDataMonitors())
            {
                if (dm.getUniqueKey().equals(dataMonitor.getUniqueKey()))
                {
                    return moc.getMapObject();
                }
            }
        }

        return null;
    }

    @Override
    public void setZOrder(IMapObject mapObject, int zOrder)
    {
        getGlobe().setZOrder(mapObject, zOrder);
    }

    @Override
    public Optional<IMapObjectController> getMapObjectController(IMapObject mapObject)
    {
        return getMapObjectController(mapObject.getKey());
    }

    @Override
    public Optional<IMapObjectController> getMapObjectController(String key)
    {
        return Optional.ofNullable(mapObjectManager.getMapObjectController(key));
    }

    @Override
    public void selectMapObject(String mapObjectKey)
    {
        mapObjectManager.setSelected(mapObjectKey);
    }

    @Override
    public ILayerManagerDelegate getLayerManagerDelegate()
    {
        return getBean(IRaptorLayerManager.class);
    }

    @Override
    public IExplorerManagerDelegate getExplorerManagerDelegate()
    {
        return getBean(IExplorerManager.class);
    }

    @Override
    public void addTrackSelectListener(ITrackSelectListener listener)
    {
        mapObjectManager.addTrackSelectListener(listener);
    }

    @Override
    public void removeTrackSelectListener(ITrackSelectListener listener)
    {
        mapObjectManager.removeTrackSelectListener(listener);
    }

    @Override
    public void addTrackContextMenuListener(ITrackContextMenuListener listener)
    {
        mapObjectManager.addTrackContextMenuListener(listener);
    }

    @Override
    public void removeTrackContextMenuListener(ITrackContextMenuListener listener)
    {
        mapObjectManager.removeTrackContextMenuListener(listener);
    }

    @Override
    public void commitUpdates(IPersistable entity)
    {
        getBean(IPersistServiceProvider.class).getPersistService().persistEntity(entity);
    }

    @Override
    public IDataMonitorServices getAppDataMonitorServices()
    {
        return dataMonitorServices;
    }

    /**
     * Get the app's icon image.
     *
     * @param path The path to the image.
     * @return The icon image.
     * @since 2.16
     */
    private BufferedImage getAppIcon(String path)
    {
        BufferedImage result = null;

        // Icon is optional. null indicates that no icon is being provided.
        if (!StringUtils.isEmpty(path))
        {
            URL url = plugin.getClass().getClassLoader().getResource(path);

            // Make sure the resource exists.
            if (url != null)
            {
                try
                {
                    result = ImageIO.read(url);
                } catch (IOException e)
                {
                    logger.error("Error retrieving app icon for plugin {}", plugin.getUniqueKey(), e);
                }
            } else
            {
                logger.error("Could not find icon resource at {} for plugin {}", path, plugin.getUniqueKey());
            }
        }

        return result;
    }

    @Override
    protected IPersistableState getPersistableState()
    {
        return plugin;
    }
}
