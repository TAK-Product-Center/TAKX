package gov.takx.core.view.gui.explorer.view;

import gov.takx.api.explorer.IExplorable;
import gov.takx.commons.swing.entity.EntityTreeModelEvent;
import gov.takx.core.explorer.common.IControllerExplorable;
import gov.takx.core.explorer.common.IPathExplorableGroup;
import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.HashSet;

/**
 * Manages and tracks empty paths within the explorer. Empty paths are essentially nodes within the explorer tree model that
 * only contain groups, and not individual items. For instance, when an {@link gov.takx.api.shapes.IRectangle} is created the
 * explorer places it under a series of path nodes determined by
 * {@link gov.takx.core.explorer.controller.path.ShapeMapObjectPathProvider}. e.g. {@code Shapes/Drawing Objects/Square}. If
 * all objects under that path are removed then we want to hide the entire path rather than show an empty tree to the user.
 * The path nodes themselves do not count towards the "empty" calculation, and thus we can hide entire portions of the tree
 * if there are no descendants worth displaying. The determination of what is "hideable" is made in
 * {@link #canHideInTree(IExplorable)}. Hiding, rather than outright deleting the path nodes, reduces issues with stale
 * references.
 *
 * @since 5.7
 */
public class EmptyPathManager
{
    private final Collection<IExplorable> explorableEmpty = new HashSet<>();
    private ExplorerTreeTableModel explorerTreeTableModel;

    /**
     * @param explorerTreeTableModel the explorer tree table model to use for child count calculation
     * @throws IllegalStateException if the model has already been set
     */
    public void init(@Nonnull ExplorerTreeTableModel explorerTreeTableModel)
    {
        if (this.explorerTreeTableModel != null) throw new IllegalStateException("Init has already been called");
        this.explorerTreeTableModel = explorerTreeTableModel;
        explorerTreeTableModel.addEntityTreeModelListener(this::modelChanged);
    }

    /**
     * @param explorable an explorable within this model
     * @return {@code true} if the explorable represents an empty path, otherwise {@code false}
     */
    public boolean isEmptyPath(@Nonnull IExplorable explorable)
    {
        return explorableEmpty.contains(explorable);
    }

    /**
     * Recalculates empty path flag whenever nodes are added or removed from the explorer tree.
     */
    private void modelChanged(EntityTreeModelEvent<IExplorable> event)
    {
        var object = event.entity();
        var child = event.child();
        switch (event.type())
        {
            case Insert ->
            {
                var emptyPath = calculateEmptyPath(child);
                if (emptyPath) explorableEmpty.add(child);
                rollUpEmptyPath(object);
            }
            case Remove ->
            {
                //noinspection DataFlowIssue child is always present for remove event
                if (explorerTreeTableModel.getParents(child).isEmpty()) explorableEmpty.remove(child);
                rollUpEmptyPath(object);
            }
        }
    }

    private void rollUpEmptyPath(IExplorable explorable)
    {
        var emptyPath = calculateEmptyPath(explorable);
        if (emptyPath != explorableEmpty.contains(explorable))
        {
            if (emptyPath)
            {
                explorableEmpty.add(explorable);
            } else
            {
                explorableEmpty.remove(explorable);
            }
            explorerTreeTableModel.getParents(explorable).forEach(this::rollUpEmptyPath);
        }
    }

    private boolean calculateEmptyPath(IExplorable explorable)
    {
        return ExplorerPanel.isTreeNodeType(explorable, explorerTreeTableModel) && canHideInTree(explorable) && explorableEmpty.containsAll(explorerTreeTableModel.getChildren(explorable));
    }

    private boolean canHideInTree(@Nonnull IExplorable explorable)
    {
        //noinspection ConditionCoveredByFurtherCondition - Leaving the covered condition in case the other needs to be removed
        return explorable instanceof IControllerExplorable && explorable instanceof IPathExplorableGroup;
    }
}
