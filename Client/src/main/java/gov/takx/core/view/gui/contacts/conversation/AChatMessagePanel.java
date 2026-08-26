package gov.takx.core.view.gui.contacts.conversation;

import gov.tak.api.chat.IChatMessage;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.core.chat.ChatMessageUtils;
import gov.takx.platform.utils.StandardTimeFormatting;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.text.JTextComponent;

import java.awt.event.ActionListener;

/**
 * Base class for a panel that displays an {@link IChatMessage} to the user.
 *
 * @since 3.1
 */
@PseudoBean
public abstract class AChatMessagePanel extends JPanel
{
    @Inject
    protected StandardTimeFormatting timeFormatting;

    private JButton detailsButton;
    private long time;

    /**
     * @since 5.2
     */
    private final ActionListener detailsButtonActionListener;

    /**
     * @param detailsButtonActionListener listener that responds to "Details" button clicks
     * @since 5.2
     */
    AChatMessagePanel(ActionListener detailsButtonActionListener)
    {
        this.detailsButtonActionListener = detailsButtonActionListener;
    }

    /**
     * Sets a message on the panel to be displayed to the user.
     *
     * @param message   The message to display
     */
    protected void setMessage(@Nonnull IChatMessage message)
    {
        time = message.getSendTime();
        updateTimeLabel();

        final JTextComponent messageTextArea = getMessageTextArea();
        messageTextArea.setText(message.getText());

        if (ChatMessageUtils.hasChatLinks(message)) detailsButton.setVisible(true);
    }

    /**
     * Initializes the "Details" button created by subclasses that allows the user to view chate message package contents.
     *
     * @param button The "Details" button created by subclasses
     * @since 5.2
     */
    protected void setDetailsButton(@Nonnull JButton button)
    {
        this.detailsButton = button;

        button.setVisible(false);
        button.addActionListener(detailsButtonActionListener);
    }

    /**
     * Reset the JTextArea size to allow it to be made smaller. Without this, the area will grow but not shrink.
     *
     * @since 5.2
     */
    public void resetSize()
    {
        getMessageTextArea().setSize(0, 0);
    }

    /**
     * @return The time label
     */
    protected abstract JLabel getTimeLabel();

    /**
     * @return The text area used to display message text to the user
     */
    protected abstract JTextArea getMessageTextArea();

    /**
     * Updates the time label with the current time formatting.
     *
     * @since 5.2
     */
    public void updateTimeLabel()
    {
        getTimeLabel().setText("(" + timeFormatting.formatTimeOnlyWithCurrent(time) + ")");
    }
}
