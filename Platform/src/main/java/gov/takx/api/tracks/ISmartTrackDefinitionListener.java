package gov.takx.api.tracks;

import gov.takx.platform.tracks.DefinitionEvent;

/**
 * Ability to receive definition events from the {@link ISmartTrackManager} when either a
 * track style or connecting line definition is added or removed.
 *
 * @since 1.12
 */
public interface ISmartTrackDefinitionListener
{
    /**
     * Notification that either a track style or connecting line definition has been added to the SmartTrack manager.
     *
     * @param event Definition event
     */
    void onDefinitionRegistered(DefinitionEvent event);

    /**
     * Notification that either a track style or connecting line definition has been removed from the SmartTrack manager.
     *
     * @param event Definition event
     */
    void onDefinitionUnregistered(DefinitionEvent event);
}
