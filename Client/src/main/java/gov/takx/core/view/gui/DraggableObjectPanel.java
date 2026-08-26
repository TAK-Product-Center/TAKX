package gov.takx.core.view.gui;

import com.formdev.flatlaf.FlatClientProperties;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.RemoveObserver;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.event.PluginAdded;
import gov.takx.api.event.mapobject.MapObjectAdded;
import gov.takx.api.event.mapobject.MapObjectRemoved;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.MapObjectGroupNames;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.platform.IProgressMonitor;
import gov.takx.api.platform.IRaptorLifecycleListener;
import gov.takx.api.plugin.PluginCategory;
import gov.takx.api.plugin.device.IDevicePluginDefinition;
import gov.takx.api.registry.IDevicePluginRegistry;
import gov.takx.api.registry.ISyntheticPluginRegistry;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.commons.swing.RaptorTransferable;
import gov.takx.core.view.ApplicationDataFlavor;
import gov.takx.platform.ui.UIResources;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.border.Border;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Panel that displays icon buttons for objects that can be drag/dropped onto the globe.
 *
 * @since 3.4 (renamed from DraggableRaptorObjectPanel)
 */
@PseudoBean
public abstract class DraggableObjectPanel extends ATitledPanel implements IRaptorLifecycleListener
{
    private static final String OBJECTS_IN_USE_GROUP_SUFFIX = " In Use";

    @Inject
    private IDevicePluginRegistry devicePluginRegistry;

    @Inject
    private ISyntheticPluginRegistry syntheticPluginRegistry;

    @Inject
    private IMapObjectManager mapObjectManager;

    @Inject
    private IExtendedGlobeManager globeManager;

    private final String objectsInUseGroup;

    private final DefaultComboBoxModel<JScrollPane> model = new DefaultComboBoxModel<>();
    private final CellConstraints cc = new CellConstraints();
    private final Map<String, DefaultListModel<IDevicePluginDefinition>> iconGroupMap = new HashMap<>();
    private final Map<String, Integer> mapObjectTypeCounts = new HashMap<>();
    private Cursor previousCursor;
    private IDevicePluginDefinition selectedDefinition;

    protected DraggableObjectPanel(String title)
    {
        super(title);

        initComponents();

        // Set up the model and renderer.
        comboBoxPanels.setModel(model);
        comboBoxPanels.setRenderer(new ComboBoxRenderer());

        objectsInUseGroup = title + OBJECTS_IN_USE_GROUP_SUFFIX;

        addButton.setIcon(UIResources.ADD_ACTION_BUTTON_ICON);
        addButton.putClientProperty(FlatClientProperties.MINIMUM_WIDTH, 0);
    }

    protected void setButtonTooltip(String tooltip)
    {
        addButton.setToolTipText(tooltip);
    }

    @RemoveObserver
    @Override
    public void onClearState(IProgressMonitor progressMonitor)
    {
        iconGroupMap.clear();
        mapObjectTypeCounts.clear();
    }

    /**
     * Handle a map object being added.
     *
     * @param event The event providing the added map object
     * @since 2.38 - formerly clientMapObjectAdded
     */
    private void onMapObjectAdded(@Observes MapObjectAdded event)
    {
        if (event.getMapObject().getPluginDefinition() instanceof IDevicePluginDefinition devicePluginDefinition)
        {
            addObjectToUsedPanel(devicePluginDefinition);
        }
    }

    /**
     * Handle a map object being removed.
     *
     * @param event The event providing the removed map object
     * @since 2.38 - formerly clientMapObjectRemoved
     */
    private void onMapObjectRemoved(@Observes MapObjectRemoved event)
    {
        removeObjectFromUsedPanel(event.getMapObject());
    }

    /**
     * Handle hot deployed plugins. If it is a device plugin, add its icon to our model.
     * @since 5.7
     */
    private void onPluginAdded(@Observes PluginAdded pluginAdded)
    {
        if (pluginAdded.pluginDefinition() instanceof IDevicePluginDefinition devicePluginDefinition)
        {
            addIcon(devicePluginDefinition.getGroup(), devicePluginDefinition);
        }
    }

    @InvokeOnEDT
    private void removeObjectFromUsedPanel(IMapObject mapObject)
    {
        String usedTypeName = mapObject.getType();

        mapObjectTypeCounts.merge(usedTypeName, 0, (count, ignored) -> count - 1);

        // If there are no uses of a specific type left, then remove it from the used panel
        if (mapObjectTypeCounts.get(usedTypeName) == 0)
        {
            final var listModel = iconGroupMap.get(objectsInUseGroup);
            if (listModel != null) listModel.removeElement(mapObject.getPluginDefinition());

            mapObjectTypeCounts.remove(usedTypeName);
        }
    }

    @InvokeOnEDT
    private void addObjectToUsedPanel(IDevicePluginDefinition definition)
    {
        final String type = definition.getType();

        if (!mapObjectTypeCounts.containsKey(type)) addIcon(objectsInUseGroup, definition);
        mapObjectTypeCounts.merge(type, 1, (count, ignored) -> count + 1);
    }

    /**
     * Used to filter out definitions that we don't want in the panel.
     *
     * @param definition the definition that we need to accept or reject from the panel
     * @return true if the definition will be a part of the panel and false if not
     */
    protected abstract boolean passesFilter(IDevicePluginDefinition definition);

    @Override
    public void onLoadState(IProgressMonitor progressMonitor)
    {
        if (iconGroupMap.size() <= 1)
        {
            devicePluginRegistry.getPluginDefinitions().forEach(definition -> addIcon(definition.getGroup(), definition));
        }

        addIconGroup(objectsInUseGroup); // Make sure Used panel appears when a new project is created

        mapObjectManager.getMapObjects().stream()
                .sorted(Comparator.comparing(IMapObject::getType))
                .map(IMapObject::getPluginDefinition)
                .filter(IDevicePluginDefinition.class::isInstance)
                .map(IDevicePluginDefinition.class::cast)
                .forEach(this::addObjectToUsedPanel);
    }

    @Override
    public void onShutdown(IProgressMonitor progressMonitor)
    {
        onClearState(progressMonitor);
    }

    private void addIconGroup(String groupName)
    {
        if (!iconGroupMap.containsKey(groupName))
        {
            constructNewIconGroup(groupName);
        }
    }

    private void addIcon(String groupName, IDevicePluginDefinition definition)
    {
        if (passesFilter(definition))
        {
            addIconGroup(groupName);
            iconGroupMap.get(groupName).addElement(definition);
        }
    }

    private void constructNewIconGroup(String groupName)
    {
        DefaultListModel<IDevicePluginDefinition> listModel = new DefaultListModel<>();
        iconGroupMap.put(groupName, listModel);

        final JList<IDevicePluginDefinition> iconList = new JList<>(listModel);

        iconList.setName(groupName);
        iconList.setTransferHandler(new IconTransferHandler(iconList));
        iconList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        iconList.setDragEnabled(true);
        iconList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        iconList.setVisibleRowCount(-1);
        iconList.addListSelectionListener(e -> {
            if (iconList.getSelectedIndex() >= 0)
            {
                selectedDefinition = iconList.getSelectedValue();
                addButton.setEnabled(true);
            } else
            {
                addButton.setEnabled(false);
            }
        });

        iconList.setCellRenderer(new IconsListCellRenderer());

        final var newIconTableScrollPane = new JScrollPane();
        newIconTableScrollPane.setName(groupName);
        newIconTableScrollPane.setViewportView(iconList);

        // Install the "hand" cursor while in this list
        iconList.addMouseListener(new iconListMouseHandler(iconList));

        // Force "Devices" to the top of the list.
        // Without this code, map objects were added in alphabetical order and their categories are added
        // to the list as needed. The problem was obfuscated because Acme was always included in our tests;
        // alphabetically, it was added first, so by default "Devices" was at the top of the list.
        if (groupName.equals(MapObjectGroupNames.DEVICES_GROUP))
        {
            model.insertElementAt(newIconTableScrollPane, 0);
        } else
        {
            model.addElement(newIconTableScrollPane);
        }

        panelComboAndButton.setVisible(model.getSize() > 1);
        model.setSelectedItem(model.getElementAt(0));
    }

    /**
     * Combo box selection changed.  We want to make sure that if a previous icon on a previous panel was selected
     * that the new icon panel updates the selectedDefinition for the NEWLY selected panel, and that we don't
     * keep the old selectedDefinition from the old panel.
     */
    private void comboBoxPanelsActionPerformed()
    {
        iconsPanel.removeAll();
        final var scrollPane = model.getElementAt(comboBoxPanels.getSelectedIndex());
        iconsPanel.add(scrollPane, cc.xy(1, 1));
        final var iconList = (JList<?>) scrollPane.getViewport().getView();
        selectedDefinition = (IDevicePluginDefinition) iconList.getSelectedValue();
        addButton.setEnabled(selectedDefinition != null);
        // since moving to docking we now need to repaint some "unknown" subcomponent...
        paintAll(getGraphics());
    }

    private void addButtonActionPerformed()
    {
        createSelectedObject();
    }

    /**
     * Create a new instance of the currently selected type to the center of the current globe view.
     *
     * @since 2.37
     */
    private void createSelectedObject()
    {
        if (selectedDefinition != null)
        {
            final ILocation centerLocation = globeManager.requireLastFocusedGlobe().getCenterLocation();

            if (selectedDefinition.getPluginCategory() == PluginCategory.DevicePlugin)
            {
                mapObjectManager.createDevice(selectedDefinition, centerLocation);
            } else
            {
                final MapObjectSpec spec = MapObjectSpec.builder(selectedDefinition).location(centerLocation).build();
                mapObjectManager.getOrCreateMapObject(spec);
            }
        }
    }

    private static class ComboBoxRenderer extends DefaultListCellRenderer
    {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
        {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            setText(((JComponent) value).getName());

            return this;
        }
    }

    /**
     * Deny any "drops" in the icons list, produce {@link ApplicationDataFlavor#pluginDefinitionFlavor} for any
     * icon dragged off the list.
     * @since 5.7
     */
    private static class IconTransferHandler extends TransferHandler
    {
        private final JList<IDevicePluginDefinition> iconList;

        public IconTransferHandler(JList<IDevicePluginDefinition> iconList)
        {
            this.iconList = iconList;
        }

        @Override
        public boolean canImport(TransferSupport support)
        {
            return false;
        }

        @Override
        public boolean importData(TransferSupport support)
        {
            return false;
        }

        @Override
        public int getSourceActions(JComponent c)
        {
            return COPY;
        }

        @Override
        protected Transferable createTransferable(JComponent component)
        {
            var definition = iconList.getSelectedValue();
            var transferable = new RaptorTransferable(ImageUtils.getScaledInstance(definition.getIcon(), ImageUtils.FEATURE_ICON_WIDTH));
            transferable.setTransferData(ApplicationDataFlavor.pluginDefinitionFlavor, definition);

            return transferable;
        }
    }

    /**
     * Handle the cursor and mouse clicks on the icons list.
     * @since 5.7
     */
    private class iconListMouseHandler extends MouseAdapter
    {
        private final JList<IDevicePluginDefinition> iconList;

        public iconListMouseHandler(JList<IDevicePluginDefinition> iconList)
        {
            this.iconList = iconList;
        }

        @Override
        public void mouseEntered(MouseEvent e)
        {
            previousCursor = iconList.getCursor();
            iconList.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        public void mouseExited(MouseEvent e)
        {
            if (previousCursor != null) iconList.setCursor(previousCursor);
        }

        @Override
        public void mouseClicked(MouseEvent e)
        {
            // Instantiate selected object on double click
            if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) createSelectedObject();
        }
    }

    /**
     * Renders a cell with the icon in the middle and the text centered below it.
     * @since 5.7
     */
    private static class IconsListCellRenderer extends DefaultListCellRenderer
    {
        private static final Border EMPTY_BORDER = BorderFactory.createEmptyBorder(5, 5, 5, 5);

        private final Map<Object, ImageIcon> cachedImageIcons = new HashMap<>();

        public IconsListCellRenderer()
        {
            setVerticalTextPosition(BOTTOM);
            setHorizontalTextPosition(CENTER);
            setHorizontalAlignment(CENTER);
            setBorder(EMPTY_BORDER);
        }

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
        {
            final var definition = (IDevicePluginDefinition) value;
            setText(definition.getType());

            // Get and cache the scaled icon to be rendered
            final var imageIcon = cachedImageIcons
                    .computeIfAbsent(value, k -> new ImageIcon(ImageUtils.getScaledInstance(definition.getIcon(), ImageUtils.FEATURE_ICON_WIDTH)));

            setIcon(imageIcon);
            setForeground(list.getForeground());
            return this;
        }
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JComboBox<JScrollPane> comboBoxPanels;
    protected JPanel panelComboAndButton;
    private JButton addButton;
    private JPanel iconsPanel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var panel1 = new JPanel();
        var label1 = new JLabel();
        comboBoxPanels = new JComboBox<>();
        panelComboAndButton = new JPanel();
        addButton = new JButton();
        iconsPanel = new JPanel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setPreferredSize(new Dimension(300, 400));
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "2*(default), fill:default:grow"));

        //======== panel1 ========
        {
            panel1.setName("panel1");
            panel1.setLayout(new FormLayout(
                    "default, $lcgap, default:grow",
                    "default"));

            //---- label1 ----
            label1.setText("Show:");
            label1.putClientProperty("FlatLaf.styleClass", "large");
            label1.setName("label1");
            panel1.add(label1, cc.xy(1, 1));

            //---- comboBoxPanels ----
            comboBoxPanels.setMaximumRowCount(12);
            comboBoxPanels.setFont(comboBoxPanels.getFont().deriveFont(comboBoxPanels.getFont().getStyle() | Font.BOLD, comboBoxPanels.getFont().getSize() + 2f));
            comboBoxPanels.setName("comboBoxPanels");
            comboBoxPanels.addActionListener(e -> comboBoxPanelsActionPerformed());
            panel1.add(comboBoxPanels, cc.xy(3, 1));
        }
        add(panel1, cc.xy(1, 1));

        //======== panelComboAndButton ========
        {
            panelComboAndButton.setName("panelComboAndButton");
            panelComboAndButton.setLayout(new FormLayout(
                    "2*(default, $lcgap), default:grow, $lcgap, default",
                    "default"));

            //---- addButton ----
            addButton.setText("Add");
            addButton.setEnabled(false);
            addButton.setName("addButton");
            addButton.addActionListener(e -> addButtonActionPerformed());
            panelComboAndButton.add(addButton, cc.xy(1, 1));
        }
        add(panelComboAndButton, cc.xy(1, 2));

        //======== iconsPanel ========
        {
            iconsPanel.setName("iconsPanel");
            iconsPanel.setLayout(new FormLayout(
                    "default:grow",
                    "fill:default:grow"));
        }
        add(iconsPanel, cc.xy(1, 3));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }
}
