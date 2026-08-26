package gov.takx.core.view.gui.layers.viewmanager;

import gov.takx.api.layer.IRaptorLayer;
import org.jdesktop.swingx.JXTable;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A {@link AMapGroupPanel} implementation specifically for {@link IRaptorLayer} display in the Maps Panel
 */
public class LayerGroupPanel extends AMapGroupPanel<LayerGroupPanel.LayerGroupTable> implements LayerTableUI.IPopupListener
{
    private final Map<IRaptorLayer, ColorAnimator> animatorMap = new ConcurrentHashMap<>();

    protected boolean dragEnabled;

    public LayerGroupPanel(String group, LayerViewPanel owner, LayerViewManager manager)
    {
        this(group, owner, manager, new LayerTableModel(group));
    }

    public LayerGroupPanel(final String group, final LayerViewPanel owner, final LayerViewManager manager, AbstractTableModel tableModel)
    {
        super(group, owner, manager, new LayerGroupTable(manager), tableModel);

        groupTable.installUI(group, this);

        // Custom title rendering to show animated colors
        groupTable.getColumnModel().getColumn(LayerViewUtils.Column.TITLE.ordinal()).setCellRenderer(new LayerTitleCellRenderer());

        // The selection behavior is unusual - most of the time it acts as a single selection model.  But when key
        // modifiers CTRL or SHIFT are used, it acts like a multiple selection interval model.  This emulates most
        // of the behavior of the original LayerTable.  This applies both to mouse and key presses.
        groupTable.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                updateSelectionMode(e);
                updateFavorite(e.getPoint());
                toggleBoundsLayer(e.getPoint());
                manager.setSelectedLayers(getSelectedLayers()); // CORE-3942
            }
        });

        groupTable.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyPressed(KeyEvent e)
            {
                updateSelectionMode(e);
            }
        });

        groupTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting())
            {
                owner.setSelectedGroup(group);
            } else
            {
                manager.setSelectedLayers(getSelectedLayers());
            }
        });
        refreshTable();
    }

    @Override
    protected void filterChanged()
    {
        updateDragEnabled();
    }

    @Override
    public void setDragEnabled(boolean dragEnabled)
    {
        this.dragEnabled = dragEnabled;
        updateDragEnabled();
    }

    /**
     * Update the layer table UI's draggability.
     */
    protected void updateDragEnabled()
    {
        // Drag is enabled only when the search filter is NOT enabled
        groupTable.getUI().setDragEnabled(dragEnabled && !filterEnabled);
        groupTable.repaint();
    }

    public void layerAdded(IRaptorLayer addedLayer)
    {
        ColorAnimator animator = animatorMap.get(addedLayer);
        if (animator != null) animator.stop();
        animator = new ColorAnimator(groupTable, Color.GREEN, Color.WHITE, 400, 8000);
        animatorMap.put(addedLayer, animator);
        refreshTable();
    }

    /**
     * Clear out the search pattern, animators and layer selection.
     */
    @Override
    public void clear()
    {
        super.clear();
        animatorMap.clear();
    }

    /**
     * Implementation of the LayerTableUI popup listener interface method.  Display the layer popup menu based upon
     * the selected layer.
     */
    @Override
    public void showPopup(MouseEvent e)
    {
        new LayerPopupMenu().show(this, getSelectedLayer(), e.getPoint());
    }

    /**
     * Get selected layer(s) in the layer table.
     */
    protected List<IRaptorLayer> getSelectedLayers()
    {
        List<IRaptorLayer> selected = new ArrayList<>();
        for (int row : groupTable.getSelectedRows())
        {
            selected.add((IRaptorLayer) tableModel.getValueAt(row, LayerViewUtils.Column.OBJECT.ordinal()));
        }
        return selected;
    }

    /**
     * Get the (first) selected layer.
     */
    protected IRaptorLayer getSelectedLayer()
    {
        return layerForRow(groupTable.getSelectedRow());
    }

    /**
     * Get the {@link IRaptorLayer} under a given point on the table.
     */
    protected IRaptorLayer layerForPoint(Point p)
    {
        return layerForRow(rowForPoint(p));
    }

    /**
     * Get the {@link IRaptorLayer} for a given row.
     */
    protected IRaptorLayer layerForRow(int row)
    {
        return row < 0 ? null : manager.getLayer(group, row);
    }

    /**
     * Get the current label color for a layer.  If an enabled color animator is available for the layer, then this
     * will return the color animator's current color (based upon time).  If there is no enabled color animator for the
     * layer, then {@code null} is returned.
     */
    private Color getAnimatedColor(IRaptorLayer layer)
    {
        ColorAnimator animator = animatorMap.get(layer);
        if (animator != null)
        {
            if (!animator.hasStarted()) animator.start();

            if (animator.hasNext())
            {
                return animator.next();
            } else
            {
                animator.stop();
                animatorMap.remove(layer);
            }
        }
        return null;
    }

    /**
     * Enable/disable a layer as favorite when the 'star' icon is clicked.
     */
    private void updateFavorite(Point mouseLocation)
    {
        int row = rowForPoint(mouseLocation);
        int col = groupTable.columnAtPoint(mouseLocation);
        if (row < 0 || col != LayerViewUtils.Column.FAVORITE.ordinal()) return;

        Object o = tableModel.getValueAt(row, LayerViewUtils.Column.OBJECT.ordinal());
        if (o instanceof IRaptorLayer layer)
        {
            manager.setFavorite(layer, !manager.isFavorite(layer));
            groupTable.repaint();
        }
    }

    /**
     * Enable/disable drawing the bounding box for a layer when the "square" button is clicked
     */
    private void toggleBoundsLayer(Point mouseLocation)
    {
        int row = rowForPoint(mouseLocation);
        int col = groupTable.columnAtPoint(mouseLocation);
        if (row < 0 || col != LayerViewUtils.Column.BOUNDING_BOX.ordinal()) return;

        Object o = tableModel.getValueAt(row, LayerViewUtils.Column.OBJECT.ordinal());
        if (o instanceof IRaptorLayer layer)
        {
            manager.toggleBoundsLayer(layer);
            groupTable.repaint();
        }
    }

    /**
     * Update the table selection mode.  If CTRL or SHIFT is held down during a mouse or key press, then multiple
     * interval selection is enabled.  Otherwise, only single selection is enabled.
     *
     * @param e input event (either mouse or key event).
     */
    private void updateSelectionMode(InputEvent e)
    {
        ListSelectionModel model = groupTable.getSelectionModel();
        int currentSelectionMode = model.getSelectionMode();
        int mod = e.getModifiersEx();

        // If the selection mode must be changed, try not to clear out the current selection.
        if ((mod & InputEvent.CTRL_DOWN_MASK) != 0 || (mod & InputEvent.SHIFT_DOWN_MASK) != 0)
        {
            if (currentSelectionMode != ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
            {
                model.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            }
        } else
        {
            if (currentSelectionMode != ListSelectionModel.SINGLE_SELECTION)
            {
                model.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            }
        }
    }

    protected static class LayerGroupTable extends JXTable
    {
        private final LayerViewManager manager;

        protected LayerGroupPanel layerGroupPanel;

        public LayerGroupTable(LayerViewManager manager)
        {
            this.manager = manager;
        }

        /**
         * Create and install our custom UI.
         *
         * @param group The layer group being handled
         * @param layerGroupPanel The panel containing this table
         * @see LayerTableUI
         */
        void installUI(String group, LayerGroupPanel layerGroupPanel)
        {
            this.layerGroupPanel = layerGroupPanel;

            final LayerTableUI layerTableUI = new LayerTableUI(manager, group);
            layerTableUI.setPopupListener(layerGroupPanel);
            layerTableUI.setDragEnabled(true);

            setUI(layerTableUI);
        }

        @Override
        public String getToolTipText(MouseEvent event)
        {
            return manager.getLayerTooltip(layerGroupPanel.layerForPoint(event.getPoint()));
        }

        @Override
        public Point getToolTipLocation(MouseEvent event)
        {
            final Point point = event.getPoint();
            int row = rowAtPoint(point);
            if (row < 0) return point;
            return new Point(getWidth() - 2, row * getRowHeight());
        }

        /**
         * @return Our UI, properly typed
         */
        @Override
        public LayerTableUI getUI()
        {
            return (LayerTableUI) super.getUI();
        }
    }

    private class LayerTitleCellRenderer extends DefaultTableCellRenderer
    {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused, int row, int column)
        {
            super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            if (value == null) return this;

            var layer = (IRaptorLayer) table.getModel().getValueAt(row, LayerViewUtils.Column.OBJECT.ordinal());
            setIcon(layer.getSmallIcon());
            Color c = getAnimatedColor(layer);
            if (c != null) setText("<html><b><font color=" + colorToHex(c) + ">" + value + "</font></b></html>");
            return this;
        }

        private String colorToHex(Color c)
        {
            return String.format("#%06x", c.getRGB() & 0x00FFFFFF);
        }
    }
}
