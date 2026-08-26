package gov.takx.dataservices.persist;

import com.atakmap.coremap.conversions.Angle;
import com.atakmap.coremap.conversions.Span;
import com.atakmap.coremap.maps.coords.NorthReference;
import gov.tak.api.cot.CotTypes;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.mapobject.IShapeBullseyeMapObject;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.IRaptorPlugin;
import gov.takx.api.shapes.IMutableBullseye;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Entity;

/**
 * Configuration of a bullseye shape.
 * @since 5.5
 */
@Entity
public class ShapeBullseyeMapObject extends AShapeRingMapObject<IMutableBullseye> implements IShapeBullseyeMapObject
{
    public ShapeBullseyeMapObject()
    {
        super();
    }

    @SuppressWarnings("unused")
    public ShapeBullseyeMapObject(@Nonnull MapObjectSpec mapObjectSpec, @Nullable IPluginDefinition<? extends IRaptorPlugin> pluginDefinition)
    {
        super(mapObjectSpec, pluginDefinition);
        setCotType(CotTypes.SHAPE_BULLSEYE);
        setMetaBoolean(StandardAttributeNames.RANGE_BEARING, true);
    }

    @Override
    public void setRadius(double radius, @Nonnull Span units) throws IllegalArgumentException
    {
        shape.setRadius(radius, units);
    }

    @Override
    public double getRadius()
    {
        return shape.getRadius();
    }

    @Override
    public void setRingCount(int ringCount) throws IllegalArgumentException
    {
        shape.setRingCount(ringCount);
    }

    @Override
    public int getRingCount()
    {
        return shape.getRingCount();
    }

    @Override
    public void setRingRadius(double ringRadius, @Nonnull Span units) throws IllegalArgumentException
    {
        shape.setRingRadius(ringRadius, units);
    }

    @Override
    public double getRingRadius()
    {
        return shape.getRingRadius();
    }

    @Override
    public void setPointingEdgeToCenter(boolean pointingEdgeToCenter)
    {
        shape.setPointingEdgeToCenter(pointingEdgeToCenter);
    }

    @Override
    public boolean isPointingEdgeToCenter()
    {
        return shape.isPointingEdgeToCenter();
    }

    @Override
    public void setNorthReference(@Nonnull NorthReference northReference)
    {
        shape.setNorthReference(northReference);
    }

    @Nonnull
    @Override
    public NorthReference getNorthReference()
    {
        return shape.getNorthReference();
    }

    @Override
    public void setBearingUnits(@Nonnull Angle bearingUnits) throws IllegalArgumentException
    {
        shape.setBearingUnits(bearingUnits);
    }

    @Nonnull
    @Override
    public Angle getBearingUnits()
    {
        return shape.getBearingUnits();
    }

    @Override
    public void setSpanUnits(@Nonnull Span units)
    {
        shape.setSpanUnits(units);
    }

    @Nonnull
    @Override
    public Span getSpanUnits()
    {
        return shape.getSpanUnits();
    }

    @Override
    public void setShowRangeRings(boolean showRangeRings)
    {
        shape.setShowRangeRings(showRangeRings);
    }

    @Override
    public boolean isShowRangeRings()
    {
        return shape.isShowRangeRings();
    }
}
