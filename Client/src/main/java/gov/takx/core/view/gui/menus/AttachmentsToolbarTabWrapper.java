package gov.takx.core.view.gui.menus;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.filesystem.IVirtualFileSystem;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.api.toolbar.TabType;
import gov.takx.commons.swing.AlwaysOnTopFileChooser;
import gov.takx.commons.swing.AlwaysOnTopFileChooserFactory;
import gov.takx.core.toolbar.ToolbarSeparator;
import gov.takx.core.view.gui.maps.importers.FileImporter;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

/**
 */
@PseudoBean
public class AttachmentsToolbarTabWrapper extends AToolbarTabWrapper
{
    private static final String AUDIO_TOOLTIP = "Attach an audio file to this project. The chosen file will be dropped in the center of the globe view and added to the Files Panel.";
    private static final String PHOTO_TOOLTIP = "Attach a photo file to this project. The chosen file will be dropped in the center of the globe view and added to the Files Panel.";
    private static final String VIDEO_TOOLTIP = "Attach a video file to this project. The chosen file will be dropped in the center of the globe view and added to the Files Panel.";
    private static final String FILE_TOOLTIP = "Attach a document, spreadsheet, powerpoint, or other type of File to this project. The chosen file will be dropped in the center of the globe view and added to the Files Panel.";

    private static final FileNameExtensionFilter AUDIO_FILE_EXTENSION_FILTER =
            new FileNameExtensionFilter("Audio Files (*.wav, *.aiff, *.au, *.mp3)", "wav", "aiff", "au", "mp3");

    private static final FileNameExtensionFilter IMAGE_FILE_EXTENSION_FILTER =
            new FileNameExtensionFilter("Image Files (*.jpg, *.bmp, *.png)", "png", "bmp", "jpg");

    private static final FileNameExtensionFilter VIDEO_FILE_EXTENSION_FILTER =
            new FileNameExtensionFilter("Video Files (*.avi, *.mov, *.mpg, *.mp4)", "avi", "mov", "mpg", "mp4");

    @Inject
    @VFSManagerScope(VFSScope.Project)
    private IVirtualFileSystemManager projectVFSManager;

    @PostConstruct
    private void postConstruct()
    {
        // TODO KMB: May want to remove this tab and just make the buttons available
        createTab(TabType.ATTACHMENTS, false); // Hidden by default
    }

    @Override
    protected void addComponents()
    {
        tab.addComponent(createButton(ButtonType.AUDIO, getAttachMediaActionListener("Add an Audio File", AUDIO_FILE_EXTENSION_FILTER), AUDIO_TOOLTIP));
        tab.addComponent(createButton(ButtonType.PHOTO, getAttachMediaActionListener("Add a Photo", IMAGE_FILE_EXTENSION_FILTER), PHOTO_TOOLTIP));
        tab.addComponent(createButton(ButtonType.VIDEO, getAttachMediaActionListener("Add a Video File", VIDEO_FILE_EXTENSION_FILTER), VIDEO_TOOLTIP));
        tab.addComponent(new ToolbarSeparator(TabType.ATTACHMENTS, tab.getComponents().size()));

        tab.addComponent(createButton(ButtonType.FILE, getAttachMediaActionListener("Add a File", null), FILE_TOOLTIP));
        tab.addComponent(new ToolbarSeparator(TabType.ATTACHMENTS, tab.getComponents().size()));
    }

    @Nonnull
    private ActionListener getAttachMediaActionListener(@Nullable final String title, @Nullable final FileNameExtensionFilter fileNameExtensionFilter)
    {
        return e -> {
            AlwaysOnTopFileChooser mediaChooser = AlwaysOnTopFileChooserFactory.getFileChooser(getClass());
            mediaChooser.setApproveButtonText("Add");
            mediaChooser.setAcceptAllFileFilterUsed(false);
            mediaChooser.setDialogTitle(title);
            mediaChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            mediaChooser.setMultiSelectionEnabled(true);

            if (fileNameExtensionFilter == null)
            {
                mediaChooser.setAcceptAllFileFilterUsed(true);
            } else
            {
                mediaChooser.setFileFilter(fileNameExtensionFilter);
            }

            int result = mediaChooser.showOpenDialog(JFrame.getFrames()[0]);
            if (JFileChooser.APPROVE_OPTION == result)
            {
                // We need to validate the files exist before doing starting the import
                List<File> files = FileImporter.validateAttachmentFiles(mediaChooser.getSelectedFiles());
                if (!files.isEmpty())
                {
                    final IVirtualFileSystem fileSystem = projectVFSManager.getVirtualFileSystemForAttachments();
                    final ILocation location = getCenterLocationOnGlobe();
                    new FileImporter(files.toArray(new File[0]), fileSystem, location).doImport();
                }
            }
        };
    }
}
