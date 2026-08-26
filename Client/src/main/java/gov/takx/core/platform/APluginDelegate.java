package gov.takx.core.platform;

import gov.nasa.worldwind.geom.Sector;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.filesystem.IVirtualFileSystem;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.globe.ICameraEye;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.globe.IFavorite;
import gov.takx.api.globe.IFavoritesService;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.media.IMediaManager;
import gov.takx.api.persist.IPersistableState;
import gov.takx.api.persist.IPluginStatePersist;
import gov.takx.api.ping.IGeoPingService;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.api.plugin.IPluginDelegate;
import gov.takx.api.plugin.PluginCategory;
import gov.takx.api.processmanager.IProcessManager;
import gov.takx.api.project.IProjectAssistant;
import gov.takx.api.project.IProjectManager;
import gov.takx.api.registry.IPluginInspector;
import gov.takx.api.tasks.ITaskManager;
import gov.takx.api.tasks.ITaskRunnable;
import gov.takx.api.timer.ITimerService;
import gov.takx.api.toolbar.IToolbarManager;
import gov.takx.commons.utils.PlatformVersion;
import gov.takx.core.configuration.ProgramEnvironment;
import gov.takx.core.globe.FavoritesService;
import gov.takx.core.globe.utils.GlobeRuler;
import gov.takx.core.project.ProjectAssistant;
import gov.takx.core.registry.PluginInspectorManager;
import gov.takx.core.tasks.PluginTask;
import gov.takx.module.IPluginModuleManager;
import gov.takx.platform.KeyUtils;
import gov.takx.platform.cdi.InjectableContainerSupport;
import gov.takx.platform.utils.ILengthMeasurer;
import jakarta.inject.Inject;

import java.io.File;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Common interface for all plugin delegates to provide access to services made available to all plugin types.
 *
 * @since 2.0.0
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public abstract class APluginDelegate extends InjectableContainerSupport implements IPluginDelegate
{
    protected final String family;
    protected final String type;
    protected final String unitId;
    protected final String uniqueKey;

    @Inject
    protected IProjectManager projectManager;

    @Inject
    protected IExtendedGlobeManager globeManager;

    @Inject
    protected ITaskManager taskManager;

    /**
     * Constructor for delegates whose unique key is based on just family and type.
     *
     * @param family Plugin family
     * @param type   Plugin type
     */
    protected APluginDelegate(String family, String type)
    {
        this(family, type, null, KeyUtils.getUniqueKey(family, type));
    }

    /**
     * Constructor for delegates whose unique key is based on family, type, and unit id.
     *
     * @param family Plugin family
     * @param type   Plugin type
     * @param unitId Unit id
     */
    protected APluginDelegate(String family, String type, String unitId)
    {
        this(family, type, unitId, KeyUtils.getUniqueKey(family, type, unitId));
    }

    /**
     * Constructor for delegates whose unique key is specified, and not (possibly) derived from the typical
     * family, type, and optional unit Id.
     *
     * @param family    Plugin family
     * @param type      Plugin type
     * @param unitId    Unit id, may be {@code null}
     * @param uniqueKey The unique key to use for this delegate
     */
    protected APluginDelegate(String family, String type, String unitId, String uniqueKey)
    {
        this.family = family;
        this.type = type;
        this.unitId = unitId;
        this.uniqueKey = uniqueKey;
    }

    @Override
    public String getProjectName()
    {
        return getProjectManager().getProjectName();
    }

    @Override
    public PlatformVersion getPlatformVersion()
    {
        return getBean(ProgramEnvironment.class).getPlatformVersion();
    }

    @Override
    public IPluginInspector getPluginInspector()
    {
        return getBean(PluginInspectorManager.class).getPluginInspector();
    }

    @Override
    public IPluginInspector getPluginInspector(PluginCategory... pluginCategories)
    {
        return getBean(PluginInspectorManager.class).getPluginInspector(pluginCategories);
    }

    @Override
    public ILengthMeasurer getLengthMeasurer()
    {
        return new GlobeRuler();
    }

    @Override
    public IVirtualFileSystem getVirtualFileSystem()
    {
        return getVirtualFileSystem(VFSScope.Project);
    }

    @Override
    public boolean isValidProjectSession()
    {
        return getProjectManager().isValidSession();
    }

    @Override
    public IFavoritesService getFavoritesService()
    {
        return new FavoritesService(uniqueKey);
    }

    @Override
    public IGeoPingService getGeoPingService()
    {
        return getBean(IGeoPingService.class);
    }

    @Override
    public ITimerService getTimerService()
    {
        return getBean(ITimerService.class);
    }

    @Override
    public IMediaManager getMediaManager()
    {
        return getBean(IMediaManager.class);
    }

    @Override
    public ScheduledExecutorService getExecutorService()
    {
        return getBean(ICoreExecutorService.class);
    }

    @Override
    public IToolbarManager getToolbarManager()
    {
        return getBean(IToolbarManager.class);
    }

    @Override
    public IProjectAssistant getProjectAssistant()
    {
        return new ProjectAssistant();
    }

    @Override
    public String getProgramDataDirectory()
    {
        return getBean(ProgramEnvironment.class).getProgramRoot();
    }

    @Override
    public void registerExternalProcess(int pid)
    {
        getBean(IProcessManager.class).registerExternalProcess(pid);
    }

    @Override
    public File getJarMountPoint()
    {
        return getBean(IPluginModuleManager.class).getPluginJarMountPoint();
    }

    @Override
    public void gotoLocationOnGlobe(IFavorite bookmark)
    {
        getGlobe().goTo(bookmark);
    }

    @Override
    public void gotoLocationOnGlobe(ICameraEye cameraEye)
    {
        getGlobe().goTo(cameraEye);
    }

    @Override
    public void gotoLocationOnGlobe(double lat, double lon)
    {
        getGlobe().goTo(lat, lon);
    }

    @Override
    public void gotoLocationOnGlobe(ILocation location)
    {
        getGlobe().goTo(location);
    }

    @Override
    public void gotoLocationOnGlobe(Sector sector)
    {
        getGlobe().goTo(sector);
    }

    @Override
    public void gotoLocationOnGlobe(IRaptorLayer layer)
    {
        getGlobe().goTo(layer);
    }

    @Override
    public void setGlobeHeading(double heading)
    {
        getGlobe().getGlobeViewController().setHeading(heading, true);
    }

    /**
     * @return The project manager
     */
    protected IProjectManager getProjectManager()
    {
        return projectManager;
    }

    /**
     * @return The last focused globe
     */
    protected IApplicationGlobe getGlobe()
    {
        return globeManager.getLastFocusedGlobe();
    }

    @Override
    public void submitTask(ITaskRunnable runnable)
    {
        if (runnable.getName() == null)
        {
            throw new IllegalArgumentException("Task Runnable must not return null for getName()");
        }
        taskManager.execute(new PluginTask(runnable));
    }

    protected IPersistableState getPersistableState()
    {
        return null;
    }

    @Override
    public void doStore()
    {
        final IPersistableState persistableState = getPersistableState();

        if (persistableState != null) getBean(IPluginStatePersist.class).saveOrUpdateState(uniqueKey, persistableState);
    }
}
