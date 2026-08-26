package gov.takx.api.playback.globe;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.playback.IPlaybackLane;

import java.util.Collection;

/**
 * Contract for a playback lane that represents a globe and shows Map Object track points.
 *
 * @since 1.47
 */
@NoPluginImplementations
public interface IGlobePlaybackLane extends IPlaybackLane
{
    /**
     * @return The list of Map Objects being shown in this lane
     */
    Collection<IMapObject> getMapObjects();

    /**
     * Set the list of Map Objects being shown.
     *
     * @param mapObjects Map objects to show in lane
     * @since 2.18
     */
    void addMapObjects(Collection<IMapObject> mapObjects);

    /**
     * Set the list of Map Objects being shown.
     *
     * @param mapObjects Map objects to show in lane
     * @since 2.18
     */
    void removeMapObjects(Collection<IMapObject> mapObjects);
}
