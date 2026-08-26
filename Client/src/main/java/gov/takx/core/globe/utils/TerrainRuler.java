package gov.takx.core.globe.utils;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.terrain.Terrain;
import gov.takx.api.platform.IProgressCallback;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;

/**
 * This class was adapted from {@link gov.nasa.worldwind.util.measure.LengthMeasurer} to work with a {@link Terrain}
 * object instead of a {@link Globe}.
 *
 * @since 1.16
 */
public class TerrainRuler
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final double DEFAULT_TERRAIN_SAMPLING_STEPS = 128; // number of samples when following terrain
    private static final double DEFAULT_MAX_SEGMENT_LENGTH = 100e3; // size above which segments are subdivided
    private static final double DEFAULT_MIN_SEGMENT_LENGTH = 30; // minimum length of a terrain following subdivision

    private ArrayList<? extends Position> positions;
    private ArrayList<? extends Position> subdividedPositions;
    private boolean followTerrain = false;
    private int pathType = WorldWind.GREAT_CIRCLE;
    private double maxSegmentLength = DEFAULT_MAX_SEGMENT_LENGTH;
    private Sector sector;
    private double lengthTerrainSamplingSteps = DEFAULT_TERRAIN_SAMPLING_STEPS;
    protected double length = -1;
    private IProgressCallback callback;

    public TerrainRuler()
    {
    }

    protected void clearCachedValues()
    {
        subdividedPositions = null;
        length = -1;
    }

    public IProgressCallback getProgressCallback()
    {
        return callback;
    }

    public void setProgessCallback(IProgressCallback callback)
    {
        this.callback = callback;
    }

    public ArrayList<? extends Position> getPositions()
    {
        return positions;
    }

    public void setPositions(@Nonnull ArrayList<? extends LatLon> positions, double elevation)
    {
        ArrayList<Position> newPositions = new ArrayList<>();
        for (LatLon pos : positions)
        {
            newPositions.add(new Position(pos, elevation));
        }

        setPositions(newPositions);
    }

    public void setPositions(@Nonnull ArrayList<? extends Position> positions)
    {
        this.positions = positions;
        if (this.positions.size() > 2)
        {
            sector = Sector.boundingSector(this.positions);
        } else
        {
            sector = null;
        }

        clearCachedValues();
    }

    public boolean isFollowTerrain()
    {
        return followTerrain;
    }

    /**
     * Set whether measurements should account for terrain deformations.
     *
     * @param followTerrain set to true if measurements should account for terrain deformations.
     */
    public void setFollowTerrain(boolean followTerrain)
    {
        if (this.followTerrain != followTerrain)
        {
            this.followTerrain = followTerrain;
            clearCachedValues();
        }
    }

    public int getPathType()
    {
        return pathType;
    }

    /**
     * Sets the type of path used when subdividing long segments, one of {@link WorldWind#GREAT_CIRCLE}, which draws
     * segments as a great circle, {@link WorldWind#LINEAR}, which determines the intermediate positions between
     * segments by interpolating the segment endpoints, or {@link WorldWind#RHUMB_LINE}, which draws segments as a line
     * of constant heading.
     *
     * @param pathType the type of path to measure.
     */
    public void setPathType(int pathType)
    {
        if (this.pathType != pathType)
        {
            this.pathType = pathType;
            clearCachedValues();
        }
    }

    /**
     * Get the maximum length a segment can have before being subdivided along a line following the current pathType.
     *
     * @return the maximum length a segment can have before being subdivided.
     */
    public double getMaxSegmentLength()
    {
        return maxSegmentLength;
    }

    /**
     * Set the maximum length a segment can have before being subdivided along a line following the current pathType.
     *
     * @param length the maximum length a segment can have before being subdivided.
     */
    public void setMaxSegmentLength(double length)
    {
        if (length <= 0)
        {
            String message = "Argument out of range " + length;
            logger.error("{}", message);
            throw new IllegalArgumentException(message);
        }

        if (maxSegmentLength != length)
        {
            maxSegmentLength = length;
            clearCachedValues();
        }
    }

    public Sector getBoundingSector()
    {
        if (sector == null && positions != null && positions.size() > 2)
        {
            sector = Sector.boundingSector(positions);
        }

        return sector;
    }

    /**
     * Returns true if the current position list describe a closed path - one which last position is equal
     * to the first.
     *
     * @return true if the current position list describe a closed path.
     */
    public boolean isClosedShape()
    {
        return positions != null
                && positions.size() > 1
                && positions.get(0).equals(positions.get(positions.size() - 1));
    }

    /**
     * Get the number of terrain elevation samples used along the path to approximate it's terrain following length.
     *
     * @return the number of terrain elevation samples used.
     */
    public double getLengthTerrainSamplingSteps()
    {
        return lengthTerrainSamplingSteps;
    }

    /**
     * Set the number of terrain elevation samples to be used along the path to approximate it's terrain following
     * length.
     *
     * @param steps the number of terrain elevation samples to be used.
     */
    public void setLengthTerrainSamplingSteps(double steps)
    {
        if (steps < 1)
        {
            String message = "Argument out of range " + steps;
            logger.error("{}", message);
            throw new IllegalArgumentException(message);
        }

        if (lengthTerrainSamplingSteps != steps)
        {
            lengthTerrainSamplingSteps = steps;
            subdividedPositions = null;
            length = -1;
        }
    }

    /**
     * Get the path length in meter.
     * <p/>
     * If the measurer is set to follow terrain, the computed length will account for terrain deformations as
     * if someone was walking along that path. Otherwise the length is the sum of the cartesian distance between
     * each positions.
     *
     * @param terrain the terrain to draw terrain information from.
     * @return the current path length or -1 if the position list is too short.
     */
    public double getLength(@Nonnull Terrain terrain)
    {
        if (length < 0)
        {
            length = computeLength(terrain, followTerrain);
        }

        return length;
    }

    // *** Computing length *****************************************************************************

    protected double computeLength(Terrain terrain, boolean followTerrain)
    {
        if (positions == null || positions.size() < 2)
        {
            return -1;
        }

        if (subdividedPositions == null)
        {
            // Subdivide path so as to have at least segments smaller then maxSegmentLength. If follow terrain,
            // subdivide so as to have at least lengthTerrainSamplingSteps segments, but no segments shorter then
            // DEFAULT_MIN_SEGMENT_LENGTH either.
            double maxLength = maxSegmentLength;
            if (followTerrain)
            {
                // Recurse to compute overall path length not following terrain
                double pathLength = computeLength(terrain, !followTerrain);
                // Determine segment length to have enough sampling points
                maxLength = pathLength / lengthTerrainSamplingSteps;
                maxLength = Math.min(Math.max(maxLength, DEFAULT_MIN_SEGMENT_LENGTH), getMaxSegmentLength());
            }
            subdividedPositions = subdividePositions(terrain, maxLength, followTerrain);
        }

        Globe globe = terrain.getGlobe();

        // Sum each segment length
        double length = 0;
        Vec4 p1 = globe.computePointFromPosition(subdividedPositions.get(0));
        for (int i = 1; i < subdividedPositions.size(); i++)
        {
            Vec4 p2 = globe.computePointFromPosition(subdividedPositions.get(i));
            length += p1.distanceTo3(p2);
            p1 = p2;
        }
        return length;
    }

    /**
     * Subdivide a list of positions so that no segment is longer then the provided maxLength. Only the positions
     * between start and {@code start + count - 1} will be processed.
     * <p/>
     * If needed, new intermediate positions will be created along lines that follow the given pathType - one of
     * {@link WorldWind#LINEAR}, {@link WorldWind#RHUMB_LINE} or {@link WorldWind#GREAT_CIRCLE}.
     * All position elevations will be either at the terrain surface if followTerrain is {@code true},
     * or interpolated according to the original elevations.
     *
     * @param terrain       the terrain to draw elevations and points from.
     * @param maxLength     the maximum length for one segment.
     * @param followTerrain true if the positions should be on the terrain surface.
     * @return a list of positions with no segment longer then maxLength and elevations following terrain or not.
     */
    protected ArrayList<? extends Position> subdividePositions(Terrain terrain, double maxLength, boolean followTerrain)
    {
        ArrayList<Position> newPositions = new ArrayList<>();
        // Add first position
        Position pos1 = positions.get(0);
        if (followTerrain)
        {
            newPositions.add(new Position(pos1, terrain.getElevation(pos1)));
        } else
        {
            newPositions.add(pos1);
        }

        Globe globe = terrain.getGlobe();

        int totalSteps = 1;

        for (int i = 1; i < positions.size(); i++)
        {
            Position pos2 = positions.get(i);
            double arcLengthRadians = LatLon.greatCircleDistance(pos1, pos2).radians;
            double arcLength = arcLengthRadians * globe.getRadiusAt(LatLon.interpolate(.5, pos1, pos2));
            if (arcLength > maxLength)
            {
                // if necessary subdivide segment at regular intervals smaller then maxLength
                Angle segmentAzimuth = null;
                Angle segmentDistance = null;
                int steps = (int) Math.ceil(arcLength / maxLength);  // number of intervals - at least two
                for (int j = 1; j < steps; j++)
                {
                    float s = (float) j / steps;
                    LatLon destLatLon;
                    if (pathType == WorldWind.LINEAR)
                    {
                        destLatLon = LatLon.interpolate(s, pos1, pos2);
                    } else if (pathType == WorldWind.RHUMB_LINE)
                    {
                        if (segmentAzimuth == null)
                        {
                            segmentAzimuth = LatLon.rhumbAzimuth(pos1, pos2);
                            segmentDistance = LatLon.rhumbDistance(pos1, pos2);
                        }
                        destLatLon = LatLon.rhumbEndPosition(pos1, segmentAzimuth.radians,
                                s * segmentDistance.radians);
                    } else // GREAT_CIRCLE
                    {
                        if (segmentAzimuth == null)
                        {
                            segmentAzimuth = LatLon.greatCircleAzimuth(pos1, pos2);
                            segmentDistance = LatLon.greatCircleDistance(pos1, pos2);
                        }
                        destLatLon = LatLon.greatCircleEndPosition(pos1, segmentAzimuth.radians,
                                s * segmentDistance.radians);
                    }
                    // Set elevation
                    double elevation;
                    if (followTerrain)
                    {
                        elevation = terrain.getElevation(destLatLon);
                    } else
                    {
                        elevation = pos1.getElevation() * (1 - s) + pos2.getElevation() * s;
                    }
                    // Add new position
                    newPositions.add(new Position(destLatLon, elevation));

                    if (callback != null)
                    {
                        callback.onProgress(100 * totalSteps / lengthTerrainSamplingSteps);
                    }

                    totalSteps++;
                }
            }
            // Finally add the segment end position
            if (followTerrain)
            {
                newPositions.add(new Position(pos2, terrain.getElevation(pos2)));
            } else
            {
                newPositions.add(pos2);
            }
            // Prepare for next segment
            pos1 = pos2;
        }
        return newPositions;
    }
}
