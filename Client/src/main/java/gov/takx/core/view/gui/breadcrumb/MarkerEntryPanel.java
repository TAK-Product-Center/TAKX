package gov.takx.core.view.gui.breadcrumb;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Displays a {@link gov.tak.api.maps.Marker marker's} icon, name and track count. For use in the {@link TracksPanel}.
 *
 * @since 5.3
 */
public class MarkerEntryPanel extends JPanel
{
    MarkerEntryPanel()
    {
        initComponents();
    }

    /**
     * Sets the panel's icon, name and track count.
     *
     * @param icon The marker's icon; may be {@code null} if marker is currently being deleted
     * @param markerName The marker's name
     * @param count The marker's track count
     */
    public void update(@Nullable Icon icon, @Nonnull String markerName, long count)
    {
        markerLabel.setIcon(icon);
        markerLabel.setText(markerName);
        trackCountLabel.setText(count + (count == 1 ? " track" : " tracks"));
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        markerLabel = new JLabel();
        trackCountLabel = new JLabel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow, $lcgap, default",
                "24dlu"));

        //---- markerLabel ----
        markerLabel.setText("marker icon and name");
        markerLabel.setName("markerLabel");
        add(markerLabel, cc.xy(1, 1));

        //---- trackCountLabel ----
        trackCountLabel.setText("track count");
        trackCountLabel.setName("trackCountLabel");
        add(trackCountLabel, cc.xy(3, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JLabel markerLabel;
    private JLabel trackCountLabel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
