package gov.takx.core.view.gui.takserver.timeline;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import jakarta.annotation.Nonnull;

import javax.swing.JLabel;
import javax.swing.JPanel;

import java.awt.BorderLayout;
import java.awt.Font;

/**
 * Displays a single {@link TimelineEntry}
 *
 * @since 5.0
 */
public class TimelineEntryPanel extends JPanel
{
    public TimelineEntryPanel()
    {
        initComponents();
    }

    /**
     * @param entry The {@link TimelineEntry} to display
     */
    public void setValues(@Nonnull TimelineEntry entry)
    {
        iconLabel.setIcon(entry.icon());
        titleLabel.setText(entry.title());
        dateLabel.setText(entry.date());
        descriptionLabel.setText(entry.description());
        extraContentPanel.removeAll();
        if (entry.extraDetailsProvider() != null)
        {
            JPanel extraDetails = entry.extraDetailsProvider().get();
            if (extraDetails != null)
            {
                extraContentPanel.add(extraDetails, BorderLayout.CENTER);
            }
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        iconLabel = new JLabel();
        titleLabel = new JLabel();
        dateLabel = new JLabel();
        descriptionLabel = new JLabel();
        extraContentPanel = new JPanel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setBorder(Borders.DLU4_BORDER);
        setName("this");
        setLayout(new FormLayout(
                "16dlu, $lcgap, default:grow",
                "3*(default, $lgap), default:grow"));

        //---- iconLabel ----
        iconLabel.setName("iconLabel");
        add(iconLabel, cc.xywh(1, 1, 1, 3, CellConstraints.CENTER, CellConstraints.CENTER));

        //---- titleLabel ----
        titleLabel.setText("title");
        titleLabel.setFont(titleLabel.getFont().deriveFont(titleLabel.getFont().getStyle() | Font.BOLD, titleLabel.getFont().getSize() + 1f));
        titleLabel.setName("titleLabel");
        add(titleLabel, cc.xy(3, 1));

        //---- dateLabel ----
        dateLabel.setText("date");
        dateLabel.setName("dateLabel");
        add(dateLabel, cc.xy(3, 3));

        //---- descriptionLabel ----
        descriptionLabel.setText("description");
        descriptionLabel.setName("descriptionLabel");
        add(descriptionLabel, cc.xy(3, 5));

        //======== extraContentPanel ========
        {
            extraContentPanel.setName("extraContentPanel");
            extraContentPanel.setLayout(new BorderLayout());
        }
        add(extraContentPanel, cc.xy(3, 7));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JLabel iconLabel;
    private JLabel titleLabel;
    private JLabel dateLabel;
    private JLabel descriptionLabel;
    private JPanel extraContentPanel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
