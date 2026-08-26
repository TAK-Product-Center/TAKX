package gov.takx.api.shapes;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.coordinatesystem.ILocation;

import java.util.List;

/**
 * Interface to an immutable ellipse shape.
 *
 */
@NoPluginImplementations
public interface IEllipse extends IRingedShape
{
    /**
     * @return semi-major axis length
     */
    double getSemiMajorAxis();

    /**
     * @return semi-minor axis length
     */
    double getSemiMinorAxis();

    /**
     * @return list of vertex locations
     */
    List<ILocation> getVertices();
}
