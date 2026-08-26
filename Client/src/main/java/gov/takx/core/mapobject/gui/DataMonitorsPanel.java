package gov.takx.core.mapobject.gui;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.mapobject.IDataMonitor;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.platform.IDataMonitorServices;
import gov.takx.api.plugin.datamonitor.DataMonitorEvent;
import gov.takx.api.plugin.datamonitor.DataMonitorEventType;
import gov.takx.api.ui.IResourceCleaner;
import jakarta.annotation.Nonnull;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Panel to display the data monitor's state and value for a Map Object.
 *
 */
public class DataMonitorsPanel extends JPanel implements IResourceCleaner, PropertyChangeListener
{
    private final DataMonitorTreeNode root = new DataMonitorTreeNode("root");
    private final DefaultTreeModel model = new DefaultTreeModel(root);
    private final Map<IDataMonitor, DataMonitorTreeNode> monitorToNodeMap = new HashMap<>();
    private Comparator<IDataMonitor> comparator;
    private IMapObjectController controller;

    /**
     * Create a new empty panel.
     *
     * @since 5.8
     */
    public DataMonitorsPanel()
    {
        initComponents();
        treeDataMonitors.addMouseListener(new MouseListener());
        treeDataMonitors.setToggleClickCount(0);
        treeDataMonitors.setModel(model);
        treeDataMonitors.setCellRenderer(new DataMonitorTreeNodeRenderer());
    }

    /**
     * @param mapObject the map object to display data monitors for
     */
    public void setMapObject(@Nonnull IMapObject mapObject)
    {
        controller = mapObject.getController();
        comparator = controller.getComparator();
        controller.addPropertyChangeListener(this); // Listen for addition/removal of data monitors on this MOC
        controller.getDataMonitors().forEach(this::addDataMonitor);
    }

    @Override
    public void releaseResources()
    {
        if (controller != null) controller.removePropertyChangeListener(this);
        clear();
    }

    @SuppressWarnings("unchecked")
    @InvokeOnEDT
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        final String propertyName = evt.getPropertyName();

        switch (propertyName)
        {
            case IDataMonitorServices.DATA_MONITOR_ADDED -> addDataMonitor((IDataMonitor) evt.getNewValue());
            case IDataMonitorServices.DATA_MONITOR_REMOVED -> removeDataMonitor((IDataMonitor) evt.getOldValue());
            case IDataMonitorServices.DATA_MONITOR_SORT_ORDER_CHANGED -> reSort((Comparator<IDataMonitor>) evt.getNewValue());
            case IDataMonitor.STATE_CHANGED, IDataMonitor.MUTE -> updateDataMonitor((IDataMonitor) evt.getSource(), false);
            case IDataMonitor.VALUE_CHANGED -> updateDataMonitor((IDataMonitor) evt.getSource(), true);
            default ->
                {
                }
        }

        repaint();
    }

    /**
     * Update our comparator and rebuild the model using the new sort order.
     *
     * @param comparator Comparator to install
     * @since 2.37
     */
    private void reSort(Comparator<IDataMonitor> comparator)
    {
        this.comparator = comparator;
        final ArrayList<IDataMonitor> dataMonitors = new ArrayList<>(monitorToNodeMap.keySet());

        clear();

        // Just add them all back and they will sort properly
        dataMonitors.stream()
                .filter(dm -> dm.getParent() == null)
                .forEach(dataMonitor -> addDataMonitorToTree(root, dataMonitor));
    }

    private void clear()
    {
        root.removeAllChildren();
        monitorToNodeMap.clear();
        model.reload();
    }

    private void addDataMonitor(IDataMonitor dataMonitor)
    {
        if (dataMonitor.getParent() == null) addDataMonitorToTree(root, dataMonitor);
    }

    /**
     * Add a data monitor to our tree.  Recursively add any child data monitors.
     *
     * @param parent Tree node in which to add the data monitor
     * @param dm     Data monitor to add
     * @since 2.37
     */
    private void addDataMonitorToTree(DataMonitorTreeNode parent, IDataMonitor dm)
    {
        DataMonitorTreeNode newNode = new DataMonitorTreeNode(dm);
        monitorToNodeMap.put(dm, newNode);

        List<IDataMonitor> siblings = new ArrayList<>();
        siblings.add(newNode.dataMonitor);

        if (parent.getChildCount() > 0)
        {
            DataMonitorTreeNode nextSibling = (DataMonitorTreeNode) parent.getFirstChild();

            while (nextSibling != null)
            {
                siblings.add(nextSibling.dataMonitor);
                nextSibling = (DataMonitorTreeNode) nextSibling.getNextSibling();
            }

            siblings.sort(comparator);
        }

        model.insertNodeInto(newNode, parent, siblings.indexOf(newNode.dataMonitor));

        for (IDataMonitor next : dm.getChildren())
        {
            addDataMonitorToTree(newNode, next);
            treeDataMonitors.setShowsRootHandles(true);
        }

        model.reload();
    }

    private void removeDataMonitor(IDataMonitor dm)
    {
        DataMonitorTreeNode node = monitorToNodeMap.remove(dm);

        if (node != null) model.removeNodeFromParent(node);
    }

    private void updateDataMonitor(IDataMonitor dm, boolean isValue)
    {
        DataMonitorTreeNode node = monitorToNodeMap.get(dm);

        if (node != null)
        {
            if (isValue)
            {
                node.statusPanel.updateValue(dm.getValue());
            } else
            {
                node.statusPanel.updateStatus(dm, dm.getCurrentState(), dm.getUnclearedAlarmState());
            }

            // Manually notify the model that the node needs
            // to be reloaded so that the sizing is revalidated.
            model.reload(node);
        }
    }

    private class MouseListener extends MouseAdapter
    {
        @Override
        public void mouseReleased(MouseEvent e)
        {
            TreePath path = treeDataMonitors.getPathForLocation(e.getX(), e.getY());

            // Make sure the click was on a node and not on a handle
            // (the little plus/minus signs in the tree)
            if (path != null)
            {
                DataMonitorTreeNode node = (DataMonitorTreeNode) path.getLastPathComponent();
                IDataMonitor dataMonitor = node.getDataMonitor();

                if (dataMonitor != null)
                {
                    DataMonitorEventType eventType;

                    // Right-click
                    if (MouseEvent.BUTTON3 == e.getButton())
                    {
                        eventType = DataMonitorEventType.RightClick;
                    } else if (e.getClickCount() == 2)
                    {
                        eventType = DataMonitorEventType.DoubleClick;
                    } else
                    {
                        eventType = DataMonitorEventType.Click;
                    }

                    dataMonitor.onDataMonitorEvent(
                            new DataMonitorEvent(this, eventType, Map.of(IDataMonitor.MOUSE_EVENT, e)));
                }
            }
        }
    }

    private static class DataMonitorTreeNode extends DefaultMutableTreeNode
    {
        private StatusPanel statusPanel;
        private IDataMonitor dataMonitor;

        public DataMonitorTreeNode(String name)
        {
            super(name, true);
        }

        public DataMonitorTreeNode(IDataMonitor dm)
        {
            statusPanel = new StatusPanel(dm);
            dataMonitor = dm;
        }

        public IDataMonitor getDataMonitor()
        {
            return dataMonitor;
        }

        public JPanel getPanel()
        {
            return statusPanel;
        }
    }

    private static class DataMonitorTreeNodeRenderer extends DefaultTreeCellRenderer
    {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus)
        {
            JPanel panel = ((DataMonitorTreeNode) value).getPanel();

            if (panel == null) panel = new JPanel();

            return panel;
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var scrollPane1 = new JScrollPane();
        treeDataMonitors = new JTree();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "default"));

        //======== scrollPane1 ========
        {
            scrollPane1.setName("scrollPane1");

            //---- treeDataMonitors ----
            treeDataMonitors.setToggleClickCount(0);
            treeDataMonitors.setShowsRootHandles(false);
            treeDataMonitors.setRootVisible(false);
            treeDataMonitors.setVisibleRowCount(3);
            treeDataMonitors.setName("treeDataMonitors");
            scrollPane1.setViewportView(treeDataMonitors);
        }
        add(scrollPane1, cc.xy(1, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JTree treeDataMonitors;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
