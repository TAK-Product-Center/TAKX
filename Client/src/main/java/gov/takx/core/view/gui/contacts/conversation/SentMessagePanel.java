package gov.takx.core.view.gui.contacts.conversation;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.chat.IChatMessage;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.lang.invoke.MethodHandles;

/**
 * Panel responsible for housing and displaying a single {@link gov.tak.api.chat.IChatMessage}, sent from the client, on the
 * {@link ConversationPanel}.
 *
 * @since 3.1
 */
class SentMessagePanel extends AChatMessagePanel
{
    SentMessagePanel(@Nonnull IChatMessage message, @Nonnull ActionListener detailsButtonActionListener)
    {
        super(detailsButtonActionListener);
        initComponents();
        setDetailsButton(moreDetailsButton);
        setMessage(message);
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
        var senderLabel = new JLabel();
        messageTextArea = new SentMessagePanel.CustomOrderTextArea();
        moreDetailsButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "pref:grow, $lcgap, right:pref",
                "fill:6dlu, $lgap, default, $lgap, fill:[10dlu,pref]:grow, $lgap, default"));

        //---- timeLabel ----
        timeLabel.setText("Time");
        timeLabel.setForeground(new Color(0x99c6ff));
        timeLabel.putClientProperty("FlatLaf.styleClass", "small");
        timeLabel.setName("timeLabel");
        add(timeLabel, cc.xy(1, 3, CellConstraints.RIGHT, CellConstraints.DEFAULT));

        //---- senderLabel ----
        senderLabel.setText("Me:");
        senderLabel.setFont(senderLabel.getFont().deriveFont(senderLabel.getFont().getStyle() | Font.BOLD));
        senderLabel.setForeground(new Color(0x99c6ff));
        senderLabel.setName("senderLabel");
        add(senderLabel, cc.xy(3, 3));

        //---- messageTextArea ----
        messageTextArea.setText("Message");
        messageTextArea.setOpaque(false);
        messageTextArea.setBorder(new EmptyBorder(5, 5, 5, 5));
        messageTextArea.setEditable(false);
        messageTextArea.putClientProperty("FlatLaf.styleClass", "large");
        messageTextArea.setLineWrap(true);
        messageTextArea.setWrapStyleWord(true);
        messageTextArea.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        messageTextArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        messageTextArea.setName("messageTextArea");
        add(messageTextArea, cc.xywh(1, 5, 3, 1));

        //---- moreDetailsButton ----
        moreDetailsButton.setText("Details");
        moreDetailsButton.setName("moreDetailsButton");
        add(moreDetailsButton, cc.xywh(1, 7, 3, 1, CellConstraints.RIGHT, CellConstraints.DEFAULT));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JLabel timeLabel;
    private SentMessagePanel.CustomOrderTextArea messageTextArea;
    private JButton moreDetailsButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables

    /**
     * We're using JTextArea for its character wrapping feature. To get text to align to the right, we set
     * {@link JTextArea#setComponentOrientation(ComponentOrientation)} with {@link ComponentOrientation#RIGHT_TO_LEFT}.
     * This works well, but causes special characters (like punctuation) to move, e.g. "hello!" becomes "!hello" and
     * "-4" becomes "4-". To get around this, this custom class escapes non-alphanumeric characters with an invisible
     * LRM character so that they're not moved. The LRM character is removed before returning selected text.
     * <p>
     * NOTE: Some fonts (like Monospaced, Arial, Cambria) will sometimes try to render the LRM characters for parts of a
     * String when line/word-wrapping is applied. SansSerif and Serif do not try to render the character. If you change
     * the font used by the text area, be sure to test long text Strings at various widths/wrapping points.
     */
    private static class CustomOrderTextArea extends JTextArea
    {
        private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

        private static final String LRM_CHAR = "\u200e";

        /**
         * Escapes all non-letter characters with an invisible LRM character, {@code \u200e} to preserve their locations.
         *
         * @param text The original text
         */
        @Override
        public void setText(String text)
        {
            // The leading LRM char fixes an issue that occurs when the very first character is a special character.
            // Without it, the text is rendered incorrectly and exceptions may be thrown
            super.setText(LRM_CHAR + text.replaceAll("([^A-Za-z])", "$1" + LRM_CHAR));
        }

        /**
         * Removes the LRM character we added in {@link #setText(String)} when text is copied.
         *
         * @return The original message text.
         */
        @Override
        public String getSelectedText()
        {
            final String selectedText = super.getSelectedText();

            return selectedText == null ? null : selectedText.replaceAll(LRM_CHAR, "");
        }

        @Override
        public Dimension getPreferredSize()
        {
            try
            {
                return super.getPreferredSize();
            }
            // TextLayout.checkTextHit() had trouble processing LRM chars in edge cases. The leading LRM_CHAR in the
            // setText() method above should address this issue. This catch is a precaution to prevent side effects if
            // the error occurs again. We will revisit if necessary.
            catch (IllegalArgumentException e)
            {
                final String message = e.getMessage();

                if ("TextHitInfo is out of range".equals(message))
                {
                    // Intentionally omitting exception to limit log size
                    logger.error("Error calculating sent message component preferred size. {}", message);
                    return new Dimension();
                } else
                {
                    throw e;
                }
            }
        }
    }
}
