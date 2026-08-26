package gov.takx.core.view.gui.layers.viewmanager;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.project.IProjectManager;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.commons.swing.CollapsiblePanel;
import gov.takx.commons.swing.IPanelStateListener;
import gov.takx.commons.swing.SplitButton;
import gov.takx.core.globe.tak.WidgetLayerManager;
import gov.takx.core.view.gui.ATitledPanel;
import gov.takx.core.view.gui.layers.ComboLayerPopupMenu;
import gov.takx.core.view.gui.layers.viewmanager.properties.LayerPropertiesDialog;
import gov.takx.platform.ui.UIResources;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import java.awt.Font;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static gov.takx.core.view.gui.layers.viewmanager.LayerViewUtils.FAVORITES;

/**
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class LayerViewPanel extends ATitledPanel implements IPanelStateListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private IProjectManager projectManager;

    @Inject
    private IExtendedGlobeManager globeManager;

    @Inject
    private WidgetLayerManager widgetLayerManager;

    private final LayerViewManager layerViewManager;
    private final Map<String, GroupComponents> groupsToPanels;
    private final LayerSearchPanel searchPanel;
    private LayerPropertiesDialog propertiesDialog;
    private String lastSelectedGroup;

    protected LayerViewPanel(LayerViewManager layerViewManager)
    {
        super(ButtonType.MAPS.getDisplayText());

        this.layerViewManager = layerViewManager;
        initComponents();
        groupsToPanels = new HashMap<>();
        searchPanel = new LayerSearchPanel(this);
        searchPane.add(searchPanel, new CellConstraints(1, 1));
        lastSelectedGroup = null;
        addButton.setIcon(UIResources.ADD_ACTION_BUTTON_ICON);
        deleteButton.setIcon(UIResources.REMOVE_ACTION_BUTTON_ICON);
        goToButton.setIcon(UIResources.GOTO_ACTION_BUTTON_ICON);
        propertiesButton.setIcon(UIResources.SETTINGS_ACTION_BUTTON_ICON);

        addButton.setArrowLocation(SplitButton.ArrowLocation.BOTTOM);
        addButton.setPopupMenu(new ComboLayerPopupMenu());

        // Adjust speed of vertical JScrollPane since default is very slow in this setting
        groupsScrollPane.getVerticalScrollBar().setUnitIncrement(25);
    }

    /**
     * Prepare for project startup.
     */
    public void loadState()
    {
        checkBoxOnline.setSelected(!projectManager.getCurrentOpenProject().isWMSOfflineMode());
    }

    /**
     * Clear-out temporary data from sub-panels and prepare for project shutdown.
     */
    public void clearState()
    {
        searchPanel.clear();
        groupsToPanels.values().forEach(components -> components.view.clear());

        if (propertiesDialog != null)
        {
            propertiesDialog.dispose();
            propertiesDialog = null;
        }
    }

    /**
     * Repaint a group view.
     */
    public void repaintGroup(String group)
    {
        groupsToPanels.get(group).view.repaint();
    }

    public void setSelectedGroup(String groupSelected)
    {
        if (lastSelectedGroup != null && lastSelectedGroup.equals(groupSelected)) return;
        lastSelectedGroup = groupSelected;
        groupsToPanels.forEach((group, panel) -> {
            if (!group.equals(groupSelected)) panel.view.clearSelection();
        });
    }

    public void setFilterString(String filterString)
    {
        for (GroupComponents groupComponents : groupsToPanels.values())
        {
            groupComponents.view.setFilterString(filterString);
        }
    }

    public void refreshTable(String group)
    {
        GroupComponents components = groupsToPanels.get(group);
        if (components != null) components.view.refreshTable();
    }

    public void onLayerSelected(IRaptorLayer layer)
    {
        if (propertiesDialog != null) propertiesDialog.setRaptorLayer(layer);
    }

    public void updateGroup(String group, boolean isGroupVisible)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Setting group: {}, visible: {}", group, isGroupVisible);
        }
        GroupComponents components = groupsToPanels.get(group);
        if (components != null)
        {
            components.collapsiblePanel.setVisible(isGroupVisible);
            components.view.refreshTable();
        }
    }

    public void addGroupLayer(String group, IRaptorLayer addedLayer)
    {
        GroupComponents components = groupsToPanels.get(group);
        if (components != null)
        {
            components.collapsiblePanel.setVisible(true);
            components.collapsiblePanel.setCollapsed(false);
            if (components.view instanceof LayerGroupPanel layerGroupPanel) layerGroupPanel.layerAdded(addedLayer);
        }
    }

    public List<String> getOpenGroups()
    {
        List<String> openGroups = new ArrayList<>();
        for (Map.Entry<String, GroupComponents> entry : groupsToPanels.entrySet())
        {
            CollapsiblePanel panel = entry.getValue().collapsiblePanel;
            if (panel.isVisible() && !panel.isCollapsed()) openGroups.add(entry.getKey());
        }
        return openGroups;
    }

    public void setOpenGroups(List<String> openGroups)
    {
        for (GroupComponents components : groupsToPanels.values())
        {
            components.collapsiblePanel.setCollapsed(true);
        }

        for (String group : openGroups)
        {
            GroupComponents components = groupsToPanels.get(group);
            if (components != null && components.collapsiblePanel.isVisible()) components.collapsiblePanel.setCollapsed(false);
        }
    }

    /**
     * Notify the favorites table that its contents may have changed.
     */
    public void refreshFavorites()
    {
        GroupComponents components = groupsToPanels.get(FAVORITES);
        if (components != null) components.view.refreshTable();
    }

    @InvokeOnEDT
    void addGroup(String group)
    {
        var groupPanel = switch (group)
        {
            case FAVORITES -> new LayerFavoritesPanel(this, layerViewManager);
            case LayerViewUtils.EXTRA_TOOLS -> new LayerComboPanel(group, LayerViewUtils.EXTRAS_TOOLS_GROUP, this, layerViewManager);
            case LayerViewUtils.WIDGETS -> new WidgetGroupPanel(group, this, layerViewManager, widgetLayerManager);
            default -> new LayerGroupPanel(group, this, layerViewManager);
        };

        CollapsiblePanel collapsiblePanel = new CollapsiblePanel(group, FAVORITES.equals(group));
        collapsiblePanel.setContentView(groupPanel);
        groupsPane.add(collapsiblePanel);
        groupsToPanels.put(group, new GroupComponents(groupPanel, collapsiblePanel));
        if (LayerViewUtils.COLLAPSED_GROUPS.contains(group)) collapsiblePanel.setCollapsed(true);
        if (LayerViewUtils.UNDRAGGABLE_GROUPS.contains(group)) groupPanel.setDragEnabled(false);
    }

    public void setDeleteEnabled(boolean enabled)
    {
        deleteButton.setEnabled(enabled);
    }

    public void setPropertiesEnabled(boolean enabled)
    {
        propertiesButton.setEnabled(enabled);
    }

    public void setGoToEnabled(boolean enabled)
    {
        goToButton.setEnabled(enabled);
    }

    /**
     * Currently unused, but retained description for posterity or eventual resurrection.
     */
    public String getDescription()
    {
        return "<html>All of the layers used in the current project are displayed below and are grouped by Imagery, " +
                "Elevation, Shapes, Overlays, and Extras. Choose the layer group to display from the \"Show\" drop " +
                "down list. Add new layers by pressing the Add button. Edit layer properties by selecting the layer " +
                "and pressing the Properties button. Go To the bounds of a layer by selecting the layer and pressing " +
                "the Go To button. Delete a layer by selecting the layer and pressing the Delete button.</html>";
    }

    @Override
    public void setPanelDirty()
    {
        // do nothing
    }

    @Override
    public void setPanelClean()
    {
        if (propertiesDialog.isShowing())
        {
            for (GroupComponents components : groupsToPanels.values())
            {
                components.view.repaint();
            }
        }
    }

    private void deleteButtonActionPerformed()
    {
        layerViewManager.deleteSelectedLayers();
    }

    private void gotoButtonActionPerformed()
    {
        layerViewManager.goToSelectedLayer();
    }

    private void propertiesButtonActionPerformed()
    {
        final IRaptorLayer selectedLayer = layerViewManager.getSelectedLayer();

        if (selectedLayer != null)
        {
            if (propertiesDialog == null)
            {
                propertiesDialog = new LayerPropertiesDialog(this);
                propertiesDialog.addCloseHandler(d -> propertiesDialog = null);
            }

            propertiesDialog.setRaptorLayer(selectedLayer);
            propertiesDialog.setVisible(true);
        }
    }

    private void checkBoxOnlineActionPerformed()
    {
        // Set the value in the project preferences and set the active value on the globe
        final boolean offlineMode = !checkBoxOnline.isSelected();
        projectManager.getCurrentOpenProject().setWMSOfflineMode(offlineMode);
        globeManager.getPrimaryGlobeDelegate().setWMSOfflineMode(offlineMode);
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var buttonsPane = new JPanel();
        addButton = new SplitButton();
        deleteButton = new JButton();
        goToButton = new JButton();
        propertiesButton = new JButton();
        var separator5 = new JSeparator();
        searchPane = new JPanel();
        var separator4 = new JSeparator();
        groupsScrollPane = new JScrollPane();
        var groupsMainPanel = new JPanel();
        groupsPane = new JPanel();
        checkBoxOnline = new JCheckBox();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "2*(default, $lgap), fill:min:grow, $lgap, default"));

        //======== buttonsPane ========
        {
            buttonsPane.setName("buttonsPane");
            buttonsPane.setLayout(new FormLayout(
                    "3*(default, $lcgap), default",
                    "default"));

            //---- addButton ----
            addButton.setHorizontalTextPosition(SwingConstants.CENTER);
            addButton.setVerticalTextPosition(SwingConstants.BOTTOM);
            addButton.setToolTipText("Add Layer");
            addButton.setText("Add");
            addButton.setName("addButton");
            buttonsPane.add(addButton, cc.xy(1, 1, CellConstraints.RIGHT, CellConstraints.FILL));

            //---- deleteButton ----
            deleteButton.setHorizontalTextPosition(SwingConstants.CENTER);
            deleteButton.setVerticalTextPosition(SwingConstants.BOTTOM);
            deleteButton.setVerifyInputWhenFocusTarget(false);
            deleteButton.setToolTipText("Delete Layer");
            deleteButton.setText("Delete");
            deleteButton.setName("deleteButton");
            deleteButton.addActionListener(e -> deleteButtonActionPerformed());
            buttonsPane.add(deleteButton, cc.xy(3, 1, CellConstraints.FILL, CellConstraints.FILL));

            //---- goToButton ----
            goToButton.setHorizontalTextPosition(SwingConstants.CENTER);
            goToButton.setVerticalTextPosition(SwingConstants.BOTTOM);
            goToButton.setToolTipText("Go To Layer");
            goToButton.setText("Go To");
            goToButton.setName("goToButton");
            goToButton.addActionListener(e -> gotoButtonActionPerformed());
            buttonsPane.add(goToButton, cc.xy(5, 1, CellConstraints.FILL, CellConstraints.FILL));

            //---- propertiesButton ----
            propertiesButton.setHorizontalTextPosition(SwingConstants.CENTER);
            propertiesButton.setVerticalTextPosition(SwingConstants.BOTTOM);
            propertiesButton.setToolTipText("Show Layer Properties");
            propertiesButton.setText("Properties");
            propertiesButton.setName("propertiesButton");
            propertiesButton.addActionListener(e -> propertiesButtonActionPerformed());
            buttonsPane.add(propertiesButton, cc.xy(7, 1, CellConstraints.FILL, CellConstraints.FILL));
        }
        add(buttonsPane, cc.xy(1, 1));

        //---- separator5 ----
        separator5.setName("separator5");
        add(separator5, cc.xy(1, 2));

        //======== searchPane ========
        {
            searchPane.setName("searchPane");
            searchPane.setLayout(new FormLayout(
                    "default:grow",
                    "default:grow"));
        }
        add(searchPane, cc.xy(1, 3));

        //---- separator4 ----
        separator4.setName("separator4");
        add(separator4, cc.xy(1, 4));

        //======== groupsScrollPane ========
        {
            groupsScrollPane.setName("groupsScrollPane");

            //======== groupsMainPanel ========
            {
                groupsMainPanel.setName("groupsMainPanel");
                groupsMainPanel.setLayout(new FormLayout(
                        "default:grow",
                        "default, $lgap, default:grow"));

                //======== groupsPane ========
                {
                    groupsPane.setName("groupsPane");
                    groupsPane.setLayout(new BoxLayout(groupsPane, BoxLayout.Y_AXIS));
                }
                groupsMainPanel.add(groupsPane, cc.xy(1, 1, CellConstraints.FILL, CellConstraints.FILL));
            }
            groupsScrollPane.setViewportView(groupsMainPanel);
        }
        add(groupsScrollPane, cc.xy(1, 5));

        //---- checkBoxOnline ----
        checkBoxOnline.setText("<html>Connect to Imagery and Elevation Servers ");
        checkBoxOnline.setSelected(true);
        checkBoxOnline.setFont(checkBoxOnline.getFont().deriveFont(checkBoxOnline.getFont().getStyle() | Font.BOLD, checkBoxOnline.getFont().getSize() - 1f));
        checkBoxOnline.setToolTipText("If checked, TAKX will automatically connect to WMS servers associated with imagery and elevation layers to obtain data for those layers.");
        checkBoxOnline.setName("checkBoxOnline");
        checkBoxOnline.addActionListener(e -> checkBoxOnlineActionPerformed());
        add(checkBoxOnline, cc.xy(1, 7));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private SplitButton addButton;
    private JButton deleteButton;
    private JButton goToButton;
    private JButton propertiesButton;
    private JPanel searchPane;
    private JScrollPane groupsScrollPane;
    private JPanel groupsPane;
    private JCheckBox checkBoxOnline;
    // JFormDesigner - End of variables declaration  //GEN-END:variables

    private record GroupComponents(AMapGroupPanel<?> view, CollapsiblePanel collapsiblePanel)
    {
    }
}
