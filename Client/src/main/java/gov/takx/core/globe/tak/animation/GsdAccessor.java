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
 * A {@link PropertyAccessor} for ground surface distance in TAK globe.
 *
 * @since 3.4.1
 */
final class GsdAccessor implements PropertyAccessor.DoubleAccessor
{
    MapRenderer<?> mapRenderer;

    public GsdAccessor(@Nonnull MapRenderer<?> mapRenderer)
    {
        this.mapRenderer = mapRenderer;
    }

    @Override
    public Double getDouble()
    {
        return mapRenderer.getMapSceneModel(false, IMapRendererEnums.DisplayOrigin.UpperLeft).gsd;
    }

    @Override
    public boolean setDouble(Double gsd)
    {
        MapSceneModel mapSceneModel = mapRenderer.getMapSceneModel(false, IMapRendererEnums.DisplayOrigin.UpperLeft);
        MapCamera camera = mapSceneModel.camera;
        PointD target = camera.target;
        GeoPoint centerPoint = GeoPoint.createMutable();
        if (!mapSceneModel.mapProjection.inverse(new PointD(target.x, target.y, target.z), centerPoint)) return false;
        return mapRenderer.lookAt(centerPoint, gsd, camera.azimuth, 90d + camera.elevation, CameraCollision.AdjustFocus, false);
    }
}
