package gov.takx.dataservices.shapes;

import gov.nasa.worldwind.geom.Extent;
import gov.nasa.worldwind.geom.ExtentHolder;
import gov.nasa.worldwind.globes.Globe;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.shapes.IMutableRangeFan;
import gov.takx.api.shapes.ShapeProperties;
import gov.takx.platform.utils.ShapeUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of a range fan shape.
 *
 * @since 1.49
 */
public class RangeFan extends ARegularShape implements IMutableRangeFan, ExtentHolder
{
    /**
     * The default list of colors used for rendering range rings.
     * @since 3.4
     */
    public static final List<Color> DEFAULT_COLORS = List.of(Color.red, Color.blue, Color.green.darker(), Color.orange.darker(), Color.magenta);

    private boolean showEdges = true;
    private boolean showOrientation;
    private boolean showRangeValues = true;
    private double sweep = 360;
    private double minDistance = 100;
    private double maxDistance = 500;
    private double interval = 100;
    private int rangeType = GREAT_CIRCLE;
    private double sampleDistance = 5;
    private List<Double> rangeDistances = null; // Only used if discrete distances are set
    private List<String> rangeLabels = null; // Only used if discrete labels are set
    private List<Color> rangeColors = DEFAULT_COLORS;
    private List<List<ILocation>> ringLocations;

    /**
     * Default constructor.
     * @since 3.3
     */
    public RangeFan()
    {
    }

    /**
     * Create a new range fan instance with the given name, center point, min/max distances and ring interval.
     *
     * @param name             display name for shape
     * @param center           Location of center point
     * @param minDistance      minimum distance (distance to first range ring), in meters
     * @param maxDistance      maximum distance (distance beyond which no range rings are rendered), in meters
     * @param interval         Interval between range rings, in meters
     */
    public RangeFan(@Nonnull String name, @Nonnull ILocation center, double minDistance,
                    double maxDistance, double interval)
    {
        if (maxDistance <= minDistance) throw new IllegalArgumentException("maxDistance must be > minDistance");

        setName(name);
        setLocation(center);
        setMinDistance(minDistance);
        setMaxDistance(maxDistance);
        setInterval(interval);
    }

    /**
     * Create a new range fan instance with the given name, center point, ring distances, and labels.
     *
     * @param name             display name for shape
     * @param center           Location of center point
     * @param rangeDistances   List of distances, in meters, from the center at which to draw range rings.
     * @param rangeLabels      List of labels to place on each range ring.  Must have an equal number of elements as
     *                         the {@code rangeDistances}.  Can be {@code null}, and if so, then the labels will default
     *                         to the range distances.
     */
    public RangeFan(@Nonnull String name, @Nonnull ILocation center,
                    @Nonnull List<Double> rangeDistances, @Nullable List<String> rangeLabels)
    {
        setName(name);
        setLocation(center);
        setRangeDistances(rangeDistances, rangeLabels);
    }

    @Override
    public boolean contains(ILocation location)
    {
        // determine if the range fan is using min/max/interval or specific range distances
        final double radius = rangeDistances != null ? rangeDistances.get(rangeDistances.size() - 1) : maxDistance;
        return ShapeUtils.circleContainsPoint(getLocation(), radius, location);
    }

    @Override
    public void setLocation(ILocation location)
    {
        propertyChangeSupport.updateAndFire(ShapeProperties.LOCATION, getLocation(), location, () -> {
            super.setLocation(location);
            setFollowTerrainRingLocations(null);
        });
    }

    @Override
    public void setOrientation(double orientationDegrees)
    {
        propertyChangeSupport.updateAndFire(ShapeProperties.ORIENTATION, getOrientation(), orientationDegrees, () -> {
            super.setOrientation(orientationDegrees);
            setFollowTerrainRingLocations(null);
        });
    }

    @Override
    public boolean isShowEdges()
    {
        return showEdges;
    }

    @Override
    public void setShowEdges(boolean showEdges)
    {
        propertyChangeSupport.updateAndFire(ShapeProperties.SHOW_EDGES, this.showEdges, showEdges,
                () -> this.showEdges = showEdges);
    }

    @Override
    public boolean isShowOrientation()
    {
        return showOrientation;
    }

    @Override
    public void setShowOrientation(boolean showOrientation)
    {
        propertyChangeSupport.updateAndFire(ShapeProperties.SHOW_ORIENTATION, this.showOrientation, showOrientation,
                () -> this.showOrientation = showOrientation);
    }

    @Override
    public boolean isShowRangeValues()
    {
        return showRangeValues;
    }

    @Override
    public void setShowRangeValues(boolean showRangeValues)
    {
        propertyChangeSupport.updateAndFire(ShapeProperties.SHOW_RANGE_VALUES, this.showRangeValues, showRangeValues,
                () -> this.showRangeValues = showRangeValues);
    }

    @Override
    public double getInterval()
    {
        return interval;
    }

    @Override
    public void setInterval(double interval)
    {
        if (interval <= 0) throw new IllegalArgumentException("interval must be > 0");

        propertyChangeSupport.updateAndFire(ShapeProperties.INTERVAL, this.interval, interval, () -> {
            this.interval = interval;
            setFollowTerrainRingLocations(null);
        });
    }

    @Override
    public void setRangeDistances(@Nonnull List<Double> rangeDistances, @Nullable List<String> rangeLabels) throws IllegalArgumentException
    {
        if (rangeDistances.isEmpty()) throw new IllegalArgumentException("'rangeDistances' must not be empty");

        if (rangeLabels != null && rangeDistances.size() != rangeLabels.size())
        {
            throw new IllegalArgumentException("'rangeDistances' and 'rangeLabels' must have the same number of elements: "
                    + rangeDistances.size() + " != " + rangeLabels.size());
        }

        double lastDistance = 0;
        for (double distance : rangeDistances)
        {
            if (distance <= 0) throw new IllegalArgumentException("range distances must be > 0; got " + distance);
            if (distance <= lastDistance)
            {
                throw new IllegalArgumentException(String.format("range distances must be strictly increasing: %f <= %f", distance, lastDistance));
            }
            lastDistance = distance;
        }

        propertyChangeSupport.updateAndFire(ShapeProperties.RANGE_DISTANCES, this.rangeDistances, rangeDistances, () -> {
            this.rangeDistances = new ArrayList<>(rangeDistances);
            setFollowTerrainRingLocations(null);
        });

        propertyChangeSupport.updateAndFire(ShapeProperties.RANGE_LABELS, this.rangeLabels, rangeLabels,
                () -> this.rangeLabels = rangeLabels == null ? null : new ArrayList<>(rangeLabels));
    }

    @Override
    public List<Double> getRangeDistances()
    {
        return rangeDistances != null ? Collections.unmodifiableList(rangeDistances) : null;
    }

    @Override
    public List<String> getRangeLabels()
    {
        return rangeLabels != null ? Collections.unmodifiableList(rangeLabels) : null;
    }

    @Override
    public void clearRangeDistances()
    {
        propertyChangeSupport.updateAndFire(ShapeProperties.RANGE_DISTANCES, rangeDistances, null, () -> rangeDistances = null);
        propertyChangeSupport.updateAndFire(ShapeProperties.RANGE_LABELS, rangeLabels, null, () -> rangeLabels = null);
    }

    @Override
    public void setRangeColors(@Nonnull List<Color> rangeColors)
    {
        if (rangeColors.isEmpty()) throw new IllegalArgumentException("'rangeColors' must not be empty");
        for (Color color : rangeColors) if (color == null) throw new IllegalArgumentException("'rangeColors' entries must not be null");

        propertyChangeSupport.updateAndFire(ShapeProperties.RANGE_COLORS, this.rangeColors, rangeColors,
                () -> this.rangeColors = new ArrayList<>(rangeColors));
    }

    @Nonnull
    @Override
    public List<Color> getRangeColors()
    {
        return Collections.unmodifiableList(rangeColors);
    }

    /**
     * @return Maximum distance at which to draw range rings
     */
    @Override
    public double getMaxDistance()
    {
        return maxDistance;
    }

    @Override
    public void setMaxDistance(double maxDistance)
    {
        if (maxDistance <= 0) throw new IllegalArgumentException("'maxDistance' must be > 0");

        propertyChangeSupport.updateAndFire(ShapeProperties.MAX_DISTANCE, this.maxDistance, maxDistance, () -> {
            this.maxDistance = maxDistance;
            setFollowTerrainRingLocations(null);
        });
    }

    @Override
    public double getMinDistance()
    {
        return minDistance;
    }

    @Override
    public void setMinDistance(double minDistance)
    {
        if (minDistance <= 0) throw new IllegalArgumentException("'minDistance' must be > 0");

        propertyChangeSupport.updateAndFire(ShapeProperties.MIN_DISTANCE, this.minDistance, minDistance, () -> {
            this.minDistance = minDistance;
            setFollowTerrainRingLocations(null);
        });
    }

    @Override
    public double getSweep()
    {
        return sweep;
    }

    @Override
    public void setSweep(double sweep)
    {
        if (sweep < 1 || sweep > 360) throw new IllegalArgumentException("'sweep' must be >= 1.0 and <= 360.0");

        propertyChangeSupport.updateAndFire(ShapeProperties.SWEEP, this.sweep, sweep, () -> {
            this.sweep = sweep;
            setFollowTerrainRingLocations(null);
        });
    }

    @Override
    public int getRangeType()
    {
        return rangeType;
    }

    @Override
    public void setRangeType(int rangeType)
    {
        propertyChangeSupport.updateAndFire(ShapeProperties.RANGE_TYPE, this.rangeType, rangeType,
                () -> this.rangeType = rangeType);
    }

    @Override
    public double getSampleDistance()
    {
        return sampleDistance;
    }

    @Override
    public void setSampleDistance(double sampleDistance)
    {
        if (sampleDistance <= 0) throw new IllegalArgumentException("'sampleDistance' must be > 0");

        propertyChangeSupport.updateAndFire(ShapeProperties.SAMPLE_DISTANCE, this.sampleDistance, sampleDistance,
                () -> this.sampleDistance = sampleDistance);
    }

    @Override
    public Extent getExtent(Globe globe, double verticalExaggeration)
    {
        List<Double> rangeDistances = getRangeDistances();
        double radius = rangeDistances != null && !rangeDistances.isEmpty() ? rangeDistances.get(rangeDistances.size() - 1) : getMaxDistance();
        return ExtentFactory.createExtent(globe, getLocation(), radius);
    }

    @Override
    public List<List<ILocation>> getFollowTerrainRingLocations()
    {
        return ringLocations;
    }

    @Override
    public void setFollowTerrainRingLocations(List<List<ILocation>> ringLocations)
    {
        propertyChangeSupport.updateAndFire(ShapeProperties.LOCATIONS, this.ringLocations, ringLocations, () -> this.ringLocations = ringLocations);
    }
}
