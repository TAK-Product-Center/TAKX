package gov.takx.core.mapobject;

import gov.takx.api.mapobject.IIconMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectImageUtils;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.synthetic.ISyntheticPluginDefinition;
import gov.takx.api.url.Schemes;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.module.IPluginModuleManager;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * A utility class for handling image URIs related to Map Objects.
 */
@ApplicationScoped
public class MapObjectImageUtils implements IMapObjectImageUtils
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final Set<URI> FAILED_URIS = ConcurrentHashMap.newKeySet();
    private static final Set<String> MISSING_JAR_RESOURCES = ConcurrentHashMap.newKeySet();
    private static final Map<URI, BufferedImage> RELOCATED_IMAGES = new ConcurrentHashMap<>();
    private static final Map<File, BufferedImage> FILE_IMAGES = new ConcurrentHashMap<>();
    private static final String ICONS_PATH = "/icons/";

    private final IPluginModuleManager pluginModuleManager;

    @Inject
    MapObjectImageUtils(IPluginModuleManager pluginModuleManager)
    {
        this.pluginModuleManager = pluginModuleManager;
    }

    @Override
    public boolean isSyntheticDefaultIcon(IPluginDefinition<?> pluginDefinition)
    {
        // The default icon is used when it is the exact URI specified by a synthetic plugin definition,
        // and also when there is no URI specified. When no URI is specified calls to getImage() will
        // fall back to the default image URI, so both cases are valid here.
        return pluginDefinition instanceof ISyntheticPluginDefinition
                && pluginDefinition.getIconURI().map(uri -> ImageUtils.DEFAULT_IMAGE_URI.equals(uri)).orElse(true);
    }

    @Override
    public BufferedImage loadImage(@Nonnull URI imageUri, @Nonnull ClassLoader classLoader)
    {
        if (FAILED_URIS.contains(imageUri))
        {
            logger.debug("Skip known bad URI: '{}'", imageUri);
            return null;
        }

        logger.debug("loadImage '{}' using {}", imageUri, classLoader);
        try
        {
            final var image = switch (imageUri.getScheme().toLowerCase())
            {
                case Schemes.BASE64, Schemes.ICONSET, Schemes.UI_RESOURCE, Schemes.VFS -> loadUrlImage(imageUri);
                case IIconMapObject.PROTOCOL_SCHEME_FILE -> loadFileImage(imageUri);
                case IIconMapObject.PROTOCOL_SCHEME_JAR -> loadJarImage(imageUri, classLoader);
                case IIconMapObject.PROTOCOL_SCHEME_SHELL -> loadShellImage(imageUri);
                default ->
                {
                    logger.error("The following URI scheme is not presently supported : {}", imageUri);
                    yield null;
                }
            };

            if (image == null) FAILED_URIS.add(imageUri);
            return image;
        } catch (Exception e)
        {
            // Shouldn't happen since all the paths above handle exceptions, but for safety...
            logger.error("Couldn't find the icon specified by imageURI ({}); using the default icon instead", imageUri, e);
        }

        return null;
    }

    @Nullable
    @Override
    public URL getIconFileURL(@Nonnull IMapObject mapObject)
    {
        try
        {
            return mapObject.getIconImageURI().toURL();
        } catch (MalformedURLException e)
        {
            logger.error("Map object '{}' had invalid image URI", mapObject.getKey(), e);
            return null;
        }
    }

    /**
     * Load an image from a URL for which we have a built-in stream handler.
     *
     * @param imageUri URI of image to load
     * @return image or {@code null} on error
     * @since 5.2
     */
    @Nullable
    private BufferedImage loadUrlImage(@Nonnull URI imageUri)
    {
        try
        {
            return ImageUtils.getImage(imageUri.toURL(), false);
        } catch (MalformedURLException e)
        {
            logger.error("Error restoring URI: {}", imageUri, e);
        }
        return null;
    }

    /**
     * Load an image from the file path.  The URI will contain the path to the image file.
     *
     * @param imageUri URI of image to load
     * @return image or {@code null} on error
     * @since 3.0
     */
    @Nullable
    private BufferedImage loadFileImage(@Nonnull URI imageUri)
    {
        final String path = imageUri.getPath();
        var imageFile = new File(path);

        // If something in 'resources' is being referred to by absolute path, and it is not found, then
        // strip the absolute portion, and keep the relative path in 'resources'.
        if (!imageFile.exists() && path.contains("resources")) imageFile = new File(path.substring(path.indexOf("resources")));

        return FILE_IMAGES.computeIfAbsent(imageFile, k -> ImageUtils.getImage(k, false));
    }

    /**
     * Load an image from a Jar resource.
     *
     * @param imageUri    URI of image to load
     * @param classLoader Class loader to use in case the URI references a Jar resource
     * @return image from jar or {@code null} on error
     * @since 3.0
     */
    @Nullable
    private BufferedImage loadJarImage(@Nonnull URI imageUri, @Nonnull ClassLoader classLoader)
    {
        if (RELOCATED_IMAGES.containsKey(imageUri)) return RELOCATED_IMAGES.get(imageUri);

        // Previously, JAR URLs were stripped of everything except the resource path within the jar.  This dates back
        // to pre-history when all plugin content was on the core's classpath.  So, for backward compatibility, check
        // for that situation, and try to find the appropriate resource within all of our plugin deployments.

        final String imageUriString = imageUri.toString();
        final int bangIndex = imageUriString.indexOf('!');

        if (bangIndex < 0)
        {
            return findImageInCoreOrPlugins(imageUri, imageUri.getSchemeSpecificPart(), classLoader);
        } else
        {
            // Just try to directly read the given URI.  If that fails, we'll fall back to searching for the resource
            // across plugin deployments. This handles the case where a project package is created and moved to a
            // machine that has a different installation location for plugins.
            try
            {
                final var image = ImageIO.read(imageUri.toURL());
                if (image != null) return image;
            } catch (MalformedURLException e)
            {
                logger.error("Invalid image URI: {}; {}", imageUri, e.getMessage());
            } catch (IOException e)
            {
                logger.debug("Failed to read '{}'", imageUri, e);
                // If we can't read it directly, fall through and try to find it
            }

            // Extract the resource path from the URL, and try to find that
            final String resourcePath = imageUriString.substring(bangIndex + 1);

            // Special handling for platform icons.  Since they are recorded as jar URLs, they can refer to an older
            // version of the jar if the user has installed a newer version of TAKX.  Check for this condition, and
            // load them as UI resources.
            if (resourcePath.startsWith(ICONS_PATH) && imageUriString.contains("/platform-"))
            {
                return UIResources.getImage(PLATFORM_ICONS, resourcePath.substring(ICONS_PATH.length()));
            } else
            {
                return findImageInCoreOrPlugins(imageUri, resourcePath, classLoader);
            }
        }
    }

    /**
     * Try to load a resource as an image from the given {@code classLoader}, then trying the core, and failing that,
     * from any plugin deployment.
     *
     * @param imageUri Original URI of the image being loaded
     * @param resourcePath Path of resource to read
     * @param classLoader Initial class loader to use
     * @return located image, or {@code null} if not found anywhere
     * @since 5.5.1
     */
    @Nullable
    private BufferedImage findImageInCoreOrPlugins(@Nonnull URI imageUri, @Nonnull String resourcePath, @Nonnull ClassLoader classLoader)
    {
        URL resourceUrl = classLoader.getResource(resourcePath); // First try the given class loader

        if (MISSING_JAR_RESOURCES.add(resourcePath))
        {
            logger.warn("Image resource '{}' couldn't be located; using first matching core or plugin resource.", resourcePath);
        }

        if (resourceUrl == null) resourceUrl = getClass().getResource(resourcePath); // Then see if it came from the core
        if (resourceUrl == null) resourceUrl = pluginModuleManager.getPluginResource(resourcePath); // Then try all plugins

        if (resourceUrl != null)
        {
            final var image = ImageUtils.getImage(resourceUrl, false);
            if (image != null) RELOCATED_IMAGES.put(imageUri, image);
            return image;
        } else
        {
            logger.error("Unable to find image resource: {} using {}", resourcePath, classLoader);
        }
        return null;
    }

    /**
     * Get an image from the "system icon" for a given file.  The URI will contain the path to the image file.
     *
     * @param imageUri URI of file to use for obtaining the "system/shell icon"
     * @return image or {@code null} on error
     * @see FileUtils#getSystemIconImage(File)
     * @since 3.0
     */
    @Nullable
    private BufferedImage loadShellImage(@Nonnull URI imageUri)
    {
        try
        {
            // Windows file names with spaces are automatically URL-encoded to %20, so we need to decode (using getPath())
            return FileUtils.getSystemIconImage(new File(imageUri.getPath()));
        } catch (Exception e)
        {
            logger.warn("Error restoring SHELL URI: {}", imageUri);
        }
        return null;
    }
}
