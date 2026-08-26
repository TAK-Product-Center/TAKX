package gov.takx.commons.swing;

import gov.tak.api.annotation.IncubatingApi;
import gov.tak.api.annotation.Nullable;
import jakarta.annotation.Nonnull;

import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Utility methods for swing trees and tree models.
 *
 * @since 5.8
 */
@IncubatingApi(since = "5.8")
public class TreeUtils
{
    /**
     * @param treePath a tree path
     * @return a List containing all path components in the given tree path, or an empty List if the path was null
     * @param <T> the type of the path components
     */
    @SuppressWarnings("unused")
    @Nonnull
    public static <T> List<T> getPathComponents(@Nullable TreePath treePath)
    {
        return treePath == null ? Collections.emptyList() : getPathComponents(treePath, 0, treePath.getPathCount());
    }

    /**
     * @param treePath a tree path
     * @param start the start index within the path component array, inclusive
     * @return a List containing the path components between the start (inclusive) index and the end of the path
     * @param <T> the type of the path components
     */
    @SuppressWarnings("unused")
    @Nonnull
    public static <T> List<T> getPathComponents(@Nonnull TreePath treePath, int start)
    {
        return getPathComponents(treePath, start, treePath.getPathCount());
    }

    /**
     * @param treePath a tree path
     * @param start the start index within the path component array, inclusive
     * @param end the end index within the path component array, exclusive
     * @return a List containing the path components between the start (inclusive) and end (exclusive) indexes
     * @param <T> the type of the path components
     */
    @Nonnull
    public static <T> List<T> getPathComponents(@Nonnull TreePath treePath, int start, int end)
    {
        List<T> path = new ArrayList<>();
        for (var i = start; i < end; i++)
        {
            //noinspection unchecked
            path.add((T) treePath.getPathComponent(i));
        }
        return path;
    }

    /**
     * @param model a tree model
     * @param path a path within the tree model
     * @return a List of all child paths for the given path
     */
    @Nonnull
    public static List<TreePath> getChildPaths(@Nonnull TreeModel model, @Nonnull TreePath path)
    {
        var node = path.getLastPathComponent();
        return IntStream.range(0, model.getChildCount(node)).mapToObj(i -> path.pathByAddingChild(model.getChild(node, i))).toList();
    }

    /**
     * @param path a tree path
     * @return a List of all parent paths for the given path
     */
    @Nonnull
    public static List<TreePath> getParentPaths(@Nonnull TreePath path)
    {
        var parentPaths = new ArrayList<TreePath>();
        var parent = path.getParentPath();
        while (parent != null)
        {
            parentPaths.add(parent);
            parent = parent.getParentPath();
        }
        return parentPaths;
    }
}
