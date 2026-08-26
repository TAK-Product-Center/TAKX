package gov.takx.core.view.gui.explorer.view;

import gov.takx.api.explorer.IExplorable;
import gov.takx.api.explorer.IExplorableGroup;
import gov.takx.commons.swing.entity.EntitySelectionEvent;
import gov.takx.commons.swing.entity.EntitySelectionListener;
import gov.takx.commons.swing.entity.EntityTreeTableSelectionModel;
import gov.takx.core.explorer.controller.IExplorerController;
import gov.takx.core.explorer.model.IExplorerModel;
import jakarta.annotation.Nonnull;

import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.TreePath;

import java.awt.event.MouseEvent;
import java.util.List;

/**
 * An extension of {@link ExplorerPanel} designed to allow the user to select from the explorable hierarchy via checkboxes.
 * This class configures elements of the parent class to hide most of the interactive features and expose checkboxes.
 *
 * @since 5.3
 */
public class SelectionExplorerPanel extends ExplorerPanel
{
    private final EntityTreeTableSelectionModel<IExplorable> selectionModel;
    private final EntitySelectionListener<IExplorable> tableSelectionListener = this::tableSelection;

    /**
     * Construct a new instance from the given model and controller.
     *
     * @param model the explorer model
     * @param controller the explorer controller
     */
    public SelectionExplorerPanel(@Nonnull IExplorerModel model, @Nonnull IExplorerController controller)
    {
        super(model, controller, true, true, false, false, false);
        selectionModel = new EntityTreeTableSelectionModel<>(baseExplorerModel);

        explorerTable.clearAutoHideColumns();
        explorerTable.getColumnExts().forEach(column -> column.setVisible(false));
        explorerTable.getSelectColumn().setVisible(true);
        explorerTable.setColumnsVisible(true, entityDefinition.getSummaryColumn(), entityDefinition.getIconColumn());
        explorerTable.moveColumn(explorerTable.getSelectColumn(), 0);
        explorerTree.getColumn(entityDefinition.getVisibilityRollUpColumn()).setVisible(false);

        showOrHideExplorable(controller.getGroupsRoot(), false);
        showOrHideExplorable(controller.getAlarmsController().getAlertsRoot(), false);
    }

    /**
     * Set the given {@code explorables} as selected. Useful for preconfiguring the user selections.
     *
     * @param explorables to select
     */
    public void setSelectedExplorables(List<IExplorable> explorables)
    {
        var tableSelectionModel = explorerTable.getEntitySelectionModel();
        tableSelectionModel.removeSelectionListener(tableSelectionListener);
        // Convert explorables to their respective paths
        var treePaths = explorables.stream()
                .map(explorable -> new TreePath(explorable.getExplorablePath().toArray()))
                .toArray(TreePath[]::new);
        selectionModel.setSelectionPaths(treePaths);
        applySelectionToTable();
        tableSelectionModel.addSelectionListener(tableSelectionListener);
    }

    /**
     * Returns the List of selected paths. The last item in each list is usually just an {@link IExplorable} and not an
     * {@link IExplorableGroup}.
     *
     * @return the List of paths selected
     * @since 5.7
     */
    @SuppressWarnings("unchecked")
    @Nonnull
    public List<List<IExplorable>> getSelectionPaths()
    {
        return (List<List<IExplorable>>) ((List<?>) selectionModel.getExpandedSelectionPaths());
    }

    /**
     * @param explorable explorable of interest.
     * @since 5.7
     */
    @Override
    public void showDialog(@Nonnull IExplorable explorable)
    {
        // Not supported in selection panel
    }

    @Override
    protected void explorerTablePopupTrigger(@Nonnull MouseEvent event)
    {
        // Not supported in selection panel
    }

    @Override
    protected void explorerTreePopupTrigger(@Nonnull MouseEvent event)
    {
        // Not supported in selection panel
    }

    /**
     * @param treeSelectionEvent selection event for the Explorer tree
     * @since 5.7
     */
    @Override
    protected void explorerTreeSelectionChanged(TreeSelectionEvent treeSelectionEvent)
    {
        selectionModel.setCurrentParentPath(treeSelectionEvent.getPath());
        var tableSelectionModel = explorerTable.getEntitySelectionModel();
        tableSelectionModel.removeSelectionListener(tableSelectionListener);
        tableSelectionModel.clearSelection();
        super.explorerTreeSelectionChanged(treeSelectionEvent);
        applySelectionToTable();
        tableSelectionModel.addSelectionListener(tableSelectionListener);
    }

    private void applySelectionToTable()
    {
        var tableSelectionModel = explorerTable.getEntitySelectionModel();
        var explorables = tableModel.getEntities();
        explorables.forEach(explorable -> tableSelectionModel.setSelectionState(explorable, selectionModel.getSelectionState(explorable)));
    }

    private void tableSelection(EntitySelectionEvent<IExplorable> event)
    {
        switch (event.getState())
        {
            case SELECTED -> event.getEntities().forEach(selectionModel::addSelection);
            case NOT_SELECTED -> event.getEntities().forEach(selectionModel::removeSelection);
        }
    }
}