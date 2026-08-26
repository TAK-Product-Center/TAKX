package gov.takx.core.view.gui.contacts.datapackage;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import java.awt.event.ActionListener;
import java.util.List;

import static gov.takx.platform.ui.ResourceDirectory.TAK_ASSETS_PNG;

/**
 * Displays a list of {@link gov.takx.core.chat.ChatLink ChatLinks}.
 *
 * @since 5.2
 */
public class ChatLinksPanel extends JPanel
{
    private static final RowSpec ROW_SPEC = RowSpec.decode("fill:pref");
    private static final ImageIcon FOLDER_ICON = UIResources.getActionButtonIcon(TAK_ASSETS_PNG, "Folder-Open_32x32.png");
    private final CellConstraints cellConstraints = new CellConstraints();
    private final FormLayout chatLinksPanelLayout;

    /**
     * @param openPackagesFolder listener that will open the chat data packages folder
     */
    public ChatLinksPanel(@Nonnull ActionListener openPackagesFolder)
    {
        initComponents();

        folderButton.setIcon(FOLDER_ICON);
        folderButton.addActionListener(openPackagesFolder);

        chatLinksPanelLayout = (FormLayout) chatLinksPanel.getLayout();
    }

    /**
     * @param conversationName The name of the conversation that the data package is associated with
     * @param panels The panels to add
     */
    void add(@Nonnull String conversationName, @Nonnull List<ChatLinkPanel> panels)
    {
        clear();
        conversationLabel.setText(conversationName);
        countLabel.setText("Item count: " + panels.size());
        panels.forEach(this::addPanel);

        revalidate();
    }

    /**
     * Clears all the {@link ChatLinkPanel ChatLinkPanels} from this component.
     */
    void clear()
    {
        chatLinksPanel.removeAll();
        int rowCount;
        while ((rowCount = chatLinksPanelLayout.getRowCount()) > 0) chatLinksPanelLayout.removeRow(rowCount);
    }

    /**
     * @param visible Boolean indicating if the chat data package folder button should be displayed
     */
    void setFolderButtonVisible(boolean visible)
    {
        folderButton.setVisible(visible);
    }

    /**
     * @param panel The panel to add
     */
    private void addPanel(@Nonnull ChatLinkPanel panel)
    {
        chatLinksPanelLayout.appendRow(ROW_SPEC);
        chatLinksPanel.add(panel, cellConstraints.xy(1, chatLinksPanelLayout.getRowCount()));
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        conversationLabel = new JLabel();
        countLabel = new JLabel();
        var scrollPane = new JScrollPane();
        chatLinksPanel = new JPanel();
        folderButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setBorder(Borders.DLU4_BORDER);
        setName("this");
        setLayout(new FormLayout(
                "[200dlu,default]:grow",
                "2*(default, $lgap), fill:default:grow, $lgap, default"));

        //---- conversationLabel ----
        conversationLabel.setFont(conversationLabel.getFont().deriveFont(conversationLabel.getFont().getSize() + 2f));
        conversationLabel.setName("conversationLabel");
        add(conversationLabel, cc.xy(1, 1));

        //---- countLabel ----
        countLabel.setName("countLabel");
        add(countLabel, cc.xy(1, 3));

        //======== scrollPane ========
        {
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            scrollPane.setName("scrollPane");

            //======== chatLinksPanel ========
            {
                chatLinksPanel.setName("chatLinksPanel");
                chatLinksPanel.setLayout(new FormLayout(
                        "default:grow",
                        ""));
            }
            scrollPane.setViewportView(chatLinksPanel);
        }
        add(scrollPane, cc.xy(1, 5));

        //---- folderButton ----
        folderButton.setFocusable(false);
        folderButton.setToolTipText("Open packages folder");
        folderButton.setName("folderButton");
        add(folderButton, cc.xy(1, 7, CellConstraints.RIGHT, CellConstraints.DEFAULT));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JLabel conversationLabel;
    private JLabel countLabel;
    private JPanel chatLinksPanel;
    private JButton folderButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
