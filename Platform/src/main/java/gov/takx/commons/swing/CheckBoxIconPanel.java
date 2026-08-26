package gov.takx.commons.swing;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import jakarta.annotation.Nullable;

import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Combines a {@code JCheckBox} with a {@code JLabel} to allow for a custom icon that <i>doesn't replace the checkbox</i>.
 *
 * @since 3.4.1
 */
public class CheckBoxIconPanel extends JPanel
{
    public CheckBoxIconPanel()
    {
        initComponents();
    }

    /**
     * @param selected Sets the state of the checkbox
     */
    public void setSelected(boolean selected)
    {
        checkBox.setSelected(selected);
    }

    /**
     * @param icon The icon to display next to the checkbox
     */
    public void setIcon(@Nullable Icon icon)
    {
        iconLabel.setIcon(icon);
    }

    /**
     * @param text The text to display next to the checkbox
     */
    public void setText(@Nullable String text)
    {
        textLabel.setText(text);
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        checkBox = new JCheckBox();
        iconLabel = new JLabel();
        textLabel = new JLabel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setLayout(new FormLayout(
                "2*(default, $lcgap), default:grow",
                "default"));
        add(checkBox, cc.xy(1, 1));
        add(iconLabel, cc.xy(3, 1));

        //---- textLabel ----
        textLabel.setText("text");
        add(textLabel, cc.xy(5, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JCheckBox checkBox;
    protected JLabel iconLabel;
    private JLabel textLabel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
