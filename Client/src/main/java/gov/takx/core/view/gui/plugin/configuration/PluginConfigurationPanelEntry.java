package gov.takx.core.view.gui.plugin.configuration;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import java.awt.Color;

/**
 * Renders a plugin configuration panel's name and icon. To be used in the {@link PluginConfigurationListPanel} panel list.
 *
 * @since 5.0
 */
public class PluginConfigurationPanelEntry extends JPanel
{
    public PluginConfigurationPanelEntry()
    {
        initComponents();
        label.setOpaque(true);
    }

    /**
     * @param text The text to display
     * @param icon The optional icon to display next to the text
     * @param foreground The text color
     * @param background The background color
     */
    public void update(@Nonnull String text, @Nullable Icon icon, @Nonnull Color foreground, @Nonnull Color background)
    {
        label.setText(text);
        label.setIcon(icon);
        label.setForeground(foreground);
        label.setBackground(background);
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        label = new JLabel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "fill:[14dlu,default]:grow"));

        //---- label ----
        label.setText("text");
        label.setIconTextGap(6);
        label.setName("label");
        add(label, cc.xy(1, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JLabel label;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
