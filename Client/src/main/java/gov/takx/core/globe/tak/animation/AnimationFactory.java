package gov.takx.core.globe.tak.animation;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.animation.AngleAnimator;
import gov.nasa.worldwind.animation.CompoundAnimator;
import gov.nasa.worldwind.animation.DoubleAnimator;
import gov.nasa.worldwind.animation.Interpolator;
import gov.nasa.worldwind.animation.PositionAnimator;
import gov.nasa.worldwind.animation.ScheduledInterpolator;
import gov.nasa.worldwind.animation.SmoothInterpolator;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.util.PropertyAccessor;
import gov.nasa.worldwind.view.ViewElevationAnimator;
import gov.nasa.worldwind.view.orbit.FlyToOrbitViewAnimator;
import gov.nasa.worldwind.view.orbit.OrbitView;
import gov.tak.api.engine.map.IMapRendererEnums;
import gov.tak.api.engine.map.MapRenderer;
import gov.tak.api.engine.map.MapSceneModel;
import gov.tak.api.engine.map.coords.GeoPoint;
import gov.tak.api.engine.math.PointD;
import gov.takx.core.globe.TakWorldWindow;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * An implementation of {@link IAnimationFactory} for TAK globe.
 *
 * @since 3.4.1
 */
@ApplicationScoped
public class AnimationFactory implements IAnimationFactory
{
    @Override
    public IAnimation createComposite(MapRenderer<?> mapRenderer, long lengthMs, IAnimation... animations)
    {
        return new TakCompoundAnimator(new ScheduledInterpolator(lengthMs), animations);
    }

    @Override
    public IAnimation createPanTo(MapRenderer<?> mapRenderer, double lat, double lon, double alt)
    {
        PositionAccessor positionAccessor = new PositionAccessor(mapRenderer);
        MapSceneModel mapSceneModel = mapRenderer.getMapSceneModel(false, IMapRendererEnums.DisplayOrigin.UpperLeft);
        GeoPoint from = GeoPoint.createMutable();
        mapSceneModel.mapProjection.inverse(new PointD(mapSceneModel.camera.target.x, mapSceneModel.camera.target.y, mapSceneModel.camera.target.z), from);

        Position begin = Position.fromDegrees(from.getLatitude(), from.getLongitude(), from.getAltitude());
        Position end = Position.fromDegrees(lat, lon, alt);
        long lengthMillis = AnimationUtils.getPanAnimationLength(begin, end);

        return new TakPositionAnimator(new SmoothInterpolator(lengthMillis), begin, end, positionAccessor);
    }

    @Override
    public IAnimation createZoomTo(MapRenderer<?> mapRenderer, double gsd)
    {
        GsdAccessor gsdAccessor = new GsdAccessor(mapRenderer);
        MapSceneModel mapSceneModel = mapRenderer.getMapSceneModel(false, IMapRendererEnums.DisplayOrigin.UpperLeft);

        double begin = mapSceneModel.gsd;
        double end = gsd;
        long lengthMillis = AnimationUtils.getZoomAnimationLength(begin, end);

        return new TakDoubleAnimator(new ScheduledInterpolator(lengthMillis), begin, end, gsdAccessor);
    }

    @Override
    public IAnimation createHeadingTo(MapRenderer<?> mapRenderer, double heading)
    {
        HeadingAccessor headingAccessor = new HeadingAccessor(mapRenderer);
        MapSceneModel mapSceneModel = mapRenderer.getMapSceneModel(false, IMapRendererEnums.DisplayOrigin.UpperLeft);

        Angle begin = Angle.fromDegrees(mapSceneModel.camera.azimuth);
        Angle end = Angle.fromDegrees(heading);
        long lengthMillis = AnimationUtils.getHeadingAnimationLength(begin, end);

        return new TakAngleAnimator(new ScheduledInterpolator(lengthMillis), begin, end, headingAccessor);
    }

    @Override
    public IAnimation createTiltTo(MapRenderer<?> mapRenderer, double tilt)
    {
        TiltAccessor tiltAccessor = new TiltAccessor(mapRenderer);
        MapSceneModel mapSceneModel = mapRenderer.getMapSceneModel(false, IMapRendererEnums.DisplayOrigin.UpperLeft);

        Angle begin = Angle.fromDegrees(90d + mapSceneModel.camera.elevation);
        Angle end = Angle.fromDegrees(tilt);
        long lengthMillis = AnimationUtils.getTiltAnimationLength(begin, end);

        return new TakAngleAnimator(new ScheduledInterpolator(lengthMillis), begin, end, tiltAccessor);
    }

    @Override
    public IAnimation createFlyTo(MapRenderer<?> mapRenderer, double lat, double lon, double alt, double gsd, double heading, double tilt, boolean useMidZoom)
    {
        TakWorldWindow wwd = (TakWorldWindow) mapRenderer.getRenderContext().getRenderSurface();

        var panTo = (TakPositionAnimator) createPanTo(mapRenderer, lat, lon, alt);
        var headingTo = (TakAngleAnimator) createHeadingTo(mapRenderer, heading);
        var tiltTo = (TakAngleAnimator) createTiltTo(mapRenderer, tilt);
        var rollTo = new AngleAnimator(null, Angle.ZERO, Angle.ZERO, new NullAngleAccessor());
        var mapSceneModel = mapRenderer.getMapSceneModel(false, IMapRendererEnums.DisplayOrigin.UpperLeft);
        double beginZoom = com.atakmap.map.MapSceneModel.range(mapSceneModel.gsd, mapSceneModel.camera.fov, mapSceneModel.height);
        double endZoom = com.atakmap.map.MapSceneModel.range(gsd, mapSceneModel.camera.fov, mapSceneModel.height);
        var zoomAccessor = new ZoomAccessor(mapRenderer);
        var viewElevationAnimator = new ViewElevationAnimator(useMidZoom ? wwd.getView().getGlobe() : null,
                beginZoom, endZoom, panTo.getBegin(), panTo.getEnd(), WorldWind.ABSOLUTE, zoomAccessor);
        long lengthMillis = AnimationUtils.getFlyToAnimationLength(panTo.getBegin(), panTo.getEnd());

        return new TakFlyToAnimator((OrbitView) wwd.getView(),
                new ScheduledInterpolator(lengthMillis), WorldWind.ABSOLUTE,
                panTo,
                viewElevationAnimator,
                headingTo,
                tiltTo,
                rollTo);
    }

    /**
     * An {@link gov.nasa.worldwind.util.PropertyAccessor.AngleAccessor} that performs a no-op.
     */
    private static class NullAngleAccessor implements PropertyAccessor.AngleAccessor
    {
        @Override
        public Angle getAngle()
        {
            return Angle.ZERO;
        }

        @Override
        public boolean setAngle(Angle angle)
        {
            return false;
        }
    }

    /**
     * An {@link IAnimation} implementation that animates a compound animation.
     */
    private static class TakCompoundAnimator extends CompoundAnimator implements IAnimation
    {
        public TakCompoundAnimator(Interpolator interpolator, IAnimation... animators)
        {
            super(interpolator, animators);
        }
    }

    /**
     * An {@link IAnimation} implementation that animates a {@link Position} using the supplied {@link PropertyAccessor.PositionAccessor}.
     */
    private static class TakPositionAnimator extends PositionAnimator implements IAnimation
    {
        public TakPositionAnimator(Interpolator interpolator, Position begin, Position end, PropertyAccessor.PositionAccessor propertyAccessor)
        {
            super(interpolator, begin, end, propertyAccessor);
        }
    }

    /**
     * An {@link IAnimation} implementation that animates a Double using the supplied {@link PropertyAccessor.DoubleAccessor}.
     */
    private static class TakDoubleAnimator extends DoubleAnimator implements IAnimation
    {
        public TakDoubleAnimator(Interpolator interpolator, double begin, double end, PropertyAccessor.DoubleAccessor propertyAccessor)
        {
            super(interpolator, begin, end, propertyAccessor);
        }
    }

    /**
     * An {@link IAnimation} implementation that animates an {@link Angle} using the supplied {@link PropertyAccessor.AngleAccessor}.
     */
    private static class TakAngleAnimator extends AngleAnimator implements IAnimation
    {
        public TakAngleAnimator(Interpolator interpolator, Angle begin, Angle end, PropertyAccessor.AngleAccessor propertyAccessor)
        {
            super(interpolator, begin, end, propertyAccessor);
        }
    }

    /**
     * An {@link IAnimation} implementation that animates a fly to animation using the supplied {@link PropertyAccessor.AngleAccessor}.
     */
    private static class TakFlyToAnimator extends FlyToOrbitViewAnimator implements IAnimation
    {
        public TakFlyToAnimator(OrbitView orbitView, Interpolator interpolator, int altitudeMode, PositionAnimator centerAnimator, DoubleAnimator zoomAnimator, AngleAnimator headingAnimator, AngleAnimator pitchAnimator, AngleAnimator rollAnimator)
        {
            super(orbitView, interpolator, altitudeMode, centerAnimator, zoomAnimator, headingAnimator, pitchAnimator, rollAnimator);
        }
    }
}
