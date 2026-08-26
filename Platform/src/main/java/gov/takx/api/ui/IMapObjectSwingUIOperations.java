package gov.takx.api.ui;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IGlobeDelegate;
import gov.takx.api.globe.IWorldWindowGL;
import gov.takx.api.plugin.device.IDevicePluginDelegate;

import javax.swing.Action;

import java.awt.event.MouseEvent;

/**
 * Contract for the UI operations provided by the core for a Map Object, via its map object controller.
 *
 * @since 2.0.0
 */
@NoPluginImplementations
public interface IMapObjectSwingUIOperations extends IDeviceSwingUIOperations
{
    /**
     * Show the plugin dialog, unless a custom action is provided through the delegate, in which case that
     * action will be performed instead
     *
     * @see IDevicePluginDelegate#setPluginDialogAction(Action)
     */
    void showPluginDialog();

    /**
     * Creates a popup menu using this controllers map object as the primary map object
     *
     * @param mouseEvent event that caused the popup (used to position the popup menu)
     */
    void createPopupMenu(MouseEvent mouseEvent);

    /**
     * Creates a popup menu using this controllers map object as the primary map object.
     *
     * @implNote This simply creates a synthetic mouse event and invokes {@link #createPopupMenu(MouseEvent)}.
     *
     * @param globe Globe on which to display the popup menu
     * @param location Location to navigate the globe view to
     * @since 5.0
     */
    default void createPopupMenu(IGlobeDelegate globe, ILocation location)
    {
        final IWorldWindowGL wwd = globe.getWorldWindow();

        final MouseEvent mouseEvent = new MouseEvent(wwd.getComponent(), 1, System.currentTimeMillis(),
                0, 0, 0, 1, false, 1);

        globe.goToAndTranslateMouseEvent(location, mouseEvent);
        createPopupMenu(mouseEvent);
    }

    /**
     * this is called if we want to display a help dialog for the associated map object
     */
    void showHelpDialog();

    /**
     * Flag used to indicate if audio alarm playback is enabled.  This flag is used to indicate
     * on a per map object basis if audio alarm playback is enabled or disabled, as defined in the user interface
     * by the end user.  In other words, the developer may define 2 distinct audible alerts for Warning and Critical,
     * and upon placing the device onto the map the user may opt to disable audio feedback on that instance, which
     * would result in this method being called to turn off audio alarm playback.
     *
     * @param flag true, if audio alarms are to be played by the system.
     */
    void setAudioAlarmPlayback(boolean flag);

    /**
     * Checks to see if this map object has been set up to play an audio alarm.
     *
     * @return true, if an audio alarm should be played
     */
    boolean isAudioAlarmPlaybackEnabled();

    /**
     * @return {@code true} if the dialog is open {@code false} otherwise
     */
    boolean isDialogOpen();

    /**
     * Display a dialog with the current CoT representation of the map object.
     * @since 3.2 (semantics changed in 5.0)
     */
    void showCotDialog();

    /**
     * Display a dialog with the latest CoT event received for the map object. Note: this differs from
     * {@link #showCotDialog()} which shows the current CoT representation of the MO. This method only shows received
     * CoT events.
     * @since 5.0
     */
    void showLatestReceivedCotDialog();
}
