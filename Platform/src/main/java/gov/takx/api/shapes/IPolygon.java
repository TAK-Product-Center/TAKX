package gov.takx.api.shapes;

import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;

import java.util.Set;

/**
 * Interface to an immutable polygon shape.
 *
 */
@NoPluginImplementations
public interface IPolygon extends IIrregularShape
{
    /**
     * @return Any polygons within this polygon that have been marked as "holes".
     * @since 3.3
     */
    @Nonnull
    Set<IPolygon> getHoles();

}
