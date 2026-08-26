package gov.takx.api.explorer;

import gov.tak.api.annotation.DeprecatedApi;
import gov.takx.api.annotation.Experimental;
import gov.takx.api.annotation.NoPluginImplementations;

import java.util.ArrayList;
import java.util.List;

/**
 * Interface for explorable groups.
 *
 * @implNote We need to come up with an explicit way to declare that an explorable should be considered a group, even if it
 * has no children. That may very well involve un-deprecating this, but that would also mean converting it to more of a
 * tagging interface.
 * @since 2.38 originally created as a core capability in client in  1.48
 * @deprecated Group functionality is being merged into the base {@link IExplorable} interface
 */
@NoPluginImplementations
@Experimental
@Deprecated(since = "5.3", forRemoval = true)
@DeprecatedApi(since = "5.3", forRemoval = true, removeAt = "5.6")
public interface IExplorableGroup extends IExplorable
{
    /**
     * Get the full explorer path for this group.
     *
     * @return the full explorer path for this group.
     * @deprecated Use {@link IExplorable#getExplorablePath()} to get the actual path of this explorable
     */
    default List<String> getFullPath()
    {
        List<String> fullPath = new ArrayList<>(getParentPath());
        fullPath.add(getDisplayName());
        return fullPath;
    }

    /**
     * Determine whether this is a rules-based group.
     *
     * @return {@code true} if this is a rules-based group, or {@code false} otherwise.
     */
    default boolean isRuleGroup()
    {
        return false;
    }

    /**
     * This implementation always returns {@code true}, since anything extending {@link IExplorableGroup} is a group.
     *
     * @return @return {@code true}, since all {@link IExplorableGroup} implementations are groups
     * @since 5.3
     */
    @Override
    default boolean isGroup()
    {
        return true;
    }
}
