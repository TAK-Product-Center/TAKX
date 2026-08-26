package gov.takx.core.project;

import gov.takx.api.event.StartBackgroundProjectServices;
import gov.takx.api.event.StartPlugins;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;

/**
 * This class declares constants for the service start and stop ordering (observer priority).  Any service that
 * starts/stops as part of the project session will observe the appropriate event with a priority set using these
 * constants.  This centralizes the ordering management and makes it easier to maintain.
 *
 * @see StartProjectServices
 * @see StartBackgroundProjectServices
 * @see StartPlugins
 * @see StopProjectServices
 * @since 2.0.0
 */
public class ServiceOrdering
{
    // Start order (fired with StartProjectServices)
    public static final int START_GlobeBookmarks = 5;
    public static final int START_GlobeService = 10;
    public static final int START_PlaybackSystem = 20;
    public static final int START_DockingSystem = 30;
    public static final int START_CreateTAKLayers = 40;
    public static final int START_InstallTAKLayers = 50;
    public static final int START_PersistServiceProvider = 100;
    public static final int START_RaptorLayerManager = 110;
    public static final int START_LayerViewManager = 120;
    public static final int START_SmartTrackManager = 130;
    public static final int START_MapObjectStateLoader = 140;
    public static final int START_MapObjectManager = 150;
    public static final int START_DeploymentContainerManager = 160; // DataPortal needs plugin instances
    public static final int START_DataPortal = 170;
    public static final int START_DeviceGateway = 180;
    public static final int START_SharedMapObjectManager = 190;
    public static final int START_VirtualFileSystemManager = 200;
    public static final int START_CommServiceManager = 210;
    public static final int START_ConnectionManager = 220;
    public static final int START_SurfaceStructureService = 230;
    public static final int START_ExplorerManager = 235;
    public static final int START_GenericCoreService = 240;
    public static final int START_PluginRegistryManager = 250; // Need containers and most services to validate plugins
    public static final int START_ServicesStarted = 260;

    // StartPlugins event ordering
    public static final int START_PLUGINS_MapObjectManager = 10;
    public static final int START_PLUGINS_LocationProviderManager = 20;
    public static final int START_PLUGINS_ConnectionManager = 30;
    public static final int START_PLUGINS_ServicesStarted = 40;
    public static final int START_PLUGINS_CoreAppManager = 50;

    // Background service order (fired with StartBackgroundProjectServices)
    public static final int START_BACKGROUND_CommonCommoManager = 10;
    public static final int START_BACKGROUND_GenericService = 20;

    // Stop order
    //
    // Stop the MOM before shutting down the gateway so the plugin can stop using things like
    // the DataStructureFactory before they are all reset in the gateway.

    public static final int STOP_CoreAppManager = 10;
    public static final int STOP_ServiceStopping = 20;
    public static final int STOP_DeploymentContainerManager = 30;
    public static final int STOP_GenericCoreService = 40;
    public static final int STOP_SurfaceStructureService = 50;
    public static final int STOP_RaptorLayerManager = 60;
    public static final int STOP_TAKLayers = 70;
    public static final int STOP_GlobeService = 80;
    public static final int STOP_DataPortal = 90;
    public static final int STOP_ConnectionManager = 100;
    public static final int STOP_CommServiceManager = 110;
    public static final int STOP_SharedMapObjectManager = 120;
    public static final int STOP_MapObjectManager = 130;
    public static final int STOP_DeviceGateway = 140;
    public static final int STOP_SmartTrackManager = 150;
    public static final int STOP_VirtualFileSystemManager = 160;
    public static final int STOP_PersistServiceProvider = 170;
}
