package gov.takx.core.view.gui.contacts.conversation;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.chat.IChatMessage;
import jakarta.annotation.Nonnull;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionListener;

/**
 * Panel responsible for housing and displaying a single {@link IChatMessage}, sent from another TAK user, on the
 * {@link ConversationPanel}.
 *
 * @since 3.1
 */
class ReceivedMessagePanel extends AChatMessagePanel
{
    ReceivedMessagePanel(@Nonnull IChatMessage message, @Nonnull ActionListener detailsButtonActionListener)
    {
        super(detailsButtonActionListener);
        initComponents();
        setDetailsButton(moreDetailsButton);
        setMessage(message);
    }

    @Override
    public void setMessage(@Nonnull IChatMessage message)
    {
        super.setMessage(message);

        senderLabel.setText(message.getSenderName() + ":");
    }

    @Override
    protected JLabel getTimeLabel()
    {
        return timeLabel;
    }

    @Override
    protected JTextArea getMessageTextArea()
    {
        return messageTextArea;
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        timeLabel = new JLabel();
        senderLabel = new JLabel();
        messageTextArea = new JTextArea();
        moreDetailsButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default, $lcgap, default:grow",
                "fill:6dlu, $lgap, default, $lgap, fill:[10dlu,pref]:grow, $lgap, default"));

        //---- timeLabel ----
        timeLabel.setText("Time");
        timeLabel.setForeground(new Color(0xff493c));
        timeLabel.putClientProperty("FlatLaf.styleClass", "small");
        timeLabel.setName("timeLabel");
        add(timeLabel, cc.xy(1, 3));

        //---- senderLabel ----
        senderLabel.setText("Sender");
        senderLabel.setFont(senderLabel.getFont().deriveFont(senderLabel.getFont().getStyle() | Font.BOLD));
        senderLabel.setForeground(new Color(0xff493c));
        senderLabel.setName("senderLabel");
        add(senderLabel, cc.xy(3, 3));

        //---- messageTextArea ----
        messageTextArea.setText("Message");
        messageTextArea.setOpaque(false);
        messageTextArea.setBorder(new EmptyBorder(5, 5, 5, 5));
        messageTextArea.setLineWrap(true);
        messageTextArea.setWrapStyleWord(true);
        messageTextArea.setEditable(false);
        messageTextArea.putClientProperty("FlatLaf.styleClass", "large");
        messageTextArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        messageTextArea.setName("messageTextArea");
        add(messageTextArea, cc.xywh(1, 5, 3, 1));

        //---- moreDetailsButton ----
        moreDetailsButton.setText("Details");
        moreDetailsButton.setName("moreDetailsButton");
        add(moreDetailsButton, cc.xywh(1, 7, 3, 1, CellConstraints.LEFT, CellConstraints.DEFAULT));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JLabel timeLabel;
    private JLabel senderLabel;
    private JTextArea messageTextArea;
    private JButton moreDetailsButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
