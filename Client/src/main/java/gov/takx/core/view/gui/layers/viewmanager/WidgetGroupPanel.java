package gov.takx.core.view.gui.layers.viewmanager;

import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.IWidget;
import gov.takx.api.globe.IWidgetLayerManager;
import gov.takx.core.globe.tak.WidgetLayerManager;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.event.Observes;
import org.jdesktop.swingx.JXTable;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A {@link AMapGroupPanel} implementation specifically for {@link IWidget} display in the Maps Panel
 *
 * @since 5.0
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class WidgetGroupPanel extends AMapGroupPanel<JXTable>
{
    /**
     * Creates a new {@link AMapGroupPanel} to display {@link IWidget}s in the Maps Panel
     * @param group The name of the Group to create
     * @param owner The panel in which to place this group
     * @param manager Unused in this class
     * @param widgetLayerManager The {@link WidgetLayerManager} used to get access to widget management
     */
    public WidgetGroupPanel(String group, LayerViewPanel owner, LayerViewManager manager, WidgetLayerManager widgetLayerManager)
    {
        super(group, owner, manager, new JXTable(), new WidgetTableModel(widgetLayerManager));

        // Must do these in reverse order since making a column invisible removes it from the column model
        groupTable.getColumnExt(LayerViewUtils.Column.FAVORITE.ordinal()).setVisible(false);
        groupTable.getColumnExt(LayerViewUtils.Column.STATUS.ordinal()).setVisible(false);
    }

    /**
     * Widget list does not support dragging of rows.
     */
    @Override
    public void setDragEnabled(boolean dragEnabled)
    {
        // Noop - we don't support dragging
    }

    /**
     * When a new widget is added, update our table.
     */
    private void onWidgetAdded(@Observes IWidgetLayerManager.WidgetInstanceAdded ignored)
    {
        rebuildTableModel();
    }

    /**
     * When a widget is removed, update our table.
     */
    private void onWidgetRemoved(@Observes IWidgetLayerManager.WidgetInstanceRemoved ignored)
    {
        rebuildTableModel();
    }

    /** @since 5.4 */
    @InvokeOnEDT
    private void rebuildTableModel()
    {
        ((WidgetTableModel) tableModel).refreshWidgetList();
    }

    /**
     * A version of the group table model which provides Widgets from the Widget Layer Manager.
     */
    private static class WidgetTableModel extends AMapGroupPanel.AGroupTableModel<IWidget>
    {
        private final WidgetLayerManager widgetLayerManager;
        private List<IWidget> widgets = Collections.emptyList();

        WidgetTableModel(@Nonnull WidgetLayerManager widgetLayerManager)
        {
            this.widgetLayerManager = widgetLayerManager;
        }

        @Override
        protected void setEntryVisible(int rowIndex, boolean visible)
        {
            widgetLayerManager.setWidgetVisibility(getRowObject(rowIndex).getClass(), visible);
        }

        @Override
        protected IWidget getRowObject(int rowIndex)
        {
            return widgets.get(rowIndex);
        }

        @Override
        public int getRowCount()
        {
            return widgets.size();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex)
        {
            final var widget = getRowObject(rowIndex);

            return switch (LayerViewUtils.Column.VALUES[columnIndex])
            {
                // Visible if ANY widget of this type is visible
                case VISIBLE -> widgetLayerManager.getWidgetsOfClass(widget.getClass()).stream().anyMatch(IWidget::isVisible);
                case TITLE -> widget.getName();
                case OBJECT -> widget;
                default -> null;
            };
        }

        /**
         * Reload our widget list, taken from the widgets on the primary globe.
         * @since 5.4
         */
        private void refreshWidgetList()
        {
            widgets = widgetLayerManager.getWidgetList().stream()
                    .filter(IWidget::showInWidgetList)
                    .sorted(Comparator.comparing(IWidget::getName))
                    .toList();

            fireTableDataChanged();
        }
    }
}
