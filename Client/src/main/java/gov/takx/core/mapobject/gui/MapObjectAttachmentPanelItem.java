package gov.takx.core.mapobject.gui;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.comms.takserver.IExtendedDataPackageManager;
import gov.tak.api.icons.TakColor;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.media.IMedia;
import gov.takx.api.media.IMediaManager;
import gov.takx.api.media.IMediaPlayerManager;
import gov.takx.api.media.MediaType;
import gov.takx.api.notification.INotificationService;
import gov.takx.api.notification.Notification;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * A panel to display a single {@link IMapObject} attachment file and provide interactivity like send, delete, and open
 * @since 5.3
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class MapObjectAttachmentPanelItem extends JPanel
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final Dimension ATTACHMENT_SIZE = new Dimension(172, 172);
    private static final ImageIcon SEND_IMAGE = UIResources.getSizedIcon(PLATFORM_ICONS, "send.png", UIResources.TOOLBAR_BUTTON_IMAGE_SIZE, true);
    private static final ImageIcon DELETE_IMAGE = UIResources.getSizedIcon(PLATFORM_ICONS, "delete.png", UIResources.TOOLBAR_BUTTON_IMAGE_SIZE, true);

    private static final BufferedImage AUDIO_DEFAULT_IMAGE = UIResources.getSizedImage(PLATFORM_ICONS, "fileType_audio.png", ATTACHMENT_SIZE, true);
    private static final BufferedImage DOC_DEFAULT_IMAGE = UIResources.getSizedImage(PLATFORM_ICONS, "fileType_doc.png", ATTACHMENT_SIZE, true);
    private static final BufferedImage ESRI_DEFAULT_IMAGE = UIResources.getSizedImage(PLATFORM_ICONS, "fileType_esri.png", ATTACHMENT_SIZE, true);
    private static final BufferedImage FILE_DEFAULT_IMAGE = UIResources.getSizedImage(PLATFORM_ICONS, "fileType_genericDoc.png", ATTACHMENT_SIZE, true);
    private static final BufferedImage IMAGE_DEFAULT_IMAGE = UIResources.getSizedImage(PLATFORM_ICONS, "fileType_image.png", ATTACHMENT_SIZE, true);
    private static final BufferedImage PDF_DEFAULT_IMAGE = UIResources.getSizedImage(PLATFORM_ICONS, "fileType_pdf.png", ATTACHMENT_SIZE, true);
    private static final BufferedImage PPT_DEFAULT_IMAGE = UIResources.getSizedImage(PLATFORM_ICONS, "fileType_ppt.png", ATTACHMENT_SIZE, true);

    // these values won't ever be null because the above initializers have 'defaultOnError' set to true
    private static final List<BufferedImage> DEFAULT_IMAGES = List.of(AUDIO_DEFAULT_IMAGE, DOC_DEFAULT_IMAGE, ESRI_DEFAULT_IMAGE, FILE_DEFAULT_IMAGE, IMAGE_DEFAULT_IMAGE, PDF_DEFAULT_IMAGE, PPT_DEFAULT_IMAGE);

    private final IMapObject mapObject;
    private final BufferedImage background;

    @Inject
    private IMediaManager mediaManager;

    @Inject
    private IMediaPlayerManager mediaPlayerManager;

    @Inject
    private INotificationService notificationService;

    @Inject
    private IExtendedDataPackageManager dataPackageManager;

    /**
     * Constructs a panel to display a single {@link IMapObject} attachment file and provide interactivity like send, delete, and open,
     * as well as an optional callback {@link Runnable} to call when the attachment is deleted
     * @param mapObject The map object this attachment is from
     * @param moAttachment The specific attachment {@link File} to display
     * @param deleteCallback An optional Runnable that will get called when the attachment is deleted
     */
    public MapObjectAttachmentPanelItem(@Nonnull IMapObject mapObject, @Nonnull File moAttachment, @Nullable Runnable deleteCallback)
    {
        initComponents();
        this.mapObject = mapObject;
        sendItemButton.setIcon(SEND_IMAGE);
        deleteButton.setIcon(DELETE_IMAGE);
        String name = moAttachment.getName();
        String extension = FileUtils.getExtension(moAttachment).toUpperCase(Locale.ROOT);
        MediaType mediaType = MediaType.getMediaTypeForExtension(extension);
        fileNameLabel.setText(name);
        fileNameLabel.setForeground(TakColor.GRAY.getColor());
        fileTypeLabel.setText(getTypeDescription(extension, mediaType));
        fileTypeLabel.setForeground(TakColor.GRAY.getColor());
        // this is not a simple "send map item as CoT", it's actually a "send DataPackage" under the hood
        sendItemButton.addActionListener(actionEvent -> new SendAsDataPackage(name,
                () -> {
                    try
                    {
                        File dataPackage = dataPackageManager.createNewTempDataPackage(name + "_" + UUID.randomUUID() + ".zip");
                        dataPackageManager.addMapObjectToDataPackage(dataPackage, mapObject, f -> f.getName().equalsIgnoreCase(name));
                        return dataPackage;
                    } catch (IOException e)
                    {
                        return null;
                    }
                },
                () -> {
                    String errorMessage = "Error in sending the attachment DataPackage for " + name;
                    notificationService.show(Notification.builder(errorMessage, Notification.Type.Error).build());
                    logger.error(errorMessage);
                },
                () -> {
                    String errorMessage = "Failed to create the attachment DataPackage for " + name;
                    notificationService.show(Notification.builder(errorMessage, Notification.Type.Error).build());
                    logger.error(errorMessage);
                }).setVisible(true));
        deleteButton.addActionListener(e -> {
            mapObject.deleteAttachments(moAttachment);
            if (deleteCallback != null) deleteCallback.run();
        });

        setToolTipText(name);

        if (mediaType != MediaType.Unknown)
        {
            BufferedImage tempImage = ImageUtils.getImage(moAttachment, true);
            // noinspection DataFlowIssue - the scaled image won't ever be null, defaultOnError is true
            background = tempImage == ImageUtils.getDefaultImage() ? getDefaultBackground(extension) : ImageUtils.getScaledInstanceToMaxSize(tempImage, ATTACHMENT_SIZE.width);

            addMouseListener(new MouseAdapter()
            {
                @Override
                public void mousePressed(MouseEvent e)
                {
                    openMedia(moAttachment, mediaType);
                }
            });
        } else
        {
            background = getDefaultBackground(extension);
            addMouseListener(new MouseAdapter()
            {
                @Override
                public void mousePressed(MouseEvent e)
                {
                    if (AlwaysOnTopJOptionPane.showConfirmDialog(null, "You are about to open an external program to display this file.\n"
                            + "Do you want to continue?", "Open File?", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
                    {
                        FileUtils.browseFile(moAttachment);
                    }
                }
            });
        }
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
            case Image, Audio, Video -> mediaManager.getMedia(mapObject.getUid(), mediaPath.getName());
            default -> null;
        };
        if (media == null) media = mediaManager.addMediaFromLocalFile(mediaPath, mapObject.getUid());
        mediaPlayerManager.createDefaultPlayer(media);
    }

    /**
     * @param extension an upper case String containing the extension ("PNG", "PDF", "BMP")
     * @return The default image associated with the provided extension
     */
    @Nonnull
    private BufferedImage getDefaultBackground(@Nonnull String extension)
    {
        // see if there's an easy to identify media type
        BufferedImage returnImage = switch (MediaType.getMediaTypeForExtension(extension))
        {
            case Audio -> AUDIO_DEFAULT_IMAGE;
            case Image, Video -> IMAGE_DEFAULT_IMAGE;
            default -> FILE_DEFAULT_IMAGE;
        };

        // check for more specific ones
        if (returnImage == FILE_DEFAULT_IMAGE)
        {
            // TODO TAKX-4149: Consolidate extension 'what is this' into shared utility, also below for fallback of type description?
            returnImage = switch (extension)
            {
                case "PPT", "PPTX" -> PPT_DEFAULT_IMAGE;
                case "PDF" -> PDF_DEFAULT_IMAGE;
                case "DOC", "DOCX" -> DOC_DEFAULT_IMAGE;
                case "SHP", "SHX", "DBF", "SBN", "SBX", "ATX", "FBN", "FBX", "AIN", "AIH", "IXS", "MXS", "PRJ", "XML", "CPG" -> ESRI_DEFAULT_IMAGE;
                default -> FILE_DEFAULT_IMAGE;
            };
        }

        return returnImage;
    }

    /**
     * Get a description for a given file type. Defaults to "&lt;File Extension&gt; File"
     * @param extension The {@link File} extension, as an upper case string ("MP4", "JPEG")
     * @param mediaType The {@link MediaType} of the file (can be {@link MediaType#Unknown})
     * @return A String describing the file (e.g. - "MP4 Video")
     */
    @Nonnull
    private String getTypeDescription(@Nonnull String extension, @Nonnull MediaType mediaType)
    {
        return switch (mediaType)
        {
            case Image -> extension.equals("BMP") ? "Bitmap Image" : extension + " Image";
            case Audio -> extension + " Format";
            case Video -> extension + " Video";
            case Unknown -> switch (extension)
            {
                case "PDF" -> "Adobe Acrobat Document";
                case "DOC", "DOCX" -> "Microsoft Word Document";
                case "PPT", "PPTX" -> "Microsoft PowerPoint Document";
                case "XLS", "XLSX" -> "Microsoft Excel Document";
                case "RTF" -> "Rich Text Format Document";
                case "TXT" -> "Text Document";
                default -> extension + " File";
            };
        };
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        final Graphics2D graphics2D = (Graphics2D) g;
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int drawWidth = background.getWidth();
        int drawHeight = background.getHeight();
        // default images are drawn differently - smaller and centered
        if (DEFAULT_IMAGES.contains(background))
        {
            drawWidth /= 2;
            drawHeight /= 2;
        }
        graphics2D.drawImage(background, (ATTACHMENT_SIZE.width / 2) - (drawWidth / 2),
                (ATTACHMENT_SIZE.height / 2) - (drawHeight / 2), drawWidth, drawHeight, null);
        super.paintChildren(graphics2D);
        graphics2D.dispose();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        fileTypeLabel = new JLabel();
        fileNameLabel = new JLabel();
        sendItemButton = new JButton();
        deleteButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setMaximumSize(new Dimension(172, 172));
        setMinimumSize(new Dimension(172, 172));
        setPreferredSize(new Dimension(172, 172));
        setBorder(null);
        setForeground(Color.white);
        setBackground(new Color(0x222222));
        setName("this");
        setLayout(new FormLayout(
                "0dlu, $lcgap, left:45dlu, 2*($lcgap, right:22dlu)",
                "top:79dlu, $lgap, bottom:11dlu, $lgap, bottom:10dlu"));

        //---- fileTypeLabel ----
        fileTypeLabel.setText("type");
        fileTypeLabel.setFont(fileTypeLabel.getFont().deriveFont(fileTypeLabel.getFont().getStyle() | Font.BOLD, fileTypeLabel.getFont().getSize() + 2f));
        fileTypeLabel.setName("fileTypeLabel");
        add(fileTypeLabel, cc.xy(3, 3));

        //---- fileNameLabel ----
        fileNameLabel.setText("label");
        fileNameLabel.setName("fileNameLabel");
        add(fileNameLabel, cc.xy(3, 5));

        //---- sendItemButton ----
        sendItemButton.setName("sendItemButton");
        add(sendItemButton, cc.xywh(5, 3, 1, 3));

        //---- deleteButton ----
        deleteButton.setName("deleteButton");
        add(deleteButton, cc.xywh(7, 3, 1, 3));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JLabel fileTypeLabel;
    private JLabel fileNameLabel;
    private JButton sendItemButton;
    private JButton deleteButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
