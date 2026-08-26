package gov.takx.core.view.gui.notification;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.platform.utils.StandardTimeFormatting;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import java.time.Instant;

/**
 * Panel that displays info about a single {@link gov.takx.api.notification.Notification}.
 *
 * @since 5.2
 */
@PseudoBean
public class NotificationPanel extends JPanel
{
    @Inject
    private StandardTimeFormatting standardTimeFormatting;

    public NotificationPanel()
    {
        initComponents();
    }

    /**
     * @param icon The notification icon
     * @param message The notification message
     * @param instant The notification time
     */
    public void setNotification(@Nonnull Icon icon, @Nonnull String message, @Nonnull Instant instant)
    {
        iconLabel.setIcon(icon);
        messageTextArea.setText(message);
        messageTextArea.invalidate();
        timeLabel.setText(standardTimeFormatting.formatTimeSimpleOrMonthDay(instant));
    }

    /**
     * Reset the JTextArea size to allow it to be made smaller. Without this, the area will grow but not shrink.
     */
    public void resetSize()
    {
        messageTextArea.setSize(0, 0);
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        iconLabel = new JLabel();
        messageTextArea = new JTextArea();
        timeLabel = new JLabel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setBorder(Borders.createEmptyBorder("4dlu, 2dlu, 4dlu, 2dlu"));
        setName("this");
        setLayout(new FormLayout(
                "24dlu, $lcgap, default:grow, $lcgap, 24dlu",
                "fill:default"));

        //---- iconLabel ----
        iconLabel.setName("iconLabel");
        add(iconLabel, cc.xy(1, 1, CellConstraints.CENTER, CellConstraints.TOP));

        //---- messageTextArea ----
        messageTextArea.setText("Message");
        messageTextArea.setWrapStyleWord(true);
        messageTextArea.setLineWrap(true);
        messageTextArea.setEditable(false);
        messageTextArea.setOpaque(false);
        messageTextArea.setName("messageTextArea");
        add(messageTextArea, cc.xy(3, 1));

        //---- timeLabel ----
        timeLabel.setText("text");
        timeLabel.setName("timeLabel");
        add(timeLabel, cc.xy(5, 1, CellConstraints.DEFAULT, CellConstraints.TOP));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JLabel iconLabel;
    private JTextArea messageTextArea;
    private JLabel timeLabel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
