package gov.takx.core.globe.tak.animation;

import com.atakmap.math.MathUtils;
import gov.nasa.worldwind.animation.AnimationSupport;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.takx.platform.preferences.PreferenceEntry;

/**
 * Utility functions for animations.
 * 
 * @since 3.4.1
 */
final class AnimationUtils
{
    /**
     * Get the appropriate length for a fly to animation between two positions.
     * @param begin the starting lat/long
     * @param end the ending lat/long
     * @return the length in milliseconds
     */
    public static long getFlyToAnimationLength(LatLon begin, LatLon end)
    {
        Long minLength = PreferenceEntry.ANIMATION_FLYTO_MIN_LENGTH.getLongValue();
        Long maxLength = PreferenceEntry.ANIMATION_FLYTO_MAX_LENGTH.getLongValue();
        return AnimationSupport.getScaledTimeMillisecs(begin, end, minLength, maxLength);
    }

    /**
     * Get the appropriate length for a heading animation between two angles. 
     * @param begin the starting angle
     * @param end the ending angle
     * @return the length in milliseconds
     */
    public static long getHeadingAnimationLength(Angle begin, Angle end)
    {
        Long minLength = PreferenceEntry.ANIMATION_HEADING_MIN_LENGTH.getLongValue();
        Long maxLength = PreferenceEntry.ANIMATION_HEADING_MAX_LENGTH.getLongValue();
        return AnimationSupport.getScaledTimeMillisecs(begin, end, Angle.POS180, minLength, maxLength);
    }

    /**
     * Get the appropriate length for a pan animation between two positions.
     * @param begin the starting lat/long
     * @param end the ending lat/long
     * @return the length in milliseconds
     */
    public static long getPanAnimationLength(LatLon begin, LatLon end)
    {
        Long minLength = PreferenceEntry.ANIMATION_PAN_MIN_LENGTH.getLongValue();
        Long maxLength = PreferenceEntry.ANIMATION_PAN_MAX_LENGTH.getLongValue();
        return MathUtils.clamp(1000, minLength, maxLength);
    }

    /**
     * Get the appropriate length for a tilt animation between two angles.
     * @param begin the starting angle
     * @param end the ending angle
     * @return the length in milliseconds
     */
    public static long getTiltAnimationLength(Angle begin, Angle end)
    {
        Long minLength = PreferenceEntry.ANIMATION_TILT_MIN_LENGTH.getLongValue();
        Long maxLength = PreferenceEntry.ANIMATION_TILT_MAX_LENGTH.getLongValue();
        return AnimationSupport.getScaledTimeMillisecs(begin, end, Angle.POS90, minLength, maxLength);
    }

    /**
     * Get the appropriate length for a zoom animation between two gsds.
     * @param begin the starting gsd
     * @param end the ending gsd
     * @return the length in milliseconds
     */
    public static long getZoomAnimationLength(double begin, double end)
    {
        Long minLength = PreferenceEntry.ANIMATION_ZOOM_MIN_LENGTH.getLongValue();
        Long maxLength = PreferenceEntry.ANIMATION_ZOOM_MAX_LENGTH.getLongValue();
        return MathUtils.clamp(4000, minLength, maxLength);
    }
}
