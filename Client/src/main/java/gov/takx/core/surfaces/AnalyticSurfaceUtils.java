package gov.takx.core.surfaces;

import gov.nasa.worldwind.util.BufferFactory;
import gov.nasa.worldwind.util.BufferWrapper;
import gov.nasa.worldwind.util.WWMath;
import gov.takx.api.surfaces.IAnalyticSurfaceUtils;
import gov.takx.api.surfaces.IBufferWrapper;

/**
 * Set of utility methods around WorldWind's WWMath class and BufferWrapper class.  This design protects the plugin
 * developer from having to be exposed to any WorldWind classes.
 *
 * @since 1.4
 */
public class AnalyticSurfaceUtils implements IAnalyticSurfaceUtils
{
    public IBufferWrapper createBuffer(int numValues)
    {

        BufferFactory.DoubleBufferFactory factory = new BufferFactory.DoubleBufferFactory();
        BufferWrapper bw = factory.newBuffer(numValues);
        return new SurfaceBuffer(bw);
    }

    @Override
    public double logBase2(double value)
    {
        return WWMath.logBase2(value);
    }

    @Override
    public boolean isPowerOfTwo(int value)
    {
        return WWMath.isPowerOfTwo(value);
    }

    @Override
    public int powerOfTwoCeiling(int reference)
    {
        return WWMath.powerOfTwoCeiling(reference);
    }

    @Override
    public int powerOfTwoFloor(int reference)
    {
        return WWMath.powerOfTwoFloor(reference);
    }

    @Override
    public double clamp(double v, double min, double max)
    {
        return WWMath.clamp(v, min, max);
    }

    @Override
    public double computeInterpolationFactor(double v, double x, double y)
    {
        return WWMath.computeInterpolationFactor(v, x, y);
    }

    @Override
    public double mix(double a, double x, double y)
    {
        return WWMath.mix(a, x, y);
    }

    @Override
    public double mixSmooth(double a, double x, double y)
    {
        return WWMath.mixSmooth(a, x, y);
    }

    @Override
    public double convertMetersToFeet(double meters)
    {
        return WWMath.convertMetersToFeet(meters);
    }

    @Override
    public double convertMetersToMiles(double meters)
    {
        return WWMath.convertMetersToMiles(meters);
    }

    @Override
    public double convertSecondsToMillis(double seconds)
    {
        return WWMath.convertSecondsToMillis(seconds);
    }

    @Override
    public double convertMillisToSeconds(double millis)
    {
        return WWMath.convertMillisToSeconds(millis);
    }

    @Override
    public double convertMinutesToMillis(double minutes)
    {
        return WWMath.convertMinutesToMillis(minutes);
    }

    @Override
    public double convertMillisToMinutes(double millis)
    {
        return WWMath.convertMillisToMinutes(millis);
    }

    @Override
    public double convertHoursToMillis(double hours)
    {
        return WWMath.convertHoursToMillis(hours);
    }

    @Override
    public double convertMillisToHours(double mills)
    {
        return WWMath.convertMillisToHours(mills);
    }
}
