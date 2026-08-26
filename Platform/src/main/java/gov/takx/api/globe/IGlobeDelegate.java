package gov.takx.api.globe;

import com.atakmap.map.MapRenderer3;
import com.atakmap.map.elevation.ElevationManager;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globes.ElevationModel;
import gov.nasa.worldwind.layers.Layer;
import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.engine.map.MapRenderer;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.annotation.Internal;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.layer.IRestorable;
import gov.takx.api.layer.LayerGroup;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectSelectListener;
import gov.takx.api.mapobject.MapObjectSelectEvent;
import gov.takx.core.globe.layers.IGlobeLayer;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.JPanel;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Delegate that provides access to a TAKX globe. The delegate provides layer management abilities, and direct
 * access to the TAK Map Engine globe component.
 * <p/>
 * TAKX supports multiple, independent globes, each with an independent view, but sharing the same "model" (meaning
 * all the Map Objects and track data). Using {@link IGlobeManager#createGlobe()}, a plugin can create an
 * additional globe with an independent view. Each globe has a unique ID that is never reused during a TAKX session.
 * Features such as "center on" and "keep in view" operate independently on each globe. TAKX always creates
 * an initial "primary" globe upon starting, and it always has a static ID of {@link #PRIMARY_GLOBE_ID}.
 * <p/>
 * Secondary globes start life with a copy of all the sharable layers defined in the primary globe.
 * <p/>
 * To support a consistent look and feel, you can call {@link #createFocusHandlingPanel()} to construct a panel
 * that contains the globe component and has a small border that will change color to indicate that the globe
 * in the panel has focus.
 *
 * @since 1.3
 */
@SuppressWarnings("unused")
@NoPluginImplementations
public interface IGlobeDelegate
{
    /**
     * The ID of the primary globe.
     */
    int PRIMARY_GLOBE_ID = 1;

    /**
     * Supported snapshot formats
     *
     * @since 1.26
     */
    enum SnapshotFormat
    {
        GEOTIFF("tif"),
        /** @since 5.4.1 */
        JPEG("jpg"),
        /** @since 5.4.1 */
        PNG("png"),
        ;

        private final String extension;

        SnapshotFormat(String extension)
        {
            this.extension = extension;
        }

        public String getExtension()
        {
            return extension;
        }

        /**
         * Gets the Snapshot format whose extension matches the given extension.
         *
         * @param extension The extension to match
         * @return The format that matches the extension or {@code null} if there is no match.
         */
        public static SnapshotFormat getValueByExtension(String extension)
        {
            if (extension == null) return null;
            if (extension.charAt(0) == '.') extension = extension.substring(1);

            for (SnapshotFormat snapshotFormat : values())
            {
                if (snapshotFormat.getExtension().equalsIgnoreCase(extension)) return snapshotFormat;
            }
            return null;
        }

        /**
         * @return An array of supported format extensions.
         */
        public static String[] getExtensions()
        {
            return Stream.of(values()).map(SnapshotFormat::getExtension).toArray(String[]::new);
        }
    }

    /**
     * @return The ID of the globe for which this object is a delegate
     * @since 1.14
     */
    int getGlobeId();

    /**
     * @return {@code true} if this is the primary globe, {@code false} if this is a secondary globe
     * @since 5.7
     */
    boolean isPrimaryGlobe();

    /**
     * Given a WorldWind component, locate the globe that contains it. This is useful for dealing with mouse events
     * that occur on the WW component and you need to find the corresponding TAKX globe (say for a popup menu).
     *
     * @param wwd The WW component of interest
     * @return The globe containing the given WW component, {@code null} if not found
     * @since 1.34
     */
    IGlobeDelegate getGlobeForComponent(WorldWindow wwd);

    /**
     * Get the number of globes that have been created.  The globes will have Ids of 1 - N inclusive, where N is
     * the value returned by this method.  Note that not all globe ids in this range will refer to an extant
     * globe. Globes can be removed, see {@link IGlobeManager#deleteGlobe(int)}.
     *
     * @return Number of globes in this project session
     * @see #getGlobeIds()
     * @since 1.14
     */
    int getGlobeCount();

    /**
     * Get a collection of all the globe ids currently in use.
     *
     * @return Set of globe ids
     */
    Set<Integer> getGlobeIds();

    /**
     * In order to provide visual feedback to the user on which globe has the focus, a panel with a standardized
     * look and feel can be created.  This panel will contain the globe component surrounded by a small border that
     * will change from black to cyan when the globe in the panel has the focus.
     * <p>
     * It is recommended that all secondary globes be displayed using this type of panel.
     *
     * @return Panel that contains the globe component and tracks focus
     * @since 1.14
     */
    JPanel createFocusHandlingPanel();

    /**
     * A no-op method.
     *
     * @param elevationModel ignored
     * @return {@code null}
     * @deprecated use {@link gov.nasa.worldwind.globes.Globe#getElevationModel()} to get {@link gov.nasa.worldwind.terrain.CompoundElevationModel}
     * and use {@link gov.nasa.worldwind.terrain.CompoundElevationModel#addElevationModel(ElevationModel)}
     */
    @DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
    @Deprecated(since = "5.6", forRemoval = true)
    default IRaptorLayer addElevationModel(ElevationModel elevationModel)
    {
        return null;
    }

    /**
     * Install a new layer. Some layer names are reserved for standard {@link IRaptorLayer} instances. These are
     * specified by {@link ReservedLayers}. Plugins should not create layers that name clash with the standard layers.
     * <p>
     * Layer state can be saved in the project database when the project is closed. If you would like you use this
     * service, have your Layer class implement {@link IRestorable}. When the project is reopened, the
     * layer.restoreState(String) method will be called right after the layer object is created.
     * <p>
     * The layer is assumed to be sharable across multiple globes.  If this is not the case (such as when the layer
     * manages user interaction or unique rendering state) you should call
     * {@link #addLayer(gov.nasa.worldwind.layers.Layer, LayerGroup, boolean)}
     *
     * @param layer      Layer to install.
     * @param layerGroup The Layer group to which to add the layer
     * @return IRaptorLayer that wraps the given layer. All changes to layer meta-data (name, visibility, etc) should
     * be made though this interface, so that they can be persisted.
     * @see ReservedLayers
     */
    IRaptorLayer addLayer(Layer layer, LayerGroup layerGroup);

    /**
     * Install a new layer. Some layer names are reserved for standard {@link IRaptorLayer} instances. These are
     * specified by {@link ReservedLayers}. Plugins should not create layers that name clash with the standard layers.
     * <p>
     * Layer state can be saved in the project database when the project is closed. If you would like you use this
     * service, have your Layer class implement {@link IRestorable}. When the project i
     * s reopened, the layer.restoreState(String) method will be called right after the layer object is created.
     *
     * @param layer      Layer to install.
     * @param layerGroup The Layer group to which to add the layer
     * @param sharable   Pass {@code true} if this layer can be shared across multiple globes, {@code false} otherwise
     * @return {@link IRaptorLayer} that wraps the given layer. All changes to layer meta-data (name, visibility, etc.)
     * should be made though this interface, so that they can be persisted.
     * @see ReservedLayers
     * @since 1.26
     */
    IRaptorLayer addLayer(Layer layer, LayerGroup layerGroup, boolean sharable);

    /**
     * Install a new TAK layer. Some layer names are reserved for standard {@link IRaptorLayer} instances. These are
     * specified by {@link ReservedLayers}. Plugins should not create layers that name clash with the standard layers.
     * <p>
     * Layer state can be saved in the project database when the project is closed. If you would like you use this
     * service, have your Layer class implement {@link IRestorable}. When the project is reopened, the
     * layer.restoreState(String) method will be called right after the layer object is created.
     * <p>
     * The layer is assumed to be sharable across multiple globes.  If this is not the case (such as when the layer
     * manages user interaction or unique rendering state) you should call
     * {@link #addLayer(gov.nasa.worldwind.layers.Layer, LayerGroup, boolean)}
     *
     * @param layer      TAK layer to install.
     * @param layerGroup The Layer group to which to add the layer
     * @return IRaptorLayer that wraps the given layer. All changes to layer meta-data (name, visibility, etc) should
     * be made though this interface, so that they can be persisted.
     * @see ReservedLayers
     * @since 5.2
     */
    IRaptorLayer addLayer(com.atakmap.map.layer.Layer layer, LayerGroup layerGroup);

    /**
     * Install a new TAK layer. Some layer names are reserved for standard {@link IRaptorLayer} instances. These are
     * specified by {@link ReservedLayers}. Plugins should not create layers that name clash with the standard layers.
     * <p>
     * Layer state can be saved in the project database when the project is closed. If you would like you use this
     * service, have your Layer class implement {@link IRestorable}. When the project i
     * s reopened, the layer.restoreState(String) method will be called right after the layer object is created.
     *
     * @param layer      TAK layer to install.
     * @param layerGroup The Layer group to which to add the layer
     * @param sharable   Pass {@code true} if this layer can be shared across multiple globes, {@code false} otherwise
     * @return {@link IRaptorLayer} that wraps the given layer. All changes to layer meta-data (name, visibility, etc.)
     * should be made though this interface, so that they can be persisted.
     * @see ReservedLayers
     * @since 5.2
     */
    IRaptorLayer addLayer(com.atakmap.map.layer.Layer layer, LayerGroup layerGroup, boolean sharable);

    /**
     * Install a new imagery layer. Some layer names are reserved for standard {@link IRaptorLayer} instances. These are
     * specified by {@link ReservedLayers}. Plugins should not create layers that name clash with the standard layers.
     * <p>
     * Layer state can be saved in the project database when the project is closed. If you would like you use this
     * service, have your Layer class implement {@link IRestorable}. When the project is reopened, the
     * layer.restoreState(String) method will be called right after the layer object is created.
     * <p>
     * The layer is assumed to be sharable across multiple globes.  If this is not the case (such as when the layer
     * manages user interaction or unique rendering state) you should call
     * {@link #addLayer(gov.nasa.worldwind.layers.Layer, LayerGroup, boolean)}
     *
     * @param layer Layer to install.
     * @return {@link IRaptorLayer} that wraps the given layer. All changes to layer meta-data (name, visibility, etc.)
     * should be made though this interface, so that they can be persisted.
     * @see ReservedLayers
     */
    IRaptorLayer addImageryLayer(Layer layer);

    /**
     * Get a layer by its name. Returns {@code null} if no layer by the specified name exists. Checks all layer groups
     * including elevation models. Note that there is no assurance that layers have unique names, and in the case
     * a name is specified that is tied to 2 different layers, only the first one in the list will be returned.
     * However, this method (and the addLayer methods) is relying on the plugin developer to assign names to their
     * layers that should be uncommon enough (and still human readable) to be unique, and therefore when looking up
     * the layer by name, it should be the only one by that name.
     *
     * @param name layer name
     * @return the IRaptorLayer
     * @since 1.45
     */
    IRaptorLayer getLayer(String name);

    /**
     * Get a layer by its name and group Returns {@code null} if no layer by the specified name exists in the specified group.
     * Similar to {@link #getLayer(String)}, if there are 2 layers with the same name in the same group, only the first
     * will be returned.  Again, this method is provided for a plugin to lookup a layer that was previously added,
     * so it is up to the developer to come up with an uncommon enough name (and still human readable) that it is
     * highly unlikely to have been duplicated elsewhere.
     *
     * @param name layer name
     * @return the IRaptorLayer
     * @since 1.45
     */
    IRaptorLayer getLayer(String name, LayerGroup group);

    /**
     * Remove a layer from the globe.
     *
     * @param layer Layer to remove.
     */
    void removeLayer(IRaptorLayer layer);

    /**
     * Get the {@link IGlobeLayer} corresponding to the given {@link IRaptorLayer}.
     *
     * @param raptorLayer Raptor layer of interest
     * @return corresponding globe layer
     * @since 3.0
     */
    @Internal
    @Nullable
    IGlobeLayer getGlobeLayer(IRaptorLayer raptorLayer);

    /**
     * Get a reference to the WorldWind globe component.
     *
     * @return The globe component.
     * @since 1.34
     */
    IWorldWindowGL getWorldWindow();

    /**
     * @return legacy TAK Map engine map renderer.
     * @since 5.2
     * @deprecated use {@link #getPortableMapRenderer()}
     */
    @DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
    @Deprecated(since = "5.6", forRemoval = true)
    MapRenderer3 getMapRenderer();

    /**
     * @return portable TAK Map engine map renderer.
     * @since 5.6
     */
    MapRenderer<?> getPortableMapRenderer();

    /**
     * Add a listener for globe events.  If the given listener is already registered, it will not be added a second time.
     *
     * @param l Listener to register
     * @since 1.11
     */
    void addGlobeComponentListener(IGlobeComponentListener l);

    /**
     * Remove a listener for globe events.
     *
     * @param l Listener to unregister
     * @since 1.11
     */
    void removeGlobeComponentListener(IGlobeComponentListener l);

    /**
     * Register as a select listener to IMapObjects to receive {@link MapObjectSelectEvent}
     * notifications. Be sure to call {@link #removeSelectListener(IMapObjectSelectListener)}
     * when notifications are no longer needed.
     *
     * @param listener the map object select listener to add
     * @see MapObjectSelectEvent
     * @see IMapObjectSelectListener
     * @since 1.40
     */
    void addSelectListener(IMapObjectSelectListener listener);

    /**
     * Un-register as a select listener to IMapObject select events
     *
     * @param listener the map object select listener to remove
     * @see MapObjectSelectEvent
     * @see IMapObjectSelectListener
     * @since 1.40
     */
    void removeSelectListener(IMapObjectSelectListener listener);

    /**
     * @return Test if the globe component has been initialized and is ready for use.
     * @see #addGlobeComponentListener(IGlobeComponentListener)
     * @since 1.11
     */
    boolean isGlobeComponentReady();

    /**
     * Specify a handler to be executed when this globe component is ready for use.  If the globe component is
     * presently ready, then this handler will be invoked immediately.  Invocation of the handler is always made
     * on the EDT.
     *
     * @param handler The handler to invoke if/when the globe component becomes ready
     * @see #isGlobeComponentReady()
     * @since 2.23
     */
    void doWhenReady(Runnable handler);

    /**
     * Request a redraw by the globe component.  This request is queued and coalesced with other requests that come
     * in within a time window (currently about .3 seconds).
     */
    default void redraw()
    {
        redraw(false);
    }

    /**
     * Request a redraw by the globe component.
     * <p/>
     * Note: passing {@code true} for {@code immediate} can cause significant slowdown if called too frequently.
     * It should only be used for interactions where the user must see the visible changes immediately.
     * Use with caution.
     *
     * @param immediate Pass {@code true} to redraw immediately, else queue request
     * @since 5.3
     */
    void redraw(boolean immediate);

    /**
     * This method creates a snapshot of the globe within the given bounds using the given format. If multiple calls to
     * this method will be made or if the resulting file will be manipulated after creation then pass in an ISnapshotListener.
     * The ISnapshotListener is recommended since subsequent call will be ignored if the export has not completed.
     *
     * @param minLocation         The minimum lat lon bounds for the snapshot.
     * @param maxLocation         The maximum lat lon bounds for the snapshot.
     * @param snapshotImageFormat The image format that the snapshot file will be saved as.
     * @param outputFile          The file that the snapshot will be written to. If {@code null} then the user will be prompted to choose an output file location.
     * @param snapshotListener    A callback that will be invoked after the snapshot file is created
     * @since 1.14
     */
    void createSnapshot(ILocation minLocation, ILocation maxLocation, SnapshotFormat snapshotImageFormat, File outputFile, ISnapshotListener snapshotListener);

    /**
     * Presents a file chooser to the user and with the supported file types implemented in TAKX and then performs
     * the snapshot operation.
     *
     * @param minLocation      The minimum lat lon bounds for the snapshot.
     * @param maxLocation      The maximum lat lon bounds for the snapshot.
     * @param snapshotListener A callback that will be invoked after the snapshot file is created
     * @since 1.14
     */
    void createSnapshotWithFileChooser(ILocation minLocation, ILocation maxLocation, ISnapshotListener snapshotListener);

    /**
     * Start centering the view on the given Map Object on this globe. Only one Map Object can have the "centered on"
     * setting for a globe.  If a previous Map Object is already configured as "center on" for this globe, then it
     * will be removed, as if a call to {@link #stopCenterOn()}} was made prior to calling this method.
     * <p>
     * Whenever the location of the Map Object that is "centered on" for a given globe changes, that globe will have
     * its view adjusted so that the given Map Object is kept at the center of the view.
     *
     * @param mapObject The map object to center on
     * @since 1.14
     */
    void startCenterOn(IMapObject mapObject);

    /**
     * Stop centering the view on a Map Object.  If no Map Object is currently being tracked, then this call
     * does nothing.  Otherwise, it removes the previously configured Map Object.
     */
    void stopCenterOn();

    /**
     * @return The Map Object that is configured as the "center on" for this globe, may be {@code null}
     * @see #startCenterOn(IMapObject)
     * @see #stopCenterOn()
     * @since 1.14
     */
    IMapObject getCenterOn();

    /**
     * Test to see if the given Map Object is the currently configured "center on" Map Object for this globe.
     *
     * @param mapObject Map Object of interest
     * @return {@code true} if configured as center on, {@code false} if not
     * @since 1.14
     */
    boolean isCenterOn(IMapObject mapObject);

    /**
     * Add a Map Object to the set of objects to be kept in the view of this globe.
     * <p>
     * Any time a Map Object that has been marked for "keep in view" has a location change, the globe view will
     * be adjusted so that all Map Objects that are to be kept in view are visible.
     * This may involve panning and/or zooming the globe's view.
     *
     * @param mapObject The map object to keep in view
     * @since 1.14
     */
    void addKeepInView(IMapObject mapObject);

    /**
     * Remove the given Map Object from the set of Map Objects being kept in view on this globe.  If the given Map
     * Object is not being tracked, then this call does nothing.
     *
     * @param mapObject The map object to remove from the tracking set
     * @since 1.14
     */
    void removeKeepInView(IMapObject mapObject);

    /**
     * Determine if a given Map Object is being kept in view on this globe.
     *
     * @param mapObject The Map Object of interest
     * @return {@code true} if this Map Object is being kept in view
     * @see #addKeepInView(IMapObject)
     * @see #removeKeepInView(IMapObject)
     * @since 1.14
     */
    boolean isKeepInView(IMapObject mapObject);

    /**
     * Navigate the globe view to the given lat/lon coordinates.
     *
     * @param lat Latitude on which to center
     * @param lon Longitude on which to center
     * @since 2.30
     */
    default void goTo(double lat, double lon)
    {
        goTo(lat, lon, true);
    }

    /**
     * Navigate the globe view to the given lat/lon coordinates.
     *
     * @param lat  Latitude on which to center
     * @param lon  Longitude on which to center
     * @param zoom Pass {@code true} to zoom in on the location, {@code false} to keep current eye altitude
     * @since 3.0
     */
    default void goTo(double lat, double lon, boolean zoom)
    {
        getGlobeViewController().goTo(lat, lon, zoom);
    }

    /**
     * Navigate the globe view to the given location.
     *
     * @param location Location on which to center
     * @since 2.30
     */
    default void goTo(ILocation location)
    {
        goTo(location, true);
    }

    /**
     * Navigate the globe view to the given location.
     *
     * @param location Location on which to center
     * @param zoom     Pass {@code true} to zoom in on the location, {@code false} to keep current eye altitude
     * @since 3.0
     */
    default void goTo(ILocation location, boolean zoom)
    {
        getGlobeViewController().goTo(location, zoom);
    }

    /**
     * Change the globe view to the view settings in the given camera eye.
     *
     * @param bookmark View settings to install
     * @since 2.30
     */
    default void goTo(IFavorite bookmark)
    {
        getGlobeViewController().goTo((ICameraEye) bookmark);
    }

    /**
     * Change the globe view to the view settings in the given camera eye.
     *
     * @param cameraEye View settings to install
     * @since 2.30
     */
    default void goTo(ICameraEye cameraEye)
    {
        getGlobeViewController().goTo(cameraEye);
    }

    /**
     * Navigate the globe view to show the given sector.
     *
     * @param sector Sector whose extent should be shown in the view
     * @since 2.30
     */
    default void goTo(Sector sector)
    {
        getGlobeViewController().goTo(sector);
    }

    /**
     * Navigate the globe view to show the given sector with no animations.  This is strictly in support of capturing
     * a screen snapshot of a given region.
     *
     * @param sector Sector whose extent should be shown in the view
     * @deprecated Not for plugin use
     * @since 3.5
     */
    @Internal
    @Deprecated
    @DeprecatedApi(since = "5.7", forRemoval = true, removeAt = "5.10")
    void goToImmediate(Sector sector);

    /**
     * Navigate the globe view to show the given layer.
     *
     * @param layer Layer whose extent should be shown in the view
     * @since 2.31
     */
    default void goTo(IRaptorLayer layer)
    {
        getGlobeViewController().goTo(layer);
    }

    /**
     * @return The location of the center of the globe view.
     * @since 2.37
     */
    GeoPoint getCenterLocation();

    /**
     * @return The position of the center of the globe view.
     * @since 2.37
     */
    Position getCenterPosition();

    /**
     * Navigate to the globe view to the given location and update the given MouseEvent by translating its point
     * to the center of the globe component.
     *
     * @param location Location on which to center
     * @param event    Mouse event to update
     * @since 3.0
     */
    default void goToAndTranslateMouseEvent(ILocation location, MouseEvent event)
    {
        centerOn(location, false); // Center globe on given location

        // reset event point to center of globe component
        final IWorldWindowGL wwd = getWorldWindow();
        event.translatePoint(wwd.getSurfaceWidth() / 2 - event.getX(), wwd.getSurfaceHeight() / 2 - event.getY());
    }

    /**
     * Center the globe view on the given location, no other view parameters are affected.
     *
     * @param location Location to center on
     * @since 3.1
     */
    default void centerOn(ILocation location)
    {
        centerOn(location, true);
    }

    /**
     * Center the globe view on the given location, no other view parameters are affected.
     *
     * @param location Location to center on
     * @param animate  Pass {@code true} to animate the movement, {@code false} to move instantly
     * @since 5.3
     */
    default void centerOn(ILocation location, boolean animate)
    {
        getGlobeViewController().centerOn(location.getLatitude(), location.getLongitude(), location.getElevation(), animate);
    }

    /**
     * Takes a screen point and returns the location on the globe. The elevation of the returned point is determined
     * using the highest resolution terrain model loaded for that location.
     *
     * @param point screen point to convert to location
     * @return Location of the point on the globe. {@code null} if the point is not on the globe.
     * @see ElevationManager#getElevation(double, double, ElevationManager.QueryParameters)
     *
     * @since 5.4 (moved down from IApplicationGlobe)
     */
    GeoPoint getGlobeLocationFromPoint(Point point);

    /**
     * @return The globe's view controller
     * @since 5.7
     */
    @Nonnull
    IGlobeViewController getGlobeViewController();
}
