package gov.takx.api.shapes;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.coordinatesystem.ILocation;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Interface to a mutable irregular shape. See {@link IIrregularShape} for more information.
 */
@NoPluginImplementations
public interface IMutableIrregularShape extends IIrregularShape, IMutableShape
{
    /**
     * Set the list of vertex locations that define the shape
     *
     * @param vertices list of vertex locations
     * @see ILocation
     */
    void setVertices(@Nonnull List<? extends ILocation> vertices);
}
