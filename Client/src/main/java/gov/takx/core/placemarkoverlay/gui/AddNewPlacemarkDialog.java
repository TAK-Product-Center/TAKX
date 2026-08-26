package gov.takx.core.placemarkoverlay.gui;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.commons.swing.AlwaysOnTopFileChooser;
import gov.takx.commons.swing.AlwaysOnTopFileChooserFactory;
import gov.takx.core.placemarkoverlay.CSVPlaceMark;
import gov.takx.core.placemarkoverlay.PlacemarkOverlay;
import gov.takx.core.placemarkoverlay.PlacemarkOverlayManager;
import gov.takx.core.placemarkoverlay.PlacemarkOverlayPropertiesModel;
import gov.takx.core.placemarkoverlay.PlacemarkOverlayUtils;
import gov.takx.core.view.gui.DialogHeaderPanel;
import gov.takx.platform.ui.AStandardDialog;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 */
public class AddNewPlacemarkDialog extends AStandardDialog
{
    private static final String DESCRIPTION = "<html>This feature allows you to add a single point placemark to an " +
            "existing set of placemarks. Begin by choosing the existing set of placemarks (the layer) to which you " +
            "want to add a placemark. Then fill in the placemark's details in the table below.</html>";
    private final PlacemarkOverlayManager manager;
    private final DefaultComboBoxModel<PlacemarkOverlay> comboBoxModel;
    private final PlacemarkOverlayPropertiesModel tableModel;

    public AddNewPlacemarkDialog(PlacemarkOverlayManager manager, ILocation location)
    {
        initComponents();

        this.manager = manager;

        comboBoxModel = new DefaultComboBoxModel<>();
        comboBoxLayer.setModel(comboBoxModel);
        comboBoxLayer.setRenderer(new PlacemarkOverlayRenderer());

        tableModel = (PlacemarkOverlayPropertiesModel) tableProperties.getModel();

        dialogHeaderPanel.setDescription(DESCRIPTION);
        dialogHeaderPanel.setIcon("placemark.png");
        textFieldLocation.setText(location.getLatitude() + "," + location.getLongitude());
        checkLocation();
    }

    @Override
    public void setVisible(boolean visible)
    {
        if (visible)
        {
            // Clear and reload the layer selection combo box.
            comboBoxModel.removeAllElements();

            for (PlacemarkOverlay placemarkOverlay : manager.getLayers())
            {
                if (placemarkOverlay.isEditable())
                {
                    comboBoxModel.addElement(placemarkOverlay);
                }
            }

            // Select the first item in the list.
            comboBoxLayer.setSelectedIndex(0);
        }

        super.setVisible(visible);
    }

    private void comboBoxLayerItemStateChanged()
    {
        int index = comboBoxLayer.getSelectedIndex();

        if (index != -1)
        {
            tableModel.getDataVector().removeAllElements();

            PlacemarkOverlay overlay = comboBoxModel.getElementAt(index);
            List<String> headers = overlay.getHeaders();

            for (String header : headers)
            {
                if (!header.equals(CSVPlaceMark.COMMENTS) && !overlay.getLatitudeHeader().equals(header) && !overlay.getLongitudeHeader().equals(header))
                {
                    tableModel.addRow(new Object[]{header, new JTextField()});
                }
            }
        }
    }

    private void addButtonActionPerformed()
    {
        Map<String, String> map = new HashMap<>();
        PlacemarkOverlay overlay = comboBoxModel.getElementAt(comboBoxLayer.getSelectedIndex());

        if (tableProperties.isEditing())
        {
            tableProperties.getCellEditor().stopCellEditing();
        }

        for (int i = 0; i < tableModel.getRowCount(); i++)
        {
            map.put((String) tableModel.getValueAt(i, 0), ((JTextField) tableModel.getValueAt(i, 1)).getText());
        }

        String locationString = textFieldLocation.getText();
        ILocation location = CoordinateSystem.computeLatLonFromString(locationString);
        map.put(overlay.getLatitudeHeader(), String.valueOf(location.getLatitude()));
        map.put(overlay.getLongitudeHeader(), String.valueOf(location.getLongitude()));

        manager.addPlacemark(overlay, map, textFieldFileName.getText());
        setVisible(false);
    }

    private void cancelButtonActionPerformed()
    {
        setVisible(false);
    }

    private void textFieldLocationKeyReleased()
    {
        checkLocation();
    }

    private void checkLocation()
    {
        String locationString = textFieldLocation.getText();
        ILocation location = CoordinateSystem.computeLatLonFromString(locationString);
        addButton.setEnabled(location != null);
    }

    private void buttonSelectImageActionPerformed()
    {
        AlwaysOnTopFileChooser fileChooser = AlwaysOnTopFileChooserFactory.getFileChooser(this);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        if (fileChooser.showOpenDialog(JFrame.getFrames()[0]) == JFileChooser.APPROVE_OPTION)
        {
            File f = fileChooser.getSelectedFile();

            if (PlacemarkOverlayUtils.checkImage(f))
            {
                String path = f.getPath();
                textFieldFileName.setText(path);
                setFilePathAttribute(path);
            }
        }
    }

    @InvokeOnEDT
    private void setFilePathAttribute(final String filePath)
    {
        for (int i = 0; i < tableModel.getRowCount(); i++)
        {
            String value = ((String) tableModel.getValueAt(i, 0)).toLowerCase();
            if (value.contains("file"))
            {
                JTextField tf = ((JTextField) tableModel.getValueAt(i, 1));
                tf.setText(filePath);
                tableProperties.repaint();
                break;
            }
        }
    }

    private static class PlacemarkOverlayRenderer extends DefaultListCellRenderer
    {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
        {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            setText(((PlacemarkOverlay) value).getLayerName());
            return this;
        }
    }

    @SuppressWarnings("DuplicatedCode")
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        dialogHeaderPanel = new DialogHeaderPanel();
        var contentPanel = new JPanel();
        var panelContainer = new JPanel();
        var panelContent = new JPanel();
        var labelLayerChoices = new JLabel();
        comboBoxLayer = new JComboBox<>();
        var separator2 = new JSeparator();
        var label2 = new JLabel();
        var label1 = new JLabel();
        textFieldLocation = new JTextField();
        var label3 = new JLabel();
        var panel1 = new JPanel();
        textFieldFileName = new JTextField();
        var buttonSelectImage = new JButton();
        var panel2 = new JPanel();
        var scrollPaneProperties = new JScrollPane();
        tableProperties = new KeyValueTable();
        var buttonBar = new JPanel();
        addButton = new JButton();
        var cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(Borders.DIALOG_BORDER);
            dialogPane.setName("dialogPane");
            dialogPane.setLayout(new BorderLayout());

            //---- dialogHeaderPanel ----
            dialogHeaderPanel.setName("dialogHeaderPanel");
            dialogPane.add(dialogHeaderPanel, BorderLayout.NORTH);

            //======== contentPanel ========
            {
                contentPanel.setName("contentPanel");
                contentPanel.setLayout(new FormLayout(
                        "default:grow",
                        "default, $lgap, default"));

                //======== panelContainer ========
                {
                    panelContainer.setName("panelContainer");
                    panelContainer.setLayout(new FormLayout(
                            "default:grow",
                            "fill:default:grow"));

                    //======== panelContent ========
                    {
                        panelContent.setName("panelContent");
                        panelContent.setLayout(new FormLayout(
                                "default, $lcgap, default:grow",
                                "4*(default, $lgap), default"));

                        //---- labelLayerChoices ----
                        labelLayerChoices.setText("Add to Layer:");
                        labelLayerChoices.setName("labelLayerChoices");
                        panelContent.add(labelLayerChoices, cc.xy(1, 1, CellConstraints.RIGHT, CellConstraints.DEFAULT));

                        //---- comboBoxLayer ----
                        comboBoxLayer.setName("comboBoxLayer");
                        comboBoxLayer.addItemListener(e -> comboBoxLayerItemStateChanged());
                        panelContent.add(comboBoxLayer, cc.xy(3, 1));

                        //---- separator2 ----
                        separator2.setName("separator2");
                        panelContent.add(separator2, cc.xywh(1, 3, 3, 1));

                        //---- label2 ----
                        label2.setText("Configure the Placemark");
                        label2.putClientProperty("FlatLaf.styleClass", "large");
                        label2.setName("label2");
                        panelContent.add(label2, cc.xywh(1, 5, 3, 1, CellConstraints.CENTER, CellConstraints.DEFAULT));

                        //---- label1 ----
                        label1.setText("Coordinates:");
                        label1.setHorizontalAlignment(SwingConstants.TRAILING);
                        label1.setName("label1");
                        panelContent.add(label1, cc.xy(1, 7));

                        //---- textFieldLocation ----
                        textFieldLocation.setToolTipText("Enter Latitude and Longitude");
                        textFieldLocation.setName("textFieldLocation");
                        textFieldLocation.addKeyListener(new KeyAdapter()
                        {
                            @Override
                            public void keyReleased(KeyEvent e)
                            {
                                textFieldLocationKeyReleased();
                            }
                        });
                        panelContent.add(textFieldLocation, cc.xy(3, 7));

                        //---- label3 ----
                        label3.setText("Image:");
                        label3.setName("label3");
                        panelContent.add(label3, cc.xy(1, 9));

                        //======== panel1 ========
                        {
                            panel1.setName("panel1");
                            panel1.setLayout(new FormLayout(
                                    "default:grow, $lcgap, $button",
                                    "default"));

                            //---- textFieldFileName ----
                            textFieldFileName.setEditable(false);
                            textFieldFileName.setName("textFieldFileName");
                            panel1.add(textFieldFileName, cc.xy(1, 1));

                            //---- buttonSelectImage ----
                            buttonSelectImage.setText("Browse...");
                            buttonSelectImage.setName("buttonSelectImage");
                            buttonSelectImage.addActionListener(e -> buttonSelectImageActionPerformed());
                            panel1.add(buttonSelectImage, cc.xy(3, 1));
                        }
                        panelContent.add(panel1, cc.xy(3, 9));
                    }
                    panelContainer.add(panelContent, cc.xy(1, 1));
                }
                contentPanel.add(panelContainer, cc.xy(1, 1));

                //======== panel2 ========
                {
                    panel2.setName("panel2");
                    panel2.setLayout(new FormLayout(
                            "default:grow",
                            "default"));

                    //======== scrollPaneProperties ========
                    {
                        scrollPaneProperties.setName("scrollPaneProperties");

                        //---- tableProperties ----
                        tableProperties.setPreferredScrollableViewportSize(new Dimension(100, 200));
                        tableProperties.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                        tableProperties.setRowSelectionAllowed(false);
                        tableProperties.setFillsViewportHeight(true);
                        tableProperties.setShowHorizontalLines(false);
                        tableProperties.setShowVerticalLines(false);
                        tableProperties.setName("tableProperties");
                        scrollPaneProperties.setViewportView(tableProperties);
                    }
                    panel2.add(scrollPaneProperties, cc.xy(1, 1));
                }
                contentPanel.add(panel2, cc.xy(1, 3));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "$glue, $button, $rgap, $button",
                        "pref"));

                //---- addButton ----
                addButton.setText("Add");
                addButton.setName("addButton");
                addButton.addActionListener(e -> addButtonActionPerformed());
                buttonBar.add(addButton, cc.xy(2, 1));

                //---- cancelButton ----
                cancelButton.setText("Cancel");
                cancelButton.setName("cancelButton");
                cancelButton.addActionListener(e -> cancelButtonActionPerformed());
                buttonBar.add(cancelButton, cc.xy(4, 1));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private DialogHeaderPanel dialogHeaderPanel;
    private JComboBox<PlacemarkOverlay> comboBoxLayer;
    private JTextField textFieldLocation;
    private JTextField textFieldFileName;
    private KeyValueTable tableProperties;
    private JButton addButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
