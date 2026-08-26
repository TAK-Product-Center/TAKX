package gov.takx.api.messages;

import gov.takx.api.annotation.Internal;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.commons.constants.CommServiceType;
import gov.takx.platform.utils.UnitsOfMeasureUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.concurrent.CountDownLatch;

/**
 * @since 1.3
 */
public interface IPrePersistRaptorDataMessage extends IRaptorDataMessage, IPrePersistRaptorMessage
{
    /**
     * Establishes the source map object family that originated this message.
     *
     * @param sourceFamilyType Source Device Type
     */
    void setSourceFamilyType(String sourceFamilyType);

    /**
     * Establishes the source map object device type that originated this message.
     *
     * @param sourceDeviceType Source Device Type
     */
    void setSourceDeviceType(String sourceDeviceType);

    /**
     * Establishes the source map object device ID that originated this message.
     *
     * @param sourceDeviceId Source Device ID
     */
    void setSourceDeviceId(String sourceDeviceId);

    /**
     * Reset the location attributes, including latitude, longitude, and elevation. Both hasLocation() and hasElevation()
     * will return false after invoking this method.
     */
    void resetLocation();

    void setSourceName(@Nonnull String sourceName);

    void setSourceType(@Nullable CommServiceType sourceType);

    /**
     * Set the location. By default, the application expects latitude and longitude values to be in decimal degrees and
     * elevation to be in meters. The input location is expected to be in these units.
     * <p>
     * This is functionally equivalent to
     * <pre>
     * if (location.hasElevation())
     * {
     *     setLocation(location.getLatitude(), location.getLongitude(), location.getElevation());
     * } else
     * {
     *     setLocation(location.getLatitude(), location.getLongitude());
     * }
     * </pre>
     *
     * @param location Location to set
     * @since 2.32
     */
    default void setLocation(ILocation location)
    {
        if (location.hasElevation())
        {
            setLocation(location.getLatitude(), location.getLongitude(), location.getElevation());
        } else
        {
            setLocation(location.getLatitude(), location.getLongitude());
        }
    }

    /**
     * Set the location. As a side effect, the message is updated to indicate is does not have an elevation.  If there
     * is an elevation, use one of the other methods instead: {@link #setLocation(double, double, double)} or
     * {@link #setLocationWithElevationInFeet(double, double, double)}.
     *
     * @param latitude  The latitude in decimal degrees.
     * @param longitude The longitude in decimal degrees.
     */
    void setLocation(double latitude, double longitude);

    /**
     * Set the location. By default, the application expects latitude and longitude values to be in decimal degrees and
     * elevation to be in meters. Please see the convenience method
     * {@link #setLocationWithElevationInFeet(double, double, double)} if you would like the application to perform the
     * conversion of the elevation from feet to meters.
     *
     * @param latitude  The latitude in decimal degrees.
     * @param longitude The longitude in decimal degrees.
     * @param elevation The elevation in meters.
     * @see #setLocationWithElevationInFeet(double, double, double)
     */
    void setLocation(double latitude, double longitude, double elevation);

    /**
     * Convenience method to allow developers to set a location value with the elevation provided in English feet.
     * Internally, all elevations are stored in meters. The application will perform the required conversion into
     * meters before persisting the value. The values used to perform the conversion can be found in the utility
     * class {@link UnitsOfMeasureUtils}. If the conversion provided by that class is
     * unsatisfactory, developers should perform the conversion themselves and use the method
     * {@link #setLocation(double, double, double)} which expects the elevation value in meters.
     *
     * @param latitude  The latitude in decimal degrees.
     * @param longitude The longitude in decimal degrees.
     * @param elevation The elevation in English feet.
     * @see #setLocation(double, double, double)
     * @since 1.14
     */
    void setLocationWithElevationInFeet(double latitude, double longitude, double elevation);

    @Internal
    void setManuallyGeneratedLocation(boolean manualLocation);

    /**
     * Sets the heading of the RDM.
     *
     * @param heading The heading in degrees.
     * @since 2.35
     */
    void setHeading(double heading);

    void setOrientation(double heading, double pitch, double roll);

    /**
     * Set speed in meters/sec. Please see the related convenience methods if you would like the application
     * to internally perform the conversion of the speed from MPH, KPH, or knots per hour to meters/sec.
     *
     * @param speed The speed in meters/sec.
     * @see #setSpeedInMph(double)
     * @see #setSpeedInKph(double)
     * @see #setSpeedInKnots(double)
     * @see UnitsOfMeasureUtils
     */
    void setSpeedInMps(double speed);

    /**
     * Convenience method to allow developers to set the speed value using Kilometers per hour.
     * Internally, all speeds are stored in meters/sec. The appropriate conversion will be applied by this method.
     * The values used to perform the conversion can be found in the utility
     * class {@link UnitsOfMeasureUtils}. If the conversion provided by that class is
     * unsatisfactory, developers should perform the conversion themselves and use the method {@link #setSpeedInMps}
     * which expects the value in meters/sec.
     *
     * @param speedInKph The speed in Kilometers per hour.
     * @see #setSpeedInMps(double)
     * @since 3.0
     */
    void setSpeedInKph(double speedInKph);

    /**
     * Convenience method to allow developers to set the speed value using English miles per hour.
     * Internally, all speeds are stored in meters/sec. The appropriate conversion will be applied by this method.
     * The values used to perform the conversion can be found in the utility
     * class {@link UnitsOfMeasureUtils}. If the conversion provided by that class is
     * unsatisfactory, developers should perform the conversion themselves and use the method {@link #setSpeedInMps}
     * which expects the value in meters/sec.
     *
     * @param speedInMph The speed in English miles per hour.
     * @see #setSpeedInMps(double)
     * @since 1.14
     */
    void setSpeedInMph(double speedInMph);

    /**
     * Convenience method to allow developers to set the speed value using nautical miles per hour (knots).
     * Internally, all speeds are stored in meters/sec. The appropriate conversion will be applied by this method.
     * The values used to perform the conversion can be found in the utility
     * class {@link UnitsOfMeasureUtils}. If the conversion provided by that class is
     * unsatisfactory, developers should perform the conversion themselves and use the method {@link #setSpeedInMps}
     * which expects the value in meters/sec.
     *
     * @param speedInKnots The speed in knots.
     * @see #setSpeedInMps(double)
     * @since 1.14
     */
    void setSpeedInKnots(double speedInKnots);

    /**
     * Set the device time in milliseconds since epoch.
     *
     * @param time The device time
     */
    void setTime(long time);

    /**
     * This setter is only needed for testing.
     */
    @Internal
    void setReceiveTime(long t);

    /**
     * Set the motion state pertaining to this message.
     *
     * @param hasMotion motion implied in the message.
     */
    void setHasMotion(boolean hasMotion);

    /**
     * Set whether this RDM should be persisted to the database. By default, RDMs are persistable, so this method needs
     * to be called if it is not desirable to add the message to the DB.
     *
     * @param persist True to cause the message to be persisted.
     */
    void setPersistable(boolean persist);

    void setRenderable(boolean flag);

    /**
     * Set whether this RDM is "imported" or not.  The default state is {@code false}.
     * <p>
     * This is typically set {@code true} in import handling to differentiate the RDMs from those that come
     * from "live" devices.
     *
     * @param imported {@code true} to mark this RDM as imported
     * @since 2.22
     */
    void setImported(boolean imported);

    /**
     * Set the batch persistence latch.  The latch is shared among all messages in the same batch.
     * When the batch is persisted, this latch will be released.  This method is only called if message
     * batching is enabled, and then only if the message is added to a batch.
     *
     * @param batchLatch batch latch
     * @since 1.49
     */
    @Internal
    void setBatchLatch(CountDownLatch batchLatch);
}
