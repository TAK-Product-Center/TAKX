package gov.takx.core.globe;

import com.atakmap.map.CameraController;
import com.atakmap.map.LegacyAdapters;
import com.atakmap.map.MapRenderer3;
import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.View;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Extent;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globes.Earth;
import gov.nasa.worldwind.view.BasicView;
import gov.nasa.worldwind.view.orbit.OrbitViewLimits;
import gov.tak.api.engine.map.IMapRendererEnums;
import gov.tak.api.engine.map.IMapRendererEnums.CameraCollision;
import gov.tak.api.engine.map.MapRenderer;
import gov.tak.api.engine.map.MapSceneModel;
import gov.tak.api.engine.map.coords.GeoPoint;
import gov.tak.platform.graphics.PointF;
import gov.tak.platform.marshal.MarshalManager;
import gov.takx.api.annotation.Headless;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.ICameraEye;
import gov.takx.api.globe.IGlobeViewController;
import gov.takx.api.globe.IWorldWindowGL;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.core.globe.tak.animation.IAnimation;
import gov.takx.core.globe.tak.animation.IAnimationFactory;
import gov.takx.core.globe.tak.animation.IAnimationService;
import gov.takx.platform.globe.CameraEyeFactory;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * Service to allow control of a globe's view parameters.
 * <p/>
 * @implNote This class takes in parameters as expressed by WorldWind, limits them against the current {@link View#getViewPropertyLimits()}
 * and translates them to TAK operations.
 *
 * @since 3.4 (renamed from NavigationManager in 5.7)
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class GlobeViewController implements IGlobeViewController
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    @Headless
    private boolean headless;

    @Inject
    private IAnimationService animationService;

    @Inject
    private IAnimationFactory animationFactory;

    private final IWorldWindowGL wwd;
    private final MapRenderer<?> renderer;
    private final MapRenderer3 mapRenderer3;

    public GlobeViewController(IWorldWindowGL wwd, MapRenderer<?> renderer)
    {
        this.wwd = wwd;
        this.renderer = renderer;
        mapRenderer3 = LegacyAdapters.adapt(renderer);
    }

    @Override
    @Nonnull
    public ICameraEye getCurrentCameraEye()
    {
        return CameraEyeFactory.createCameraEye(wwd.getView());
    }

    @Override
    public void resetWorldView()
    {
        if (headless) return;

        var eyeAltitude = Configuration.getDoubleValue(AVKey.INITIAL_ALTITUDE);

        // If that didn't work, fall back to a suitable default (2 x globe-radius).
        if (eyeAltitude == null) eyeAltitude = 2 * Earth.WGS84_EQUATORIAL_RADIUS;

        final var centerLocation = getCenterLocation();
        goTo(centerLocation.getLatitude(), centerLocation.getLongitude(), eyeAltitude);
    }

    @Override
    public void resetHeading()
    {
        startAnimation(animationFactory.createHeadingTo(renderer, 0));
    }

    @Override
    public void resetTilt()
    {
        startAnimation(animationFactory.createTiltTo(renderer, 0));
    }

    @Override
    public void resetHeadingAndTilt()
    {
        if (headless) return;
        double limitAzimuth = limitHeading(0);
        double limitTilt = limitPitch(0);
        var headingTo = animationFactory.createHeadingTo(renderer, limitAzimuth);
        var tiltTo = animationFactory.createTiltTo(renderer, limitTilt);
        var composite = animationFactory.createComposite(renderer, 1_500, headingTo, tiltTo);
        startAnimation(composite);
    }

    void panBy(float deltaX, float deltaY)
    {
        animationService.stopAllAnimations(renderer);
        CameraController.Interactive.panBy(mapRenderer3, deltaX, deltaY, IMapRendererEnums.DisplayOrigin.UpperLeft,
                CameraCollision.AdjustFocus, false, false);

        limitTargetCameraLocation();
    }

    void panTo(GeoPoint mouseDownLocation, int mouseX, int mouseY)
    {
        animationService.stopAllAnimations(renderer);
        CameraController.Interactive.panTo(mapRenderer3, toPlatformGeopoint(mouseDownLocation),
                mouseX, mouseY, IMapRendererEnums.DisplayOrigin.UpperLeft, CameraCollision.AdjustFocus, false, false);

        limitTargetCameraLocation();
    }

    private void limitTargetCameraLocation()
    {
        // Check the new target focus location to see if it needs limiting
        MapSceneModel mapSceneModel = renderer.getMapSceneModel(false, IMapRendererEnums.DisplayOrigin.UpperLeft);
        GeoPoint geoPoint = GeoPoint.createMutable();
        if (mapSceneModel.inverse(new PointF(mapSceneModel.focusx, mapSceneModel.focusy), geoPoint))
        {
            GeoPoint limitLocation = limitLatLon3(geoPoint.getLatitude(), geoPoint.getLongitude(), 0);
            if (Double.compare(geoPoint.getLatitude(), limitLocation.getLatitude()) != 0 ||
                    Double.compare(geoPoint.getLongitude(), limitLocation.getLongitude()) != 0)
            {
                CameraController.Programmatic.panTo(mapRenderer3, toPlatformGeopoint(limitLocation), false);
            }
        }
    }

    /**
     * Navigate the globe view to show the given sector with no animations.  This is strictly in support of capturing
     * a screen snapshot of a given region.
     *
     * @param sector Sector whose extent should be shown in the view
     * @since 3.5
     */
    public void goToImmediate(@Nonnull Sector sector)
    {
        final var zoomData = getSectorZoomData(sector);
        final var centerPosition = zoomData.centerPosition;
        centerOn(centerPosition.getLatitude().degrees, centerPosition.getLongitude().degrees, centerPosition.getAltitude(), false);
        setHeading(0, false);
        setTilt(0, false);
        setEyeAltitude(zoomData.zoom(), false);
    }

    @Override
    public void goTo(final double lat, final double lon, boolean zoom)
    {
        double viewElevation = wwd.getView().getEyePosition().getElevation();

        double zoomValue = zoom
                ? Math.min(viewElevation, wwd.getModel().getGlobe().getRadius() / 100.0)
                : viewElevation;
        goTo(lat, lon, zoomValue);
    }

    @Override
    public void goTo(@Nonnull Sector sector)
    {
        if (!sector.equals(Sector.EMPTY_SECTOR))
        {
            final var zoomData = getSectorZoomData(sector);
            final var centerPosition = zoomData.centerPosition;
            goTo(centerPosition.getLatitude().degrees, centerPosition.getLongitude().degrees, zoomData.zoom);
        } else
        {
            logger.warn("Request to goTo an empty sector");
        }
    }

    /**
     * From the given {@code sector} determine the center point and zoom factor to focus the view on that sector.
     * @since 5.4.1
     */
    @Nonnull
    private SectorZoomData getSectorZoomData(@Nonnull Sector sector)
    {
        final double ve = wwd.getSceneController().getVerticalExaggeration();

        final gov.nasa.worldwind.globes.Globe globe = wwd.getModel().getGlobe();
        final double[] minAndMaxElevations = globe.getMinAndMaxElevations(sector);
        final double minElevation = minAndMaxElevations[0];
        final double maxElevation = minAndMaxElevations[1];
        final Position position = new Position(sector.getCentroid(), maxElevation);
        final Angle fov = wwd.getView().getFieldOfView();

        final Extent extent = Sector.computeBoundingCylinder(globe, ve, sector, minElevation, maxElevation);
        final double zoom = extent.getRadius() / (fov.tanHalfAngle() * fov.cosHalfAngle());
        return new SectorZoomData(position, zoom);
    }

    @Override
    public void goTo(@Nonnull ICameraEye cameraEye, boolean useMidZoom)
    {
        if (headless) return;
        MapSceneModel mapSceneModel = renderer.getMapSceneModel(false, IMapRendererEnums.DisplayOrigin.UpperLeft);
        GeoPoint limitLocation = limitLatLon(cameraEye.getLatitude(), cameraEye.getLongitude(), cameraEye.getElevation());
        double limitZoom = limitZoom(cameraEye.getZoom());
        double gsd = zoomToGsd(limitZoom, mapSceneModel);
        double limitAzimuth = limitHeading(cameraEye.getHeading());
        double limitTilt = limitPitch(cameraEye.getPitch());
        IAnimation flyTo = animationFactory.createFlyTo(renderer, limitLocation.getLatitude(), limitLocation.getLongitude(), limitLocation.getAltitude(), gsd, limitAzimuth, limitTilt, useMidZoom);
        startAnimation(flyTo);
    }

    /**
     * Set the globe's tilt with animation.
     *
     * @param tilt the tilt
     * @since 3.4.1
     */
    public void setTilt(double tilt)
    {
        setTilt(tilt, true);
    }

    /**
     * Go to the given layer's valid go-to sector (if one exists) on the specified globe.
     * <p>
     * Note: This was originally part of WWUtils which moved from client to platform in version 2.38
     *
     * @param raptorLayer Layer to "go to"
     */
    @Override
    public void goTo(@Nonnull IRaptorLayer raptorLayer)
    {
        final Sector sector = raptorLayer.getSector();
        if (sector != null) goTo(sector);
    }

    public gov.tak.api.maps.coords.GeoPoint getCenterLocation()
    {
        final Position position = ((BasicView) wwd.getView()).getCenterPosition();
        return new gov.tak.api.maps.coords.GeoPoint(position.getLatitude().getDegrees(), position.getLongitude().getDegrees(), position.getElevation());
    }

    @Override
    public void centerOn(double latitude, double longitude, double eyeAltitude, boolean animated)
    {
        if (headless) return;
        GeoPoint limitLocation = limitLatLon3(latitude, longitude, eyeAltitude);
        if (animated)
        {
            IAnimation panTo = animationFactory.createPanTo(renderer, limitLocation.getLatitude(), limitLocation.getLongitude(), limitLocation.getAltitude());
            startAnimation(panTo);
        } else
        {
            animationService.stopAllAnimations(renderer);
            CameraController.Programmatic.panTo(mapRenderer3, toPlatformGeopoint(limitLocation), false);
        }
    }

    @Override
    public void goTo(double lat, double lon, double eyeAltitude)
    {
        if (headless) return;
        final var location = limitLatLon(lat, lon, 0);
        final var mapSceneModel = renderer.getMapSceneModel(false, IMapRendererEnums.DisplayOrigin.UpperLeft);
        final var flyTo = animationFactory.createFlyTo(renderer, location.getLatitude(), location.getLongitude(), 0,
                zoomToGsd(limitZoom(eyeAltitude), mapSceneModel), limitHeading(0), limitPitch(0));

        startAnimation(flyTo);
    }

    @Override
    public void setHeading(double heading, boolean animated)
    {
        if (headless) return;
        animationService.stopAllAnimations(renderer);
        CameraController.Programmatic.rotateTo(mapRenderer3, limitHeading(heading), animated);
    }

    @Override
    public void setTilt(double tilt, boolean animated)
    {
        if (headless) return;
        animationService.stopAllAnimations(renderer);
        MapSceneModel mapSceneModel = renderer.getMapSceneModel(false, IMapRendererEnums.DisplayOrigin.UpperLeft);
        GeoPoint geoPoint = GeoPoint.createMutable();
        if (mapSceneModel.inverse(new PointF(mapSceneModel.focusx, mapSceneModel.focusy), geoPoint))
        {
            double tiltBy = (90d + mapSceneModel.camera.elevation) - limitPitch(tilt);
            // stop tilting towards surface when a collision occurs
            MapRenderer3.CameraCollision collide = (tiltBy < 0d) ? MapRenderer3.CameraCollision.Abort : MapRenderer3.CameraCollision.AdjustFocus;
            CameraController.Interactive.tiltTo(mapRenderer3, limitPitch(tilt), toPlatformGeopoint(geoPoint), mapSceneModel.focusx, mapSceneModel.focusy, collide, animated);
        } else
        {
            CameraController.Programmatic.tiltTo(mapRenderer3, limitPitch(tilt), animated);
        }
    }

    @Override
    public void setEyeAltitude(double eyeAltitude, boolean animated)
    {
        if (headless) return;
        animationService.stopAllAnimations(renderer);
        MapSceneModel mapSceneModel = renderer.getMapSceneModel(false, IMapRendererEnums.DisplayOrigin.UpperLeft);
        double limitZoom = limitZoom(eyeAltitude);
        double gsd = zoomToGsd(limitZoom, mapSceneModel);
        CameraController.Programmatic.zoomTo(mapRenderer3, gsd, animated);
    }

    /**
     * Set's the globe's zoom distance and center position.
     * @param zoom the zoom level expressed as camera eye altitude
     * @param latitude the latitude position to move to center screen
     * @param longitude the longitude position to move to center screen
     * @param animated true to animate the change, otherwise false
     * @since 5.2
     */
    public void zoomTo(double zoom, double latitude, double longitude, boolean animated)
    {
        if (headless) return;
        MapSceneModel mapSceneModel = renderer.getMapSceneModel(false, IMapRendererEnums.DisplayOrigin.UpperLeft);
        double limitZoom = limitZoom(zoom);
        double gsd = zoomToGsd(limitZoom, mapSceneModel);
        double zoomScale = gsd / mapSceneModel.gsd;
        GeoPoint limitLocation = limitLatLon3(latitude, longitude, 0);
        animationService.stopAllAnimations(renderer);
        // abort camera motion if collision occurs when zooming in
        MapRenderer3.CameraCollision collide = (zoomScale < 1d) ? CameraCollision.Abort : CameraCollision.AdjustCamera;
        CameraController.Interactive.zoomTo(mapRenderer3, gsd, toPlatformGeopoint(limitLocation), mapSceneModel.width / 2f, mapSceneModel.height / 2f, collide, animated);
    }

    private void startAnimation(IAnimation animation)
    {
        animationService.stopAllAnimations(renderer);
        animationService.addAnimation(renderer, animation);
    }

    private GeoPoint limitLatLon3(double lat, double lng, double elevation)
    {
        View view = wwd.getView();
        Position limitPosition = ((OrbitViewLimits) view.getViewPropertyLimits()).limitCenterPosition(view, Position.fromDegrees(lat, lng, elevation));
        return new GeoPoint(limitPosition.latitude.degrees, limitPosition.longitude.degrees, limitPosition.elevation);
    }

    private GeoPoint limitLatLon(double lat, double lng, double elevation)
    {
        View view = wwd.getView();
        Position limitPosition = ((OrbitViewLimits) view.getViewPropertyLimits()).limitCenterPosition(view, Position.fromDegrees(lat, lng, elevation));
        return new GeoPoint(limitPosition.latitude.degrees, limitPosition.longitude.degrees, limitPosition.elevation);
    }

    private double limitHeading(double heading)
    {
        View view = wwd.getView();
        final double normalizedHeading = Angle.normalizedDegrees(heading); // Allow rotation to continue
        return view.getViewPropertyLimits().limitHeading(view, Angle.fromDegrees(normalizedHeading)).degrees;
    }

    private double limitPitch(double pitch)
    {
        View view = wwd.getView();
        return view.getViewPropertyLimits().limitPitch(view, Angle.fromDegrees(pitch)).degrees;
    }

    private double limitZoom(double zoom)
    {
        View view = wwd.getView();
        return ((OrbitViewLimits) view.getViewPropertyLimits()).limitZoom(view, zoom);
    }

    /**
     * Converts a WW zoom level to a TAK GSD. WW expresses zoom level in terms of camera eye altitude (range) and TAK expects GSD.
     * @param zoom the camera eye altitude
     * @param mapSceneModel a map scene model
     * @return the calculated GSD
     */
    private double zoomToGsd(double zoom, MapSceneModel mapSceneModel)
    {
        return com.atakmap.map.MapSceneModel.gsd(zoom, mapSceneModel.camera.fov, mapSceneModel.height);
    }

    private com.atakmap.coremap.maps.coords.GeoPoint toPlatformGeopoint(GeoPoint geoPoint)
    {
        return MarshalManager.marshal(geoPoint, GeoPoint.class, com.atakmap.coremap.maps.coords.GeoPoint.class);
    }

    /**
     * Data required to zoom to a Sector.
     *
     * @param centerPosition Center point for view
     * @param zoom Zoom factor to use
     * @since 5.4.1
     */
    private record SectorZoomData(Position centerPosition, double zoom)
    {
    }
}
