package gov.takx.api.playback;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.coordinatesystem.ILocation;

/**
 * Represents a timestamped location.
 *
 */
@NoPluginImplementations
public interface ITimeLocation extends ILocation, Comparable<ITimeLocation>
{
    /**
     * @return The timestamp on this location
     */
    long getTime();
}
