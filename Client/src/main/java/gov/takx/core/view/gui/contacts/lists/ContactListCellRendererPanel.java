package gov.takx.core.view.gui.contacts.lists;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.core.chat.Conversation;
import gov.takx.core.view.gui.contacts.conversation.UnreadMessageIconUtils;
import jakarta.annotation.Nonnull;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import java.awt.Dimension;
import java.awt.Font;

/**
 * Panel used to render a cell in a contact list.
 *
 * @since 3.0
 */
public class ContactListCellRendererPanel extends JPanel
{
    private final Font defaultNameLabelFont;
    private final Font boldNameLabelFont;

    public ContactListCellRendererPanel()
    {
        initComponents();

        defaultNameLabelFont = nameLabel.getFont();
        boldNameLabelFont = defaultNameLabelFont.deriveFont(defaultNameLabelFont.getStyle() | Font.BOLD);

        nameLabel.setMinimumSize(new Dimension(28, 20));

        // used to distinguish icon clicks from other panel clicks
        iconLabel.putClientProperty(GroupContactTreeController.ICON_LABEL_PROPERTY, true);
    }

    public void setConversation(@Nonnull Conversation conversation)
    {
        nameLabel.setText(conversation.getName());

        int numUnreadMessages = conversation.getUnreadMessageCount();

        nameLabel.setFont(numUnreadMessages > 0 ? boldNameLabelFont : defaultNameLabelFont);
        UnreadMessageIconUtils.updateLabel(numMessagesLabel, numUnreadMessages);

        invalidate();
    }

    /**
     * @param icon The contact's icon
     * @since 3.4.1
     */
    public void setIcon(@Nonnull Icon icon)
    {
        iconLabel.setIcon(icon);
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        iconLabel = new JLabel();
        nameLabel = new JLabel();
        numMessagesLabel = new JLabel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setOpaque(false);
        setName("this");
        setLayout(new FormLayout(
                "default, $lcgap, default:grow, $lcgap, default",
                "default"));

        //---- iconLabel ----
        iconLabel.setName("iconLabel");
        add(iconLabel, cc.xy(1, 1));

        //---- nameLabel ----
        nameLabel.setText("Name");
        nameLabel.setFont(nameLabel.getFont().deriveFont(nameLabel.getFont().getSize() + 1f));
        nameLabel.setName("nameLabel");
        add(nameLabel, cc.xy(3, 1));

        //---- numMessagesLabel ----
        numMessagesLabel.setHorizontalTextPosition(SwingConstants.CENTER);
        numMessagesLabel.setIconTextGap(-1);
        numMessagesLabel.setPreferredSize(new Dimension(28, 22));
        numMessagesLabel.setToolTipText("Unread messages in this conversation");
        numMessagesLabel.setName("numMessagesLabel");
        add(numMessagesLabel, cc.xy(5, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JLabel iconLabel;
    private JLabel nameLabel;
    private JLabel numMessagesLabel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
