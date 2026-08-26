package gov.takx.core.view.gui.takserver;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.comms.takserver.mission.data.Group;
import jakarta.annotation.Nonnull;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.*;

import java.awt.Color;
import java.awt.Font;
import java.util.Objects;

/**
 * Panel representing a single {@link Group}.
 *
 * @since 5.0
 */
public class TakServerGroupPanel extends JPanel
{
    private final Group group;
    private final boolean initialSelection;

    /**
     * @param group Group to display.
     */
    public TakServerGroupPanel(@Nonnull Group group)
    {
        boolean isActive = group.active();

        this.group = group;
        initialSelection = isActive;

        initComponents();

        nameLabel.setText(group.name());
        descriptionLabel.setText(Objects.requireNonNullElse(group.description(), "<no description>"));
        activeLabel.setText(isActive ? "✔" : "✖");
        activeLabel.setForeground(isActive ? Color.GREEN : Color.RED);
        selectedCheck.setSelected(isActive);
    }

    /**
     * @return Group associated with this panel.
     */
    @Nonnull
    public Group getGroup()
    {
        return group;
    }

    /**
     * @return {@code true} when the group is selected.
     */
    public boolean isSelected()
    {
        return selectedCheck.isSelected();
    }

    /**
     * @return {@code true} when the group selection state does not match the initial state.
     */
    public boolean hasSelectionChanged()
    {
        return isSelected() != initialSelection;
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        selectedCheck = new JCheckBox();
        nameLabel = new JLabel();
        activeLabel = new JLabel();
        descriptionLabel = new JLabel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setBorder(new EmptyBorder(5, 5, 5, 5));
        setName("this");
        setLayout(new FormLayout(
                "default, $lcgap, default:grow, $lcgap, right:default",
                "default, $lgap, default"));

        //---- selectedCheck ----
        selectedCheck.setName("selectedCheck");
        add(selectedCheck, cc.xywh(1, 1, 1, 3));

        //---- nameLabel ----
        nameLabel.setText("Group Name");
        nameLabel.setFont(nameLabel.getFont().deriveFont(nameLabel.getFont().getStyle() | Font.BOLD));
        nameLabel.setName("nameLabel");
        add(nameLabel, cc.xy(3, 1));

        //---- activeLabel ----
        activeLabel.setName("activeLabel");
        add(activeLabel, cc.xywh(5, 1, 1, 3));

        //---- descriptionLabel ----
        descriptionLabel.setText("Description");
        descriptionLabel.setName("descriptionLabel");
        add(descriptionLabel, cc.xy(3, 3));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JCheckBox selectedCheck;
    private JLabel nameLabel;
    private JLabel activeLabel;
    private JLabel descriptionLabel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
