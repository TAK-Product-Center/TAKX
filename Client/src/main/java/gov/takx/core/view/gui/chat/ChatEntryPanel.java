package gov.takx.core.view.gui.chat;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.core.chat.Conversation;
import gov.takx.platform.utils.StandardTimeFormatting;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseListener;

/**
 * Displays a single chat message. This panel is used in the {@link ChatsPanel} to display the most recent message from
 * each {@link Conversation}. A different panel is used to display messages in a full conversation.
 *
 * @since 5.2
 */
@PseudoBean
public class ChatEntryPanel extends JPanel
{
    private String message;
    private long time; // The message send time

    @Inject
    private StandardTimeFormatting standardTimeFormatting;

    public ChatEntryPanel()
    {
        initComponents();
    }

    /**
     * Adds a mouse listener to {@code this} component and the JTextArea to ensure all clicks are registered.
     *
     * @param listener the mouse listener
     */
    public void addMouseListener(@Nonnull MouseListener listener)
    {
        super.addMouseListener(listener);
        messageTextArea.addMouseListener(listener);
    }

    /**
     * Reset the JTextArea size to allow it to be made smaller. Without this, the area will grow but not shrink.
     */
    public void resetSize()
    {
        messageTextArea.setSize(0, 0);
    }

    /**
     * Sets the conversation name and icon. These values may change in direct conversations when the remote contact's
     * callsign or status changes.
     *
     * @param conversationName The conversation name
     * @param icon The conversation icon
     */
    public void setConversationDetails(@Nonnull String conversationName, @Nullable Icon icon)
    {
        conversationNameLabel.setText(conversationName);
        iconLabel.setIcon(icon);
    }

    /**
     * @param message The message to display
     * @param time The message timestamp
     */
    public void setMessage(@Nonnull String message, long time)
    {
        this.message = message;
        this.time = time;

        messageTextArea.setText(message);
        updateTimeLabel();
    }

    /**
     * @return The message displayed in this panel
     */
    public String getMessage()
    {
        return message;
    }

    /**
     * @return The timestamp for the message being displayed
     */
    public long getTime()
    {
        return time;
    }

    /**
     * @return The unread message count label
     * @since 5.2
     */
    public JLabel getUnreadCountLabel()
    {
        return unreadCountLabel;
    }

    /**
     * Updates the time label with the current time formatting.
     *
     * @since 5.2
     */
    void updateTimeLabel()
    {
        // If the message was sent today, display the time. Otherwise, display the date.
        dateTimeLabel.setText(standardTimeFormatting.formatTimeSimpleOrMonthDay(time));
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        iconLabel = new JLabel();
        conversationNameLabel = new JLabel();
        unreadCountLabel = new JLabel();
        dateTimeLabel = new JLabel();
        messageTextArea = new JTextArea();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setBorder(Borders.DLU2_BORDER);
        setName("this");
        setLayout(new FormLayout(
                "20dlu, $rgap, default:grow, 2*($lcgap, default)",
                "default, $lgap, fill:default:grow"));

        //---- iconLabel ----
        iconLabel.setName("iconLabel");
        add(iconLabel, cc.xy(1, 1));

        //---- conversationNameLabel ----
        conversationNameLabel.setText("Conversation Name");
        conversationNameLabel.setFont(conversationNameLabel.getFont().deriveFont(conversationNameLabel.getFont().getStyle() | Font.BOLD, conversationNameLabel.getFont().getSize() + 1f));
        conversationNameLabel.setHorizontalAlignment(SwingConstants.LEFT);
        conversationNameLabel.setName("conversationNameLabel");
        add(conversationNameLabel, cc.xy(3, 1));

        //---- unreadCountLabel ----
        unreadCountLabel.setToolTipText("Unread messages in this conversation");
        unreadCountLabel.setHorizontalTextPosition(SwingConstants.CENTER);
        unreadCountLabel.setIconTextGap(-1);
        unreadCountLabel.setPreferredSize(new Dimension(28, 22));
        unreadCountLabel.setName("unreadCountLabel");
        add(unreadCountLabel, cc.xy(5, 1));

        //---- dateTimeLabel ----
        dateTimeLabel.setText("time");
        dateTimeLabel.setName("dateTimeLabel");
        add(dateTimeLabel, cc.xy(7, 1));

        //---- messageTextArea ----
        messageTextArea.setText("Message");
        messageTextArea.setEditable(false);
        messageTextArea.setLineWrap(true);
        messageTextArea.setWrapStyleWord(true);
        messageTextArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        messageTextArea.setOpaque(false);
        messageTextArea.setName("messageTextArea");
        add(messageTextArea, cc.xywh(3, 3, 5, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JLabel iconLabel;
    private JLabel conversationNameLabel;
    private JLabel unreadCountLabel;
    private JLabel dateTimeLabel;
    private JTextArea messageTextArea;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
