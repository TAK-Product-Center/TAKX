package gov.tak.api.icons;

import gov.tak.api.annotation.IncubatingApi;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/**
 * Contract for a user icon set, which is defined by a zip file containing directories (groups) and image files.  Each
 * image file will be represented by a {@link IUserIcon}.  The zip file contains a manifest of sorts, named
 * {@code iconset.xml}, containing metadata about the icon set and the icons it contains.
 *
 * @since 5.2
 */
@NoPluginImplementations
@IncubatingApi(since = "5.2")
public interface IUserIconSet
{
    /**
     * @return {@code true} if required properties are present
     */
    boolean isValid();

    /**
     * @return Path to the icon set zip file
     */
    @Nonnull
    Path getIconSetPath();

    /**
     * @return The name of the icon set, as defined in the manifest
     */
    @Nonnull
    String getName();

    /**
     * @return The UID of the icon set, as defined in the manifest
     */
    @Nonnull
    String getUid();

    /**
     * @return The version of the icon set, as defined in the manifest
     */
    int getVersion();

    /**
     * @return A collection of all the icons defined in this icon set
     */
    @Nonnull
    List<IUserIcon> getIcons();

    /**
     * @return {@code true} if this icon set contains any icons
     */
    boolean hasIcons();

    /**
     * @return The default (or last selected) group within this icon set
     */
    @Nonnull
    String getSelectedGroup();

    /**
     * @param selectedGroup The selected group within this icon set
     */
    void setSelectedGroup(@Nonnull String selectedGroup);

    /**
     * @return The name of the icon to select by default for a "friendly" marker
     */
    @Nullable
    String getDefaultFriendly();

    /**
     * @return The name of the icon to select by default for a "hostile" marker
     */
    @Nullable
    String getDefaultHostile();

    /**
     * @return The name of the icon to select by default for a "neutral" marker
     */
    @Nullable
    String getDefaultNeutral();

    /**
     * @return The name of the icon to select by default for a "unknown" marker
     */
    @Nullable
    String getDefaultUnknown();

    /**
     * @return {@code true} if the image should not be resized to our standard size
     */
    boolean isSkipResize();

    /**
     * @return the {@link IUserIcon icon} with the given {@code filename}, {@code null} if not found
     */
    @Nullable
    IUserIcon getIcon(@Nonnull String filename);

    /**
     * @return {@code true} if an icon with the given file name is in this icon set
     */
    boolean hasIcon(@Nonnull String filename);

    /**
     * @return The known group names, sorted alphabetically.  May be empty, never {@code null}.
     */
    @Nonnull
    Collection<String> getGroups();

    /**
     * @param group Group of interest
     * @return All {@link IUserIcon icons} in the named group, or an empty list if the group is not found
     */
    @Nonnull
    List<IUserIcon> getIconsInGroup(@Nonnull String group);

    /**
     * Get the {@link IUserIcon icon} to use to represent a given group.
     *
     * @param group Name of the group
     * @return Icon for that group, {@code null} if the given {@code group} has no icon recorded for it
     */
    @Nullable
    IUserIcon getIconForGroup(@Nonnull String group);
}
