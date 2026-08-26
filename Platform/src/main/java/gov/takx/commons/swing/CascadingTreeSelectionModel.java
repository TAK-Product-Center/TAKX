package gov.takx.commons.swing;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import java.util.HashSet;
import java.util.Set;

/**
 * An extension of {@link DefaultTreeSelectionModel} that cascades selections up and down the tree to better support tri-state indicators in the UI.
 * <p>
 * Definitions:
 * <ul>
 * <li>Fully selected is defined as a node that is either a selected leaf node, or a non-leaf node where all children are fully selected.</li>
 * <li>Unselected is defined as a leaf node that is not selected, or a non-leaf node that has no fully or partially selected children.</li>
 * <li>Partially selected is defined as a node whose children do not all have the same selected state. Leaf nodes cannot be partially selected.</li>
 * </ul>
 * Any node that is either fully or partially selected will return {@code true} from {@link #isPathSelected(TreePath)}.
 * This class essentially allows only leaf node selection. i.e. there is no difference between selecting a node and selecting all
 * of its children. Selecting a non-leaf node alone, without selecting its children, is currently impossible (though it is possible in theory to
 * support it in the future).
 *
 * @implNote This was partially derived from {@link CombiningTreeSelectionModel}, but the selection implications are different and thus required a new
 * class.
 * @since 5.8
 */
public class CascadingTreeSelectionModel extends DefaultTreeSelectionModel
{
    private final TreeModel model;
    private final Set<TreePath> partialSelections = new HashSet<>();

    /**
     * Construct a new selection model, using a tree model to determine full or partial selection state.
     *
     * @param model the TreeModel to use for determining full and selected states
     */
    public CascadingTreeSelectionModel(@Nonnull TreeModel model)
    {
        this.model = model;
        model.addTreeModelListener((TreeModelCombinedListener) this::treeNodesUpdated);
        setSelectionMode(DISCONTIGUOUS_TREE_SELECTION);
    }

    /**
     * Combines the full, empty, and partial selection tests and returns a single {@link Boolean} value that can represent
     * all three states.
     *
     * @param path the path to check for selection
     * @return {@code true} if the path is fully selected, {@code false} if the path is fully unselected, or {@code null} if
     * the path is partially selected
     */
    @Nullable
    public Boolean getSelectionState(@Nonnull TreePath path)
    {
        return getSelectionCount() == 0 ? Boolean.FALSE : partialSelections.contains(path) ? null : isPathSelected(path);
    }

    @Override
    public void clearSelection()
    {
        super.clearSelection();
        partialSelections.clear();
    }

    @Override
    public void setSelectionPaths(@Nonnull TreePath[] pPaths)
    {
        clearSelection();
        addSelectionPaths(pPaths);
    }

    @Override
    public void addSelectionPaths(@Nonnull TreePath[] paths)
    {
        for (var path : paths)
        {
            // Select path and all descendants
            addSelectionPathRollDown(path);
            // Select all ancestors, partially or fully
            TreeUtils.getParentPaths(path).forEach(parent -> {
                updatePartial(parent);
                super.addSelectionPaths(new TreePath[]{parent});
            });
        }
    }

    @Override
    public void removeSelectionPaths(@Nonnull TreePath[] paths)
    {
        for (var path : paths)
        {
            // Unselect path and all descendants
            removeSelectionPathRollDown(path);
            // Unselect parent if no siblings are selected
            TreeUtils.getParentPaths(path).forEach(parent -> {
                var state = updatePartial(parent);
                if (Boolean.FALSE.equals(state)) super.removeSelectionPaths(new TreePath[]{parent});
            });
        }
    }

    /**
     * Recalculate the partial state for a tree path.
     */
    private Boolean updatePartial(@Nonnull TreePath path)
    {
        var selected = false;
        var unselected = false;
        var partial = false;
        for (var child : TreeUtils.getChildPaths(model, path))
        {
            var childState = getSelectionState(child);
            if (childState == null)
            {
                partial = true;
                break;
            } else if (childState)
            {
                selected = true;
            } else
            {
                unselected = true;
            }
            if (selected && unselected)
            {
                partial = true;
                break;
            }
        }

        if (partial != partialSelections.contains(path))
        {
            if (partial)
            {
                partialSelections.add(path);
            } else
            {
                partialSelections.remove(path);
            }
        }
        return partial ? null : selected;
    }

    /**
     * Add selection path and select all descendants
     */
    private void addSelectionPathRollDown(@Nonnull TreePath path)
    {
        partialSelections.remove(path);
        super.addSelectionPaths(new TreePath[]{path});
        TreeUtils.getChildPaths(model, path).forEach(this::addSelectionPathRollDown);
    }

    /**
     * Add selection path and select all descendants
     */
    private void removeSelectionPathRollDown(@Nonnull TreePath path)
    {
        partialSelections.remove(path);
        super.removeSelectionPaths(new TreePath[]{path});
        TreeUtils.getChildPaths(model, path).forEach(this::removeSelectionPathRollDown);
    }

    /**
     * Handles adjusting the selection if the model changes. For {@link TreeModelCombinedListener.Type#Remove} events,
     * the removed paths are unselected. For {@link TreeModelCombinedListener.Type#Insert} events, the new children are
     * selected if the parent is fully selected. For {@link TreeModelCombinedListener.Type#Structure} events,
     * the path and all children are unselected.
     */
    private void treeNodesUpdated(@Nonnull TreeModelCombinedListener.CombinedTreeModelEvent event)
    {
        switch (event.getType())
        {
            case Remove ->
            {
                var parent = event.getTreePath();
                for (var child : event.getChildren()) removeSelectionPath(parent.pathByAddingChild(child));
            }
            case Insert ->
            {
                var parent = event.getTreePath();
                if (Boolean.TRUE.equals(getSelectionState(parent)))
                {
                    // Cascade selection to children
                    for (var child : event.getChildren()) addSelectionPath(parent.pathByAddingChild(child));
                }
            }
            // For a structure change we can't depend on any of the paths being the same, so we unselect everything
            case Structure -> removeSelectionPath(event.getTreePath());
        }
    }
}
