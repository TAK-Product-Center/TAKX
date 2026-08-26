package gov.takx.api.mapobject;

import java.util.EventObject;

/**
 * Event object that describes a track selection event. A track selection event occurs when the user
 * clicks on a track on the globe.
 *
 */
public class TrackSelectEvent extends EventObject
{
    /**
     * Possible actions that can trigger a selection event.
     */
    public enum TrackSelectAction
    {
        /**
         * Action triggered when the user clicks once on a track.
         */
        CLICK,

        /**
         * Action triggered when the user double clicks on a track.
         */
        DOUBLE_CLICK,

        /**
         * Action triggered when the user right clicks on a track.
         *
         * @since 1.49
         */
        RIGHT_CLICK
    }

    private TrackSelectAction action;

    /**
     * Create a track selection event.
     *
     * @param source The object that created the track that was selected.
     * @param action The action that triggered the selection (click, double click, etc.)
     */
    public TrackSelectEvent(IMapObject source, TrackSelectAction action)
    {
        super(source);
        this.action = action;
    }

    /**
     * Get the action that triggered this event.
     *
     * @return The action that triggered the event.
     */
    public TrackSelectAction getAction()
    {
        return action;
    }
}
