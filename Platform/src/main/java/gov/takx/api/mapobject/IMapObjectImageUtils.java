package gov.takx.api.mapobject;

import gov.tak.api.annotation.DeprecatedApi;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.plugin.IPluginDefinition;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.image.BufferedImage;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Methods for handling Map Object image URIs with relative paths during persistence.
 */
@InjectableService
@NoPluginImplementations
public interface IMapObjectImageUtils
{
    /**
     * Determine if the given plugin definition is synthetic, and the configured icon URI is the default image URI.
     * <p/>
     * When a Map Object is created without a real plugin definition, a synthetic one is created for it, and it is
     * given a default icon.  If a plugin or a user subsequently sets an icon on the Map Object, then we don't want
     * to reset it to the default icon.
     *
     * @param pluginDefinition The plugin definition to test
     * @return {@code true} if the definition is synthetic and has a default icon
     * @since 5.1
     */
    boolean isSyntheticDefaultIcon(IPluginDefinition<?> pluginDefinition);

    /**
     * Take a fully qualified URI and pare it down to a URI which will be transportable
     * across machines using the packaging method.
     *
     * @param uri URI to process
     * @return String form for storage in persistence
     * @deprecated No longer used for persistence of MO URIs
     */
    @Deprecated(since = "5.5.1", forRemoval = true)
    @DeprecatedApi(since = "5.5.1", forRemoval = true, removeAt = "5.8")
    @Nullable
    default String deflateURI(@Nonnull URI uri)
    {
        return uri.toString();
    }

    /**
     * Inflate the URL (as a String) from a "compressed" version found in the package portable data up
     * to a URI which has absolute reference and is locatable on the running application.
     *
     * @param sourceUrl Source URL to inflate
     * @return Absolute URI for the URL
     * @deprecated No longer used for persistence of MO URIs
     */
    @Deprecated(since = "5.5.1", forRemoval = true)
    @DeprecatedApi(since = "5.5.1", forRemoval = true, removeAt = "5.8")
    @Nullable
    default URI inflateURI(@Nonnull String sourceUrl)
    {
        try
        {
            return new URI(sourceUrl);
        } catch (URISyntaxException e)
        {
            return null;
        }
    }

    /**
     * Load an image from the given URI.
     *
     * @param imageUri    URI of image to read
     * @param classLoader Class loader to use in case the URI references a Jar resource
     * @return loaded image, {@code null} on any error3
     * @since 3.0
     */
    @Nullable
    BufferedImage loadImage(@Nonnull URI imageUri, @Nonnull ClassLoader classLoader);

    /**
     * Gets the file URL to a map object's stored icon.
     *
     * @param mapObject Map object to get icon URL to.
     * @return URL to map object's icon. {@code null} if there is no icon, or the icon path is invalid.
     * @since 5.1
     */
    @Nullable
    URL getIconFileURL(@Nonnull IMapObject mapObject);
}
