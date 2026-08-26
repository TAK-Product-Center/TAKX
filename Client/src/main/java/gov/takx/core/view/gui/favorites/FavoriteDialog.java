package gov.takx.core.view.gui.favorites;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.globe.IFavorite;
import gov.takx.api.globe.IFavoritesManager;
import gov.takx.api.globe.IProjectFavoritesManager;
import gov.takx.api.globe.IUserFavoritesManager;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.core.view.gui.DialogHeaderPanel;
import gov.takx.core.view.gui.GUIUtils;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.ui.AStandardDialog;
import gov.takx.platform.ui.UIResources;
import gov.takx.platform.utils.units.AltitudeUnits;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import java.awt.BorderLayout;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import static gov.takx.api.coordinatesystem.CoordinateSystem.getLocationString;
import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public abstract class FavoriteDialog extends AStandardDialog
{
    @Inject
    protected IExtendedGlobeManager globeManager;

    @Inject
    protected IProjectFavoritesManager projectViewBookmarkManager;

    @Inject
    protected IUserFavoritesManager userViewBookmarkManager;

    private final DecimalFormat format;

    /**
     * Constructor.
     *
     * @param userFavorite True if this dialog is for a user favorite; false for project favorite
     * @param iconFileName The name of the icon file within {@link gov.takx.platform.ui.ResourceDirectory#PLATFORM_ICONS}
     * @param description  The description of the dialog
     */
    protected FavoriteDialog(boolean userFavorite, String iconFileName, String description)
    {
        this(null, userFavorite, iconFileName, description);
    }

    /**
     * Constructor.
     *
     * @param favorite     The favorite to be displayed in the dialog (may be {@code null} for a new favorite)
     * @param userFavorite True if this dialog is for a user favorite; false for project favorite
     * @param iconFileName The name of the icon file within {@link gov.takx.platform.ui.ResourceDirectory#PLATFORM_ICONS}
     * @param description  The description of the dialog
     */
    protected FavoriteDialog(@Nullable IFavorite favorite, boolean userFavorite, String iconFileName, String description)
    {
        initComponents();
        setModal(true);
        getRootPane().setDefaultButton(okButton);

        GUIUtils.addSingleActionDocumentListener(nameTextField, e -> documentPerformed());

        // Prepare other non-location fields.
        dialogHeaderPanel.setIcon(iconFileName);
        dialogHeaderPanel.setDescription(description);
        userRadioButton.setSelected(userFavorite);
        nameTextField.setText(favorite != null ? favorite.getDescription() : "");
        format = (DecimalFormat) NumberFormat.getInstance();
        format.setMaximumFractionDigits(2);

        // Create a smaller version of the user and project icon for this dialog...
        projectLabel.setIcon(UIResources.getStandardSizedIcon(PLATFORM_ICONS, "folder_outline.png"));
        userLabel.setIcon(UIResources.getStandardSizedIcon(PLATFORM_ICONS, "user.png"));
    }

    protected abstract boolean saveFavorite();

    protected boolean userSelected()
    {
        return userRadioButton.isSelected();
    }

    protected String getFavoriteName()
    {
        return nameTextField.getText();
    }

    public void okButtonActionPerformed()
    {
        if (getFavoriteName().isBlank())
        {
            AlwaysOnTopJOptionPane.showErrorDialog("Please provide a Name", "Error");
            nameTextField.requestFocus();
        } else
        {
            if (saveFavorite()) dispose();
        }
    }

    protected void updateCurrentView(double latitude, double longitude, double elevation, double heading, double pitch, double zoom)
    {
        final var elevationString = UserPreferenceEntry.DISPLAY_ALTITUDE_UNITS.getValueAs(AltitudeUnits.class).format(elevation, 1);

        labelLocation.setText(getLocationString(latitude, longitude, UserPreferenceEntry.COORDINATE_SYSTEM.getValueAs(CoordinateSystem.class)));
        labelElevation.setText(elevationString);

        labelHeading.setText(format.format(heading) + '\u00B0');
        labelTilt.setText(format.format(pitch) + '\u00B0');
        labelZoom.setText(format.format(zoom));
    }

    protected void cancelButtonActionPerformed()
    {
        dispose();
    }

    /**
     * Check the given description against all descriptions used in the given manager.  If a duplicate is found,
     * report it to the user and return {@code false}, otherwise return {@code true}.
     *
     * @param description Description to test against all existing favorites
     * @param manager Manager to use to obtain existing favorites
     * @return {@code true} if the description is ok to use, {@code false} otherwise
     * @since 5.7
     */
    protected boolean checkAndReportDuplicateDescription(@Nonnull String description, @Nonnull IFavoritesManager manager)
    {
        final boolean nameInUse = manager.getBookmarks().stream()
                .anyMatch(favorite -> description.equals(favorite.getDescription()));

        if (nameInUse)
        {
            AlwaysOnTopJOptionPane.showErrorDialog(this, String.format("A favorite with the name %s already exists ", description), "Duplicate Favorite");
            return false;
        }
        return true;
    }

    private void documentPerformed()
    {
        okButton.setEnabled(!nameTextField.getText().isEmpty());
    }

    @SuppressWarnings("DuplicatedCode")
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        dialogHeaderPanel = new DialogHeaderPanel();
        var contentPanel = new JPanel();
        var label1 = new JLabel();
        var favoriteTypePanel = new JPanel();
        var projectRadioButton = new JRadioButton();
        projectLabel = new JLabel();
        userRadioButton = new JRadioButton();
        userLabel = new JLabel();
        var nameLabel = new JLabel();
        nameTextField = new JTextField();
        var locationLabel = new JLabel();
        labelLocation = new JLabel();
        var elevationLabel = new JLabel();
        labelElevation = new JLabel();
        var headingLabel = new JLabel();
        labelHeading = new JLabel();
        var pitchLabel = new JLabel();
        labelTilt = new JLabel();
        var zoomLabel = new JLabel();
        labelZoom = new JLabel();
        var buttonBar = new JPanel();
        okButton = new JButton();
        var cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Add a Favorite");
        setModal(true);
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
                        "default, $lcgap, default:grow, $lcgap, default, $lcgap, default:grow",
                        "4*(default, $lgap), default"));

                //---- label1 ----
                label1.setText("Type:");
                label1.setName("label1");
                contentPanel.add(label1, cc.xy(1, 1));

                //======== favoriteTypePanel ========
                {
                    favoriteTypePanel.setName("favoriteTypePanel");
                    favoriteTypePanel.setLayout(new FormLayout(
                            "default, $lcgap, default, $ugap, default, $lcgap, default",
                            "default"));

                    //---- projectRadioButton ----
                    projectRadioButton.setSelected(true);
                    projectRadioButton.setName("projectRadioButton");
                    favoriteTypePanel.add(projectRadioButton, cc.xy(1, 1));

                    //---- projectLabel ----
                    projectLabel.setText("Project");
                    projectLabel.setName("projectLabel");
                    favoriteTypePanel.add(projectLabel, cc.xy(3, 1));

                    //---- userRadioButton ----
                    userRadioButton.setName("userRadioButton");
                    favoriteTypePanel.add(userRadioButton, cc.xy(5, 1));

                    //---- userLabel ----
                    userLabel.setText("User");
                    userLabel.setName("userLabel");
                    favoriteTypePanel.add(userLabel, cc.xy(7, 1));
                }
                contentPanel.add(favoriteTypePanel, cc.xywh(3, 1, 5, 1));

                //---- nameLabel ----
                nameLabel.setText("Name:");
                nameLabel.setName("nameLabel");
                contentPanel.add(nameLabel, cc.xy(1, 3));

                //---- nameTextField ----
                nameTextField.setName("nameTextField");
                contentPanel.add(nameTextField, cc.xywh(3, 3, 5, 1));

                //---- locationLabel ----
                locationLabel.setText("Location:");
                locationLabel.setName("locationLabel");
                contentPanel.add(locationLabel, cc.xy(1, 5));

                //---- labelLocation ----
                labelLocation.setText("text");
                labelLocation.setName("labelLocation");
                contentPanel.add(labelLocation, cc.xywh(3, 5, 5, 1, CellConstraints.FILL, CellConstraints.FILL));

                //---- elevationLabel ----
                elevationLabel.setText("Elevation:");
                elevationLabel.setName("elevationLabel");
                contentPanel.add(elevationLabel, cc.xy(1, 7));

                //---- labelElevation ----
                labelElevation.setText("text");
                labelElevation.setName("labelElevation");
                contentPanel.add(labelElevation, cc.xy(3, 7, CellConstraints.FILL, CellConstraints.FILL));

                //---- headingLabel ----
                headingLabel.setText("Heading:");
                headingLabel.setName("headingLabel");
                contentPanel.add(headingLabel, cc.xy(5, 7));

                //---- labelHeading ----
                labelHeading.setText("text");
                labelHeading.setName("labelHeading");
                contentPanel.add(labelHeading, cc.xy(7, 7, CellConstraints.FILL, CellConstraints.FILL));

                //---- pitchLabel ----
                pitchLabel.setText("Tilt:");
                pitchLabel.setName("pitchLabel");
                contentPanel.add(pitchLabel, cc.xy(1, 9));

                //---- labelTilt ----
                labelTilt.setText("text");
                labelTilt.setName("labelTilt");
                contentPanel.add(labelTilt, cc.xy(3, 9, CellConstraints.FILL, CellConstraints.FILL));

                //---- zoomLabel ----
                zoomLabel.setText("Zoom:");
                zoomLabel.setName("zoomLabel");
                contentPanel.add(zoomLabel, cc.xy(5, 9));

                //---- labelZoom ----
                labelZoom.setText("text");
                labelZoom.setName("labelZoom");
                contentPanel.add(labelZoom, cc.xy(7, 9, CellConstraints.FILL, CellConstraints.FILL));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "$glue, $button, $rgap, $button",
                        "default"));

                //---- okButton ----
                okButton.setText("OK");
                okButton.setEnabled(false);
                okButton.setName("okButton");
                okButton.addActionListener(e -> okButtonActionPerformed());
                buttonBar.add(okButton, cc.xy(2, 1));

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

        //---- favoritesTypeButtonGroup ----
        var favoritesTypeButtonGroup = new ButtonGroup();
        favoritesTypeButtonGroup.add(projectRadioButton);
        favoritesTypeButtonGroup.add(userRadioButton);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private DialogHeaderPanel dialogHeaderPanel;
    private JLabel projectLabel;
    private JRadioButton userRadioButton;
    private JLabel userLabel;
    protected JTextField nameTextField;
    private JLabel labelLocation;
    private JLabel labelElevation;
    private JLabel labelHeading;
    private JLabel labelTilt;
    private JLabel labelZoom;
    private JButton okButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
