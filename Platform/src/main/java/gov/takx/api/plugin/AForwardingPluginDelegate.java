package gov.takx.api.plugin;

import gov.nasa.worldwind.geom.Sector;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.filesystem.IVirtualFileSystem;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.globe.ICameraEye;
import gov.takx.api.globe.IFavorite;
import gov.takx.api.globe.IFavoritesService;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.media.IMediaManager;
import gov.takx.api.ping.IGeoPingService;
import gov.takx.api.project.IProjectAssistant;
import gov.takx.api.registry.IPluginInspector;
import gov.takx.api.tasks.ITaskRunnable;
import gov.takx.api.timer.ITimerService;
import gov.takx.api.toolbar.IToolbarManager;
import gov.takx.commons.utils.PlatformVersion;
import gov.takx.platform.utils.ILengthMeasurer;

import java.io.File;
import java.util.concurrent.ScheduledExecutorService;

/**
 * A plugin delegate that simply forwards all method invocations to a given target plugin delegate.
 *
 * @since 2.0.0
 */
public class AForwardingPluginDelegate implements IPluginDelegate
{
    protected IPluginDelegate target;

    public AForwardingPluginDelegate(IPluginDelegate target)
    {
        this.target = target;
    }

    @Override
    public ScheduledExecutorService getExecutorService()
    {
        return target.getExecutorService();
    }

    @Override
    public IFavoritesService getFavoritesService()
    {
        return target.getFavoritesService();
    }

    @Override
    public IGeoPingService getGeoPingService()
    {
        return target.getGeoPingService();
    }

    @Override
    public File getJarMountPoint()
    {
        return target.getJarMountPoint();
    }

    @Override
    public ILengthMeasurer getLengthMeasurer()
    {
        return target.getLengthMeasurer();
    }

    @Override
    public IPluginInspector getPluginInspector()
    {
        return target.getPluginInspector();
    }

    @Override
    public IPluginInspector getPluginInspector(PluginCategory... pluginCategories)
    {
        return target.getPluginInspector(pluginCategories);
    }

    @Override
    public String getProgramDataDirectory()
    {
        return target.getProgramDataDirectory();
    }

    @Override
    public IProjectAssistant getProjectAssistant()
    {
        return target.getProjectAssistant();
    }

    @Override
    public String getProjectName()
    {
        return target.getProjectName();
    }

    @Override
    public PlatformVersion getPlatformVersion()
    {
        return target.getPlatformVersion();
    }

    @Override
    public IToolbarManager getToolbarManager()
    {
        return target.getToolbarManager();
    }

    @Override
    public ITimerService getTimerService()
    {
        return target.getTimerService();
    }

    @Override
    public IMediaManager getMediaManager()
    {
        return target.getMediaManager();
    }

    @Override
    public IVirtualFileSystem getVirtualFileSystem()
    {
        return target.getVirtualFileSystem();
    }

    @Override
    public IVirtualFileSystem getVirtualFileSystem(VFSScope scope)
    {
        return target.getVirtualFileSystem(scope);
    }

    @Override
    public boolean isValidProjectSession()
    {
        return target.isValidProjectSession();
    }

    @Override
    public void registerExternalProcess(int pid)
    {
        target.registerExternalProcess(pid);
    }

    @Override
    public void gotoLocationOnGlobe(IFavorite bookmark)
    {
        target.gotoLocationOnGlobe(bookmark);
    }

    @Override
    public void gotoLocationOnGlobe(ICameraEye cameraEye)
    {
        target.gotoLocationOnGlobe(cameraEye);
    }

    @Override
    public void gotoLocationOnGlobe(double lat, double lon)
    {
        target.gotoLocationOnGlobe(lat, lon);
    }

    @Override
    public void gotoLocationOnGlobe(ILocation location)
    {
        target.gotoLocationOnGlobe(location);
    }

    @Override
    public void gotoLocationOnGlobe(Sector sector)
    {
        target.gotoLocationOnGlobe(sector);
    }

    @Override
    public void gotoLocationOnGlobe(IRaptorLayer layer)
    {
        target.gotoLocationOnGlobe(layer);
    }

    @Override
    public void setGlobeHeading(double heading)
    {
        target.setGlobeHeading(heading);
    }

    @Override
    public void submitTask(ITaskRunnable runnable)
    {
        target.submitTask(runnable);
    }

    @Override
    public void doStore()
    {
        target.doStore();
    }
}
