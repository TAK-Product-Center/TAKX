package gov.takx.core.view.gui.contacts.datapackage;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;
import static gov.takx.platform.ui.ResourceDirectory.TAK_ASSETS_PNG;

/**
 * Displays info about a single {@link gov.takx.core.chat.ChatLink}, including buttons for supported actions.
 *
 * @since 5.2
 */
@PseudoBean
public class ChatLinkPanel extends JPanel
{
    private static final ImageIcon ATTACHMENT_ICON = UIResources.getActionButtonIcon(PLATFORM_ICONS, "attach.png");
    private static final ImageIcon GOTO_ICON = UIResources.getActionButtonIcon(TAK_ASSETS_PNG, "PanTo_32x32.png");
    private static final ImageIcon MENU_ICON = UIResources.getActionButtonIcon(PLATFORM_ICONS, "menu.png");

    /**
     * @param name The chat link name
     * @param openAttachmentRunnable Optional runnable that opens a links associated file if applicable
     * @param goToRunnable Optional runnable that moves the map to the item's location
     * @param openMenuRunnable Optional runnable that opens the item's radial menu if available
     */
    public ChatLinkPanel(@Nonnull String name, @Nullable Runnable openAttachmentRunnable,
                         @Nullable Runnable goToRunnable, @Nullable Runnable openMenuRunnable)
    {
        initComponents();

        attachmentButton.setIcon(ATTACHMENT_ICON);
        goToButton.setIcon(GOTO_ICON);
        openMenuButton.setIcon(MENU_ICON);
        nameLabel.setText(name);

        configureButton(attachmentButton, openAttachmentRunnable, true);
        configureButton(openMenuButton, openMenuRunnable, true);
        configureButton(goToButton, goToRunnable, false);
    }

    /**
     * Configures a button by setting the provided Runnable as its ActionListener or hiding/disabling the button if no
     * Runnable is provided.
     * 
     * @param button The button to configure
     * @param runnable The runnable to use as the ActionListener
     * @param hideIfUnsupported Boolean indicating if the button should be hidden from view if no ActionListener is provided
     */
    private void configureButton(@Nonnull JButton button, @Nullable Runnable runnable, boolean hideIfUnsupported)
    {
        if (runnable != null)
        {
            button.addActionListener(e -> runnable.run());
        } else
        {
            button.setEnabled(false);
            if (hideIfUnsupported) button.setVisible(false);
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        nameLabel = new JLabel();
        attachmentButton = new JButton();
        openMenuButton = new JButton();
        goToButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setBorder(Borders.DLU2_BORDER);
        setName("this");
        setLayout(new FormLayout(
                "default:grow, 3*($lcgap, 24dlu)",
                "default"));

        //---- nameLabel ----
        nameLabel.setText("name");
        nameLabel.setName("nameLabel");
        add(nameLabel, cc.xy(1, 1));

        //---- attachmentButton ----
        attachmentButton.setToolTipText("Open the file");
        attachmentButton.setName("attachmentButton");
        add(attachmentButton, cc.xy(3, 1));

        //---- openMenuButton ----
        openMenuButton.setToolTipText("Open the item menu");
        openMenuButton.setName("openMenuButton");
        add(openMenuButton, cc.xy(5, 1));

        //---- goToButton ----
        goToButton.setToolTipText("Go to the item");
        goToButton.setName("goToButton");
        add(goToButton, cc.xy(7, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JLabel nameLabel;
    private JButton attachmentButton;
    private JButton openMenuButton;
    private JButton goToButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
