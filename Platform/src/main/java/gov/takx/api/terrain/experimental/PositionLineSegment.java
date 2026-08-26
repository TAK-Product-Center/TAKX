package gov.takx.api.terrain.experimental;

import gov.nasa.worldwind.geom.Position;

/**
 * Represents a line segment defined by two {@link Position positions}.
 * @param posA the first position of the line segment
 * @param posB the second position of the line segment
 * @since 5.2
 */
public record PositionLineSegment(Position posA, Position posB)
{
}
