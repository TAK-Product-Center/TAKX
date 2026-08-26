package gov.takx.dataservices.persist;

import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.mapobject.IShapeRangeFanMapObject;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.IRaptorPlugin;
import gov.takx.api.shapes.IMutableRangeFan;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Entity;

import java.awt.Color;
import java.util.List;

/**
 * A ShapeRangeFanMapObject is responsible for rendering the visual components of that comprise a "range fan."  A series of
 * concentric arcs along a central line of bearing (LOB) at specified intervals.  Typically, used to represent a
 * field of view or radar sweep or just sight distances from a given point (like a bulls eye configuration).
 *
 * @since 1.16
 */
@Entity
public class ShapeRangeFanMapObject extends AShapeMapObject<IMutableRangeFan> implements IShapeRangeFanMapObject
{
    @SuppressWarnings({"UnusedDeclaration"}) // Default constructor for Hibernate
    protected ShapeRangeFanMapObject()
    {
    }

    @SuppressWarnings("unused")
    public ShapeRangeFanMapObject(MapObjectSpec mapObjectSpec, IPluginDefinition<? extends IRaptorPlugin> pluginDefinition)
    {
        super(mapObjectSpec, pluginDefinition);
    }

    @Override
    public void setOrientation(double orientation)
    {
        shape.setOrientation(orientation);
    }

    @Override
    public double getOrientation()
    {
        return shape.getOrientation();
    }

    @Override
    public boolean isShowEdges()
    {
        return shape.isShowEdges();
    }

    @Override
    public void setShowEdges(boolean showEdges)
    {
        shape.setShowEdges(showEdges);
    }

    @Override
    public boolean isShowOrientation()
    {
        return shape.isShowOrientation();
    }

    @Override
    public void setShowOrientation(boolean showOrientation)
    {
        shape.setShowOrientation(showOrientation);
    }

    @Override
    public boolean isShowRangeValues()
    {
        return shape.isShowRangeValues();
    }

    @Override
    public void setShowRangeValues(boolean showRangeValues)
    {
        shape.setShowRangeValues(showRangeValues);
    }

    @Override
    public double getInterval()
    {
        return shape.getInterval();
    }

    @Override
    public void setInterval(double interval)
    {
        shape.setInterval(interval);
    }

    @Override
    public void setRangeDistances(@Nonnull List<Double> rangeDistances) throws IllegalArgumentException
    {
        shape.setRangeDistances(rangeDistances);
    }

    @Override
    public void setRangeDistances(@Nonnull List<Double> rangeDistances, @Nullable List<String> rangeLabels) throws IllegalArgumentException
    {
        shape.setRangeDistances(rangeDistances, rangeLabels);
    }

    @Override
    public List<Double> getRangeDistances()
    {
        return shape.getRangeDistances();
    }

    @Override
    public List<String> getRangeLabels()
    {
        return shape.getRangeLabels();
    }

    @Override
    public void clearRangeDistances()
    {
        shape.clearRangeDistances();
    }

    @Override
    public void setRangeColors(@Nonnull List<Color> colors)
    {
        shape.setRangeColors(colors);
    }

    @Nonnull
    @Override
    public List<Color> getRangeColors()
    {
        return shape.getRangeColors();
    }

    @Override
    public double getMaxDistance()
    {
        return shape.getMaxDistance();
    }

    @Override
    public void setMaxDistance(double maxDistance)
    {
        shape.setMaxDistance(maxDistance);
    }

    @Override
    public double getMinDistance()
    {
        return shape.getMinDistance();
    }

    @Override
    public void setMinDistance(double minDistance)
    {
        shape.setMinDistance(minDistance);
    }

    @Override
    public double getSweep()
    {
        return shape.getSweep();
    }

    @Override
    public void setSweep(double sweep)
    {
        shape.setSweep(sweep);
    }

    @Override
    public int getRangeType()
    {
        return shape.getRangeType();
    }

    @Override
    public void setRangeType(int rangeType)
    {
        shape.setRangeType(rangeType);
    }

    @Override
    public double getSampleDistance()
    {
        return shape.getSampleDistance();
    }

    @Override
    public void setSampleDistance(double sampleDistance)
    {
        shape.setSampleDistance(sampleDistance);
    }

    @Override
    public List<List<ILocation>> getFollowTerrainRingLocations()
    {
        return shape.getFollowTerrainRingLocations();
    }

    @Override
    public void setFollowTerrainRingLocations(List<List<ILocation>> ringLocations)
    {
        shape.setFollowTerrainRingLocations(ringLocations);
    }
}
