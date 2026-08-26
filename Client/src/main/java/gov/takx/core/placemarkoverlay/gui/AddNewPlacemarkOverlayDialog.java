package gov.takx.core.placemarkoverlay.gui;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.registry.IIconSelectionListener;
import gov.takx.commons.swing.AlwaysOnTopFileChooserFactory;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.swing.PlaceholderTextField;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.placemarkoverlay.PlacemarkOverlay;
import gov.takx.core.placemarkoverlay.PlacemarkOverlayData;
import gov.takx.core.placemarkoverlay.PlacemarkOverlayManager;
import gov.takx.core.placemarkoverlay.PlacemarkOverlayUtils;
import gov.takx.core.view.gui.DialogHeaderPanel;
import gov.takx.platform.ui.AStandardDialog;
import gov.takx.platform.ui.UIResources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 */
public class AddNewPlacemarkOverlayDialog extends AStandardDialog implements IIconSelectionListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final static String DESCRIPTION = "<html>Select a file with location information and other data, and " +
            "placemarks will be displayed on the globe to represent the data associated with " +
            "each location in the file. Click on a placemark to see it's data in the Properties panel. A Placemark " +
            "overlay can be turned on and off as a single layer in the Layers panel.</html>";

    private final PlacemarkOverlayManager manager;
    private final PlacemarkIconSelectionPanel placemarkIconSelectionPanel;

    public AddNewPlacemarkOverlayDialog(PlacemarkOverlayManager manager)
    {
        this.manager = manager;
        initComponents();
        setModal(true);

        setLocation(getLocation().x, getLocation().y - 30);

        dialogHeaderPanel.setDescription(DESCRIPTION);
        dialogHeaderPanel.setIcon("placemark.png");

        placemarkIconSelectionPanel = new PlacemarkIconSelectionPanel();
        Dimension p = placemarkIconSelectionPanel.getPreferredSize();
        placemarkIconSelectionPanel.setPreferredSize(new Dimension(p.width, p.height - 230));
        placemarkIconSelectionPanel.setCustomLabel("Please Select Placemarks Icon:");
        iconSelectionPanel.add(placemarkIconSelectionPanel, new CellConstraints(1, 1));
        iconButton.setIcon(placemarkIconSelectionPanel.getSelectedImage());
        placemarkIconSelectionPanel.setIconSelectionListener(this);

        openFileChooser.setMultiSelectionEnabled(false);
        openFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        openFileChooser.addChoosableFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));
        openFileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Tab Delimited (*.txt)", "txt"));
        openFileChooser.setAcceptAllFileFilterUsed(true);
        openFileChooser.setDialogTitle("Open Delimited File");
        openFileChooser.setControlButtonsAreShown(false);

        labelAdvanced.setIcon(UIResources.getStandardSizedIcon(PLATFORM_ICONS, "placemark.png"));
        labelProject.setIcon(UIResources.getStandardSizedIcon(PLATFORM_ICONS, "folder.png"));
        labelGlobal.setIcon(UIResources.getStandardSizedIcon(PLATFORM_ICONS, "global.png"));

        notEditableLabel.setIcon(UIResources.LOCKED_ICON);
        editableLabel.setIcon(UIResources.UNLOCKED_ICON);

        tableDisplayOnMouseOver.setTableHeader(null);
    }

    /**
     * Set the file chooser to a file that has been selected by a means other than the UI.
     *
     * @param f The file that has been selected.
     */
    public void setSelectedFile(File f)
    {
        openFileChooser.setSelectedFile(f);
    }

    @Override
    public void setVisible(boolean visible)
    {
        if (visible)
        {
            openFileChooser.cancelSelection();
            dialogPane.remove(panelAdvanced);
            dialogPane.add(contentPanel, BorderLayout.CENTER);
            showIconSelectionPanel(false);
            imageBrowseButton.setVisible(false);
            buttonAdvanced.setVisible(true);
            radioButtonProject.setSelected(true);
            layerNameTextField.requestFocus();
            layerNameTextField.setCaretPosition(0);
            radioButtonNotEditable.setSelected(true);
        }

        super.setVisible(visible);
    }

    /**
     * Do necessary checks on name, file and icon and attempt to parse the file as CSV data
     */
    private void okButtonActionPerformed()
    {
        if (validateFields())
        {
            createLayer(!buttonAdvanced.isVisible());
        }
    }

    @Override
    public void iconSelected(String iconPath)
    {
        iconButton.setIcon(placemarkIconSelectionPanel.getSelectedImage());
    }

    private char getDelimiter()
    {
        String delimActionCmd = buttonGroupDelim.getSelection().getActionCommand();

        return switch (delimActionCmd)
        {
            case "tab" -> '\t';
            case "space" -> ' ';
            case "semicolon" -> ';';
            case "other" ->
            {
                String otherText = textFieldOther.getText();
                yield null != otherText && !otherText.isEmpty() ? otherText.charAt(0) : ',';
            }
            default -> ',';
        };
    }

    private void createLayer(boolean hasAdvancedOptions)
    {
        File fileCSVInput = openFileChooser.getSelectedFile();
        String sLayerName = layerNameTextField.getText();

        try
        {
            PlacemarkOverlay placemarkOverlay = hasAdvancedOptions
                    ? new PlacemarkOverlay(
                            sLayerName,
                            fileCSVInput,
                            getDelimiter(),
                            placemarkIconSelectionPanel.getSelectedImagePath(),
                            (String) comboBoxId.getSelectedItem(),
                            (String) comboBoxImagery.getSelectedItem(),
                            getTooltipColumns(),
                            radioButtonGlobal.isSelected() ? VFSScope.Global : VFSScope.Project,
                            radioButtonEditable.isSelected(), true)
                    : new PlacemarkOverlay(sLayerName, fileCSVInput, placemarkIconSelectionPanel.getSelectedImagePath(), true);
            manager.addOverlay(placemarkOverlay, true);
            setVisible(false);
            dispose();
        } catch (IOException e)
        {
            logger.error("Unable to read file.", e);
            JOptionPane.showMessageDialog(this, "Unable to read file.  Please check your placemark overlay source.");
        }
    }

    private Collection<String> getTooltipColumns()
    {
        Collection<String> tooltipColumns = new ArrayList<>();

        for (int i = 0; i < tableDisplayOnMouseOver.getRowCount(); i++)
        {
            if ((Boolean) tableDisplayOnMouseOver.getValueAt(i, 0))
            {
                tooltipColumns.add((String) tableDisplayOnMouseOver.getValueAt(i, 1));
            }
        }

        return tooltipColumns;
    }

    private boolean validateFields()
    {
        // Check if the layer name has been entered and is not blank
        String sLayerName = layerNameTextField.getText();

        if (StringUtils.isEmpty(sLayerName))
        {
            AlwaysOnTopJOptionPane.showErrorDialog(this, "Please provide a valid layer name.", "Invalid Input");
            return false;
        }

        // Check to see if a layer already exists with the provided name
        if (manager.doesLayerNameExist(sLayerName))
        {
            AlwaysOnTopJOptionPane.showErrorDialog(this,
                    "A placemark overlay with this name already exists. Please choose a unique layer name.",
                    "Duplicate Layer Name");
            return false;
        }

        // Check if the user provided a fileCSVInput and ensure it is readable
        File fileCSVInput = openFileChooser.getSelectedFile();

        if (fileCSVInput == null || fileCSVInput.getName().isEmpty())
        {
            AlwaysOnTopJOptionPane.showErrorDialog(this, "Please select a file.", "No File Selected");
            return false;
        } else if (!fileCSVInput.canRead())
        {
            AlwaysOnTopJOptionPane.showErrorDialog(this, "Error opening file: " + fileCSVInput, "Error Reading File");
            return false;
        }

        return true;
    }

    private void cancelButtonActionPerformed()
    {
        setVisible(false);
        dispose();
    }

    /**
     * Open a file chooser to browse and add an image to the icons panel
     */
    private void imageBrowseButtonActionPerformed()
    {
        JFileChooser openFileChooser = AlwaysOnTopFileChooserFactory.getFileChooser(PlacemarkIconSelectionPanel.class);
        openFileChooser.setMultiSelectionEnabled(false);
        openFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        openFileChooser.setFileFilter(new FileNameExtensionFilter("Image Files", "png", "bmp", "jpg"));
        openFileChooser.setDialogTitle("Choose a Placemarks Icon");

        int retVal = openFileChooser.showOpenDialog(this);
        if (retVal != JFileChooser.APPROVE_OPTION)
        {
            return;
        }

        File file = openFileChooser.getSelectedFile();
        if (!file.exists())
        {
            JOptionPane.showMessageDialog(this, "Error reading image file : " + file);
        } else
        {
            addImage(file);
        }
    }

    /**
     * Manually add an image to the icon panel
     *
     * @param imageFile file handle to the image file to add
     */
    private void addImage(File imageFile)
    {
        try
        {
            // TODO KMB: May want to enforce specific size, like FEATURE_IMAGE_SIZE.
            final BufferedImage image = UIResources.getImage(imageFile.toURI().toURL());
            final ImageIcon icon = new ImageIcon(image);
            placemarkIconSelectionPanel.addImage(icon, imageFile.getPath());
        } catch (MalformedURLException e)
        {
            logger.error("Failed to access file: {}", imageFile, e);
        }
    }

    private void iconButtonActionPerformed()
    {
        showIconSelectionPanel(!iconSelectionPanel.isVisible());
    }

    private void showIconSelectionPanel(boolean visible)
    {
        iconSelectionPanel.setVisible(visible);
        imageBrowseButton.setVisible(visible);
        pack();
        repaint();
    }

    private void buttonAdvancedActionPerformed()
    {
        if (validateFields())
        {
            try
            {
                PlacemarkOverlayData data = new PlacemarkOverlayData(openFileChooser.getSelectedFile(), getDelimiter(), null, null);
                prepareAdvancedPanel(data);
                dialogPane.remove(contentPanel);
                dialogPane.add(panelAdvanced, BorderLayout.CENTER);
                imageBrowseButton.setVisible(false);
                buttonAdvanced.setVisible(false);
                pack();
                repaint();
            } catch (IOException ioe)
            {
                AlwaysOnTopJOptionPane.showErrorDialog(this, "Error reading Placemarks file.\n" + ioe.getMessage(),
                        "Error Reading Placemarks");
            }
        }
    }

    private void prepareAdvancedPanel(PlacemarkOverlayData data)
    {
        String[] headers = data.getHeaders().toArray(new String[0]);

        // Prepare id combo box
        DefaultComboBoxModel<String> idModel = new DefaultComboBoxModel<>(headers);
        comboBoxId.setModel(idModel);

        // Prepare image combo box
        DefaultComboBoxModel<String> imageModel = new DefaultComboBoxModel<>(headers);
        comboBoxImagery.setModel(imageModel);
        imageModel.insertElementAt("", 0);
        comboBoxImagery.setSelectedIndex(0);

        // Prepare table.
        tableDisplayOnMouseOver.setModel(new TooltipSelectionTableModel(headers));
        TableColumn column = tableDisplayOnMouseOver.getColumnModel().getColumn(0);
        column.setPreferredWidth(25);
        column.setMaxWidth(25);
    }

    private void textFieldOtherKeyTyped()
    {
        radioButtonOther.setSelected(true);
    }

    private static class TooltipSelectionTableModel extends DefaultTableModel
    {
        public TooltipSelectionTableModel(Object[] headers)
        {
            setColumnCount(2);
            for (Object header : headers)
            {
                if (!PlacemarkOverlayUtils.isLatitudeColumn((String) header) &&
                        !PlacemarkOverlayUtils.isLongitudeColumn((String) header))
                {
                    addRow(new Object[]{true, header});
                }
            }
        }

        @Override
        public Class<?> getColumnClass(int columnIndex)
        {
            if (columnIndex == 0)
            {
                return Boolean.class;
            } else
            {
                return String.class;
            }
        }
    }

    @SuppressWarnings("DuplicatedCode")
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        var panelOverlayLayerName = new JPanel();
        var label2 = new JLabel();
        layerNameTextField = new PlaceholderTextField();
        var label3 = new JLabel();
        openFileChooser = AlwaysOnTopFileChooserFactory.getFileChooser(this);
        var separator1 = new JSeparator();
        var panelPlacemarkIcon = new JPanel();
        var label8 = new JLabel();
        var panel9 = new JPanel();
        var radioButtonComma = new JRadioButton();
        var radioButtonTab = new JRadioButton();
        var radioButtonSemicolon = new JRadioButton();
        var radioButtonSpace = new JRadioButton();
        radioButtonOther = new JRadioButton();
        textFieldOther = new JTextField();
        var selectedIconLabel = new JLabel();
        iconButton = new JButton();
        iconSelectionPanel = new JPanel();
        var buttonBar = new JPanel();
        imageBrowseButton = new JButton();
        buttonAdvanced = new JButton();
        var okButton = new JButton();
        var cancelButton = new JButton();
        dialogHeaderPanel = new DialogHeaderPanel();
        panelAdvanced = new JPanel();
        var panel8 = new JPanel();
        labelAdvanced = new JLabel();
        var separator2 = new JSeparator();
        var label1 = new JLabel();
        var panel5 = new JPanel();
        var panel2 = new JPanel();
        radioButtonProject = new JRadioButton();
        labelProject = new JLabel();
        var panel4 = new JPanel();
        radioButtonGlobal = new JRadioButton();
        labelGlobal = new JLabel();
        var label7 = new JLabel();
        var editablePanel = new JPanel();
        radioButtonNotEditable = new JRadioButton();
        notEditableLabel = new JLabel();
        radioButtonEditable = new JRadioButton();
        editableLabel = new JLabel();
        var label4 = new JLabel();
        comboBoxId = new JComboBox<>();
        var label5 = new JLabel();
        comboBoxImagery = new JComboBox<>();
        var label6 = new JLabel();
        var scrollPane1 = new JScrollPane();
        tableDisplayOnMouseOver = new JTable();
        buttonGroupDelim = new ButtonGroup();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Add Placemark Overlay");
        setName("this");
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(Borders.DIALOG_BORDER);
            dialogPane.setName("dialogPane");
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setName("contentPanel");
                contentPanel.setLayout(new FormLayout(
                        "default:grow",
                        "default, $ugap, default, $lgap, fill:default:grow, 2*($lgap, default), $lgap, fill:default:grow"));

                //======== panelOverlayLayerName ========
                {
                    panelOverlayLayerName.setName("panelOverlayLayerName");
                    panelOverlayLayerName.setLayout(new FormLayout(
                            "default, $lcgap, default:grow",
                            "default"));

                    //---- label2 ----
                    label2.setText("Name:");
                    label2.setName("label2");
                    panelOverlayLayerName.add(label2, cc.xy(1, 1));

                    //---- layerNameTextField ----
                    layerNameTextField.setPlaceholderText("Enter a name for the Placemarks Overlay here...");
                    layerNameTextField.setName("layerNameTextField");
                    panelOverlayLayerName.add(layerNameTextField, cc.xy(3, 1));
                }
                contentPanel.add(panelOverlayLayerName, cc.xy(1, 1));

                //---- label3 ----
                label3.setText("<html>Select a CSV file with location information and other data to display as placemarks on the globe.</html>");
                label3.setName("label3");
                contentPanel.add(label3, cc.xy(1, 3));

                //---- openFileChooser ----
                openFileChooser.setName("openFileChooser");
                contentPanel.add(openFileChooser, cc.xy(1, 5));

                //---- separator1 ----
                separator1.setName("separator1");
                contentPanel.add(separator1, cc.xy(1, 7));

                //======== panelPlacemarkIcon ========
                {
                    panelPlacemarkIcon.setName("panelPlacemarkIcon");
                    panelPlacemarkIcon.setLayout(new FormLayout(
                            "default, $lcgap, left:default",
                            "2*(default)"));

                    //---- label8 ----
                    label8.setText("Record Delimiter:");
                    label8.setName("label8");
                    panelPlacemarkIcon.add(label8, cc.xy(1, 1));

                    //======== panel9 ========
                    {
                        panel9.setName("panel9");
                        panel9.setLayout(new FormLayout(
                                "5*(default, $lcgap), default",
                                "default"));

                        //---- radioButtonComma ----
                        radioButtonComma.setText("Comma");
                        radioButtonComma.setSelected(true);
                        radioButtonComma.setActionCommand("comma");
                        radioButtonComma.setName("radioButtonComma");
                        panel9.add(radioButtonComma, cc.xy(1, 1));

                        //---- radioButtonTab ----
                        radioButtonTab.setText("Tab");
                        radioButtonTab.setActionCommand("tab");
                        radioButtonTab.setName("radioButtonTab");
                        panel9.add(radioButtonTab, cc.xy(3, 1));

                        //---- radioButtonSemicolon ----
                        radioButtonSemicolon.setText("Semicolon");
                        radioButtonSemicolon.setActionCommand("semicolon");
                        radioButtonSemicolon.setName("radioButtonSemicolon");
                        panel9.add(radioButtonSemicolon, cc.xy(5, 1));

                        //---- radioButtonSpace ----
                        radioButtonSpace.setText("Space");
                        radioButtonSpace.setActionCommand("space");
                        radioButtonSpace.setName("radioButtonSpace");
                        panel9.add(radioButtonSpace, cc.xy(7, 1));

                        //---- radioButtonOther ----
                        radioButtonOther.setText("Other:");
                        radioButtonOther.setActionCommand("other");
                        radioButtonOther.setName("radioButtonOther");
                        panel9.add(radioButtonOther, cc.xy(9, 1));

                        //---- textFieldOther ----
                        textFieldOther.setName("textFieldOther");
                        textFieldOther.addKeyListener(new KeyAdapter()
                        {
                            @Override
                            public void keyTyped(KeyEvent e)
                            {
                                textFieldOtherKeyTyped();
                            }
                        });
                        panel9.add(textFieldOther, cc.xy(11, 1));
                    }
                    panelPlacemarkIcon.add(panel9, cc.xy(3, 1));

                    //---- selectedIconLabel ----
                    selectedIconLabel.setText("Placemark Icon:");
                    selectedIconLabel.setName("selectedIconLabel");
                    panelPlacemarkIcon.add(selectedIconLabel, cc.xy(1, 2));

                    //---- iconButton ----
                    iconButton.setName("iconButton");
                    iconButton.addActionListener(e -> iconButtonActionPerformed());
                    panelPlacemarkIcon.add(iconButton, cc.xy(3, 2));
                }
                contentPanel.add(panelPlacemarkIcon, cc.xy(1, 9));

                //======== iconSelectionPanel ========
                {
                    iconSelectionPanel.setName("iconSelectionPanel");
                    iconSelectionPanel.setLayout(new FormLayout(
                            "default:grow",
                            "top:[default,250dlu]:grow"));
                }
                contentPanel.add(iconSelectionPanel, cc.xy(1, 11));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "$button, $glue, 2*($button, $rgap), $button",
                        "pref"));

                //---- imageBrowseButton ----
                imageBrowseButton.setText("Browse for an Icon");
                imageBrowseButton.setName("imageBrowseButton");
                imageBrowseButton.addActionListener(e -> imageBrowseButtonActionPerformed());
                buttonBar.add(imageBrowseButton, cc.xy(1, 1));

                //---- buttonAdvanced ----
                buttonAdvanced.setText("Advanced");
                buttonAdvanced.setName("buttonAdvanced");
                buttonAdvanced.addActionListener(e -> buttonAdvancedActionPerformed());
                buttonBar.add(buttonAdvanced, cc.xy(3, 1));

                //---- okButton ----
                okButton.setText("Finish");
                okButton.setName("okButton");
                okButton.addActionListener(e -> okButtonActionPerformed());
                buttonBar.add(okButton, cc.xy(5, 1));

                //---- cancelButton ----
                cancelButton.setText("Cancel");
                cancelButton.setName("cancelButton");
                cancelButton.addActionListener(e -> cancelButtonActionPerformed());
                buttonBar.add(cancelButton, cc.xy(7, 1));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);

            //---- dialogHeaderPanel ----
            dialogHeaderPanel.setName("dialogHeaderPanel");
            dialogPane.add(dialogHeaderPanel, BorderLayout.NORTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());

        //======== panelAdvanced ========
        {
            panelAdvanced.setName("panelAdvanced");
            panelAdvanced.setLayout(new FormLayout(
                    "right:default, $lcgap, default:grow",
                    "2*(default, $lgap), 4*(default, $ugap), fill:default:grow, $ugap, top:default"));

            //======== panel8 ========
            {
                panel8.setName("panel8");
                panel8.setLayout(new FormLayout(
                        "default:grow",
                        "default"));

                //---- labelAdvanced ----
                labelAdvanced.setText("<html>Configure advanced placemark options. Select the scope of the placemarks, whether or not individual placemarks are editable, the column that placemarks will use as their names and what will be displayed when you mouse-over them.");
                labelAdvanced.setName("labelAdvanced");
                panel8.add(labelAdvanced, cc.xy(1, 1));
            }
            panelAdvanced.add(panel8, cc.xywh(1, 1, 3, 1));

            //---- separator2 ----
            separator2.setName("separator2");
            panelAdvanced.add(separator2, cc.xywh(1, 3, 3, 1));

            //---- label1 ----
            label1.setText("Add Placemarks To:");
            label1.setName("label1");
            panelAdvanced.add(label1, cc.xy(1, 5));

            //======== panel5 ========
            {
                panel5.setName("panel5");
                panel5.setLayout(new FormLayout(
                        "default, $ugap, default",
                        "default"));

                //======== panel2 ========
                {
                    panel2.setName("panel2");
                    panel2.setLayout(new FormLayout(
                            "default, $lcgap, default",
                            "default"));

                    //---- radioButtonProject ----
                    radioButtonProject.setSelected(true);
                    radioButtonProject.setToolTipText("Placemarks will only be visible in this project.");
                    radioButtonProject.setName("radioButtonProject");
                    panel2.add(radioButtonProject, cc.xy(1, 1));

                    //---- labelProject ----
                    labelProject.setText("Project");
                    labelProject.setToolTipText("Placemarks will only be visible in this project.");
                    labelProject.setName("labelProject");
                    panel2.add(labelProject, cc.xy(3, 1));
                }
                panel5.add(panel2, cc.xy(1, 1));

                //======== panel4 ========
                {
                    panel4.setName("panel4");
                    panel4.setLayout(new FormLayout(
                            "default, $lcgap, default",
                            "default"));

                    //---- radioButtonGlobal ----
                    radioButtonGlobal.setToolTipText("Placemarks will be visible in all projects opened by any user.");
                    radioButtonGlobal.setName("radioButtonGlobal");
                    panel4.add(radioButtonGlobal, cc.xy(1, 1));

                    //---- labelGlobal ----
                    labelGlobal.setText("Global");
                    labelGlobal.setToolTipText("Placemarks will be visible in all projects opened by any user.");
                    labelGlobal.setName("labelGlobal");
                    panel4.add(labelGlobal, cc.xy(3, 1));
                }
                panel5.add(panel4, cc.xy(3, 1));
            }
            panelAdvanced.add(panel5, cc.xy(3, 5));

            //---- label7 ----
            label7.setText("Editable:");
            label7.setName("label7");
            panelAdvanced.add(label7, cc.xy(1, 7));

            //======== editablePanel ========
            {
                editablePanel.setName("editablePanel");
                editablePanel.setLayout(new FormLayout(
                        "default, $lcgap, default, $ugap, default, $lcgap, default",
                        "default"));

                //---- radioButtonNotEditable ----
                radioButtonNotEditable.setSelected(true);
                radioButtonNotEditable.setToolTipText("No users will be allowed to edit existing placemarks.");
                radioButtonNotEditable.setName("radioButtonNotEditable");
                editablePanel.add(radioButtonNotEditable, cc.xy(1, 1));

                //---- notEditableLabel ----
                notEditableLabel.setText("No");
                notEditableLabel.setName("notEditableLabel");
                editablePanel.add(notEditableLabel, cc.xy(3, 1));

                //---- radioButtonEditable ----
                radioButtonEditable.setToolTipText("Any user will be able to edit existing placemarks.");
                radioButtonEditable.setName("radioButtonEditable");
                editablePanel.add(radioButtonEditable, cc.xy(5, 1));

                //---- editableLabel ----
                editableLabel.setText("Yes");
                editableLabel.setName("editableLabel");
                editablePanel.add(editableLabel, cc.xy(7, 1));
            }
            panelAdvanced.add(editablePanel, cc.xy(3, 7));

            //---- label4 ----
            label4.setText("Placemark Name:");
            label4.setName("label4");
            panelAdvanced.add(label4, cc.xy(1, 9));

            //---- comboBoxId ----
            comboBoxId.setName("comboBoxId");
            panelAdvanced.add(comboBoxId, cc.xy(3, 9));

            //---- label5 ----
            label5.setText("Reference Image:");
            label5.setName("label5");
            panelAdvanced.add(label5, cc.xy(1, 11));

            //---- comboBoxImagery ----
            comboBoxImagery.setName("comboBoxImagery");
            panelAdvanced.add(comboBoxImagery, cc.xy(3, 11));

            //---- label6 ----
            label6.setText("<html>Display on<br>Mouse-Over:");
            label6.setName("label6");
            panelAdvanced.add(label6, cc.xy(1, 13, CellConstraints.DEFAULT, CellConstraints.TOP));

            //======== scrollPane1 ========
            {
                scrollPane1.setName("scrollPane1");

                //---- tableDisplayOnMouseOver ----
                tableDisplayOnMouseOver.setPreferredScrollableViewportSize(new Dimension(100, 150));
                tableDisplayOnMouseOver.setName("tableDisplayOnMouseOver");
                scrollPane1.setViewportView(tableDisplayOnMouseOver);
            }
            panelAdvanced.add(scrollPane1, cc.xy(3, 13));
        }

        //---- buttonGroupDelim ----
        buttonGroupDelim.add(radioButtonComma);
        buttonGroupDelim.add(radioButtonTab);
        buttonGroupDelim.add(radioButtonSemicolon);
        buttonGroupDelim.add(radioButtonSpace);
        buttonGroupDelim.add(radioButtonOther);

        //---- buttonGroupScope ----
        var buttonGroupScope = new ButtonGroup();
        buttonGroupScope.add(radioButtonProject);
        buttonGroupScope.add(radioButtonGlobal);

        //---- buttonGroupEditable ----
        var buttonGroupEditable = new ButtonGroup();
        buttonGroupEditable.add(radioButtonNotEditable);
        buttonGroupEditable.add(radioButtonEditable);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel dialogPane;
    private JPanel contentPanel;
    private PlaceholderTextField layerNameTextField;
    private JFileChooser openFileChooser;
    private JRadioButton radioButtonOther;
    private JTextField textFieldOther;
    private JButton iconButton;
    private JPanel iconSelectionPanel;
    private JButton imageBrowseButton;
    private JButton buttonAdvanced;
    private DialogHeaderPanel dialogHeaderPanel;
    private JPanel panelAdvanced;
    private JLabel labelAdvanced;
    private JRadioButton radioButtonProject;
    private JLabel labelProject;
    private JRadioButton radioButtonGlobal;
    private JLabel labelGlobal;
    private JRadioButton radioButtonNotEditable;
    private JLabel notEditableLabel;
    private JRadioButton radioButtonEditable;
    private JLabel editableLabel;
    private JComboBox<String> comboBoxId;
    private JComboBox<String> comboBoxImagery;
    private JTable tableDisplayOnMouseOver;
    private ButtonGroup buttonGroupDelim;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
