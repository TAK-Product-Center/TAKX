package gov.takx.api.shapes;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.coordinatesystem.ILocation;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Interface to an immutable irregular shape. This includes all shapes that are defined by a set of vertex locations
 * rather than a center point and a set of defining characteristics (like radius, width, etc.).
 *
 */
@NoPluginImplementations
public interface IIrregularShape extends IShape
{
    /**
     * Get the list of vertices that define the shape
     *
     * @return list of vertex locations
     * @see ILocation
     */
    @Nonnull
    List<ILocation> getVertices();
}
