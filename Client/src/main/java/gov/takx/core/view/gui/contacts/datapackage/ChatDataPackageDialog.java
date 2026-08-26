package gov.takx.core.view.gui.contacts.datapackage;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.comms.takserver.IExtendedDataPackageManager;
import gov.tak.api.contact.IContact;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.IMultiSelector;
import gov.takx.api.notification.INotificationService;
import gov.takx.api.notification.Notification;
import gov.takx.api.shapes.ShapeType;
import gov.takx.commons.lang.ErrorThrowingBiConsumer;
import gov.takx.commons.swing.AlwaysOnTopFileChooser;
import gov.takx.commons.swing.ImageFileChooserFilter;
import gov.takx.core.chat.ChatDataPackageService;
import gov.takx.core.chat.ChatLink;
import gov.takx.platform.ui.AStandardDialog;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Dialog that presents the user with data package options when constructing a package to be sent directly to another
 * user.
 *
 * @since 5.2
 */
@PseudoBean
public class ChatDataPackageDialog extends AStandardDialog
{
    private static final ImageIcon FILES_ICON = UIResources.getActionButtonIcon(PLATFORM_ICONS, "files.png");
    private static final ImageIcon IMAGES_ICON = UIResources.getActionButtonIcon(PLATFORM_ICONS, "photos.png");
    private static final ImageIcon LASSO_ICON = UIResources.getActionButtonIcon(PLATFORM_ICONS, "multiselector.png");

    @Inject
    private IExtendedDataPackageManager dataPackageManager;
    @Inject
    private IMultiSelector multiSelector;
    @Inject
    private ChatDataPackageService chatDataPackageService;
    @Inject
    private INotificationService notificationService;

    private final IContact contact;

    /**
     * @param contact The recipient contact
     */
    public ChatDataPackageDialog(@Nonnull IContact contact)
    {
        this.contact = contact;

        initComponents();

        descriptionLabel.setText("Sending to: " + contact.getName());
        filesButton.setIcon(FILES_ICON);
        imagesButton.setIcon(IMAGES_ICON);
        lassoButton.setIcon(LASSO_ICON);
    }

    /**
     * Presents the user with a file chooser to select images and calls the {@link ChatDataPackageService} to send a
     * data package containing the selected items to the contact.
     */
    private void imagesButtonClicked()
    {
        chooseFilesAndSendPackage(chooser -> chooser.setFileFilter(new ImageFileChooserFilter()),
                (packageFile, files) -> dataPackageManager.addImagesToDataPackage(packageFile, files));
    }

    /**
     * Presents the user with a file chooser to select files and calls the {@link ChatDataPackageService} to send a
     * data package containing the selected items to the contact.
     */
    private void filesButtonClicked()
    {
        chooseFilesAndSendPackage(chooser -> chooser.setAcceptAllFileFilterUsed(true),
                (packageFile, files) -> dataPackageManager.addFilesToDataPackage(packageFile, files));
    }

    /**
     * Triggers the lasso select tool to allow the user to select map objects and calls the {@link ChatDataPackageService}
     * to send a data package containing the selected items to the contact.
     */
    private void lassoButtonClicked()
    {
        multiSelector.arm(ShapeType.FREEHAND_POLYGON, selected -> {
            try
            {
                if (!selected.isEmpty())
                {
                    long timestamp = System.currentTimeMillis();
                    chatDataPackageService.sendPackage(contact, file -> {
                        dataPackageManager.addMapObjectsToDataPackage(file, selected);
                        return selected.stream().map(mo -> new ChatLink(timestamp, mo.getUid(), mo.getName())).toList();
                    });
                } else
                {
                    notificationService.show(Notification.builder("No files selected for chat data package. Ignoring.").build());
                }
            } finally
            {
                multiSelector.disarmAndClear();
            }
        });

        dispose();
    }

    /**
     * @param fileChooserConsumer consumer that accepts a file chooser and finishes initializing it before it's displayed
     * @param fileConsumer        consumer that accepts a data package file and the list of files selected by the user and adds
     *                            the files to the data package according to the file type
     */
    private void chooseFilesAndSendPackage(@Nonnull Consumer<JFileChooser> fileChooserConsumer,
                                           @Nonnull ErrorThrowingBiConsumer<File, List<File>, IOException> fileConsumer)
    {
        var fileChooser = new AlwaysOnTopFileChooser();

        fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
        fileChooser.setDialogTitle("Select");
        fileChooser.setMultiSelectionEnabled(true);

        fileChooserConsumer.accept(fileChooser);

        if (fileChooser.showDialog(this, "Select") == JFileChooser.APPROVE_OPTION)
        {
            final File[] selectedFiles = fileChooser.getSelectedFiles();

            if (selectedFiles == null) return;

            long timestamp = System.currentTimeMillis();
            chatDataPackageService.sendPackage(contact, file -> {
                fileConsumer.accept(file, Arrays.asList(selectedFiles));
                return Stream.of(selectedFiles).map(f -> new ChatLink(timestamp, "", f.getName())).toList();
            });
        }

        dispose();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        var descriptionPanel = new JPanel();
        descriptionLabel = new JLabel();
        var buttonPanel = new JPanel();
        imagesButton = new JButton();
        filesButton = new JButton();
        lassoButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Select Data Package Items");
        setResizable(false);
        setModal(true);
        setName("this");
        var contentPane = getContentPane();
        contentPane.setLayout(new FormLayout(
                "[200dlu,default]:grow",
                "default, [50dlu,default]"));

        //======== descriptionPanel ========
        {
            descriptionPanel.setBorder(Borders.DLU2_BORDER);
            descriptionPanel.setName("descriptionPanel");
            descriptionPanel.setLayout(new FormLayout(
                    "default:grow",
                    "default"));

            //---- descriptionLabel ----
            descriptionLabel.setText("text");
            descriptionLabel.setName("descriptionLabel");
            descriptionPanel.add(descriptionLabel, cc.xy(1, 1, CellConstraints.CENTER, CellConstraints.DEFAULT));
        }
        contentPane.add(descriptionPanel, cc.xy(1, 1));

        //======== buttonPanel ========
        {
            buttonPanel.setBorder(Borders.DLU4_BORDER);
            buttonPanel.setName("buttonPanel");
            buttonPanel.setLayout(new FormLayout(
                    "2*(default, $ugap), default",
                    "default"));

            //---- imagesButton ----
            imagesButton.setText("Images");
            imagesButton.setVerticalTextPosition(SwingConstants.BOTTOM);
            imagesButton.setHorizontalTextPosition(SwingConstants.CENTER);
            imagesButton.setName("imagesButton");
            imagesButton.addActionListener(e -> imagesButtonClicked());
            buttonPanel.add(imagesButton, cc.xy(1, 1));

            //---- filesButton ----
            filesButton.setText("Files");
            filesButton.setHorizontalTextPosition(SwingConstants.CENTER);
            filesButton.setVerticalTextPosition(SwingConstants.BOTTOM);
            filesButton.setName("filesButton");
            filesButton.addActionListener(e -> filesButtonClicked());
            buttonPanel.add(filesButton, cc.xy(3, 1));

            //---- lassoButton ----
            lassoButton.setText("Lasso");
            lassoButton.setVerticalTextPosition(SwingConstants.BOTTOM);
            lassoButton.setHorizontalTextPosition(SwingConstants.CENTER);
            lassoButton.setName("lassoButton");
            lassoButton.addActionListener(e -> lassoButtonClicked());
            buttonPanel.add(lassoButton, cc.xy(5, 1));
        }
        contentPane.add(buttonPanel, cc.xy(1, 2, CellConstraints.CENTER, CellConstraints.DEFAULT));
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JLabel descriptionLabel;
    private JButton imagesButton;
    private JButton filesButton;
    private JButton lassoButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
