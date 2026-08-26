package gov.takx.core.view.gui.takserver;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.comms.takserver.IDataSyncManager;
import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.commons.swing.AlwaysOnTopFileChooserFactory;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.core.mapobject.CancelableMapObjectSelectionListener;
import gov.takx.platform.ui.AStandardDialog;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dialog responsible for creating user log entries to attach to missions.
 *
 * @since 5.0
 */
@PseudoBean
public class DataFeedUserLogDialog extends AStandardDialog
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm");
    private final Map<File, JPanel> attachedFilePanels = new ConcurrentHashMap<>();
    private final ITakServerConnection connection;
    private final String missionName;
    @Inject
    private IDataSyncManager dataSyncManager;
    private List<IMapObject> attachedMapItems;

    public DataFeedUserLogDialog(@Nonnull ITakServerConnection connection, @Nonnull String missionName)
    {
        this.connection = connection;
        this.missionName = missionName;

        initComponents();

        timeText.setText(DATE_TIME_FORMATTER.format(Instant.now().atZone(ZoneId.systemDefault())));
    }

    /**
     * Sets the attached map object from the given list.
     *
     * @param mapObjects Map objects to attach to the log.
     */
    private void setMapObjects(@Nullable List<IMapObject> mapObjects)
    {
        attachedMapItems = mapObjects;

        if (mapObjects != null && !mapObjects.isEmpty()) selectedMapItemLabel.setText(mapObjects.get(0).getName());
    }

    /**
     * Validates all inputs are OK for the creation of a user log.
     */
    private void validateInputs()
    {
        okButton.setEnabled(!entryText.getText().isBlank());
    }

    /**
     * Called when {@link #okButton} is pressed.
     */
    private void onOk()
    {
        String entry = entryText.getText();
        List<String> keywords = Arrays.stream(tagsText.getText().split("\\s*,\\s*"))
                .filter(tag -> !tag.isBlank())
                .toList();
        List<File> files = new ArrayList<>(attachedFilePanels.keySet());
        IMapObject mapItem = attachedMapItems == null || attachedMapItems.isEmpty() ? null : attachedMapItems.get(0);
        dataSyncManager.addUserLog(connection, missionName, entry, keywords, files, mapItem)
                .thenCompose(IDataSyncManager.MissionAddLogEntry::logEntryFuture)
                .whenComplete((result, error) -> {
                    // We really only need to handle error feedback since the success of this operation visually updates
                    // the date feed info panel.
                    if (error != null)
                    {
                        logger.error("Uncaught exception adding user log to mission '{}'", missionName, error);
                        AlwaysOnTopJOptionPane.showErrorDialog(
                                "Uncaught exception adding user log to mission '" + missionName + "':\n" + error.getMessage(),
                                "User Log Upload Error");
                    }
                });

        dispose();
    }

    /**
     * Called when {@link #cancelButton} is pressed.
     */
    private void onCancel()
    {
        dispose();
    }

    /**
     * Called when {@link #selectMapItemButton} is pressed.
     */
    private void selectMapItem()
    {
        new CancelableMapObjectSelectionListener(list -> {
            if (list != null)
            {
                setMapObjects(list);
            }
        }).register();
    }

    /**
     * Called when {@link #attachButton} is pressed.
     */
    private void attachFile()
    {
        JFileChooser fileChooser = AlwaysOnTopFileChooserFactory.getFileChooser(this);
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.showDialog(this, "Select");

        File[] files = fileChooser.getSelectedFiles();
        if (files != null) addFiles(files);
    }

    @InvokeOnEDT
    private void addFiles(@Nonnull File[] files)
    {
        for (File file : files)
        {
            JPanel fileAttachment = new JPanel();
            JButton removeButton = new JButton();
            removeButton.setIcon(UIResources.DELETE_ICON);
            fileAttachment.setLayout(new BorderLayout());
            fileAttachment.add(new JLabel(file.getName()), BorderLayout.EAST);
            fileAttachment.add(removeButton, BorderLayout.WEST);
            fileAttachment.add(Box.createVerticalGlue(), BorderLayout.SOUTH);
            removeButton.addActionListener(e -> {
                attachments.remove(fileAttachment);
                attachedFilePanels.remove(file);
                attachments.revalidate();
                attachments.repaint();
            });
            attachments.add(fileAttachment);
            attachedFilePanels.put(file, fileAttachment);
        }

        attachments.revalidate();
        attachments.repaint();
    }

    /**
     * Called when {@link #entryText} is updated.
     */
    private void onEntryTextChanged()
    {
        validateInputs();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        entryLabel = new JLabel();
        entryText = new JTextField();
        timeLabel = new JLabel();
        timeText = new JTextField();
        tagsLabel = new JLabel();
        tagsText = new JTextField();
        mapItemLabel = new JLabel();
        mapItemSelectionWrapper = new JPanel();
        selectedMapItemLabel = new JLabel();
        selectMapItemButton = new JButton();
        attachmentsLabel = new JLabel();
        attachmentsWrapper = new JPanel();
        attachButton = new JButton();
        attachments = new JPanel();
        buttonBar = new JPanel();
        okButton = new JButton();
        cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Create user log");
        setMinimumSize(new Dimension(330, 310));
        setName("this");
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(Borders.createEmptyBorder("7dlu, 7dlu, 7dlu, 7dlu"));
            dialogPane.setName("dialogPane");
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setName("contentPanel");
                contentPanel.setLayout(new FormLayout(
                        "default, $lcgap, default:grow",
                        "5*(default, $lgap), fill:default:grow, $rgap, bottom:default"));

                //---- entryLabel ----
                entryLabel.setText("Entry");
                entryLabel.setName("entryLabel");
                contentPanel.add(entryLabel, cc.xy(1, 1));

                //---- entryText ----
                entryText.setName("entryText");
                entryText.addKeyListener(new KeyAdapter()
                {
                    @Override
                    public void keyReleased(KeyEvent e)
                    {
                        onEntryTextChanged();
                    }
                });
                contentPanel.add(entryText, cc.xy(3, 1));

                //---- timeLabel ----
                timeLabel.setText("DTG");
                timeLabel.setName("timeLabel");
                contentPanel.add(timeLabel, cc.xy(1, 3));

                //---- timeText ----
                timeText.setEnabled(false);
                timeText.setName("timeText");
                contentPanel.add(timeText, cc.xy(3, 3));

                //---- tagsLabel ----
                tagsLabel.setText("Tags");
                tagsLabel.setName("tagsLabel");
                contentPanel.add(tagsLabel, cc.xy(1, 5));

                //---- tagsText ----
                tagsText.setName("tagsText");
                contentPanel.add(tagsText, cc.xy(3, 5));

                //---- mapItemLabel ----
                mapItemLabel.setText("Map item");
                mapItemLabel.setName("mapItemLabel");
                contentPanel.add(mapItemLabel, cc.xy(1, 7));

                //======== mapItemSelectionWrapper ========
                {
                    mapItemSelectionWrapper.setName("mapItemSelectionWrapper");
                    mapItemSelectionWrapper.setLayout(new FormLayout(
                            "default:grow, right:default:grow",
                            "default"));

                    //---- selectedMapItemLabel ----
                    selectedMapItemLabel.setName("selectedMapItemLabel");
                    mapItemSelectionWrapper.add(selectedMapItemLabel, cc.xy(1, 1));

                    //---- selectMapItemButton ----
                    selectMapItemButton.setText("Select");
                    selectMapItemButton.setName("selectMapItemButton");
                    selectMapItemButton.addActionListener(e -> selectMapItem());
                    mapItemSelectionWrapper.add(selectMapItemButton, cc.xy(2, 1));
                }
                contentPanel.add(mapItemSelectionWrapper, cc.xy(3, 7));

                //---- attachmentsLabel ----
                attachmentsLabel.setText("Attachments");
                attachmentsLabel.setName("attachmentsLabel");
                contentPanel.add(attachmentsLabel, cc.xy(1, 9));

                //======== attachmentsWrapper ========
                {
                    attachmentsWrapper.setName("attachmentsWrapper");
                    attachmentsWrapper.setLayout(new FormLayout(
                            "right:default:grow",
                            "default"));

                    //---- attachButton ----
                    attachButton.setText("Add");
                    attachButton.setName("attachButton");
                    attachButton.addActionListener(e -> attachFile());
                    attachmentsWrapper.add(attachButton, cc.xy(1, 1));
                }
                contentPanel.add(attachmentsWrapper, cc.xy(3, 9));

                //======== attachments ========
                {
                    attachments.setName("attachments");
                    attachments.setLayout(new BoxLayout(attachments, BoxLayout.Y_AXIS));
                }
                contentPanel.add(attachments, cc.xywh(1, 11, 3, 1));

                //======== buttonBar ========
                {
                    buttonBar.setName("buttonBar");
                    buttonBar.setLayout(new FormLayout(
                            "default:grow, $lcgap, default:grow",
                            "default"));

                    //---- okButton ----
                    okButton.setText("OK");
                    okButton.setEnabled(false);
                    okButton.setName("okButton");
                    okButton.addActionListener(e -> onOk());
                    buttonBar.add(okButton, cc.xy(1, 1));

                    //---- cancelButton ----
                    cancelButton.setText("Cancel");
                    cancelButton.setName("cancelButton");
                    cancelButton.addActionListener(e -> onCancel());
                    buttonBar.add(cancelButton, cc.xy(3, 1));
                }
                contentPanel.add(buttonBar, cc.xywh(1, 13, 3, 1));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel dialogPane;
    private JPanel contentPanel;
    private JLabel entryLabel;
    private JTextField entryText;
    private JLabel timeLabel;
    private JTextField timeText;
    private JLabel tagsLabel;
    private JTextField tagsText;
    private JLabel mapItemLabel;
    private JPanel mapItemSelectionWrapper;
    private JLabel selectedMapItemLabel;
    private JButton selectMapItemButton;
    private JLabel attachmentsLabel;
    private JPanel attachmentsWrapper;
    private JButton attachButton;
    private JPanel attachments;
    private JPanel buttonBar;
    private JButton okButton;
    private JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
