package gov.tak.api.icons;

import gov.tak.api.annotation.IncubatingApi;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.net.URI;

/**
 * Contract for an icon defined in an icon set.
 *
 * @see IUserIconSet
 * @since 5.2
 */
@NoPluginImplementations
@IncubatingApi(since = "5.2")
public interface IUserIcon
{
    /**
     * @return The filename within the icon set
     */
    String getFileName();

    /**
     * @return The CoT type associated with this icon, if any
     */
    @Nullable
    String get2525cType();

    /**
     * @return The group containing this icon
     */
    @Nonnull
    String getGroup();

    /**
     * @return UID of the {@link IUserIconSet} containing this icon
     */
    @Nonnull
    String getIconsetUid();

    /**
     * @return Path within the icon set zip file for this icon
     */
    @Nonnull
    String getZipEntryPath();

    /**
     * @return The raw image bytes for this icon
     */
    @Nonnull
    byte[] getImageBytes();

    /**
     * @return {@code true} if required properties are present
     */
    boolean isValid();

    /**
     * @return A {@link URI} referring to this icon
     */
    @Nonnull
    URI toUri();
}
