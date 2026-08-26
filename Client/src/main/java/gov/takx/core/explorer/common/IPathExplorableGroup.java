package gov.takx.core.explorer.common;

import gov.takx.api.explorer.IExplorable;
import gov.takx.api.explorer.IExplorableGroup;
import gov.takx.core.explorer.controller.IExplorerController;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * An extension of {@link IExplorableGroup} and {@link IControllerExplorable} that represents a simple path in the explorable tree.
 * These paths have no significant functionality other than grouping other explorables together in the tree.
 *
 * @since 5.3
 */
public interface IPathExplorableGroup extends IExplorableGroup, IControllerExplorable
{
    Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Attempt to find the hierarchy of {@link IPathExplorableGroup}s underneath this one to match the provided path list.
     * 
     * @param path the list of path elements
     * @return the path explorable found or created at the given path, or {@code null} if one was not found or created
     */
    @Nullable
    default IPathExplorableGroup getPath(@Nonnull List<String> path)
    {
        return (IPathExplorableGroup) getOrCreatePath(path, false, true);
    }

    /**
     * Ensures that the requested path exists, creating it if necessary.
     * 
     * @param path the path
     * @return the explorable found or created at the given path
     */
    @Nonnull
    default IPathExplorableGroup getOrCreatePath(@Nonnull List<String> path)
    {
        return (IPathExplorableGroup) getOrCreatePath(path, true);
    }

    /**
     * Ensures that the requested path exists, creating it if necessary.
     *
     * @param path the path
     * @param pathsOnly {@code true} if this method should only consider explorables of type {@link IPathExplorableGroup} in the search, otherwise {@code false}
     * @return the explorable found or created at the given path
     * @throws NullPointerException if the given path does not exist and could not be created
     */
    @Nonnull
    default IExplorable getOrCreatePath(@Nonnull List<String> path, boolean pathsOnly)
    {
        return Objects.requireNonNull(getOrCreatePath(path, true, pathsOnly), "Failed to create path: " + path);
    }

    /**
     * Attempt to find or optionally create a hierarchy of {@link IPathExplorableGroup}s underneath this one to match the provided path list.
     * 
     * @param path the list of path elements
     * @param create whether to force the creation
     * @param pathsOnly {@code true} if this method should only consider explorables of type {@link IPathExplorableGroup} in the search, otherwise {@code false}
     * @return the explorable found or created at the given path, or {@code null} if one was not found or created
     */
    @Nullable
    default IExplorable getOrCreatePath(@Nonnull List<String> path, boolean create, boolean pathsOnly)
    {
        return getOrCreatePath(this, path, getController(), create, pathsOnly);
    }

    /**
     * Attempt to find or optionally create a path as a child of this one.
     * 
     * @param pathName the name of the child path
     * @param create {@code true} to request the path be created, otherwise {@code false}
     * @param pathsOnly {@code true} if this method should only consider explorables of type {@link IPathExplorableGroup} in the search, otherwise {@code false}
     * @return the explorable found or created at the given path, or {@code null} if one was not found or created
     */
    @Nullable
    default IExplorable getOrCreatePath(@Nonnull String pathName, boolean create, boolean pathsOnly)
    {
        Stream<IExplorable> stream = getChildren().stream();
        if (pathsOnly) stream = stream.filter(IPathExplorableGroup.class::isInstance);
        return stream
                .filter(child -> child.getPathName().equals(pathName))
                .findFirst()
                .orElseGet(() -> create ? new PathExplorableGroup(getController(), this, pathName) : null);
    }

    /**
     * @param supportsVisibility {@code true} if this explorable should support visibility, otherwise {@code false}
     */
    void setSupportsVisibility(boolean supportsVisibility);

    /**
     * Attempt to find or optionally create a hierarchy of {@link IExplorable}s underneath this given root.
     *
     * @param parent the parent Explorable
     * @param path the path to find or create
     * @param controller the controller to use when creating new {@link IPathExplorableGroup} instances
     * @param create {@code true} to request the path be created, otherwise {@code false}
     * @param pathsOnly {@code true} if this method should only consider explorables of type {@link IPathExplorableGroup} in the search, otherwise {@code false}
     * @return the explorable found or created at the given path, or {@code null} if one was not found or created
     */
    static IExplorable getOrCreatePath(@Nonnull IExplorable parent, @Nonnull List<String> path, @Nullable IExplorerController controller, boolean create, boolean pathsOnly)
    {
        IExplorable currentParent = parent;
        for (String pathElement : path)
        {
            if (currentParent instanceof IPathExplorableGroup pathGroup)
            {
                currentParent = pathGroup.getOrCreatePath(pathElement, create, pathsOnly);
            } else if (!pathsOnly)
            {
                // Search for child
                IExplorable child = currentParent.getChildrenStream().filter(c -> pathElement.equals(c.getPathName())).findFirst().orElse(null);
                if (child == null && create && controller != null) child = new PathExplorableGroup(controller, currentParent, pathElement); // Automatically adds itself to parent
                currentParent = child;
            }
            if (currentParent == null) return null;
        }
        return currentParent;
    }
}
