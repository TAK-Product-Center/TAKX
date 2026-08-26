package gov.takx.core.view.gui;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.ui.AStandardDialog;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import java.awt.BorderLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Dialog used to collect a location string from the user.  It must be parsable according to one of the coordinate
 * systems supported.
 * <p/>
 * Note, this code was copied and tweaked from GoToDialog.  It would be better to construct a more flexible dialog that
 * could be used for both operations.
 * TODO KMB: Combine this dialog with GoToDialog
 *
 * @author Proprietary information subject to the terms of a Non-Disclosure Agreement@since 1.16
 * @since 1.16
 */
public class EnterLocationDialog extends AStandardDialog
{
    private static final String LOCATION_START_TEXT = "Type Location Here";
    private ILocation location;
    private boolean canceled = false;

    /**
     * Construct the dialog.  This will make the dialog viewable.  No initial location string is set.
     */
    public EnterLocationDialog()
    {
        initComponents();

        setModal(true);

        String description = "<html>Enter the coordinates that you would like to set on this end-point of the line segment.</html>";

        dialogHeaderPanel.setDescription(description);

        final String coordinateSystemPref = UserPreferenceEntry.COORDINATE_SYSTEM.getStringValue();

        // Add all of our coordinate systems and select the current one
        var model = (DefaultComboBoxModel<CoordinateSystem>) coordinateComboBox.getModel();
        for (CoordinateSystem coordinateSystem : CoordinateSystem.values())
        {
            model.addElement(coordinateSystem);

            if (coordinateSystem.getName().equals(coordinateSystemPref))
            {
                model.setSelectedItem(coordinateSystem);
            }
        }

        reset();
    }

    /**
     * Set the description text at the top of the dialog.
     *
     * @param description text to show at top of dialog
     */
    public void setDescription(String description)
    {
        dialogHeaderPanel.setDescription(description);
    }

    public void setLocation(ILocation location)
    {
        // Format the location using the currently selected coordinate system
        var coordinateSystem = (CoordinateSystem) coordinateComboBox.getModel().getSelectedItem();

        locationTextField.setText(CoordinateSystem.getLocationString(location, coordinateSystem));
        okButton.setEnabled(true); // We have a location now
    }

    /**
     * Reset all of our fields to default values.
     */
    public void reset()
    {
        locationTextField.setText(LOCATION_START_TEXT);
        locationTextField.requestFocusInWindow();
        locationTextField.setSelectionStart(0);
        locationTextField.setSelectionEnd(LOCATION_START_TEXT.length());
        okButton.setEnabled(false);
    }

    /**
     * @return {@code true} if the user canceled the dialog, {@code false} otherwise
     */
    public boolean wasCanceled()
    {
        return canceled;
    }

    /**
     * Hide dialog on close.
     */
    private void cancelButtonActionPerformed()
    {
        canceled = true;
        dispose();
    }

    /**
     * User pressed ok, validate that the coordinates are valid and close if so.  The location value can be obtained
     * using {@link #getEnteredLocation()}.
     *
     * @see #getEnteredLocation()
     */
    private void okButtonActionPerformed()
    {
        if (validateLocationString())
        {
            dispose();
        } else
        {
            JOptionPane.showMessageDialog(this, "Invalid Coordinates", "Invalid Coordinates", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Get the location value specified by the user.  This is the actual parsed location, not the string value.
     *
     * @return Parsed location, may be {@code null} if the text entered could not be parsed
     */
    public ILocation getEnteredLocation()
    {
        return location;
    }

    /**
     * Callback for when the dialog was closed by the X button.
     * @since 3.3
     */
    @Override
    protected void onClosedByXButton()
    {
        cancelButtonActionPerformed();
    }

    /**
     * Hitting the escape button with this dialog in focus is effectively hitting the cancel button.
     */
    @Override
    protected void onEscape()
    {
        cancelButtonActionPerformed();
    }

    /**
     * Validate the location string entered and place the parsed location into {@link #location}. Return {@code true}
     * if the location parsed properly.
     *
     * @return {@code true} if the location is valid
     */
    private boolean validateLocationString()
    {
        location = CoordinateSystem.computeLatLonFromString(locationTextField.getText());
        return location != null;
    }

    /**
     * Disable or enable the goto location button, depending on whether or not we have a valid location entered.
     *
     * @param e KeyEvent
     */
    private void locationTextFieldKeyReleased(KeyEvent e)
    {
        boolean valid = validateLocationString();

        okButton.setEnabled(valid); // Enable the ok button if the location string is valid

        //Hitting enter while in the location text field when a valid location is entered, is the same as clicking the goto button.
        if (e.getKeyCode() == KeyEvent.VK_ENTER && okButton.isEnabled())
        {
            okButton.doClick();
        }
    }

    /**
     * Display the proper coordinate example string.
     */
    private void coordinateComboBoxItemStateChanged()
    {
        if (coordinateComboBox.getSelectedIndex() != -1)
        {
            CoordinateSystem cd = (CoordinateSystem) coordinateComboBox.getSelectedItem();
            exampleLabel.setText("E.g. " + CoordinateSystem.getExampleString(cd));
        }
    }

    /**
     * Remove the default text when the user clicks in the location field.
     */
    private void locationTextFieldMousePressed()
    {
        if (locationTextField.getText().equalsIgnoreCase(LOCATION_START_TEXT))
        {
            locationTextField.setText("");
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        dialogHeaderPanel = new DialogHeaderPanel();
        var contentPanel = new JPanel();
        coordinateComboBox = new JComboBox<>();
        exampleLabel = new JLabel();
        locationTextField = new JTextField();
        var buttonBar = new JPanel();
        okButton = new JButton();
        var cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Enter Location");
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
                        "default, $lcgap, default:grow",
                        "default, $lgap, default"));

                //---- coordinateComboBox ----
                coordinateComboBox.setName("coordinateComboBox");
                coordinateComboBox.addItemListener(e -> coordinateComboBoxItemStateChanged());
                contentPanel.add(coordinateComboBox, cc.xy(1, 1));

                //---- exampleLabel ----
                exampleLabel.setEnabled(false);
                exampleLabel.setName("exampleLabel");
                contentPanel.add(exampleLabel, cc.xy(3, 1));

                //---- locationTextField ----
                locationTextField.setText("Type Location Here");
                locationTextField.setSelectionEnd(17);
                locationTextField.setName("locationTextField");
                locationTextField.addKeyListener(new KeyAdapter()
                {
                    @Override
                    public void keyReleased(KeyEvent e)
                    {
                        locationTextFieldKeyReleased(e);
                    }
                });
                locationTextField.addMouseListener(new MouseAdapter()
                {
                    @Override
                    public void mousePressed(MouseEvent e)
                    {
                        locationTextFieldMousePressed();
                    }
                });
                contentPanel.add(locationTextField, cc.xywh(1, 3, 3, 1));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "default, $lcgap, default:grow, $lcgap, default, $rgap, default",
                        "pref"));

                //---- okButton ----
                okButton.setText("OK");
                okButton.setEnabled(false);
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
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private DialogHeaderPanel dialogHeaderPanel;
    private JComboBox<CoordinateSystem> coordinateComboBox;
    private JLabel exampleLabel;
    private JTextField locationTextField;
    private JButton okButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
