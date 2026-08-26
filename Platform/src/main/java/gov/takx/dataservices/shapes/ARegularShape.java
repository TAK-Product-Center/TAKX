package gov.takx.dataservices.shapes;

import gov.takx.api.shapes.IMutableRegularShape;
import gov.takx.api.shapes.ShapeProperties;

/**
 * Abstract base class for all regular shape instances, that is, those defined by a center point and a set of
 * characteristics (like radius, width, etc.).
 *
 */
public abstract class ARegularShape extends AShape implements IMutableRegularShape
{
    private double orientationDegrees = 0;
    private double verticalHeight = 10;
    private boolean verticalHeightEnabled = false;

    /**
     * Default constructor.
     */
    protected ARegularShape()
    {
        super();
    }

    @Override
    public double getOrientation()
    {
        return orientationDegrees;
    }

    @Override
    public void setOrientation(double orientationDegrees)
    {
        propertyChangeSupport.updateAndFire(ShapeProperties.ORIENTATION, this.orientationDegrees, orientationDegrees,
                () -> this.orientationDegrees = orientationDegrees);
    }

    @Override
    public void setVerticalHeight(double verticalHeight)
    {
        propertyChangeSupport.updateAndFire(ShapeProperties.VERTICAL_HEIGHT, this.verticalHeight, verticalHeight,
                () -> this.verticalHeight = verticalHeight);
    }

    @Override
    public void setVerticalHeightEnabled(boolean verticalHeightEnabled)
    {
        propertyChangeSupport.updateAndFire(ShapeProperties.VERTICAL_HEIGHT_ENABLED, this.verticalHeightEnabled,
                verticalHeightEnabled, () -> this.verticalHeightEnabled = verticalHeightEnabled);
    }

    @Override
    public double getVerticalHeight()
    {
        return verticalHeight;
    }

    @Override
    public boolean isVerticalHeightEnabled()
    {
        return verticalHeightEnabled;
    }

    @Override
    public boolean canUseVerticalHeight()
    {
        return true;
    }
}
