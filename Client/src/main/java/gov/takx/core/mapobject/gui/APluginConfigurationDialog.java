package gov.takx.core.mapobject.gui;

import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.plugin.IMutableRaptorPanel;
import gov.takx.api.plugin.IRaptorPanel;
import gov.takx.api.plugin.device.IPluginConfigurationPanel;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.commons.swing.IPanelStateListener;
import gov.takx.core.view.gui.plugin.configuration.PluginConfigurationListPanel;
import gov.takx.platform.ui.AStandardDialog;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionListener;

import java.awt.Component;
import java.awt.Dimension;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for configuration dialogs that make use of the {@link IPluginConfigurationPanel}. The dialog loads
 * core {@link IMutableRaptorPanel}s and plugin provided {@link IPluginConfigurationPanel}s into the panel list, sets
 * up the listeners and responds to button and selection events.
 *
 * @since 5.0
 */
public class APluginConfigurationDialog extends AStandardDialog
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final CellConstraints primaryPanelCC = new CellConstraints();

    private final List<IMutableRaptorPanel> corePanels = new ArrayList<>();
    protected final List<IPluginConfigurationPanel> pluginPanels = new ArrayList<>();
    private final ListSelectionListener listSelectionListener = e -> {
        if (!e.getValueIsAdjusting()) pack();
    };
    protected final PanelStateListener panelStateListener = new PanelStateListener();

    private int customPanelListOffset;

    public APluginConfigurationDialog(@Nonnull String title)
    {
        super(title);

        initComponents();
        pluginConfigurationListPanel.setVisible(false);
    }

    /**
     * Initializes the panel list with plugin {@link IPluginConfigurationPanel}s.
     * 
     * @param pluginPanels The plugin panels
     */
    protected void init(@Nonnull List<IPluginConfigurationPanel> pluginPanels)
    {
        init(Collections.emptyList(), null, pluginPanels);
    }

    /**
     * Initializes the panel list with core {@link IMutableRaptorPanel}s and plugin {@link IPluginConfigurationPanel}s.
     * Additionally, if provided, adds a primary configuration panel above the list of plugin panels.
     * 
     * @param corePanelAndIcons The core panels and their optionally external icons
     * @param primaryConfigurationPanel An optional primary configuration panel above the plugin panel list
     * @param pluginPanels The plugin panels
     */
    protected void init(@Nonnull List<CorePanelAndIcon> corePanelAndIcons,
                        @Nullable IRaptorPanel primaryConfigurationPanel,
                        @Nonnull List<IPluginConfigurationPanel> pluginPanels)
    {
        if (!corePanelAndIcons.isEmpty())
        {
            pluginConfigurationListPanel.setVisible(true);

            corePanelAndIcons.forEach(corePanelAndIcon -> {
                if (corePanelAndIcon.icon != null)
                {
                    // Shapes provide their icon separately from the IRaptorPanel
                    pluginConfigurationListPanel.addPanel(corePanelAndIcon.panel, corePanelAndIcon.icon);
                } else
                {
                    pluginConfigurationListPanel.addPanel(corePanelAndIcon.panel);
                }

                corePanels.add(corePanelAndIcon.panel);
            });
            customPanelListOffset = corePanelAndIcons.size();
        }

        if (primaryConfigurationPanel != null)
        {
            final var panel = primaryConfigurationPanel.getPanel();
            this.primaryConfigurationPanel.add(panel, primaryPanelCC);
            if (panel instanceof IResourceCleaner cleaner) addResourceCleaner(cleaner);
        }

        if (!pluginPanels.isEmpty())
        {
            pluginConfigurationListPanel.setVisible(true);

            // Add the panels and set the panel state listeners
            pluginPanels.forEach(panel -> {
                try
                {
                    pluginConfigurationListPanel.addPanel(panel);
                    panel.setPanelListener(panelStateListener);
                    this.pluginPanels.add(panel);
                } catch (Exception e)
                {
                    logger.error("Failed to add plugin panel", e);
                }
            });
        }

        // Wire the PCP to listen for dialog close events
        addResourceCleaner(pluginConfigurationListPanel);

        // Resize the dialog to fit the largest panel
        repack();

        // Trigger the selection of the first panel in the list so it's displayed
        pluginConfigurationListPanel.setSelectedIndex(0);

        // Resize when the selection changes
        pluginConfigurationListPanel.addListSelectionListener(listSelectionListener);
    }

    /**
     * Sets a custom component in the area just below the {@link IPluginConfigurationPanel}.
     *
     * @param component The component to add
     */
    public void setCustomComponent(@Nonnull Component component)
    {
        customComponentPanel.removeAll();
        customComponentPanel.add(component, new CellConstraints(1, 1));
        pack();
    }

    /**
     * Selects a plugin configuration panel by title. If no match is found, no action is taken.
     *
     * @param title the panel title
     */
    public void selectPageFromPluginPanels(@Nonnull String title)
    {
        int curPos = 0;

        for (IPluginConfigurationPanel customPanel : pluginPanels)
        {
            if (customPanel.getTitle().equals(title))
            {
                pluginConfigurationListPanel.setSelectedIndex(curPos + customPanelListOffset);
                return;
            }

            curPos++;
        }
    }

    /**
     * Selects a core or plugin panel by title. If no match is found, no action is taken.
     *
     * @param title the panel title
     */
    public void selectPageFromAll(@Nonnull String title)
    {
        pluginConfigurationListPanel.setSelectedIndex(title);
    }

    /**
     * Responds to requests to apply current changes. The request will be passed on to each panel managed by the dialog.
     */
    @OverridingMethodsMustInvokeSuper
    public void onApply()
    {
        corePanels.forEach(IMutableRaptorPanel::applyAction);
        pluginPanels.forEach(IPluginConfigurationPanel::applyAction);
        panelStateListener.setPanelClean();
    }

    @OverridingMethodsMustInvokeSuper
    @Override
    protected void onWindowClosing()
    {
        corePanels.clear();
        pluginPanels.clear();
        pluginConfigurationListPanel.removeListSelectionListener(listSelectionListener);
    }

    /**
     * Responds to an "Okay" button click by calling {@link #onApply()} and closing the dialog if the panel states are
     * valid. If the panel states are not valid, an error dialog will be displayed and not action will be taken.
     *
     * @see PluginConfigurationListPanel#validateAction()
     * @see IPluginConfigurationPanel#validateContents()
     */
    private void okButtonActionPerformed()
    {
        // validateAction will display an error dialog if validation fails
        if (pluginConfigurationListPanel.validateAction())
        {
            if (applyButton.isEnabled()) onApply();
            dispose();
        }
    }

    /**
     * Closes this dialog without applying any changes. {@link PluginConfigurationListPanel} is an
     * {@link gov.takx.api.ui.IResourceCleaner} and it will notify any panels that need to respond to the close.
     */
    private void cancelButtonActionPerformed()
    {
        dispose();
    }

    /**
     * Responds to an "Apply" button click by calling {@link #onApply()} if the panel states are valid. If the panel
     * states are not valid, an error dialog will be displayed and no action will be taken.
     */
    private void applyButtonActionPerformed()
    {
        // validateAction will display an error dialog if validation fails
        if (pluginConfigurationListPanel.validateAction())
        {
            onApply();
        }
    }

    /**
     * Repack the dialog to fit the configuration panels.
     * 
     * @since 5.2
     */
    protected void repack()
    {
        final Dimension dimension = pluginConfigurationListPanel.repackPanels(this);
        setMinimumSize(dimension);
        setPreferredSize(dimension);
        revalidate();
        pack();
    }

    /**
     * Holds a {@link IMutableRaptorPanel} and an {@code ImageIcon}. This is required to support any panels (like
     * {@link gov.takx.core.view.gui.maptools.ShapePropertiesEditorPanel}) that do not provide an icon.
     *
     * @param panel The panel
     * @param icon  Optional icon to be used if the panel does not provide an icon
     */
    protected record CorePanelAndIcon(@Nonnull IMutableRaptorPanel panel, @Nullable ImageIcon icon)
    {
    }

    /**
     * Responds to panel state changes by updating the "Apply" button.
     */
    protected class PanelStateListener implements IPanelStateListener
    {
        @Override
        public void setPanelDirty()
        {
            applyButton.setEnabled(true);
        }

        @Override
        public void setPanelClean()
        {
            applyButton.setEnabled(false);
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        var contentPanel = new JPanel();
        primaryConfigurationPanel = new JPanel();
        pluginConfigurationListPanel = new PluginConfigurationListPanel();
        customComponentPanel = new JPanel();
        var buttonBar = new JPanel();
        var okButton = new JButton();
        var cancelButton = new JButton();
        applyButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        var contentPane = getContentPane();
        contentPane.setLayout(new FormLayout(
                "default:grow",
                "fill:default:grow"));

        //======== contentPanel ========
        {
            contentPanel.setBorder(Borders.DLU4_BORDER);
            contentPanel.setName("contentPanel");
            contentPanel.setLayout(new FormLayout(
                    "default:grow",
                    "default, $lgap, fill:default:grow, 2*($lgap, default)"));

            //======== primaryConfigurationPanel ========
            {
                primaryConfigurationPanel.setName("primaryConfigurationPanel");
                primaryConfigurationPanel.setLayout(new FormLayout(
                        "default:grow",
                        "default"));
            }
            contentPanel.add(primaryConfigurationPanel, cc.xy(1, 1));

            //---- pluginConfigurationListPanel ----
            pluginConfigurationListPanel.setName("pluginConfigurationListPanel");
            contentPanel.add(pluginConfigurationListPanel, cc.xy(1, 3));

            //======== customComponentPanel ========
            {
                customComponentPanel.setName("customComponentPanel");
                customComponentPanel.setLayout(new FormLayout(
                        "default:grow",
                        "default"));
            }
            contentPanel.add(customComponentPanel, cc.xy(1, 5));

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "default:grow, $ugap, $button, $rgap, [50dlu,default], $lcgap, default",
                        "pref"));
                ((FormLayout) buttonBar.getLayout()).setColumnGroups(new int[][]{{3, 5, 7}});

                //---- okButton ----
                okButton.setText("OK");
                okButton.setName("okButton");
                okButton.addActionListener(e -> okButtonActionPerformed());
                buttonBar.add(okButton, cc.xy(3, 1));

                //---- cancelButton ----
                cancelButton.setText("Cancel");
                cancelButton.setName("cancelButton");
                cancelButton.addActionListener(e -> cancelButtonActionPerformed());
                buttonBar.add(cancelButton, cc.xy(5, 1));

                //---- applyButton ----
                applyButton.setText("Apply");
                applyButton.setEnabled(false);
                applyButton.setName("applyButton");
                applyButton.addActionListener(e -> applyButtonActionPerformed());
                buttonBar.add(applyButton, cc.xy(7, 1));
            }
            contentPanel.add(buttonBar, cc.xy(1, 7));
        }
        contentPane.add(contentPanel, cc.xy(1, 1));
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JPanel primaryConfigurationPanel;
    protected PluginConfigurationListPanel pluginConfigurationListPanel;
    private JPanel customComponentPanel;
    private JButton applyButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
