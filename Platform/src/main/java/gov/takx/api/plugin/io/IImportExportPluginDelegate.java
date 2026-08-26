package gov.takx.api.plugin.io;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.dataportal.IRaptorDataListener;
import gov.takx.api.explorer.IExplorerManagerDelegate;
import gov.takx.api.filesystem.IVirtualFileSystem;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.plugin.IDataDelegate;
import gov.takx.api.plugin.IPluginDelegate;
import gov.takx.api.query.IQueryService;
import gov.takx.api.registry.IDevicePluginRegistry;
import gov.takx.api.shapes.IShapeService;
import gov.takx.api.surfaces.IAnalyticSurfaceService;

import java.net.URL;

/**
 * Delegate used by Import/Export plugins to access services in TAKX.
 *
 */
@NoPluginImplementations
public interface IImportExportPluginDelegate extends IPluginDelegate, IDataDelegate
{
    /**
     * Get the query service. This service can be used to query data in the current project.
     *
     * @return Query service
     */
    IQueryService getQueryService();

    // TODO: THIS METHOD WILL BE OBSOLETE WHEN A FOREIGN KEY RELATION BETWEEN RaptorDataMessage AND RaptorDataStructure IS ADDED
    // TODO: The link now exists, but it is a Structure not an RDS, which we probably should not be giving to the Plugin Developer.

    /**
     * Get the data structure of a data message.
     *
     * @param msg RDM
     * @return Raptor Data Structure
     */
    IRaptorDataStructure getDataStructure(IRaptorDataMessage msg);

    /**
     * Get the virtual file system for a map object.
     *
     * @param mapObject Map object to retrieve a file system for.
     * @return The virtual file system for the specified map object.
     */
    IVirtualFileSystem getFileSystemForMapObject(IMapObject mapObject);

    /**
     * Get the shape service. The shape service can be used to draw
     * shapes on the map.
     *
     * @return The shape service.
     */
    IShapeService getShapeService();

    /**
     * Get a key that uniquely identifies the plugin that this delegate belongs to.
     *
     * @return A unique key for the plugin.
     */
    String getUniqueKey();

    /**
     * Get the device plugin registry, containing device plugin definitions.
     *
     * @return the device plugin registry
     */
    IDevicePluginRegistry getDevicePluginRegistry();

    /**
     * Get the Analytic Surface Service.
     *
     * @return Surface service
     */
    IAnalyticSurfaceService getAnalyticSurfaceService();

    /**
     * Add a data listener. The listener will be notified of all data messages flowing through TAKX that originate
     * in the Device Gateway en-route to the Device Plugin Controller.
     *
     * @param listener The new listener.
     * @since 1.33
     */
    void addDataListener(IRaptorDataListener listener);

    /**
     * Get the URL for the icon associated with the plugin that owns this delegate. May be {@code null}.
     *
     * @return The URL for the icon associated with the plugin that owns this delegate or {@code null} if
     * the icon location could not be determined.
     */
    URL getPluginIconURL();

    /**
     * Return the location at the center of the screen. This is helpful when creating map objects that should be
     * on the globe but that do not currently have a position value.
     */
    ILocation getLocationAtCenterOfView();

    /**
     * Return the Explorer Manager delegate that provides access to all Explorers.
     *
     * @return The Explorer Manager delegate.
     * @since 2.4
     */
    IExplorerManagerDelegate getExplorerManagerDelegate();
}
