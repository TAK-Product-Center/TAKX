package gov.takx.commons.swing;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * An extension of {@link DefaultTreeSelectionModel} that tracks tree selection in a more concise way. Nodes that are
 * "fully selected" (i.e. all descendants are selected) are reduced into a single selection path. Additionally,
 * allows the detection of "partially" selected nodes i.e. nodes that have only some of their descendants selected. This
 * better supports the usage of tri-state indicators in the UI. For usage see {@code gov.takx.core.view.gui.explorer.view.ExplorerModelManager}
 * in the client api.
 *
 * @implNote This implementation currently does not respond to {@link javax.swing.event.TreeModelEvent} events from the inner
 * {@link TreeModel}. Structural changes to the tree model after selections are made can produce invalid results.
 * @since 5.3
 */
public class CombiningTreeSelectionModel extends DefaultTreeSelectionModel
{
    private final TreeModel model;

    /**
     * Construct a new selection model, using a tree model to determine full or partial selection state.
     *
     * @param model the TreeModel to use for determining full and selected states
     */
    public CombiningTreeSelectionModel(TreeModel model)
    {
        this.model = model;
        setSelectionMode(DISCONTIGUOUS_TREE_SELECTION);
    }

    /**
     * Combines the full, empty, and partial selection tests and returns a single {@link Boolean} value that can represent
     * all three states.
     *
     * @param path the path to check for selection
     * @return {@code true} if the path is fully selected, {@code false} if the path is fully unselected, or {@code null} if
     * the path is partially selected
     * @since 5.7
     */
    @Nullable
    public Boolean getSelectionState(@Nonnull TreePath path)
    {
        if (isPathSelected(path, true)) return true;

        var selectionPaths = getSelectionPaths();
        if (selectionPaths == null) return false;

        for (var selectionPath : selectionPaths)
        {
            if (isDescendant(selectionPath, path)) return null;
        }
        return false;
    }

    /**
     * Returns the full list of selection paths, expanding any fully selected intermediate nodes into
     * lists containing all their sub paths.
     *
     * @return a List of all selected paths
     * @since 5.3
     */
    @Nonnull
    public List<List<Object>> getExpandedSelectionPaths()
    {
        List<List<Object>> allPaths = new ArrayList<>();
        for (TreePath path : getSelectionPaths())
        {
            List<Object> pathList = List.of(path.getPath());
            List<Object> parents = pathList.subList(0, pathList.size() - 1);
            Object last = path.getLastPathComponent();
            List<List<Object>> paths = expandAll(last);
            paths.forEach(l -> l.addAll(0, parents));
            allPaths.addAll(paths);
        }
        return allPaths;
    }

    /**
     * Expands a tree node into lists of all descendant paths.
     * @since 5.3
     */
    private List<List<Object>> expandAll(Object last)
    {
        List<List<Object>> paths = new ArrayList<>();
        int children = model.getChildCount(last);
        if (children == 0)
        {
            paths.add(new ArrayList<>(List.of(last)));
        } else
        {
            for (int i = 0; i < children; i++)
            {
                Object child = model.getChild(last, i);
                List<List<Object>> childPaths = expandAll(child);
                childPaths.forEach(l -> l.add(0, last));
                paths.addAll(childPaths);
            }
        }
        return paths;
    }

    /**
     * Tests whether there is any unselected node in the subtree of the given path
     *
     * @param path tree path
     * @return true if any unselected node
     */
    public boolean isPartiallySelected(@Nonnull TreePath path)
    {
        if (isPathSelected(path, true))
        {
            return false;
        }
        TreePath[] selectionPaths = getSelectionPaths();
        if (selectionPaths == null)
        {
            return false;
        }
        for (TreePath selectionPath : selectionPaths)
        {
            if (isDescendant(selectionPath, path))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Tells whether a given path is selected.  If dig = true, then a path is assumed
     * to be selected if one of its ancestors is selected.
     *
     * @param path tree path
     * @param dig  use ancestors
     * @return true if path is selected
     */
    public boolean isPathSelected(@Nonnull TreePath path, boolean dig)
    {
        if (!dig)
        {
            return super.isPathSelected(path);
        }
        while (path != null && !super.isPathSelected(path))
        {
            path = path.getParentPath();
        }
        return path != null;
    }

    /**
     * Determine if path1 is descendant of path2
     *
     * @param path1 tree path
     * @param path2 tree path
     * @return true if path1 is descendant of path2
     */
    private boolean isDescendant(TreePath path1, TreePath path2)
    {
        Object[] obj1 = path1.getPath();
        Object[] obj2 = path2.getPath();
        if (obj1.length <= obj2.length) return false;
        for (int i = 0; i < obj2.length; i++)
        {
            if (obj1[i] != obj2[i])
            {
                return false;
            }
        }
        return true;
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
        // unselect all descendants of paths[]
        List<TreePath> toBeRemoved = new ArrayList<>();
        for (TreePath path : paths)
        {
            TreePath[] selectionPaths = getSelectionPaths();
            if (selectionPaths == null)
            {
                break;
            }
            for (TreePath selectionPath : selectionPaths)
            {
                if (isDescendant(selectionPath, path))
                {
                    toBeRemoved.add(selectionPath);
                }
            }
            super.removeSelectionPaths(toBeRemoved.toArray(new TreePath[0]));
            toBeRemoved.clear();
        }

        // If all siblings are selected then unselect them and select parent recursively; otherwise just select that path
        for (TreePath path1 : paths)
        {
            TreePath path = path1;
            TreePath temp = null;
            while (areSiblingsSelected(path))
            {
                temp = path;
                if (path.getParentPath() == null)
                {
                    break;
                }
                path = path.getParentPath();
            }
            if (temp != null)
            {
                if (temp.getParentPath() != null)
                {
                    addSelectionPath(temp.getParentPath());
                } else
                {
                    if (!isSelectionEmpty())
                    {
                        removeSelectionPaths(getSelectionPaths());
                    }
                    super.addSelectionPaths(new TreePath[]{temp});
                }
            } else
            {
                super.addSelectionPaths(new TreePath[]{path});
            }
        }
    }

    /**
     * Tests whether all siblings of a given path are selected
     *
     * @param path tree path
     * @return true if all selected
     */
    private boolean areSiblingsSelected(TreePath path)
    {
        TreePath parent = path.getParentPath();
        if (parent == null)
        {
            return true;
        }
        Object node = path.getLastPathComponent();
        Object parentNode = parent.getLastPathComponent();

        int childCount = model.getChildCount(parentNode);
        for (int i = 0; i < childCount; i++)
        {
            Object childNode = model.getChild(parentNode, i);
            if (childNode == node)
            {
                continue;
            }
            if (!isPathSelected(parent.pathByAddingChild(childNode)))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public void removeSelectionPaths(@Nonnull TreePath[] paths)
    {
        for (TreePath path : paths)
        {
            if (path.getPathCount() == 1)
            {
                super.removeSelectionPaths(new TreePath[]{path});
            } else
            {
                toggleRemoveSelection(path);
            }
        }
    }

    /**
     * if any ancestor node of given path is selected then unselect it
     * and selection all its descendants except given path and descendants.
     * otherwise just unselect the given path
     *
     * @param path tree path
     */
    private void toggleRemoveSelection(TreePath path)
    {
        Stack<TreePath> stack = new Stack<>();
        TreePath parent = path.getParentPath();
        while (parent != null && !isPathSelected(parent))
        {
            stack.push(parent);
            parent = parent.getParentPath();
        }
        if (parent != null)
        {
            stack.push(parent);
        } else
        {
            super.removeSelectionPaths(new TreePath[]{path});
            return;
        }

        while (!stack.isEmpty())
        {
            TreePath temp = stack.pop();
            TreePath peekPath = stack.isEmpty() ? path : stack.peek();
            Object node = temp.getLastPathComponent();
            Object peekNode = peekPath.getLastPathComponent();
            int childCount = model.getChildCount(node);
            for (int i = 0; i < childCount; i++)
            {
                Object childNode = model.getChild(node, i);
                if (childNode != peekNode)
                {
                    super.addSelectionPaths(new TreePath[]{temp.pathByAddingChild(childNode)});
                }
            }
        }
        super.removeSelectionPaths(new TreePath[]{parent});
    }
}