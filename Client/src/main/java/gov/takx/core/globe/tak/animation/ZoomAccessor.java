package gov.takx.core.globe.tak.animation;

import gov.nasa.worldwind.util.PropertyAccessor;
import gov.tak.api.engine.map.IMapRendererEnums;
import gov.tak.api.engine.map.IMapRendererEnums.CameraCollision;
import gov.tak.api.engine.map.MapCamera;
import gov.tak.api.engine.map.MapRenderer;
import gov.tak.api.engine.map.MapSceneModel;
import gov.tak.api.engine.map.coords.GeoPoint;
import gov.tak.api.engine.math.PointD;
import jakarta.annotation.Nonnull;

/**
 * A {@link PropertyAccessor} for zoom level in TAK globe. Zoom level is expressed in distance from camera in meters.
 *
 * @since 3.4.1
 */
final class ZoomAccessor implements PropertyAccessor.DoubleAccessor
{
    MapRenderer<?> mapRenderer;

    public ZoomAccessor(@Nonnull MapRenderer<?> mapRenderer)
    {
        this.mapRenderer = mapRenderer;
    }

    @Override
    public Double getDouble()
    {
        MapSceneModel mapSceneModel = mapRenderer.getMapSceneModel(false, IMapRendererEnums.DisplayOrigin.UpperLeft);
        return com.atakmap.map.MapSceneModel.range(mapSceneModel.gsd, mapSceneModel.camera.fov, mapSceneModel.height);
    }

    @Override
    public boolean setDouble(Double zoom)
    {
        MapSceneModel mapSceneModel = mapRenderer.getMapSceneModel(false, IMapRendererEnums.DisplayOrigin.UpperLeft);
        MapCamera camera = mapSceneModel.camera;
        PointD target = camera.target;
        GeoPoint centerPoint = GeoPoint.createMutable();
        if (!mapSceneModel.mapProjection.inverse(new PointD(target.x, target.y, target.z), centerPoint)) return false;
        double gsd = com.atakmap.map.MapSceneModel.gsd(zoom, camera.fov, mapSceneModel.height);
        return mapRenderer.lookAt(centerPoint, gsd, camera.azimuth, 90d + camera.elevation, CameraCollision.AdjustFocus, false);
    }
}
