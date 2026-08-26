package gov.takx.core.globe.tak.animation;

import gov.nasa.worldwind.geom.Angle;
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
 * A {@link PropertyAccessor} for tilt in TAK globe.
 *
 * @since 3.4.1
 */
final class TiltAccessor implements PropertyAccessor.AngleAccessor
{
    MapRenderer<?> mapRenderer;

    public TiltAccessor(@Nonnull MapRenderer<?> mapRenderer)
    {
        this.mapRenderer = mapRenderer;
    }

    @Override
    public Angle getAngle()
    {
        MapSceneModel mapSceneModel = mapRenderer.getMapSceneModel(false, IMapRendererEnums.DisplayOrigin.UpperLeft);
        return Angle.fromDegrees(90d + mapSceneModel.camera.elevation);
    }

    @Override
    public boolean setAngle(Angle tilt)
    {
        MapSceneModel mapSceneModel = mapRenderer.getMapSceneModel(false, IMapRendererEnums.DisplayOrigin.UpperLeft);
        MapCamera camera = mapSceneModel.camera;
        PointD target = camera.target;
        GeoPoint centerPoint = GeoPoint.createMutable();
        if (!mapSceneModel.mapProjection.inverse(new PointD(target.x, target.y, target.z), centerPoint)) return false;
        return mapRenderer.lookAt(centerPoint, mapSceneModel.gsd, camera.azimuth, tilt.degrees, CameraCollision.AdjustFocus, false);
    }
}
