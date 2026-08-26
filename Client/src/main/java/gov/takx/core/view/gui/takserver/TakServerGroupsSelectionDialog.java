package gov.takx.core.view.gui.takserver;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import gov.tak.api.comms.takserver.IGroupManager;
import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.api.comms.takserver.mission.data.Group;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.platform.ui.AStandardDialog;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialog for users to select their groups on a TAK Server.
 *
 * @since 5.0
 */
@PseudoBean
public class TakServerGroupsSelectionDialog extends AStandardDialog
{
    private final List<TakServerGroupPanel> groupPanelList = new ArrayList<>();
    private final ITakServerConnection connection;

    @Inject
    private IGroupManager groupsManager;

    /**
     * @param connection Connection to TAK Server to manage which groups are active on.
     */
    public TakServerGroupsSelectionDialog(@Nonnull ITakServerConnection connection)
    {
        this.connection = connection;

        initComponents();
    }

    @PostConstruct
    private void postConstruct()
    {
        scrollWrapper.getVerticalScrollBar().setUnitIncrement(20);
        groupsManager.getGroups(connection).whenComplete((results, error) -> {
            if (results != null)
            {
                if (results.wasSuccess())
                    populateGroups(results.groups());
                else
                    error = results.requireError();
            }

            if (error != null)
            {
                String title = "Failed to retrieve groups";
                String message = "The groups for '" + connection.getUri() + "' could not be fetched";
                JOptionPane.showMessageDialog(this, title, message, JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    @InvokeOnEDT
    private void populateGroups(@Nonnull List<Group> groups)
    {
        CellConstraints cc = new CellConstraints();
        FormLayout layout = (FormLayout) groupsPanel.getLayout();

        groupPanelList.clear();
        groupsPanel.removeAll();

        RowSpec rowSpec = layout.getRowSpec(1);
        int y = 1;
        for (Group group : groups)
        {
            TakServerGroupPanel panel = new TakServerGroupPanel(group);
            groupPanelList.add(panel);

            layout.appendRow(rowSpec);
            groupsPanel.add(panel, cc.xy(1, y++));
        }
        groupsPanel.revalidate();
        groupsPanel.repaint();
    }

    private void onOk()
    {
        // Only submit groups if there is a change.
        if (groupPanelList.stream().anyMatch(TakServerGroupPanel::hasSelectionChanged))
        {
            List<Group> activeGroups = groupPanelList.stream()
                    .filter(TakServerGroupPanel::isSelected)
                    .map(TakServerGroupPanel::getGroup)
                    .toList();
            groupsManager.setActiveGroups(connection, activeGroups);
        }

        dispose();
    }

    private void onCancel()
    {
        dispose();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        var contentPanel = new JPanel();
        scrollWrapper = new JScrollPane();
        groupsPanel = new JPanel();
        var tempLabel = new JLabel();
        var buttonBar = new JPanel();
        var okButton = new JButton();
        var cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setMinimumSize(new Dimension(300, 225));
        setTitle("Group Selection");
        setName("this");
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(Borders.createEmptyBorder("7dlu, 7dlu, 7dlu, 7dlu"));
            dialogPane.setName("dialogPane");
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setName("contentPanel");
                contentPanel.setLayout(new FormLayout(
                        "default:grow",
                        "fill:default:grow"));

                //======== scrollWrapper ========
                {
                    scrollWrapper.setName("scrollWrapper");

                    //======== groupsPanel ========
                    {
                        groupsPanel.setName("groupsPanel");
                        groupsPanel.setLayout(new FormLayout(
                                "default:grow",
                                "2*(default)"));

                        //---- tempLabel ----
                        tempLabel.setText("<Fetching available groups...>");
                        tempLabel.setPreferredSize(new Dimension(20, 100));
                        tempLabel.setHorizontalAlignment(SwingConstants.CENTER);
                        tempLabel.setEnabled(false);
                        tempLabel.setName("tempLabel");
                        groupsPanel.add(tempLabel, cc.xy(1, 2));
                    }
                    scrollWrapper.setViewportView(groupsPanel);
                }
                contentPanel.add(scrollWrapper, cc.xy(1, 1));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.createEmptyBorder("5dlu, 0dlu, 0dlu, 0dlu"));
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "$glue, $button, $rgap, $button",
                        "pref"));

                //---- okButton ----
                okButton.setText("OK");
                okButton.setName("okButton");
                okButton.addActionListener(e -> onOk());
                buttonBar.add(okButton, cc.xy(2, 1));

                //---- cancelButton ----
                cancelButton.setText("Cancel");
                cancelButton.setName("cancelButton");
                cancelButton.addActionListener(e -> onCancel());
                buttonBar.add(cancelButton, cc.xy(4, 1));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JScrollPane scrollWrapper;
    private JPanel groupsPanel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
