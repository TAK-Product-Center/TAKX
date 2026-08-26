package gov.takx.core.globe.tak.animation;

import gov.nasa.worldwind.geom.Position;
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
 * A {@link PropertyAccessor} for position in TAK globe.
 *
 * @since 3.4.1
 */
final class PositionAccessor implements PropertyAccessor.PositionAccessor
{
    MapRenderer<?> mapRenderer;

    public PositionAccessor(@Nonnull MapRenderer<?> mapRenderer)
    {
        this.mapRenderer = mapRenderer;
    }

    @Override
    public Position getPosition()
    {
        MapSceneModel mapSceneModel = mapRenderer.getMapSceneModel(false, IMapRendererEnums.DisplayOrigin.UpperLeft);
        PointD target = mapSceneModel.camera.target;
        GeoPoint centerPoint = GeoPoint.createMutable();
        mapSceneModel.mapProjection.inverse(new PointD(target.x, target.y, target.z), centerPoint);
        return Position.fromDegrees(centerPoint.getLatitude(), centerPoint.getLongitude(), centerPoint.getAltitude());
    }

    @Override
    public boolean setPosition(Position position)
    {
        MapSceneModel mapSceneModel = mapRenderer.getMapSceneModel(false, IMapRendererEnums.DisplayOrigin.UpperLeft);
        MapCamera camera = mapSceneModel.camera;
        return mapRenderer.lookAt(new GeoPoint(position.getLatitude().degrees, position.getLongitude().degrees, position.getAltitude()), mapSceneModel.gsd, camera.azimuth, 90d + camera.elevation, CameraCollision.AdjustFocus, false);
    }
}
