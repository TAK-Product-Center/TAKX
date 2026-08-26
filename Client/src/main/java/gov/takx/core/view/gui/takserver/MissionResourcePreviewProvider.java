package gov.takx.core.view.gui.takserver;

import gov.tak.api.comms.takserver.mission.data.Resource;
import gov.takx.api.media.IMedia;
import gov.takx.api.media.IMediaManager;
import gov.takx.api.media.IMediaPlayerManager;
import gov.takx.api.media.MediaType;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.file.SizeFormatter;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.platform.ui.ResourceDirectory;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Provides panels for previewing resources from downloaded data-sync feeds.
 *
 * @since 5.0
 */
@ApplicationScoped
public class MissionResourcePreviewProvider
{
    private static final ImageIcon AUDIO_ICON = UIResources.getToolbarIcon(ResourceDirectory.PLATFORM_ICONS, "audio.png");
    private static final ImageIcon FILE_ICON = UIResources.getToolbarIcon(ResourceDirectory.PLATFORM_ICONS, "file.png");
    private static final ImageIcon VIDEO_ICON = UIResources.getToolbarIcon(ResourceDirectory.PLATFORM_ICONS, "video.png");
    private static final ImageIcon DOWNLOAD_ICON = UIResources.getToolbarIcon(ResourceDirectory.PLATFORM_ICONS, "cloud_download.png");
    private static final Dimension PREVIEW_DIMENSIONS = new Dimension(120, 95);
    private static final String MEDIA_MANAGER_KEY = "data-sync";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneId.systemDefault());

    private final IMediaManager mediaManager;
    private final IMediaPlayerManager mediaPlayerManager;

    @Inject
    public MissionResourcePreviewProvider(IMediaManager mediaManager, IMediaPlayerManager mediaPlayerManager)
    {
        this.mediaManager = mediaManager;
        this.mediaPlayerManager = mediaPlayerManager;
    }

    /**
     * Creates a panel with a visual preview of the file contents, if it is a supported media type.
     *
     * @param resource Outline of file information on TAK Server.
     * @param resourcePath Local file path to downloaded copy.
     *
     * @return Preview of local file contents if supported, otherwise {@code null}.
     */
    @Nullable
    public JPanel previewFile(@Nonnull Resource resource, @Nonnull File resourcePath)
    {
        String extension = FileUtils.getExtension(resourcePath);
        MediaType mediaType = MediaType.getMediaTypeForExtension(extension);
        switch (mediaType)
        {
            case Image ->
            {
                JPanel imageWrapper = new JPanel();

                BufferedImage image = ImageUtils.getImage(resourcePath, true);
                ImageIcon scaledImage = ImageUtils.getScaledIcon(image, PREVIEW_DIMENSIONS.width, PREVIEW_DIMENSIONS.height);
                imageWrapper.add(new JLabel(scaledImage));
                imageWrapper.addMouseListener(new MouseAdapter()
                {
                    @Override
                    public void mousePressed(MouseEvent e)
                    {
                        openMedia(resourcePath, MediaType.Image);
                    }
                });

                return imageWrapper;
            }
            case Video ->
            {
                // TODO: Get a snapshot of some frame in the video and show it here
                return createGenericPreview(resource, resourcePath, VIDEO_ICON, MediaType.Video);
            }
            case Audio ->
            {
                // TODO: Show the waveform as a preview
                return createGenericPreview(resource, resourcePath, AUDIO_ICON, MediaType.Audio);
            }
            default ->
            {
                return createGenericPreview(resource, resourcePath, FILE_ICON, MediaType.Unknown);
            }
        }
    }

    /**
     * Returns a button that the user can click to manually download a resource that did not automatically download due
     * to age, file size, etc.
     *
     * @param resource The resource to download
     * @param actionListener The listener that will respond to a click
     * @return The button
     * @see gov.takx.platform.preferences.PreferenceEntry#DATA_SYNC_LARGE_FILE_PROMPT
     * @see gov.takx.platform.preferences.PreferenceEntry#DATA_SYNC_THRESHOLD
     */
    @Nonnull
    public JButton getDownloadResourceButton(@Nonnull Resource resource, @Nonnull ActionListener actionListener)
    {
        final JButton downloadButton =
                new JButton("<html><b>" + resource.name() + "</b><br>" + SizeFormatter.formatEstimate(resource.size())
                        + "</html>", DOWNLOAD_ICON);

        downloadButton.addActionListener(e -> downloadButton.setEnabled(false));
        downloadButton.addActionListener(actionListener);

        downloadButton.setIcon(DOWNLOAD_ICON);
        return downloadButton;
    }

    /**
     * Creates a generic preview with a file type icon and the file's name.
     *
     * @param resource Outline of file information on TAK Server.
     * @param resourcePath Local file path to downloaded copy.
     * @param icon Icon to use for the generic preview content.
     * @param mediaType Media type of the previewed content.
     *
     * @return Generic preview for the given media type.
     */
    private JPanel createGenericPreview(Resource resource, File resourcePath, ImageIcon icon, MediaType mediaType)
    {
        String resourceName = resource.name();
        if (resourceName == null) resourceName = FileUtils.getNameWithoutExtension(resourcePath);
        String audioDate = DATE_FORMATTER
                .format(resource.submissionTime());

        JPanel fileProperties = new JPanel();
        fileProperties.setLayout(new BorderLayout());
        fileProperties.add(new JLabel(resourceName), BorderLayout.NORTH);
        fileProperties.add(new JLabel(audioDate), BorderLayout.SOUTH);

        JPanel fileLayout = new JPanel();
        fileLayout.setLayout(new BorderLayout());
        fileLayout.add(fileProperties, BorderLayout.CENTER);
        fileLayout.add(new JLabel(icon), BorderLayout.WEST);
        fileLayout.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                if (mediaType == MediaType.Unknown)
                {
                    if (AlwaysOnTopJOptionPane.showConfirmDialog(fileLayout, "You are about to open an external program to display this file.\n"
                            + "Do you want to continue?", "Open File?", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
                    {
                        FileUtils.browseFile(resourcePath);
                    }
                } else
                {
                    openMedia(resourcePath, mediaType);
                }
            }
        });

        return fileLayout;
    }

    /**
     * Opens a media file of the given type in the media player.
     *
     * @param mediaPath Path to media file on file system.
     * @param mediaType Type of media content.
     */
    private void openMedia(@Nonnull File mediaPath, @Nonnull MediaType mediaType)
    {
        IMedia media = switch (mediaType)
        {
            case Image, Audio, Video -> mediaManager.getMedia(MEDIA_MANAGER_KEY, mediaPath.getName());
            default -> null;
        };
        if (media == null) media = mediaManager.addMediaFromLocalFile(mediaPath, MEDIA_MANAGER_KEY);
        mediaPlayerManager.createDefaultPlayer(media);
    }
}
