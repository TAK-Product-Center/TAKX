package gov.takx.platform.ui.location;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.cot.CotTypes;
import gov.tak.api.icons.IIconSetManager;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.pointdropper.CotIcon;
import gov.takx.platform.ui.ResourceDirectory;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;

import java.awt.Image;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import static gov.takx.platform.ui.UIResources.STANDARD_IMAGE_SIZE;

/**
 * User interface for selecting a marker icon.
 *
 * @since 3.0 (pulled out of GoToLocationDialog)
 */
@SuppressWarnings("CdiInjectionPointsInspection")
@PseudoBean
public class MarkerIconPanel extends JPanel
{
    private static final URI SPOT_URI = UIResources.getUri(ResourceDirectory.TAK_ASSETS_ICONS, "gugpgpow------.png");
    private static final List<CotIcon> MIL2525C_ICONS = List.of(
            CotIcon.FriendlyGround, CotIcon.HostileGround, CotIcon.NeutralGround, CotIcon.UnknownGround);

    @Inject
    private IIconSetManager iconSetManager;

    private URI selectedUri;
    private String selectedCotType;

    public MarkerIconPanel()
    {
        initComponents();
    }

    /**
     * Fills in the toggle buttons with our standard options: none, spot, and the MIL2525C types.
     */
    @PostConstruct
    private void postConstruct()
    {
        final var buttons = Collections.list(iconButtonGroup.getElements());

        buttons.get(0).setIcon(UIResources.NONE_ICON);

        configureButton(buttons.get(1), UIResources.SPOT_ICON, SPOT_URI, CotTypes.WAYPOINT);

        // Add buttons for dropping common MIL-2525 icons
        int buttonIndex = 2;
        for (CotIcon cotIcon : MIL2525C_ICONS)
        {
            final var definition = iconSetManager.getOrCreateCotDefinition(cotIcon.getCotType());
            final var button = buttons.get(buttonIndex++);
            final var icon = definition.getIcon().getScaledInstance(STANDARD_IMAGE_SIZE.width, STANDARD_IMAGE_SIZE.height, Image.SCALE_SMOOTH);

            // We intentionally do not pass in the URI because we want the icon to be editable by the user
            // when they change the MIL-SYM attribute. Our CoT icons will be generated based on their type.
            configureButton(button, new ImageIcon(icon), null, cotIcon.getCotType());
        }
    }

    /**
     * Configure an icon selection button.  Make it visible, set the icon, and add the action listener to populate
     * the selected URI and CoT type.
     *
     * @since 5.2
     */
    private void configureButton(AbstractButton button, Icon icon, URI uri, String cotType)
    {
        button.setIcon(icon);
        button.setVisible(true);
        button.addActionListener(e -> {
            selectedUri = uri;
            selectedCotType = cotType;
        });
    }

    /**
     * @return The selected marker URI, can be {@code null} for CoT based selections <i>(or no marker selection)</i>.
     */
    @Nullable
    public URI getSelectedMarkerUri()
    {
        return selectedUri;
    }

    /**
     * @return The CoT type corresponding to the selected marker, or {@code null} if no marker is selected.
     * @since 3.4
     */
    @Nullable
    public String getSelectedCotType()
    {
        return selectedCotType;
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var noMarkerButton = new JToggleButton();
        var iconButton1 = new JToggleButton();
        var iconButton2 = new JToggleButton();
        var iconButton3 = new JToggleButton();
        var iconButton4 = new JToggleButton();
        var iconButton5 = new JToggleButton();
        iconButtonGroup = new ButtonGroup();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "6*(16dlu)",
                "16dlu"));

        //---- noMarkerButton ----
        noMarkerButton.setIcon(null);
        noMarkerButton.setSelected(true);
        noMarkerButton.setBorder(new EmptyBorder(5, 5, 5, 5));
        noMarkerButton.setName("noMarkerButton");
        add(noMarkerButton, cc.xy(1, 1, CellConstraints.CENTER, CellConstraints.CENTER));

        //---- iconButton1 ----
        iconButton1.setBorder(new EmptyBorder(5, 5, 5, 5));
        iconButton1.setName("iconButton1");
        add(iconButton1, cc.xy(2, 1, CellConstraints.CENTER, CellConstraints.CENTER));

        //---- iconButton2 ----
        iconButton2.setVisible(false);
        iconButton2.setBorder(new EmptyBorder(5, 5, 5, 5));
        iconButton2.setName("iconButton2");
        add(iconButton2, cc.xy(3, 1, CellConstraints.CENTER, CellConstraints.CENTER));

        //---- iconButton3 ----
        iconButton3.setVisible(false);
        iconButton3.setBorder(new EmptyBorder(5, 5, 5, 5));
        iconButton3.setName("iconButton3");
        add(iconButton3, cc.xy(4, 1, CellConstraints.CENTER, CellConstraints.CENTER));

        //---- iconButton4 ----
        iconButton4.setVisible(false);
        iconButton4.setBorder(new EmptyBorder(5, 5, 5, 5));
        iconButton4.setName("iconButton4");
        add(iconButton4, cc.xy(5, 1, CellConstraints.CENTER, CellConstraints.CENTER));

        //---- iconButton5 ----
        iconButton5.setVisible(false);
        iconButton5.setBorder(new EmptyBorder(5, 5, 5, 5));
        iconButton5.setName("iconButton5");
        add(iconButton5, cc.xy(6, 1, CellConstraints.CENTER, CellConstraints.CENTER));

        //---- iconButtonGroup ----
        iconButtonGroup.add(noMarkerButton);
        iconButtonGroup.add(iconButton1);
        iconButtonGroup.add(iconButton2);
        iconButtonGroup.add(iconButton3);
        iconButtonGroup.add(iconButton4);
        iconButtonGroup.add(iconButton5);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private ButtonGroup iconButtonGroup;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
