package gov.takx.platform.utils;

import gov.nasa.worldwind.geom.LatLon;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Length measurement tool useful for determining lengths of a path across multiple points on the globe.
 *
 * @since 1.5
 */
public interface ILengthMeasurer
{
    //
    // Complete list of supported unit of measure values
    //
    String METERS__METERS_SQR = "M/M\u00b2";
    String KILOMETERS__KILOMETERS_SQR = "KM/KM\u00b2";
    String KILOMETERS_HECTARE = "KM/Hectare";
    String FEET__FEET_SQR = "Feet/Feet\u00b2";
    String MILES__MILES_SQR = "Miles/Miles\u00b2";
    String NAUTICAL_MILES__MILES_SQR = "Nm/Miles\u00b2";
    String YARDS__ACRES = "Yards/Acres";

    List<String> values = Collections.unmodifiableList(Arrays.asList(METERS__METERS_SQR, KILOMETERS__KILOMETERS_SQR, KILOMETERS_HECTARE,
            FEET__FEET_SQR, MILES__MILES_SQR, NAUTICAL_MILES__MILES_SQR, YARDS__ACRES));

    /**
     * The default Unit of Measure if nothing is specified in the user preferences.
     *
     * @since 1.43
     */
    String DEFAULT_UNIT_OF_MEASURE = KILOMETERS__KILOMETERS_SQR;

    /**
     * Flag that indicates if the length measurement follows the terrain.
     *
     * @return true, if length is calculated by following the terrain, false if length is line of sight distance
     */
    boolean isFollowTerrain();

    /**
     * Identifies if the distance should be calculated using line of sight or as a terrain following path.
     *
     * @param followTerrainFlag true, if length should be calculated by following the terrain, false if length should
     *                          be calculated by using a line of sight distance.
     */
    void setFollowTerrain(boolean followTerrainFlag);

    /**
     * Set the maximum length a segment can have before being subdivided along a line following the current pathType.
     *
     * @param length the maximum length a segment can have before being subdivided
     */
    void setMaxSegmentLength(double length);

    /**
     * Get the maximum length a segment can have before being subdivided along a line following the current pathType.
     *
     * @return the maximum length a segment can have before being subdivided
     */
    double getMaxSegmentLength();

    /**
     * Identify two or more points from which the length should be calculated.
     *
     * @param positions List of two or more LatLon points from which the length should be calculated
     * @param elevation Elevation in meters above mean sea level
     */
    void setMeasurementPositions(List<? extends LatLon> positions, double elevation);

    /**
     * List of LatLon points used by the length measurer to calculate a distance.
     *
     * @return List of LatLon points
     */
    List<? extends LatLon> getMeasurementPositions();

    /**
     * Set the number of terrain elevation samples to be used along the path to approximate it's terrain following length.
     *
     * @param steps number of terrain elevation samples to be used
     */
    void setLengthTerrainSamplingSteps(double steps);

    /**
     * Get the number of terrain elevation samples used along the path to approximate it's terrain following length.
     *
     * @return number of terrain elevation samples used
     */
    double getLengthTerrainSamplingSteps();

    /**
     * Gets the type of path used when subdividing long segments, one of PathType.Circle, which draws segments as
     * a great circle, PathType.Linear, which determines the intermediate positions between segments by interpolating
     * the segment endpoints, or PathType.RhumbLine, which draws segments as a line of constant heading.
     *
     * @return the type of path to measure
     */
    MeasurementPathType getMeasurementPathType();

    /**
     * Sets the type of path used when subdividing long segments, one of PathType.Circle, which draws segments as
     * a great circle, PathType.Linear, which determines the intermediate positions between segments by interpolating
     * the segment endpoints, or PathType.RhumbLine, which draws segments as a line of constant heading.
     * <p/>
     * <b>Note:</b> Defaults to Linear unless overridden.
     *
     * @param pathType the type of path to measure
     */
    void setMeasurementPathType(MeasurementPathType pathType);

    /**
     * Path Types used to control how long segments are subdivided when measuring.
     */
    enum MeasurementPathType
    {
        GreatCircle,
        Linear,
        RhumbLine
    }

    /**
     * Computes the length of the defined path using the requested method, either line of sight or terrain following.
     *
     * @return length, in meters.
     */
    double computeLength();
}
