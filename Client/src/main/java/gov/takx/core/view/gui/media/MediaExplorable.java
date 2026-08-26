package gov.takx.core.view.gui.media;

import gov.takx.api.explorer.Explorable;
import gov.takx.api.explorer.IExplorable;
import gov.takx.api.media.IMedia;
import gov.takx.api.media.IMediaManager;
import gov.takx.api.media.IMediaPlayerManager;
import gov.takx.api.media.MediaPlayerOptions;
import gov.takx.commons.swing.ComponentUtils;
import gov.takx.core.explorer.controller.path.ExplorerPathConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import java.lang.invoke.MethodHandles;

/**
 * Class for representing a Media object in the Explorer Tree
 *
 * @since 2.8
 */
public class MediaExplorable extends Explorable
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final IMedia media;
    private final IMediaManager mediaManager;
    private final IMediaPlayerManager mediaPlayerManager;

    public MediaExplorable(IMediaManager mediaManager, IMediaPlayerManager mediaPlayerManager, IMedia media)
    {
        super(media.getName(), ExplorerPathConstants.MEDIA_PATH);
        this.mediaManager = mediaManager;
        this.mediaPlayerManager = mediaPlayerManager;
        this.media = media;

        SwingUtilities.invokeLater(() -> {
            // Set the default popup menu instead of overriding createPopupMenu to enable plugins to change the default options
            setDefaultPopupMenu(() -> new MediaPopupMenu(this));
        });

        if (logger.isDebugEnabled())
        {
            logger.debug("Creating media explorable, key = {}", getUniqueKey());
        }
    }

    @Override
    public Object getOwner()
    {
        return media;
    }

    /**
     * While the standard behavior is to display a dialog when the user double-clicks an explorable instance,
     * for media the more intuitive behavior is to display the media itself. So we don't actually show a dialog
     * here, but launch the media in a player.
     */
    @Override
    public void showDialog()
    {
        launchInPlayer();
    }

    @Override
    public void requestDelete()
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Request to delete media: {}", media);
        }

        if (confirmDelete())
        {
            mediaManager.removeMedia(media);
        }
    }

    @Override
    public void deleteSilently()
    {
        mediaManager.removeMedia(media);
    }

    @Override
    public boolean supportsSilentDeletion()
    {
        return true;
    }

    @Override
    public Boolean getBooleanValue(String key, Boolean defaultValue)
    {
        // KEYMAKER-123: Hide the checkbox since it doesn't do anything for media
        if (IExplorable.CHECKBOX_VISIBLE_KEY.equals(key))
        {
            return false;
        }
        return super.getBooleanValue(key, defaultValue);
    }

    /**
     * Launch the media in a new player
     */
    protected void launchInPlayer()
    {
        MediaPlayerOptions options = new MediaPlayerOptions(media.getName());
        mediaPlayerManager.createMediaPlayer(options, media);
    }

    /**
     * Display media properties dialog
     */
    public void displayPropertiesDialog()
    {
        MediaPropertiesDialog dlg = new MediaPropertiesDialog(media);
        ComponentUtils.centerComponentOnScreen(dlg);
        dlg.setVisible(true);
    }

    /**
     * Ask the user to confirm media deletion
     *
     * @return True if the user confirms
     */
    private boolean confirmDelete()
    {
        String message = "<html>Delete " + media + "?" +
                "<br><br>Selecting Yes will remove the media from the current project, but will not delete any files on disk.</html>";
        JOptionPane pane = new JOptionPane(message,
                JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION);
        JDialog dialog = pane.createDialog("Confirm Delete - " + media);
        ComponentUtils.centerComponentOnScreen(dialog);
        dialog.setVisible(true);

        Object result = pane.getValue();
        return (result != null && ((int) result == JOptionPane.YES_OPTION));
    }
}
