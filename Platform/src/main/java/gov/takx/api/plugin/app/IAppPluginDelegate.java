package gov.takx.api.plugin.app;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.commservices.ISerialPortManager;
import gov.takx.api.dataportal.IRaptorDataListener;
import gov.takx.api.explorer.IExplorerManagerDelegate;
import gov.takx.api.globe.IGlobeManager;
import gov.takx.api.globe.IProjectLabelService;
import gov.takx.api.io.IImportExportService;
import gov.takx.api.kml.IKmlLayerService;
import gov.takx.api.layer.ILayerManagerDelegate;
import gov.takx.api.mapobject.IDataMonitor;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.mapobject.ITrackContextMenuListener;
import gov.takx.api.mapobject.ITrackSelectListener;
import gov.takx.api.mapobject.TrackSelectEvent;
import gov.takx.api.pathsimulation.IPathSimulationService;
import gov.takx.api.persist.IPersistable;
import gov.takx.api.platform.IDataMonitorServices;
import gov.takx.api.playback.IPlaybackManager;
import gov.takx.api.plugin.IDataDelegate;
import gov.takx.api.plugin.IPluginDelegate;
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

import javax.swing.Action;

import java.util.Collection;
import java.util.Optional;

/**
 * Contract for app plugin delegates.
 *
 * @since 2.0.0
 */
@SuppressWarnings("unused") // Called from plugins
@NoPluginImplementations
public interface IAppPluginDelegate extends IPluginDelegate, IDataDelegate, ISerialPortManager
{
    /**
     * Get the identifier for the app that owns this delegate.
     *
     * @return The app identifier.
     */
    String getAppId();

    /**
     * Get the App Instance for the App for which we are the delegate.
     *
     * @return current app's instance
     * @since 1.25
     */
    IImmutableAppInstance getMyAppInstance();

    /**
     * Get a query service. The query service can query for information for any map object.
     *
     * @return A query service.
     */
    IExtendedQueryService getQueryService();

    /**
     * Get the import export service, which can be used to programmatically import/export files.
     *
     * @return The import export service.
     * @since 2.35
     */
    IImportExportService getImportExportService();

    /**
     * Obtain the playback manager for invoking and managing replay.
     *
     * @return Playback manager
     */
    IPlaybackManager getPlaybackManager();

    /**
     * Returns the project label service.
     *
     * @return Project label service
     */
    IProjectLabelService getProjectLabelService();

    /**
     * Get the connection comm service delegate to allow the app to open sockets, etc. and yet still be
     * managed by TAKX.
     *
     * @return A comm service delegate
     * @since 1.4
     */
    ICommServiceDelegate getCommServiceDelegate();

    /**
     * Add a data listener. The listener will be notified of all data messages flowing through TAKX that originate
     * in the Device Gateway en-route to the Device Plugin Controller.
     *
     * @param listener The new listener.
     */
    void addDataListener(IRaptorDataListener listener);

    /**
     * Remove a data listener.
     *
     * @param listener The listener to remove.
     */
    void removeDataListener(IRaptorDataListener listener);

    /**
     * Create a proxy connection button with the specified icon and action handler.
     * <p>
     * NOTE:  If the proxy connection is added to the connections panel, and the operator chooses to delete
     * the connection, and action event will be fired with actionSource = "Removed" to allow the action handler
     * to close the connection manually.
     *
     * @param id       an arbitrary but consistent key associated with the panel. Plugin developers are encouraged to define
     *                 their key using a GUID, storing it in a private static final variable somewhere to ensure that the
     *                 panel can be consistently identified between sessions by the platform.
     * @param name     connection name
     * @param iconPath pathname for icon file on button
     * @param action   action handler
     * @since 1.14
     */
    void createProxyConnection(String id, String name, String iconPath, Action action);

    /**
     * Remove a previously create proxy connection button.  Will also remove it from the connection panel
     * if the method displayProxyConnection was called at any point.
     *
     * @param id id of proxy connection
     * @since 1.14
     */
    void removeProxyConnection(String id);

    /**
     * Forces display of the proxy connection in the connections panel
     *
     * @param id              id of the proxy connection
     * @param connectionState state of the connection
     * @since 2.9
     */
    void displayProxyConnection(String id, ConnectionState connectionState);

    /**
     * Get the shape service. The shape service can be used to draw
     * shapes on the map.
     *
     * @return The shape service.
     */
    IShapeService getShapeService();

    /**
     * The path simulation service is used to simulate map object movement along a given path.
     *
     * @return The path simulation service.
     * @since 1.31
     */
    IPathSimulationService getPathSimulationService();

    /**
     * Get the Analytic Surface Service.
     *
     * @return Surface service
     */
    IAnalyticSurfaceService getAnalyticSurfaceService();

    /**
     * Get the Surface Structure Service.
     *
     * @return Surface structure service
     * @since 1.5
     */
    ISurfaceStructureService getSurfaceStructureService();

    /**
     * Get the Kml Layer Service
     *
     * @return Kml layer service
     * @since 1.24
     */
    IKmlLayerService getKmlLayerService();

    /**
     * Get the Smart Tracks Manager.
     *
     * @return SmartTrackManager instance
     * @since 1.12
     */
    ISmartTrackManager getSmartTrackManager();

    /**
     * Get a reference to the globe manager that can be used to manage globes, including creation and deletion.
     *
     * @return the globe manager
     * @since 1.49
     */
    IGlobeManager getGlobeManager();

    /**
     * Get the UI Space manager.
     *
     * @return Space manager instance
     * @since 1.3
     */
    IUISpaceManager getUISpaceManager();

    /**
     * Get the Preferences UI Space manager, used to add custom preference tab panels to the user preferences.
     *
     * @return the Preference space manager
     * @since 1.49
     */
    IPreferenceSpaceManager getPreferenceSpaceManager();

    /**
     * Get the terrain menu space manager.
     *
     * @return Terrain menu space manager
     * @since 2.38
     */
    ITerrainMenuSpaceManager getTerrainMenuSpaceManager();

    /**
     * Provide a project delegate interface to allow for saving and opening projects.
     *
     * @return project delegate interface
     * @since 1.4
     */
    IProjectDelegate getProjectDelegate();

    /**
     * Cause the selection of a map object on the globe.
     *
     * @param mapObjectKey unique key of a map object
     * @since 1.40
     */
    void selectMapObject(String mapObjectKey);

    /**
     * Redefines the z-order for the given {@code IMapObject}.
     *
     * @param mapObject Map object to redefine z-order
     * @param zOrder    New z-order of the map object
     * @since 1.14
     */
    void setZOrder(IMapObject mapObject, int zOrder);

    /**
     * Get the App plugin manager.
     *
     * @return App plugin manager
     */
    IAppPluginManager getAppPluginManager();

    /**
     * Commit any changes made to a persistent object, like a MapObject, and ensure the changes are pushed to disk.
     *
     * @param entity The entity to persist
     */
    void commitUpdates(IPersistable entity);

    /**
     * Get the Layer Manager delegate.
     *
     * @return The Layer manager delegate.
     * @since 1.41
     */
    ILayerManagerDelegate getLayerManagerDelegate();

    /**
     * Return the Explorer Manager delegate that provides access to all Explorers.
     *
     * @return The Explorer Manager delegate.
     * @since 2.4
     */
    IExplorerManagerDelegate getExplorerManagerDelegate();

    /**
     * Add a listener that will be invoked when the user selects 1 or more track points.  The registered listener will
     * get notified of any track selection operation (see {@link TrackSelectEvent}) on
     * any globe OR in the Device Log
     *
     * @param listener Listener to add, may not be {@code null}
     * @see #removeTrackSelectListener(ITrackSelectListener)
     * @since 1.49
     */
    void addTrackSelectListener(ITrackSelectListener listener);

    /**
     * Remove a track select listener.
     *
     * @param listener Listener to remove
     * @see #addTrackSelectListener(ITrackSelectListener)
     * @since 1.49
     */
    void removeTrackSelectListener(ITrackSelectListener listener);

    /**
     * Add a listener that will be invoked when the user right-clicks 1 or more track points.  The registered listener
     * will get notified of any right-click operation on a track on any globe OR in the Device Log. The listener is
     * responsible for returning a list of menu items which are appropriate for the tracks selected.
     *
     * @param listener Listener to add, may not be {@code null}
     * @see #removeTrackContextMenuListener(ITrackContextMenuListener)
     * @since 2.26
     */
    void addTrackContextMenuListener(ITrackContextMenuListener listener);

    /**
     * Remove a track select listener.
     *
     * @param listener Listener to remove
     * @see #addTrackContextMenuListener(ITrackContextMenuListener)
     * @since 2.26
     */
    void removeTrackContextMenuListener(ITrackContextMenuListener listener);

    /**
     * Return the map object managed by the controller that owns the specified data monitor.
     *
     * @param dataMonitor The data monitor whose owner should be found.
     * @return The map object that owns the specified data monitor.
     */
    IMapObject getMapObject(IDataMonitor dataMonitor);

    /**
     * Return the {@link IMapObjectController map object controller} for the given unique key.
     *
     * @param key Unique key of map object
     * @return Optional Map object controller, empty if no controller is found for the given key
     * @since 3.0
     */
    Optional<IMapObjectController> getMapObjectController(String key);

    /**
     * Return the {@link IMapObjectController map object controller} from the associated {@link IMapObject}.
     *
     * @param mapObject Map object
     * @return Optional Map object controller
     * @since 3.0
     */
    Optional<IMapObjectController> getMapObjectController(IMapObject mapObject);

    /**
     * Obtain a collection of all the {@link IMapObjectController map object controllers} currently in the system.
     *
     * @return Collection of all available {@link IMapObjectController} instances, may be empty
     * @since 3.0
     */
    @Override
    Collection<? extends IMapObjectController> getMapObjectControllers();

    /**
     * Get the app's data monitor services.
     *
     * @return The app's data monitor services.
     * @since 2.16
     */
    IDataMonitorServices getAppDataMonitorServices();
}
