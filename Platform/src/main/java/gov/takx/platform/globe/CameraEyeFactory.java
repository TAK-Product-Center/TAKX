package gov.takx.platform.globe;

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.view.BasicView;
import gov.nasa.worldwind.view.orbit.OrbitView;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.ICameraEye;
import gov.takx.api.shapes.IShape;
import gov.takx.api.shapes.IShapeService;
import jakarta.annotation.Nonnull;

/**
 * Provides convenient creation methods for creating camera eye representations.
 * <p/>
 * The zoom value is the distance between the eye position and the center
 * position. The center position is used as the point about which the heading and pitch rotate. It is defined by the
 * intersection of a ray from the eye position through the center of the viewport with the surface of the globe.
 *
 * @since 1.11
 */
public final class CameraEyeFactory
{
    private CameraEyeFactory()
    {
    }

    /**
     * Creates a camera eye with 0.0 for latitude, longitude, and heading, useful for creating a camera eye when calling
     * {@link IShapeService#gotoShape(IShape, ICameraEye)}
     *
     * @param heading Heading, in degrees
     * @param pitch   Pitch, in degrees
     * @param zoom    Zoom
     * @return ICameraEye implementation
     */
    public static ICameraEye createCameraEye(final double heading, final double pitch, final double zoom)
    {
        return new CameraEye(0, 0, 0, heading, pitch, zoom);
    }

    /**
     * Convenience method for creating a camera eye instance.
     *
     * @param latitude  Latitude, in degrees
     * @param longitude Longitude, in degrees
     * @param elevation Elevation, in meters
     * @param heading   Heading, in degrees
     * @param pitch     Pitch, in degrees
     * @param zoom      Zoom
     * @return ICameraEye implementation
     */
    public static ICameraEye createCameraEye(final double latitude, final double longitude, final double elevation,
                                             final double heading, final double pitch, final double zoom)
    {
        return new CameraEye(latitude, longitude, elevation, heading, pitch, zoom);
    }

    /**
     * Convenience method for creating a camera eye instance. This method uses the latitude, longitude, and
     * elevation from the location, and the heading, pitch, and zoom level defined by the {@code cameraEye}.
     *
     * @param location the location specifying the latitude, longitude, and elevation
     * @param cameraEye the heading, pitch, and zoom level to set the resulting camera eye to at that location
     * @return ICameraEye implementation
     * @since 3.3
     */
    public static ICameraEye createCameraEye(@Nonnull ILocation location, @Nonnull ICameraEye cameraEye)
    {
        return new CameraEye(location.getLatitude(), location.getLongitude(),
                location.getElevation(), cameraEye.getHeading(), cameraEye.getPitch(), cameraEye.getZoom());
    }

    /**
     * Convenience method for creating a camera eye instance from a WorldWind View.
     *
     * @param view WW View to process
     * @return ICameraEye instance initialized from view properties
     * @since 2.35
     */
    public static ICameraEye createCameraEye(View view)
    {
        if (view == null)
        {
            return createCameraEye(0, 0, 0);
        } else
        {
            final Position position = ((BasicView) view).getCenterPosition();

            // Get zoom if we have an OrbitView, otherwise use eye position elevation
            double zoomOrElevation = (view instanceof OrbitView)
                    ? ((OrbitView) view).getZoom()
                    : view.getEyePosition().elevation;

            return createCameraEye(position.getLatitude().getDegrees(), position.getLongitude().getDegrees(),
                    position.getElevation(), view.getHeading().getDegrees(), view.getPitch().getDegrees(), zoomOrElevation);
        }
    }

    /**
     * Implementation of {@link ICameraEye}.
     *
     * @since 2.30
     */
    private record CameraEye(double latitude, double longitude, double elevation, double heading, double pitch,
            double zoom) implements ICameraEye
    {
        @Override
        public double getLatitude()
        {
            return latitude;
        }

        @Override
        public double getLongitude()
        {
            return longitude;
        }

        @Override
        public double getElevation()
        {
            return elevation;
        }

        @Override
        public double getHeading()
        {
            return heading;
        }

        @Override
        public double getPitch()
        {
            return pitch;
        }

        @Override
        public double getZoom()
        {
            return zoom;
        }
    }
}
