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
 * A {@link PropertyAccessor} for heading in TAK globe.
 *
 * @since 3.4.1
 */
final class HeadingAccessor implements PropertyAccessor.AngleAccessor
{
    MapRenderer<?> mapRenderer;

    public HeadingAccessor(@Nonnull MapRenderer<?> mapRenderer)
    {
        this.mapRenderer = mapRenderer;
    }

    @Override
    public Angle getAngle()
    {
        MapSceneModel mapSceneModel = mapRenderer.getMapSceneModel(false, IMapRendererEnums.DisplayOrigin.UpperLeft);
        return Angle.fromDegrees(mapSceneModel.camera.azimuth);
    }

    @Override
    public boolean setAngle(Angle azimuth)
    {
        MapSceneModel mapSceneModel = mapRenderer.getMapSceneModel(false, IMapRendererEnums.DisplayOrigin.UpperLeft);
        MapCamera camera = mapSceneModel.camera;
        PointD target = camera.target;
        GeoPoint centerPoint = GeoPoint.createMutable();
        if (!mapSceneModel.mapProjection.inverse(new PointD(target.x, target.y, target.z), centerPoint)) return false;
        return mapRenderer.lookAt(centerPoint, mapSceneModel.gsd, azimuth.degrees, 90d + camera.elevation, CameraCollision.AdjustFocus, false);
    }
}
