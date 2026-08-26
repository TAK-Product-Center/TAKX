package gov.takx.api.mapobject;

import gov.takx.api.messages.IRaptorDataMessage;

import javax.swing.JMenuItem;

import java.util.List;

/**
 * Helper service for track listener. This service will allow the plugin to contribute menu items to a baseline
 * menu provided by takx which will allow plugins to share in the population of the menu.
 *
 * @since 2.26
 */
public interface ITrackContextMenuListener
{
    /**
     * Invoked when a track is selected with the right mouse button. The plugin should return a collection of menu
     * items which are relevant for the list of RDM's being clicked. This method will be called on the Swing
     * Event Dispatch Thread (EDT).
     *
     * @param msgs The data messages for the track point that was selected. The messages are sorted by
     *             time in ascending order (oldest messages first).
     * @return the menu items to add
     */
    List<JMenuItem> onContextMenu(List<IRaptorDataMessage> msgs);
}
