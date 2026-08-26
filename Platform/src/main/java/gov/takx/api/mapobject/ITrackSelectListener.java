package gov.takx.api.mapobject;

import gov.takx.api.messages.IRaptorDataMessage;

import java.util.List;

/**
 * Listener interface for track selection events.  Track selection events can occur either directly on the globe
 * or on table rows in the Device Log.
 *
 * @since 1.49
 */
public interface ITrackSelectListener
{
    /**
     * Invoked when a track is selected. This method will be called on the Swing Event Dispatch Thread (EDT).
     * <p>
     * Note: you SHOULD NOT use this method to create a context menu for your tracks. Instead,
     * use {@link ITrackContextMenuListener#onContextMenu(List)}
     *
     * @param event Event that describes the selection. The event object records the action that
     *              triggered the selection (click, double click, etc).
     * @param msgs  The data messages for the track point that was selected. The messages are sorted by
     *              time in ascending order (oldest messages first).
     */
    void onSelect(TrackSelectEvent event, List<IRaptorDataMessage> msgs);
}
