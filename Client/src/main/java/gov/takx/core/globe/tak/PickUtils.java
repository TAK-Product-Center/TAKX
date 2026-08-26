package gov.takx.core.globe.tak;

import android.graphics.PointF;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.MapSceneModel;
import com.atakmap.map.opengl.GLMapView;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.render.Annotation;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Path;
import gov.nasa.worldwind.render.PointPlacemark;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.render.SurfaceObject;
import gov.takx.core.globe.render.GlobeMapPointRenderer;
import gov.takx.core.globe.render.MapPointPickedObject;
import gov.takx.core.mapdata.MapPoint;
import gov.takx.platform.globe.MarkerRenderable;

import java.awt.Color;
import java.awt.Point;

/**
 * Utility class for picking operations on a TAK globe.
 *
 * @since 3.4.1
 */
public final class PickUtils
{
    private PickUtils()
    {
    }

    // A color known to not lose precision when drawing to an RGB565 buffer
    public static final Color PICK_COLOR = new Color(255, 0, 0, 255);

    private static final GlobeMapPointRenderer GLOBE_MAP_POINT_RENDERER = new GlobeMapPointRenderer();

    /**
     * Creates a {@link PickedObject} out of a user object and any current picked terrain position in the current {@link DrawContext}
     *
     * @param dc         Current {@link DrawContext}
     * @param userObject A user-defined object to represent the picked map object
     * @param pickColor  The "draw color" of the item, to be rendered on the DrawContext
     * @return A new PickedObject
     */
    public static PickedObject createPickedObject(DrawContext dc, Object userObject, Color pickColor)
    {
        PickedObject terrainObject = dc.getPickedObjects().getTerrainObject();
        Position pickedPos = null;
        if (terrainObject != null)
        {
            pickedPos = terrainObject.getPosition();
        } else if (dc.getPickPoint() != null)
        {
            pickedPos = dc.getView().computePositionFromScreenPoint(dc.getPickPoint().x, dc.getPickPoint().y);
        }
        int pickColorNoAlpha = pickColor.getRGB() & 0x00FFFFFF;
        userObject = extractDelegateOwner(userObject);
        if (userObject instanceof MapPoint mapPoint)
        {
            return new MapPointPickedObject(GLOBE_MAP_POINT_RENDERER, dc.getGlobe(), pickColorNoAlpha, mapPoint, pickedPos);
        }
        return new PickedObject(pickColorNoAlpha, userObject, pickedPos, false);
    }

    /**
     * Extract a delegate owner from the picked userObject
     * @param userObject the user object
     * @return a delegate owner object or the userObject if none is found
     * @since 3.5
     */
    private static Object extractDelegateOwner(Object userObject)
    {
        if (userObject instanceof SurfaceObject surfaceObject)
        {
            final Object delegateOwner = surfaceObject.getDelegateOwner();
            if (delegateOwner != null) return delegateOwner;
        } else if (userObject instanceof Path path)
        {
            final Object delegateOwner = path.getDelegateOwner();
            if (delegateOwner != null) return delegateOwner;
        } else if (userObject instanceof PointPlacemark pointPlacemark)
        {
            final Object delegateOwner = pointPlacemark.getDelegateOwner();
            if (delegateOwner != null) return delegateOwner;
        } else if (userObject instanceof Annotation annotation)
        {
            final Object delegateOwner = annotation.getDelegateOwner();
            if (delegateOwner != null) return delegateOwner;
        } else if (userObject instanceof MarkerRenderable markerRenderable)
        {
            return markerRenderable.getMarker();
        }
        return userObject;
    }

    /**
     * Translates the pickPoint coords through LatLonAlt coords to the "currentPass" screen coords
     *
     * @param dc        Current {@link DrawContext}
     * @param glMapView Map view
     * @param pickPoint {@link Point} where the cursor is at the time
     * @return a translated Point
     */
    public static Point getTranslatedPoint(DrawContext dc, GLMapView glMapView, Point pickPoint)
    {
        Point translatedPoint = null;
        if (pickPoint == null) return null;

        // Translate pickPoint from currentScene screen coords to LL to currentPass screen coords
        MapSceneModel currentScene = glMapView.currentScene.scene;
        MapSceneModel currentPassScene = glMapView.currentPass.scene;
        PointF pickPointAndroid = new PointF(pickPoint.x, currentScene.height - pickPoint.y);
        GeoPoint pickPointGeoPoint = currentScene.inverse(pickPointAndroid, null);
        if (pickPointGeoPoint != null)
        {
            pickPointAndroid = currentPassScene.forward(pickPointGeoPoint, pickPointAndroid);
            // Adjust for WW viewport reflecting the state of the scene and not the current pass
            int yViewportDelta = dc.getView().getViewport().height - currentPassScene.height;
            int y = currentPassScene.height - (int) pickPointAndroid.y + yViewportDelta;
            translatedPoint = new Point((int) pickPointAndroid.x, y);
        } else
        {
            translatedPoint = pickPoint;
        }
        return translatedPoint;
    }

    /**
     * Check a {@link Renderable} for indicators that it is not pickable. A renderable is not pickable if it is an {@link AVList} with a {@link AVKey#LAYER} key
     * that references a {@link Layer} that is not {@link Layer#isPickEnabled() pickable} or it contains a {@link AVKey#PICK_ENABLED} key set to {@code false}.
     * @param renderable the renderable to check
     * @return {@code true} if the renderable is not pickable, {@code false} otherwise
     * @since 5.4
     */
    public static boolean isNotPickable(Renderable renderable)
    {
        if (!(renderable instanceof AVList avList)) return false;
        boolean notPickableLayer = avList.getValue(AVKey.LAYER) instanceof Layer renderableLayer && !renderableLayer.isPickEnabled();
        boolean pickDisabled = !AVListImpl.getBooleanValue(avList, AVKey.PICK_ENABLED, true);
        return notPickableLayer || pickDisabled;
    }
}
