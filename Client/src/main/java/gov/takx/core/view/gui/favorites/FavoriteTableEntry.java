package gov.takx.core.view.gui.favorites;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.globe.IFavorite;
import gov.takx.platform.preferences.UserPreferenceEntry;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import java.awt.Color;
import java.awt.Component;

import static gov.takx.api.coordinatesystem.CoordinateSystem.getLocationString;

/**
 * Represents a {@link IFavorite} in the {@link FavoritesPanel}.
 *
 */
public class FavoriteTableEntry extends JPanel
{
    public FavoriteTableEntry(IFavorite favorite, boolean isUserFavorite, Icon icon)
    {
        initComponents();

        String description = "<html><b>" + favorite.getDescription() + "</b> " + (isUserFavorite ? "(User)" : "(Project)");
        labelEntryName.setText(description);

        labelIcon.setText(null);
        labelIcon.setIcon(icon);

        labelEntryDetails.setText(getLocationString(favorite, UserPreferenceEntry.COORDINATE_SYSTEM.getValueAs(CoordinateSystem.class)));
    }

    /**
     * Sets the foreground on the components in the panel since they contain the text.
     *
     * @param fg Foreground color
     * @since 3.3
     */
    public void setForegroundForChildComponents(Color fg)
    {
        for (Component component : getComponents())
        {
            component.setForeground(fg);
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        labelIcon = new JLabel();
        labelEntryName = new JLabel();
        labelEntryDetails = new JLabel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("panelFavoriteTableEntry");
        setLayout(new FormLayout(
                "default, $rgap, default:grow",
                "2*(default)"));

        //---- labelIcon ----
        labelIcon.setText("icon");
        labelIcon.setName("labelIcon");
        add(labelIcon, cc.xywh(1, 1, 1, 2));

        //---- labelEntryName ----
        labelEntryName.setText("name");
        labelEntryName.setName("labelEntryName");
        add(labelEntryName, cc.xy(3, 1));

        //---- labelEntryDetails ----
        labelEntryDetails.setText("details");
        labelEntryDetails.setName("labelEntryDetails");
        add(labelEntryDetails, cc.xy(3, 2));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JLabel labelIcon;
    private JLabel labelEntryName;
    private JLabel labelEntryDetails;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
