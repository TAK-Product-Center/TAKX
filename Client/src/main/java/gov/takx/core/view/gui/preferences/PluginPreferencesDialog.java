package gov.takx.core.view.gui.preferences;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.ui.IProgramPreferencesPanel;
import gov.takx.commons.swing.IPanelStateListener;
import gov.takx.commons.swing.PanelListener;
import gov.takx.core.view.gui.PreferenceSpaceManager;
import gov.takx.platform.ui.AStandardDialog;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import java.awt.BorderLayout;
import java.awt.Component;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

/**
 * Preferences dialog to display custom preference panels added by plugins via {@link gov.takx.api.ui.IPreferenceSpaceManager}.
 */
@PseudoBean
public class PluginPreferencesDialog extends AStandardDialog
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final IPanelStateListener panelListener;
    private final List<IProgramPreferencesPanel> panelList = new ArrayList<>();

    @Inject
    private PreferenceSpaceManager manager;

    public PluginPreferencesDialog()
    {
        initComponents();

        panelListener = new PanelListener(applyButton);
    }

    @PostConstruct
    private void postConstruct()
    {
        initTabs();

        preferenceTabs.setSelectedIndex(0);

        validate();
        pack();
        setLocationRelativeTo(getParent());

        panelListener.setPanelClean();
    }

    /**
     * Handles cleaning up the contained components as part of the dialog being disposed.
     *
     * @since 3.3
     */
    @Override
    protected void onWindowClosing()
    {
        panelList.forEach(tabPanel -> {
            try
            {
                tabPanel.releaseResources();
            } catch (Exception e)
            {
                logger.error("Error disposing of user preferences panel {}", tabPanel, e);
            }
        });

        panelList.clear();
        preferenceTabs.removeAll();
    }

    private void initTabs()
    {
        manager.getCustomPanels().forEach(this::addTab);
    }

    /**
     * Add a preferences panel to our tabs.
     *
     * @param tabName Name of tab
     * @param panel The panel to add
     * @since 5.0
     */
    private void addTab(String tabName, IProgramPreferencesPanel panel)
    {
        panelList.add(panel);

        panel.setPanelListener(panelListener);

        final Component componentToAdd;
        final JScrollPane scrollPane = new JScrollPane((Component) panel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(10);
        componentToAdd = scrollPane;

        preferenceTabs.addTab(tabName, componentToAdd);
    }

    private void okButtonActionPerformed()
    {
        if (applyAction()) dispose();
    }

    private void cancelButtonActionPerformed()
    {
        dispose();
    }

    private void applyButtonActionPerformed()
    {
        if (applyAction()) panelListener.setPanelClean();
    }

    private boolean applyAction()
    {
        for (IProgramPreferencesPanel tabPanel : panelList)
        {
            try
            {
                if (!tabPanel.applyAction()) return false;
            } catch (Exception e)
            {
                logger.error("Error disposing of user preferences panel {}", tabPanel, e);
            }
        }
        return true;
    }

    @SuppressWarnings("DuplicatedCode")
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        var contentPanel = new JPanel();
        preferenceTabs = new JTabbedPane();
        var buttonBar = new JPanel();
        var okButton = new JButton();
        var cancelButton = new JButton();
        applyButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("User Preferences");
        setModal(true);
        setName("this");
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(Borders.DIALOG_BORDER);
            dialogPane.setName("dialogPane");
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setName("contentPanel");
                contentPanel.setLayout(new FormLayout(
                        "pref:grow",
                        "fill:default:grow"));

                //======== preferenceTabs ========
                {
                    preferenceTabs.setName("preferenceTabs");
                }
                contentPanel.add(preferenceTabs, cc.xy(1, 1));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "$glue, $button, $rgap, $button, $lcgap, $button",
                        "pref"));

                //---- okButton ----
                okButton.setText("OK");
                okButton.setName("okButton");
                okButton.addActionListener(e -> okButtonActionPerformed());
                buttonBar.add(okButton, cc.xy(2, 1));

                //---- cancelButton ----
                cancelButton.setText("Cancel");
                cancelButton.setName("cancelButton");
                cancelButton.addActionListener(e -> cancelButtonActionPerformed());
                buttonBar.add(cancelButton, cc.xy(4, 1));

                //---- applyButton ----
                applyButton.setText("Apply");
                applyButton.setEnabled(false);
                applyButton.setName("applyButton");
                applyButton.addActionListener(e -> applyButtonActionPerformed());
                buttonBar.add(applyButton, cc.xy(6, 1));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JTabbedPane preferenceTabs;
    private JButton applyButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
