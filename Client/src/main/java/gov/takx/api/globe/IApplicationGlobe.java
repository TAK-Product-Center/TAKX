package gov.takx.api.globe;

import com.atakmap.map.Globe;
import com.atakmap.map.opengl.GLMapView;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.DrawContext;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.render.RendererConstants;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.MapToolType;
import gov.takx.api.platform.IRaptorLifecycleListener;
import gov.takx.core.globe.GlobeViewController;
import gov.takx.core.globe.render.RenderingContext;
import gov.takx.platform.utils.WWUtils;
import jakarta.annotation.Nonnull;

import java.awt.Cursor;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.io.File;

/**
 * Interface representing a globe in the TAKX application.
 *
 * @since 3.4 (renamed from IRaptorGlobe)
 */
public interface IApplicationGlobe extends IGlobeDelegate, IRaptorLifecycleListener
{
    /**
     * Set the given cursor on the globe component.
     *
     * @param cursor Cursor to install
     * @since 3.5
     */
    void setCursor(@Nonnull Cursor cursor);

    void setMapTool(MapToolType mapToolType);

    IFavorite getViewBookmark();

    /**
     * Provides a means of toggling the globe's behavior when the mouse is clicked somewhere on the globe.
     *
     * @param flag true, if a click should center the globe at the click location
     */
    void setDisableCenterGlobeOnClick(boolean flag);

    /**
     * Provides a means of locking just mouse globe navigation, including center globe on click.
     *
     * @param flag true, if mouse nav is locked
     */
    void setMouseNavLock(boolean flag);

    /**
     * Provides a means of locking just keyboard globe navigation.
     *
     * @param flag true, if keyboard nav is locked
     */
    void setKeyboardNavLock(boolean flag);

    default void resetWorldView()
    {
        getGlobeViewController().resetWorldView();
    }

    default void resetHeading()
    {
        getGlobeViewController().resetHeading();
    }

    default void resetTilt()
    {
        getGlobeViewController().resetTilt();
    }

    default void resetHeadingAndTilt()
    {
        getGlobeViewController().resetHeadingAndTilt();
    }

    IGlobeMapObject getGlobeMapObject(String key);

    void setFlatWorldMode(boolean state);

    void addGlobeMapObjectChangeListener(IGlobeMapObjectChangeListener globeMapObjectChangeListener);

    void removeGlobeMapObjectChangeListener(IGlobeMapObjectChangeListener globeMapObjectChangeListener);

    void setSelectListenerActive(boolean setActive);

    void addGlobeEyeElevationListener(IGlobeEyeElevationListener globeEyeElevationListener);

    void removeGlobeEyeElevationListener(IGlobeEyeElevationListener globeEyeElevationListener);

    void addMouseListener(MouseListener listener);

    void removeMouseListener(MouseListener listener);

    void addMouseMotionListener(MouseMotionListener listener);

    void removeMouseMotionListener(MouseMotionListener listener);

    void addMouseWheelListener(MouseWheelListener listener);

    void removeMouseWheelListener(MouseWheelListener listener);

    IGlobeMapObjectManager getGlobeMapObjectManager();

    IGlobeLayerManager getGlobeLayerManager();

    File getImageryDirectoryFile();

    java.util.List<File> getImageryDirectoryFileExtra();

    boolean isInstallLocation(String s);

    void saveLastViewBookmark();

    /**
     * Set the <em>offline mode</em> for WMS requests. If {@code offlineMode} is {@code true}, then NO network
     * traffic will be allowed for WMS requests. If {@code false}, then normal WMS traffic is allowed.
     *
     * @param offlineMode New offline mode
     */
    void setWMSOfflineMode(boolean offlineMode);

    /**
     * Set the Z Order for rendering the track points of the given map object.  All map objects start out with a
     * default Z ordering of -1 ({@link RendererConstants#NO_Z_ORDER}).  All map objects with NO Z order are rendered first with
     * no guaranteed ordering among them.  All other map objects will have their track points rendered in order of
     * increasing Z order.  The highest Z order is rendered last and ensures that it "draws on top" of the others.
     *
     * @param mapObject The map object to affect
     * @param zOrder    The new Z order for the map object, use {@link RendererConstants#NO_Z_ORDER} to clear the Z ordering
     */
    void setZOrder(IMapObject mapObject, int zOrder);

    /**
     * Get the {@link GLMapView} for the globe.
     * @return the {@link GLMapView}
     * @since 5.7
     */
    GLMapView getGLMapView();

    /**
     * @return The RenderingContext for the globe
     */
    RenderingContext getRenderingContext();

    /**
     * @return The current rendering engine
     * @since 1.49
     */
    IRenderingEngine getPrimaryRenderingEngine();

    /**
     * Invoke {@link IRenderingEngine#preRender(DrawContext)} on all rendering engines.
     *
     * @param dc Draw context
     * @since 2.29
     */
    void preRenderRenderers(DrawContext dc);

    /**
     * Invoke {@link IRenderingEngine#render(DrawContext)} on all rendering engines.
     *
     * @param dc Draw context
     * @since 2.29
     */
    void renderRenderers(DrawContext dc);

    /**
     * Copy the formatted position text (using the current cursor location and coordinate system preference)
     * to the clipboard.
     */
    void copyPositionToClipboard();

    /**
     * Copy the formatted position text (using position parameter and the current coordinate
     * system preference) to the clipboard. Use {@link #copyPositionToClipboard()} if you want to copy the
     * current position of the cursor to the clipboard.
     *
     * @param position Globe location to copy to clipboard.  Must not be {@code null}.
     * @since 2.38
     */
    default void copyPositionToClipboard(@Nonnull Position position)
    {
        copyLocationToClipboard(WWUtils.positionToRaptorLocation(position));
    }

    /**
     * Copy the formatted location text (using location parameter and the current coordinate
     * system preference) to the clipboard.
     *
     * @param location Globe location to copy to clipboard.  Must not be {@code null}.
     * @since 5.3
     */
    void copyLocationToClipboard(@Nonnull ILocation location);

    /**
     * Overridden to return the more derived type.
     * <p/>
     * {@inheritDoc}
     *
     * @since 1.34
     */
    @Override
    IApplicationGlobe getGlobeForComponent(WorldWindow wwd);

    /**
     * Deactivate a globe.  This is called when a globe is closed, not when the overall project is closing.
     * The distinction is needed since the deployments holding plugin-supplied renderers have already been shutdown
     * when the project services are closing.
     *
     * @since 2.29
     */
    void deactivate();

    /**
     * Deactivate the plugin-supplied renderers on this globe.  This is called from the PluginRendererManager
     * just before the plugin containers are destroyed.
     *
     * @since 2.29
     */
    void deactivatePluginRenderers();

    Globe getNativeGlobe();

    /**
     * @return The view controller for this globe
     * @since 5.7
     */
    @Override
    @Nonnull
    GlobeViewController getGlobeViewController();
}
