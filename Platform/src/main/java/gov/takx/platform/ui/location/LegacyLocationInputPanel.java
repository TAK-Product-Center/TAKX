package gov.takx.platform.ui.location;

import com.formdev.flatlaf.FlatClientProperties;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.ui.location.ILocationInputListener;
import gov.takx.api.ui.location.ILocationInputPanel;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.swing.PlaceholderTextField;
import gov.takx.commons.utils.StringUtils;
import jakarta.annotation.Nonnull;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;

import java.awt.Color;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The legacy user interface for providing a location.

 * <p/>
 * Plugins that wish to use this panel should create an instance of the panel, register a listener to be notified when a
 * location is selected, and add it to the appropriate parent panel/dialog. Here is an example:
 * <pre>{@code
 * final LegacyLocationInputPanel locationInputPanel = new LegacyLocationInputPanel();
 * locationInputPanel.addListener(new ILocationInputListener()
 * {
 *     @Override
 *     public void onLocationSelected(@Nonnull ILocation location, @Nonnull CoordinateSystem coordinateSystem,
 *                                    @Nullable URI markerIconUri, @Nullable String cotType)
 *     {
 *         // Handle selected location...
 *     }
 *
 *     @Override
 *     public void onCancel()
 *     {
 *         // Handle cancelled location selection...
 *     }
 * });
 *
 * myDialog.add(locationInputPanel, BorderLayout.CENTER);
 * }</pre>
 *
 * @since 3.0 (pulled out of GoToLocationDialog)
 */
public class LegacyLocationInputPanel extends JPanel implements ILocationInputPanel
{
    private static final String HELP_TEXT =
            "<html>" +
                    "Enter the coordinates that you would like to go to. Location markers can be found<br>" +
                    "in the Overlays panel once they have been dropped on the globe. You can right click<br>" +
                    "a location marker in the Overlays panel and click Go To to re-visit a marker location." +
                    CoordinateSystem.LOCATION_STRING_FORMAT_TABLE +
                    "</html>";
    private static final String DESCRIPTION =
            "<html>Location may be formatted as decimal degrees, DM, DMS, or MGRS.</html>";
    private static final String LOCATION_PLACEHOLDER_TEXT = "Enter Location Here";
    private static final String INVALID_LOCATION_ERROR = "<html>Invalid value. See info for accepted value formats.</html>";
    private static final String INVALID_MGRS_LOCATION_ERROR =
            "<html>" +
                    "Invalid value. Expected format for MGRS: ##XXX ##### #####<br>" +
                    "Examples: 09QUB 5969 93501 or 09QUB 56 94 (number groups must be equal length)" +
                    "</html>";
    private static final String INVALID_DEGREES_LOCATION_ERROR =
            "<html>" +
                    "Invalid value. Expected format for Decimal Degrees: #.###, #.###<br>" +
                    "Examples: 33.276, -85.73 or 33, -85" +
                    "</html>";
    private static final String INVALID_DMS_LOCATION_ERROR =
            "<html>" +
                    "Invalid value. Expected format for DMS: ##&deg; ##&rsquo; ##&quot;, ##&deg; ##&rsquo; ##&quot;<br>" +
                    "Examples: 32&deg;42&rsquo;9.2&quot;, -114&deg;54&rsquo;30.8&quot;" +
                    "</html>";

    private final List<ILocationInputListener> listeners = new CopyOnWriteArrayList<>();
    private ILocation location;
    private CoordinateSystem locationType;

    public LegacyLocationInputPanel()
    {
        initComponents();

        locationTextField.setPlaceholderText(LOCATION_PLACEHOLDER_TEXT);
        locationTextField.requestFocusInWindow();
        intro.setText(DESCRIPTION);
        helpButton.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_HELP);
        ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);

        // Give focus to the input field to save the user having to click in it
        SwingUtilities.invokeLater(() -> locationTextField.requestFocus());
    }

    @Override
    public void addListener(@Nonnull ILocationInputListener listener)
    {
        listeners.add(listener);
    }

    @Override
    public void removeListener(@Nonnull ILocationInputListener listener)
    {
        listeners.remove(listener);
    }

    @Override
    public void setLocation(@Nonnull ILocation location, CoordinateSystem coordinateSystem)
    {
        locationTextField.setText(CoordinateSystem.getLocationString(location, coordinateSystem));
    }

    @Override
    public void setShowMarkerIcons(boolean showMarkerIcons)
    {
        markerIconPanel.setVisible(showMarkerIcons);
    }

    /**
     * Disable or enable the goto location button, depending on whether we have a valid location entered.
     */
    private void locationTextFieldKeyReleased(KeyEvent e)
    {
        String locationString = locationTextField.getText();
        location = CoordinateSystem.computeLatLonFromString(locationString);

        final boolean isLocationValid = location != null;
        locationType = isLocationValid ? CoordinateSystem.getLocationTypeFromString(locationString) : null;
        okButton.setEnabled(locationType != null);
        updateParsedLocationLabel();

        // Hitting enter while in the location text field when a valid location is entered should commit the location
        if (e.getKeyCode() == KeyEvent.VK_ENTER && isLocationValid && locationType != null)
        {
            okButtonActionPerformed();
        }
    }

    /**
     * Updates the informational text that reacts to the user's input
     * <p>
     * If the location text is invalid, provide an error message in the dialog box. If it is valid, show the type
     * of location received and display the value. If there is no input text, set the informational text to also be blank.
     */
    private void updateParsedLocationLabel()
    {
        String currLocationText = locationTextField.getText();
        if (StringUtils.isEmpty(currLocationText))
        {
            parsedLocationLabel.setText(" ");
        } else if (locationType == null)
        {
            CoordinateSystem cs = CoordinateSystem.guessCoordinateSystemFromInvalidString(currLocationText);
            String invalidText = INVALID_LOCATION_ERROR;
            if (cs != null)
            {
                invalidText = switch (cs)
                {
                    case MGRS -> INVALID_MGRS_LOCATION_ERROR;
                    case DECIMAL_DEGREES -> INVALID_DEGREES_LOCATION_ERROR; // CoordinateSystem never guesses DM currently since the regex for DMS would match DM
                    case DM_DEGREES, DMS_DEGREES -> INVALID_DMS_LOCATION_ERROR;
                };
            }
            parsedLocationLabel.setText(invalidText);
            parsedLocationLabel.setForeground(Color.RED);
        } else
        {
            parsedLocationLabel.setText(locationType + " : " + CoordinateSystem.getLocationString(location, locationType));
            parsedLocationLabel.setForeground(null);
        }
        revalidate();
    }

    private void okButtonActionPerformed()
    {
        location = CoordinateSystem.computeLatLonFromString(locationTextField.getText());
        if (location == null)
        {
            JOptionPane.showMessageDialog(this, "Invalid Coordinates", "Invalid Coordinates", JOptionPane.ERROR_MESSAGE);
        } else
        {
            final URI markerUri = markerIconPanel.getSelectedMarkerUri();
            final String cotType = markerIconPanel.getSelectedCotType();
            listeners.forEach(listener -> listener.onLocationSelected(location, locationType, markerUri, cotType));
        }
    }

    private void cancelButtonActionPerformed()
    {
        listeners.forEach(ILocationInputListener::onCancel);
    }

    private void helpButtonActionPerformed()
    {
        AlwaysOnTopJOptionPane.showMessageDialog(HELP_TEXT, "Go To Location Help", JOptionPane.INFORMATION_MESSAGE);
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        intro = new JLabel();
        helpButton = new JButton();
        locationTextField = new PlaceholderTextField();
        parsedLocationLabel = new JLabel();
        var buttonPanel = new JPanel();
        markerIconPanel = new MarkerIconPanel();
        okButton = new JButton();
        var cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow, $lcgap, default",
                "3*(default, $lgap), default"));

        //---- intro ----
        intro.setText("Enter the intro text from the DESCRIPTION variable at the constructor");
        intro.setName("intro");
        add(intro, cc.xy(1, 1));

        //---- helpButton ----
        helpButton.setName("helpButton");
        helpButton.addActionListener(e -> helpButtonActionPerformed());
        add(helpButton, cc.xy(3, 1));

        //---- locationTextField ----
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
        add(locationTextField, cc.xywh(1, 3, 3, 1));

        //---- parsedLocationLabel ----
        parsedLocationLabel.setText(" ");
        parsedLocationLabel.setName("parsedLocationLabel");
        add(parsedLocationLabel, cc.xywh(1, 5, 3, 1));

        //======== buttonPanel ========
        {
            buttonPanel.setName("buttonPanel");
            buttonPanel.setLayout(new FormLayout(
                    "default:grow, 2*($lcgap, $button)",
                    "default"));

            //---- markerIconPanel ----
            markerIconPanel.setVisible(false);
            markerIconPanel.setName("markerIconPanel");
            buttonPanel.add(markerIconPanel, cc.xy(1, 1));

            //---- okButton ----
            okButton.setText("OK");
            okButton.setEnabled(false);
            okButton.setName("okButton");
            okButton.addActionListener(e -> okButtonActionPerformed());
            buttonPanel.add(okButton, cc.xy(3, 1));

            //---- cancelButton ----
            cancelButton.setText("Cancel");
            cancelButton.setName("cancelButton");
            cancelButton.addActionListener(e -> cancelButtonActionPerformed());
            buttonPanel.add(cancelButton, cc.xy(5, 1));
        }
        add(buttonPanel, cc.xywh(1, 7, 3, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JLabel intro;
    private JButton helpButton;
    private PlaceholderTextField locationTextField;
    private JLabel parsedLocationLabel;
    private MarkerIconPanel markerIconPanel;
    private JButton okButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
