package gov.tak.api.icons;

import gov.tak.api.annotation.IncubatingApi;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.plugin.synthetic.ISyntheticPluginDefinition;
import gov.takx.commons.swing.ImageUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.List;

/**
 * Contract for service managing custom Icon Sets.
 *
 * @since 3.1
 */
@IncubatingApi(since = "3.1")
@InjectableService
@NoPluginImplementations
public interface IIconSetManager
{
    /**
     * The UID of the legacy TAKX iconset.  This iconset replaces the icons previously in the
     * {@code resources/MapIcons} directory.
     *
     * @since 5.2
     */
    String TAKX_LEGACY_ICONSET_UID = "TAKX Legacy";

    /**
     * The UID for the Vehicle Models icon set.
     * @since 5.8
     */
    String VEHICLE_MODELS_UID = "Vehicle Models";

    /**
     * @return All discovered {@link IUserIconSet icon sets}
     * @since 5.2
     */
    @Nonnull
    List<IUserIconSet> getAllIconSets();

    /**
     * Get an {@link IUserIconSet icon set} given its UID.
     * 
     * @param uid UID of icon set to find
     * @return The identified icon set, or {@code null} if not found
     * @since 5.2
     */
    @Nullable
    IUserIconSet getIconSetByUid(@Nonnull String uid);

    /**
     * Get an {@link IUserIconSet icon set} given its name.
     *
     * @param name Name of icon set to find
     * @return The named icon set, or {@code null} if not found
     * @since 5.2
     */
    @Nullable
    IUserIconSet getIconSetByName(@Nonnull String name);

    /**
     * Get the named icon from the icon set with the given UID.
     *
     * @param iconSetUid UID of icon set containing the icon
     * @param iconName Name of icon to retrieve
     * @return user icon, {@code null} if not found
     */
    @Nullable
    IUserIcon getIcon(@Nonnull String iconSetUid, @Nonnull String iconName);

    /**
     * Get the {@link IUserIcon icon} for the given URI.  If the URI doesn't match the expected structure, or the icon
     * set UID isn't known, or the icon name isn't known within the identified icon set, then {@code null} is returned.
     *
     * @param uri URI for icon of interest
     * @return Icon or {@code null} if not found
     * @see IUserIcon#toUri()
     * @since 5.2
     */
    @Nullable
    IUserIcon getIcon(URI uri);

    /**
     * Get the {@link IUserIcon icon} for the given {@code path}.  The path is assumed to be the full path, similar
     * to the URI, but not URL encoded.  This supports finding a user icon given a path passed via CoT.
     * If the path doesn't match the expected structure, or the icon set UID isn't known, or the icon name isn't known
     * within the identified icon set, then {@code null} is returned.
     *
     * @param iconSetPath Path of icon to find
     * @return Icon or {@code null} if not found
     * @since 5.3
     */
    @Nullable
    IUserIcon getIconFromPath(@Nonnull String iconSetPath);

    /**
     * Get the image for the named icon from the icon set with the given UID.
     *
     * @param iconSetUid UID of icon set containing the icon
     * @param iconName Name of icon to retrieve
     * @return icon image, {@code null} if not found
     * @since 5.2
     */
    @Nullable
    BufferedImage getIconImage(@Nonnull String iconSetUid, @Nonnull String iconName);

    /**
     * Get the image for the given {@link IUserIcon icon}.
     *
     * @param userIcon User icon for which to get the image
     * @return icon image, of {@link ImageUtils#getDefaultImage()} if the image could not be read
     * @since 5.2
     */
    @Nonnull
    BufferedImage getIconImage(@Nonnull IUserIcon userIcon);

    /**
     * Get or create the {@link ISyntheticPluginDefinition plugin definition} for the given {@link IUserIcon icon}.
     *
     * @param userIcon User icon for which to get the plugin definition
     * @return Plugin definition
     * @since 5.2
     */
    @Nonnull
    ISyntheticPluginDefinition getOrCreatePluginDefinition(@Nonnull IUserIcon userIcon);

    /**
     * Get or create the {@link ISyntheticPluginDefinition plugin definition} for the given CoT type.
     *
     * @param cotType The CoT type for which to get the plugin definition
     * @return Plugin definition
     * @since 5.3
     */
    @Nonnull
    ISyntheticPluginDefinition getOrCreateCotDefinition(@Nonnull String cotType);

    /**
     * @param uri URI to test
     * @return {@code true} if the given URI is a structurally valid icon set URI
     * @since 5.2
     */
    boolean isIconSetUri(@Nullable URI uri);

    /**
     * Get or create the {@link ISyntheticPluginDefinition plugin definition} for the given {@link URI}.
     *
     * @param uri URI referring to an icon within an icon set
     * @return Plugin definition
     * @since 5.2
     * @throws IllegalArgumentException If the given URI is not an icon set URI
     */
    @Nonnull
    ISyntheticPluginDefinition getOrCreatePluginDefinition(@Nonnull URI uri) throws IllegalArgumentException;
}
