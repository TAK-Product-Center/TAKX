package gov.takx.core.ioservice;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.dataportal.IRaptorDataListener;
import gov.takx.api.dataportal.IRaptorDataPortal;
import gov.takx.api.explorer.IExplorerManagerDelegate;
import gov.takx.api.filesystem.IVirtualFileSystem;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.messages.NoSuchStructureException;
import gov.takx.api.plugin.IRaptorPlugin;
import gov.takx.api.plugin.io.IImportExportPlugin;
import gov.takx.api.plugin.io.IImportExportPluginDelegate;
import gov.takx.api.query.IQueryService;
import gov.takx.api.registry.IDataStructureRegistry;
import gov.takx.api.registry.IDevicePluginRegistry;
import gov.takx.api.registry.IPluginInspector;
import gov.takx.api.shapes.IShapeService;
import gov.takx.api.surfaces.IAnalyticSurfaceService;
import gov.takx.core.explorer.IExplorerManager;
import gov.takx.core.filesystem.VirtualFileSystemManagerProvider;
import gov.takx.core.messagequery.ExtendedQueryService;
import gov.takx.core.shapes.ShapeService;
import gov.takx.core.shapes.ShapeServiceFactory;
import gov.takx.core.surfaces.SurfaceService;
import gov.takx.platform.KeyUtils;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.net.URL;

/**
 * Implementation of the Import/Export Plugin Delegate.
 *
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class ImportExportPluginDelegate extends DataDelegate implements IImportExportPluginDelegate
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private IExplorerManager explorerManager;

    @Inject
    private IRaptorDataPortal dataPortal;

    @Inject
    private IDataStructureRegistry dataStructureRegistry;

    @Inject
    private ShapeServiceFactory shapeServiceFactory;

    @Inject
    private VirtualFileSystemManagerProvider virtualFileSystemManagerProvider;

    private final IRaptorPlugin pluginFromConstruction;
    private ShapeService shapeService;
    private IAnalyticSurfaceService analyticSurfaceService;
    private URL pluginIconURL;

    /**
     * Create a new delegate.
     *
     * @param plugin The plugin that owns the delegate.
     */
    public ImportExportPluginDelegate(IImportExportPlugin plugin)
    {
        this(plugin.getFamily(), plugin.getType(), plugin);
    }

    public ImportExportPluginDelegate(String family, String type)
    {
        this(family, type, null);
    }

    /**
     * Constructor. Attempts to extract the plugin's icon.
     *
     * @param family The plugin's family.
     * @param type   The plugin's type.
     * @param plugin The plugin. May be {@code null}.
     */
    private ImportExportPluginDelegate(String family, String type, IRaptorPlugin plugin)
    {
        super(family, type);
        pluginFromConstruction = plugin;
    }

    /**
     * Extract the icon URL from the plugin, if we can locate it.
     */
    @PostConstruct
    private void postConstruct()
    {
        final IRaptorPlugin plugin;

        if (pluginFromConstruction == null)
        {
            final IPluginInspector inspector = getBean(IPluginInspector.class);

            plugin = inspector.isPluginInstalled(family, type)
                    ? inspector.getPlugin(family, type)
                    : null;
        } else
        {
            plugin = pluginFromConstruction;
        }

        if (plugin != null)
        {
            String iconPath = plugin.getIconPath();

            if (iconPath != null)
            {
                pluginIconURL = plugin.getClass().getClassLoader().getResource(iconPath);
            }
        }
    }

    @Override
    public IQueryService getQueryService()
    {
        return new ExtendedQueryService();
    }

    @Override
    public ILocation getLocationAtCenterOfView()
    {
        return globeManager.getPrimaryGlobeDelegate().getCenterLocation();
    }

    @Override
    public IExplorerManagerDelegate getExplorerManagerDelegate()
    {
        return explorerManager;
    }

    @Override
    public IRaptorDataStructure getDataStructure(IRaptorDataMessage msg) throws NoSuchStructureException
    {
        return dataStructureRegistry.getDataStructure(msg);
    }

    @Override
    public synchronized IVirtualFileSystem getVirtualFileSystem(VFSScope scope)
    {
        return virtualFileSystemManagerProvider.getVirtualFileSystemManager(scope).getVirtualFileSystemForImportExport(family, type);
    }

    @Override
    public IVirtualFileSystem getFileSystemForMapObject(IMapObject mapObject)
    {
        return mapObject.getController().getVirtualFileSystem();
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
    public String getUniqueKey()
    {
        return uniqueKey;
    }

    @Override
    public IDevicePluginRegistry getDevicePluginRegistry()
    {
        return getPluginRegistryManager().getDevicePluginRegistry();
    }

    @Override
    public IAnalyticSurfaceService getAnalyticSurfaceService()
    {
        if (analyticSurfaceService == null)
        {
            analyticSurfaceService = new SurfaceService(KeyUtils.getUniqueKey(family, type));
        }

        return analyticSurfaceService;
    }

    @Override
    public void addDataListener(IRaptorDataListener listener)
    {
        dataPortal.addListener(listener);
    }

    @Override
    public URL getPluginIconURL()
    {
        return pluginIconURL;
    }
}
