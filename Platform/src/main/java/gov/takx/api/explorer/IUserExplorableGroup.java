package gov.takx.api.explorer;

import gov.tak.api.annotation.IncubatingApi;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;

/**
 * Extension of {@link IExplorableGroup} that represents groups that are primarily, though not always, created and configured by users. This interface
 * is currently necessary to ensure these explorables are grouped together in the explorer tree.
 *
 * @since 5.3
 */
@IncubatingApi(since = "5.3")
@NoPluginImplementations
public interface IUserExplorableGroup extends IExplorableGroup
{
    /**
     * @param displayName the display name for this explorable group
     * @since 5.6
     */
    void setDisplayName(@Nonnull String displayName);

    /**
     * @return {@code true} if this group can be persisted, otherwise {@code false}
     * @since 5.8
     */
    boolean isPersist();

    /**
     * @param persist {@code true} if the group should be persisted, otherwise {@code false}
     * @since 5.8
     */
    void setPersist(boolean persist);

    /**
     * @return {@code true} if this group can be edited, otherwise {@code false}
     * @since 5.8
     */
    boolean isEditable();

    /**
     * @param editable {@code true} if the group should be editable, otherwise {@code false}
     * @since 5.8
     */
    void setEditable(boolean editable);
}