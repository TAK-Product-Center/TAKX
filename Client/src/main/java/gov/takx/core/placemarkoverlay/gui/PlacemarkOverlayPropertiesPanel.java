package gov.takx.core.placemarkoverlay.gui;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.nasa.worldwind.geom.Position;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.swing.AlwaysOnTopFileChooser;
import gov.takx.commons.swing.AlwaysOnTopFileChooserFactory;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.placemarkoverlay.CSVPlaceMark;
import gov.takx.core.placemarkoverlay.PlacemarkOverlayPropertiesModel;
import gov.takx.core.placemarkoverlay.PlacemarkOverlayUtils;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.JTextComponent;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;

import static gov.takx.platform.ui.UIResources.LOCKED_ICON;
import static gov.takx.platform.ui.UIResources.UNLOCKED_ICON;

/**
 */
@PseudoBean
public class PlacemarkOverlayPropertiesPanel extends JPanel implements PropertyChangeListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String PRESS_TO_UNLOCK_MSG = "Press to unlock editing.";
    private static final String PRESS_TO_LOCK_MSG = "Press to lock editing.";
    private static final String CANNOT_EDIT_MSG = "Placemark is not editable.";
    private static final Border HOVER_BORDER = new LineBorder(Color.cyan, 3);
    private static final Border EMPTY_BORDER = new EmptyBorder(3, 3, 3, 3);

    @Inject
    private IExtendedGlobeManager globeManager;

    private CSVPlaceMark selectedCSVPlacemark = null;
    private String selectedCommentsHeader = null;
    private final PlacemarkOverlayPropertiesModel model;
    private boolean locationModified;

    private class KeyReleasedListener extends KeyAdapter
    {
        final String key;

        public KeyReleasedListener(String key)
        {
            this.key = key;
        }

        public String getKey()
        {
            return key;
        }

        @Override
        public void keyReleased(KeyEvent e)
        {
            if (selectedCSVPlacemark != null && selectedCommentsHeader != null)
            {
                selectedCSVPlacemark.updateValue(key, ((JTextComponent) e.getComponent()).getText());
            }
        }
    }

    public PlacemarkOverlayPropertiesPanel()
    {
        initComponents();
        model = (PlacemarkOverlayPropertiesModel) tableKeyValues.getModel();

        // Values are always initially locked.
        model.setEditable(false);

        // Hide lock button's border and do not highlight on mouseover.
        buttonLock.setBorder(null);
        buttonLock.setBorderPainted(false);
        buttonLock.setContentAreaFilled(false);
        buttonLock.setIcon(LOCKED_ICON);

        labelUnlock.setText(PRESS_TO_UNLOCK_MSG);

        panelImage.setBorder(EMPTY_BORDER);
        panelImageContainer.setVisible(false);

        textFieldCoordinates.setHorizontalAlignment(JTextField.CENTER);
    }

    public CSVPlaceMark getSelectedPlacemark()
    {
        return selectedCSVPlacemark;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getPropertyName().equals(IMapObject.NAME))
        {
            selectedCSVLabel.setText(selectedCSVPlacemark.toString());
        }
    }

    @InvokeOnEDT
    public void setSelectedCSVPlacemark(final CSVPlaceMark selectedCSVPlacemark, final boolean editable)
    {
        if (selectedCSVPlacemark == null)
        {
            goToButton.setEnabled(editable);
            buttonLock.setIcon(editable ? UNLOCKED_ICON : LOCKED_ICON);
            labelUnlock.setText(editable ? PRESS_TO_LOCK_MSG : CANNOT_EDIT_MSG);
            labelUnlock.setEnabled(editable);
            tableKeyValues.setEnabled(editable);

            selectedCSVLabel.setIcon(null);
            selectedCSVLabel.setText(null);
            commentsArea.setText(null);
            model.getDataVector().removeAllElements();
            model.setEditable(editable);

            if (this.selectedCSVPlacemark != null)
            {
                this.selectedCSVPlacemark.removePropertyChangeListener(this);
            }
        } else if (this.selectedCSVPlacemark != selectedCSVPlacemark)
        {
            model.getDataVector().removeAllElements();
            model.setEditable(editable);
            tableKeyValues.setEnabled(editable);
            buttonSelect.setEnabled(editable);
            buttonLock.setIcon(editable ? UNLOCKED_ICON : LOCKED_ICON);
            labelUnlock.setText(editable ? PRESS_TO_LOCK_MSG : CANNOT_EDIT_MSG);
            labelUnlock.setEnabled(editable);
            textFieldCoordinates.setEnabled(editable);
            locationModified = false;

            final File imageFile = new File(selectedCSVPlacemark.getAttributes().getImageAddress());
            selectedCSVLabel.setIcon(ImageUtils.getScaledIcon(imageFile, ImageUtils.DEFAULT_ICON_WIDTH, -1, false));
            selectedCSVLabel.setText(selectedCSVPlacemark.toString());
            goToButton.setEnabled(true);

            if (this.selectedCSVPlacemark != null)
            {
                this.selectedCSVPlacemark.removePropertyChangeListener(this);
            }

            this.selectedCSVPlacemark = selectedCSVPlacemark;
            this.selectedCSVPlacemark.addPropertyChangeListener(this);

            Map<String, String> csvMap = selectedCSVPlacemark.getCSVMap();
            selectedCommentsHeader = null;

            List<String> headers = selectedCSVPlacemark.getHeaders();
            String lat = "?";
            String lon = "?";

            for (String header : headers)
            {
                //look for the 'Comments' header - it gets a special editable text area
                if (header.equalsIgnoreCase(CSVPlaceMark.COMMENTS))
                {
                    selectedCommentsHeader = header;
                } else if (header.equals(selectedCSVPlacemark.getLatColumn()))
                {
                    lat = csvMap.get(header);
                } else if (header.equals(selectedCSVPlacemark.getLonColumn()))
                {
                    lon = csvMap.get(header);
                } else if (!header.equals(selectedCSVPlacemark.getImageColumn()))
                {
                    String value = csvMap.get(header);
                    JTextField textField = new JTextField(value);
                    textField.addKeyListener(new KeyReleasedListener(header));

                    //other headers/fields are displayed in an un-editable list.
                    model.addRow(new Object[]{header, textField});
                }
            }

            //did not find a header for comments, so use the default and add it to the headers list
            if (selectedCommentsHeader == null)
            {
                selectedCommentsHeader = CSVPlaceMark.COMMENTS;

                //add the new comments header to the Placemark Overlay header list
                headers.add(selectedCommentsHeader);
            }

            //look up comments for the selected placemark
            String comments = csvMap.get(selectedCommentsHeader);

            textFieldCoordinates.setText(lat + "," + lon);
            textFieldCoordinates.setCaretPosition(0);

            //set the comments editable text area to whatever comments the selected placemark has, even if they don't
            //yet exist (null).
            commentsArea.setText(comments);

            tableKeyValues.clearSelection();

            updateImage(editable);

            validate();
            repaint();
        }

        tableKeyValues.setEnabled(editable);
        tableKeyValues.setEditEnabled(editable);
    }

    private void updateImage(boolean editable)
    {
        ImageIcon icon = null;
        String imageColumn = selectedCSVPlacemark.getImageColumn();
        if (!StringUtils.isEmpty(imageColumn))
        {
            String imagePath = selectedCSVPlacemark.getCSVMap().get(imageColumn);
            if (!StringUtils.isEmpty(imagePath))
            {
                File imageFile = new File(imagePath);
                if (!imageFile.isAbsolute())
                {
                    imageFile = new File(selectedCSVPlacemark.getDirectory(), imagePath);
                }

                BufferedImage image = ImageUtils.getImage(imageFile, false);
                if (image != null)
                {
                    final BufferedImage scaledImage = ImageUtils.getScaledInstanceRespectingAspectRatio(image, 256, 256);
                    icon = new ImageIcon(scaledImage);
                }
            }
        }

        labelImage.setIcon(icon);
        buttonView.setVisible(icon != null);
        panelImageContainer.setVisible(icon != null || editable);
    }

    private void goToButtonActionPerformed()
    {
        if (selectedCSVPlacemark != null)
        {
            final Position position = selectedCSVPlacemark.getPosition();
            double lat = position.getLatitude().getDegrees();
            double lon = position.getLongitude().getDegrees();
            globeManager.getLastFocusedGlobe().goTo(new RaptorLocation(lat, lon));
        }
    }

    /**
     * Whenever the caret moves in the comments text area, update the selected placemark with the current text.
     */
    private void commentsAreaCaretUpdate()
    {
        if (selectedCSVPlacemark == null || selectedCommentsHeader == null)
        {
            return;
        }
        selectedCSVPlacemark.updateValue(selectedCommentsHeader, commentsArea.getText());
    }

    private void labelImageMouseClicked(MouseEvent e)
    {
        if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2)
        {
            openImage();
        }
    }

    private void buttonLockActionPerformed()
    {
        String currentText = labelUnlock.getText();

        if (!currentText.equals(CANNOT_EDIT_MSG))
        {
            if (currentText.equals(PRESS_TO_UNLOCK_MSG))
            {
                buttonLock.setIcon(UNLOCKED_ICON);
                model.setEditable(true);
                tableKeyValues.setEnabled(true);
                tableKeyValues.setEditEnabled(true);
                labelUnlock.setText(PRESS_TO_LOCK_MSG);
                textFieldCoordinates.setEnabled(true);
                buttonSelect.setEnabled(true);
                buttonSet.setEnabled(locationModified);
                repaint();
            } else
            {
                buttonLock.setIcon(LOCKED_ICON);
                buttonLock.requestFocus();
                model.setEditable(false);
                tableKeyValues.setEnabled(false);
                tableKeyValues.setEditEnabled(false);
                labelUnlock.setText(PRESS_TO_UNLOCK_MSG);
                textFieldCoordinates.setEnabled(false);
                buttonSelect.setEnabled(false);
                buttonSet.setEnabled(false);
                repaint();
            }
        }
    }

    private void buttonSetActionPerformed()
    {
        ILocation location = CoordinateSystem.computeLatLonFromString(textFieldCoordinates.getText());
        selectedCSVPlacemark.updateValue(selectedCSVPlacemark.getLatColumn(), String.valueOf(location.getLatitude()));
        selectedCSVPlacemark.updateValue(selectedCSVPlacemark.getLonColumn(), String.valueOf(location.getLongitude()));
        locationModified = false;
        buttonSet.setEnabled(false);
    }

    private void textFieldCoordinatesKeyReleased()
    {
        checkLocation();
    }

    private void checkLocation()
    {
        ILocation location = CoordinateSystem.computeLatLonFromString(textFieldCoordinates.getText());
        buttonSet.setEnabled(location != null);
        locationModified = location != null;
    }

    private void openImage()
    {
        String imagePath = selectedCSVPlacemark.getCSVMap().get(selectedCSVPlacemark.getImageColumn());
        File imageFile = new File(imagePath);

        // Check for relative path.
        if (!imageFile.isAbsolute())
        {
            imageFile = new File(selectedCSVPlacemark.getDirectory(), imagePath);
        }

        FileUtils.launchFile(imageFile);
    }

    private void labelImageMouseEntered()
    {
        panelImage.setBorder(HOVER_BORDER);
        repaint();
    }

    private void labelImageMouseExited()
    {
        panelImage.setBorder(EMPTY_BORDER);
        repaint();
    }

    private void buttonViewActionPerformed()
    {
        openImage();
    }

    private void buttonSelectActionPerformed()
    {
        AlwaysOnTopFileChooser fileChooser = AlwaysOnTopFileChooserFactory.getFileChooser(this);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        if (fileChooser.showOpenDialog(JFrame.getFrames()[0]) == JFileChooser.APPROVE_OPTION)
        {
            File f = fileChooser.getSelectedFile();

            if (PlacemarkOverlayUtils.checkImage(f))
            {
                String imageColumn = selectedCSVPlacemark.getImageColumn();

                try
                {
                    if (imageColumn == null || imageColumn.isEmpty())
                    {
                        imageColumn = selectedCSVPlacemark.addImageColumn();
                    }

                    selectedCSVPlacemark.getCSVMap().put(imageColumn, f.getPath());
                    updateImage(true);
                } catch (IOException ioe)
                {
                    logger.error("Error attempting to add image to placemark.", ioe);
                }
            }
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        detailsPanel = new JPanel();
        panel3 = new JPanel();
        selectedCSVLabel = new JLabel();
        separator1 = new JSeparator();
        panelCoordinates = new JPanel();
        labelCoordinates = new JLabel();
        textFieldCoordinates = new JTextField();
        buttonSet = new JButton();
        panelKeyValues = new JPanel();
        scrollPane1 = new JScrollPane();
        tableKeyValues = new KeyValueTable();
        panelImageContainer = new JPanel();
        panelImage = new JPanel();
        labelImage = new JLabel();
        panelImageButtons = new JPanel();
        labelImageName = new JLabel();
        buttonView = new JButton();
        buttonSelect = new JButton();
        commentsPanel = new JPanel();
        scrollPane2 = new JScrollPane();
        commentsArea = new JTextArea();
        panelLockControl = new JPanel();
        buttonLock = new JButton();
        labelUnlock = new JLabel();
        goToButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "top:default:grow"));

        //======== detailsPanel ========
        {
            detailsPanel.setName("detailsPanel");
            detailsPanel.setLayout(new FormLayout(
                    "default:grow",
                    "3*(default, $lgap), fill:default:grow, 3*($lgap, default)"));

            //======== panel3 ========
            {
                panel3.setName("panel3");
                panel3.setLayout(new FormLayout(
                        "default, center:default:grow, $lcgap, default",
                        "fill:36px"));

                //---- selectedCSVLabel ----
                selectedCSVLabel.setIconTextGap(8);
                selectedCSVLabel.putClientProperty("FlatLaf.styleClass", "large");
                selectedCSVLabel.setName("selectedCSVLabel");
                panel3.add(selectedCSVLabel, cc.xy(2, 1));
            }
            detailsPanel.add(panel3, cc.xy(1, 1, CellConstraints.LEFT, CellConstraints.CENTER));

            //---- separator1 ----
            separator1.setName("separator1");
            detailsPanel.add(separator1, cc.xy(1, 3));

            //======== panelCoordinates ========
            {
                panelCoordinates.setName("panelCoordinates");
                panelCoordinates.setLayout(new FormLayout(
                        "default, $lcgap, default:grow, $lcgap, default",
                        "default"));

                //---- labelCoordinates ----
                labelCoordinates.setText("Coordinates:");
                labelCoordinates.setName("labelCoordinates");
                panelCoordinates.add(labelCoordinates, cc.xy(1, 1));

                //---- textFieldCoordinates ----
                textFieldCoordinates.setName("textFieldCoordinates");
                textFieldCoordinates.addKeyListener(new KeyAdapter()
                {
                    @Override
                    public void keyReleased(KeyEvent e)
                    {
                        textFieldCoordinatesKeyReleased();
                    }
                });
                panelCoordinates.add(textFieldCoordinates, cc.xy(3, 1));

                //---- buttonSet ----
                buttonSet.setText("Set");
                buttonSet.setEnabled(false);
                buttonSet.setName("buttonSet");
                buttonSet.addActionListener(e -> buttonSetActionPerformed());
                panelCoordinates.add(buttonSet, cc.xy(5, 1, CellConstraints.DEFAULT, CellConstraints.FILL));
            }
            detailsPanel.add(panelCoordinates, cc.xy(1, 5));

            //======== panelKeyValues ========
            {
                panelKeyValues.setName("panelKeyValues");
                panelKeyValues.setLayout(new FormLayout(
                        "default:grow",
                        "fill:default:grow"));

                //======== scrollPane1 ========
                {
                    scrollPane1.setName("scrollPane1");

                    //---- tableKeyValues ----
                    tableKeyValues.setPreferredScrollableViewportSize(new Dimension(100, 200));
                    tableKeyValues.setFillsViewportHeight(true);
                    tableKeyValues.setShowVerticalLines(false);
                    tableKeyValues.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                    tableKeyValues.setShowHorizontalLines(false);
                    tableKeyValues.setName("tableKeyValues");
                    scrollPane1.setViewportView(tableKeyValues);
                }
                panelKeyValues.add(scrollPane1, cc.xy(1, 1));
            }
            detailsPanel.add(panelKeyValues, cc.xy(1, 7));

            //======== panelImageContainer ========
            {
                panelImageContainer.setBorder(new TitledBorder("Image"));
                panelImageContainer.setName("panelImageContainer");
                panelImageContainer.setLayout(new FormLayout(
                        "center:default:grow",
                        "default, $nlgap, default"));

                //======== panelImage ========
                {
                    panelImage.setName("panelImage");
                    panelImage.setLayout(new FormLayout(
                            "center:default",
                            "default"));

                    //---- labelImage ----
                    labelImage.setHorizontalAlignment(SwingConstants.CENTER);
                    labelImage.setName("labelImage");
                    labelImage.addMouseListener(new MouseAdapter()
                    {
                        @Override
                        public void mouseClicked(MouseEvent e)
                        {
                            labelImageMouseClicked(e);
                        }

                        @Override
                        public void mouseEntered(MouseEvent e)
                        {
                            labelImageMouseEntered();
                        }

                        @Override
                        public void mouseExited(MouseEvent e)
                        {
                            labelImageMouseExited();
                        }
                    });
                    panelImage.add(labelImage, cc.xy(1, 1));
                }
                panelImageContainer.add(panelImage, cc.xy(1, 1));

                //======== panelImageButtons ========
                {
                    panelImageButtons.setName("panelImageButtons");
                    panelImageButtons.setLayout(new FormLayout(
                            "default, $lcgap, default:grow, 2*($lcgap, default)",
                            "default"));

                    //---- labelImageName ----
                    labelImageName.setName("labelImageName");
                    panelImageButtons.add(labelImageName, cc.xy(1, 1, CellConstraints.DEFAULT, CellConstraints.FILL));

                    //---- buttonView ----
                    buttonView.setText("View");
                    buttonView.setName("buttonView");
                    buttonView.addActionListener(e -> buttonViewActionPerformed());
                    panelImageButtons.add(buttonView, cc.xy(5, 1));

                    //---- buttonSelect ----
                    buttonSelect.setText("Select");
                    buttonSelect.setName("buttonSelect");
                    buttonSelect.addActionListener(e -> buttonSelectActionPerformed());
                    panelImageButtons.add(buttonSelect, cc.xy(7, 1));
                }
                panelImageContainer.add(panelImageButtons, cc.xy(1, 3, CellConstraints.FILL, CellConstraints.DEFAULT));
            }
            detailsPanel.add(panelImageContainer, cc.xy(1, 9));

            //======== commentsPanel ========
            {
                commentsPanel.setBorder(new TitledBorder("Comments"));
                commentsPanel.setName("commentsPanel");
                commentsPanel.setLayout(new FormLayout(
                        "default:grow",
                        "default"));

                //======== scrollPane2 ========
                {
                    scrollPane2.setName("scrollPane2");

                    //---- commentsArea ----
                    commentsArea.setName("commentsArea");
                    commentsArea.addCaretListener(e -> commentsAreaCaretUpdate());
                    scrollPane2.setViewportView(commentsArea);
                }
                commentsPanel.add(scrollPane2, cc.xy(1, 1, CellConstraints.FILL, CellConstraints.FILL));
            }
            detailsPanel.add(commentsPanel, cc.xy(1, 11));

            //======== panelLockControl ========
            {
                panelLockControl.setName("panelLockControl");
                panelLockControl.setLayout(new FormLayout(
                        "default, $lcgap, default:grow, $lcgap, default",
                        "default"));

                //---- buttonLock ----
                buttonLock.setName("buttonLock");
                buttonLock.addActionListener(e -> buttonLockActionPerformed());
                panelLockControl.add(buttonLock, cc.xy(1, 1));

                //---- labelUnlock ----
                labelUnlock.setText("Unlock to Edit");
                labelUnlock.setName("labelUnlock");
                panelLockControl.add(labelUnlock, cc.xy(3, 1));

                //---- goToButton ----
                goToButton.setText("Go To");
                goToButton.setEnabled(false);
                goToButton.setName("goToButton");
                goToButton.addActionListener(e -> goToButtonActionPerformed());
                panelLockControl.add(goToButton, cc.xy(5, 1, CellConstraints.DEFAULT, CellConstraints.FILL));
            }
            detailsPanel.add(panelLockControl, cc.xy(1, 13));
        }
        add(detailsPanel, cc.xy(1, 1, CellConstraints.FILL, CellConstraints.FILL));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel detailsPanel;
    private JPanel panel3;
    private JLabel selectedCSVLabel;
    private JSeparator separator1;
    private JPanel panelCoordinates;
    private JLabel labelCoordinates;
    private JTextField textFieldCoordinates;
    private JButton buttonSet;
    private JPanel panelKeyValues;
    private JScrollPane scrollPane1;
    private KeyValueTable tableKeyValues;
    private JPanel panelImageContainer;
    private JPanel panelImage;
    private JLabel labelImage;
    private JPanel panelImageButtons;
    private JLabel labelImageName;
    private JButton buttonView;
    private JButton buttonSelect;
    private JPanel commentsPanel;
    private JScrollPane scrollPane2;
    private JTextArea commentsArea;
    private JPanel panelLockControl;
    private JButton buttonLock;
    private JLabel labelUnlock;
    private JButton goToButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
