package gov.takx.core.mapobject.gui;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.UiEventBus;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.filesystem.IVFSFile;
import gov.takx.api.filesystem.IVirtualFileSystem;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.notification.INotificationService;
import gov.takx.api.notification.Notification;
import gov.takx.api.ui.event.MapObjectSelectedEvent;
import gov.takx.commons.file.FilePath;
import gov.takx.commons.swing.AlwaysOnTopFileChooser;
import gov.takx.commons.swing.AlwaysOnTopFileChooserFactory;
import gov.takx.commons.swing.WrapLayout;
import gov.takx.core.filesystem.VirtualFileSystem;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * A panel to display and interact with a {@link IMapObject IMapObject's} attachment files located in the {@link VirtualFileSystem}
 * @since 5.3
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class MapObjectAttachmentsPanel extends MapObjectMainPreferencesPanel
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final ImageIcon PAPERCLIP_ICON = UIResources.getToolbarIcon(PLATFORM_ICONS, "paperclip.png");
    private static final ImageIcon OPEN_FOLDER_ICON = UIResources.getToolbarIcon(PLATFORM_ICONS, "folder_open.png");
    private static final ImageIcon ADD_ATTACHMENT_ICON = UIResources.getToolbarIcon(PLATFORM_ICONS, "add.png");

    private final PropertyChangeListener attachmentChangeListener = e -> updateAttachments();

    private IMapObject mapObject;
    private final boolean canChangeMapObject;

    @Inject
    @VFSManagerScope(VFSScope.Project)
    protected IVirtualFileSystemManager projectVFSManager;

    @Inject
    private INotificationService notificationService;

    @Inject
    @UiEventBus
    private EventBus uiEventBus;

    /**
     * Constructs a panel to view and interact with a given {@link IMapObject IMapObject's} attachments.
     * @param mapObject The map object to inspect the attachments of.
     * @param canChangeMapObject {@code true} to allow swapping displayed contents when {@link MapObjectSelectedEvent} is fired.
     *                           {@code false} to keep the initially displayed map object's attachments.
     */
    public MapObjectAttachmentsPanel(@Nonnull IMapObject mapObject, boolean canChangeMapObject)
    {
        super("Attachments", mapObject.getController(), null);
        initComponents();
        attachmentsPanel.setLayout(new WrapLayout(400)); // arbitrary initial value
        this.mapObject = mapObject;
        this.canChangeMapObject = canChangeMapObject;
    }

    @PostConstruct
    private void postConstruct()
    {
        scrollPane.getVerticalScrollBar().setUnitIncrement(25);
        mapObjectNameLabel.setText(mapObject.getName());
        attachmentIcon.setIcon(PAPERCLIP_ICON);
        openAttachmentsFolder.setIcon(OPEN_FOLDER_ICON);
        addAttachmentButton.setIcon(ADD_ATTACHMENT_ICON);

        uiEventBus.register(this); // new map object selected event
        mapObject.addPropertyChangeListener(IMapObject.ATTRIBUTE_SET, attachmentChangeListener);
        updateAttachments();
    }

    @InvokeOnEDT
    private void updateAttachments()
    {
        attachmentsPanel.removeAll();
        mapObject.getAttachments().forEach(moAttachment -> attachmentsPanel.add(new MapObjectAttachmentPanelItem(mapObject, moAttachment, this::updateAttachments)));
        openAttachmentsFolder.setEnabled(!mapObject.getAttachments().isEmpty());
        revalidate();
        repaint();
    }

    private void openAttachmentsFolder()
    {
        if (Desktop.isDesktopSupported())
        {
            String name = mapObject.getName();
            String uid = FileSystemUtils.sanitizeFilename(mapObject.getUid());
            IVirtualFileSystem attachmentVfs = projectVFSManager.getVirtualFileSystemForAttachments();
            final IVFSFile rootDir = attachmentVfs.getFile(new FilePath(uid));
            if (rootDir == null)
            {
                logger.warn("Could not find attachment folder for {}/{}", name, uid);
                notificationService.show(Notification.builder("Could not find attachment folder for " + name).build());
                return;
            }
            try
            {
                Desktop.getDesktop().browse(rootDir.getFile().toURI());
            } catch (IOException e)
            {
                logger.warn("Failed to open attachment folder for {}/{} at {}", name, uid, rootDir.getAbsolutePath());
                notificationService.show(Notification.builder("Failed to open attachment folder for " + name).build());
            }
        }
    }

    @InvokeOnEDT
    private void addAttachmentClicked()
    {
        AlwaysOnTopFileChooser fileChooser = AlwaysOnTopFileChooserFactory.getFileChooser(this);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        if (fileChooser.showOpenDialog(JFrame.getFrames()[0]) == JFileChooser.APPROVE_OPTION)
        {
            File f = fileChooser.getSelectedFile();
            mapObject.addAttachment(f.getName(), f);
            updateAttachments();
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    @Subscribe
    public void selectionChanged(MapObjectSelectedEvent event)
    {
        if (!canChangeMapObject) return;

        var moc = event.getMapObjectController();
        if (moc != null)
        {
            mapObject = moc.getMapObject();
            mapObjectNameLabel.setText(mapObject.getName());
            updateAttachments();
        }
    }

    @Override
    public void releaseResources()
    {
        if (uiEventBus.isRegistered(this)) uiEventBus.unregister(this);
        super.releaseResources();
    }

    @Override
    protected void applyActionPerformed()
    {
        // no-op
    }

    @Override
    public String validateContents()
    {
        return null;
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        var buttonPanel = new JPanel();
        attachmentIcon = new JLabel();
        mapObjectNameLabel = new JLabel();
        openAttachmentsFolder = new JButton();
        addAttachmentButton = new JButton();
        scrollPane = new JScrollPane();
        attachmentsPanel = new JPanel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "pref:grow, $lcgap, 0dlu",
                "0dlu, $lgap, default, $lgap, fill:160dlu:grow"));

        //======== buttonPanel ========
        {
            buttonPanel.setName("buttonPanel");
            buttonPanel.setLayout(new FormLayout(
                    "0dlu, $lcgap, 24dlu, $lcgap, default:grow, 2*($lcgap, default)",
                    "2dlu, fill:20dlu"));

            //---- attachmentIcon ----
            attachmentIcon.setMaximumSize(new Dimension(25, 25));
            attachmentIcon.setMinimumSize(new Dimension(25, 25));
            attachmentIcon.setPreferredSize(new Dimension(25, 25));
            attachmentIcon.setName("attachmentIcon");
            buttonPanel.add(attachmentIcon, cc.xy(3, 2));

            //---- mapObjectNameLabel ----
            mapObjectNameLabel.setText("moName");
            mapObjectNameLabel.setFont(mapObjectNameLabel.getFont().deriveFont(mapObjectNameLabel.getFont().getSize() + 8f));
            mapObjectNameLabel.setName("mapObjectNameLabel");
            buttonPanel.add(mapObjectNameLabel, cc.xywh(5, 2, 2, 1));

            //---- openAttachmentsFolder ----
            openAttachmentsFolder.setName("openAttachmentsFolder");
            openAttachmentsFolder.addActionListener(e -> openAttachmentsFolder());
            buttonPanel.add(openAttachmentsFolder, cc.xy(7, 2));

            //---- addAttachmentButton ----
            addAttachmentButton.setName("addAttachmentButton");
            addAttachmentButton.addActionListener(e -> addAttachmentClicked());
            buttonPanel.add(addAttachmentButton, cc.xy(9, 2));
        }
        add(buttonPanel, cc.xy(1, 3));

        //======== scrollPane ========
        {
            scrollPane.setBorder(null);
            scrollPane.setBackground(Color.white);
            scrollPane.setName("scrollPane");

            //======== attachmentsPanel ========
            {
                attachmentsPanel.setBorder(null);
                attachmentsPanel.setForeground(Color.white);
                attachmentsPanel.setName("attachmentsPanel");
                attachmentsPanel.setLayout(new GridLayout());
            }
            scrollPane.setViewportView(attachmentsPanel);
        }
        add(scrollPane, cc.xy(1, 5));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JLabel attachmentIcon;
    private JLabel mapObjectNameLabel;
    private JButton openAttachmentsFolder;
    private JButton addAttachmentButton;
    private JScrollPane scrollPane;
    private JPanel attachmentsPanel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
