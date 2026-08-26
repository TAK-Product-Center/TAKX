package gov.takx.dataservices.shapes;

import gov.takx.api.shapes.IMutableRingedShape;
import gov.takx.api.shapes.ShapeProperties;

/**
 * Abstract base for shapes that have concentric rings.
 *
 * @since 3.3
 */
public class ARingedShape extends ARegularShape implements IMutableRingedShape
{
    private int ringCount = 1;

    @Override
    public int getRingCount()
    {
        return ringCount;
    }

    @Override
    public void setRingCount(int ringCount)
    {
        if (ringCount < MIN_RING_COUNT || ringCount > MAX_RING_COUNT)
        {
            throw new IllegalArgumentException("Ring count must be between " + MIN_RING_COUNT + " and " +
                    MAX_RING_COUNT + "; value provided: " + ringCount);
        }

        propertyChangeSupport.updateAndFire(ShapeProperties.RING_COUNT, this.ringCount, ringCount,
                () -> this.ringCount = ringCount);
    }
}
