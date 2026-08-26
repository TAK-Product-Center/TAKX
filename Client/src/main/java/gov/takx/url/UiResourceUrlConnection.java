package gov.takx.url;

import com.google.common.annotations.VisibleForTesting;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.platform.ui.ResourceDirectory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static gov.takx.commons.swing.ImageUtils.DEFAULT_IMAGE_URL;

/**
 * URL connection for the {@code uiresource} URL protocol.  URLs within the {@code uiresource} consist of two
 * components, the name of a {@link gov.takx.platform.ui.ResourceDirectory} with an optional specification of whether
 * to default to an "unknown" icon, followed by the path to the actual resource.  The resource directory and default
 * indicator are separated by a colon. E.g., {@code uiresource:PLATFORM_ICONS/some_icon.png} or
 * {@code uiresource:PLATFORM_ICONS:defaultOnError/some_icon.png}.
 *
 * @see gov.takx.platform.ui.UIResources
 * @see gov.takx.platform.ui.ResourceDirectory
 * @since 5.7.1
 */
public class UiResourceUrlConnection extends URLConnection
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private URL nativeUrl;

    /**
     * Constructs a URL connection to the specified URL.
     *
     * @param url the specified URL
     */
    public UiResourceUrlConnection(URL url)
    {
        super(url);
    }

    @Override
    public InputStream getInputStream() throws IOException
    {
        connect();
        if (nativeUrl == null) throw new IOException("Failed to decode URL: " + url);
        return nativeUrl.openStream();
    }

    @Override
    public void connect() throws IOException
    {
        // Find the resource directory from the first part of the URL path
        final String path = URLDecoder.decode(url.getPath(), StandardCharsets.UTF_8);
        final String[] pathParts = path.split("/", 2);
        if (pathParts.length != 2) throw new IOException("Invalid URI path; incorrect path count: " + url);

        // See if our URL has ":defaultOnError" added to the directory.  Honestly, all we care about is the ":".
        final String resourceDirPathComponent = pathParts[0];
        final int colonIndex = resourceDirPathComponent.indexOf(':');
        final var resourceDirectory = getResourceDirectory(resourceDirPathComponent, colonIndex);
        final String resourcePath = pathParts[1];
        final boolean defaultOnError = colonIndex > 0;

        nativeUrl = getNativeUrl(resourceDirectory, resourcePath, defaultOnError);
        logger.debug("connect to {}/{} => {}", resourceDirectory, resourcePath, nativeUrl);

        connected = true;
    }

    /**
     * @param resourceDirPathComponent Path component specifying the resource directory, may include a {@code :defaultOnError} indicator
     * @param colonIndex Index of colon within the path component
     * @return The resource directory to use parsed after removing any trailing {@code :defaultOnError} indicator
     * @throws IOException if the resource directory name can't be resolved
     */
    private ResourceDirectory getResourceDirectory(String resourceDirPathComponent, int colonIndex) throws IOException
    {
        final String resourceDirName = colonIndex > 0
                ? resourceDirPathComponent.substring(0, colonIndex)
                : resourceDirPathComponent;
        try
        {
            return ResourceDirectory.valueOf(resourceDirName);
        } catch (IllegalArgumentException e)
        {
            throw new IOException("Unknown resource directory [" + resourceDirPathComponent + "] in URL: " + url, e);
        }
    }

    /**
     * Get the <em>native</em> URL for the image file in the specified directory, returning {@code null}, or optionally,
     * the default icon URL on failure. The URL is <em>native</em> in the sense that it can be processed by the default
     * URL handlers in the JDK.
     *
     * @param directory The resource directory containing the file
     * @param filePath The path to the file containing the image within the given directory
     * @param defaultOnError Whether to return the default URL if an error occurs
     * @return URL for the image file. If failure occurs and defaultOnError is true, {@link ImageUtils#DEFAULT_IMAGE_URL}
     * is returned. Otherwise, {@code null} is returned.
     */
    @VisibleForTesting
    @Nullable
    protected static URL getNativeUrl(@Nonnull ResourceDirectory directory, @Nonnull String filePath, boolean defaultOnError)
    {
        final String directoryPath = directory.getDirectoryPath();
        URL nativeUrl = null;

        try
        {
            if (directory.isExternal())
            {
                final var target = new File(directoryPath, filePath);
                nativeUrl = target.toURI().toURL();
                if (target.exists())
                {
                    return nativeUrl;
                } else
                {
                    logger.error("File not found for {}: {}", nativeUrl, filePath);
                }
            } else
            {
                nativeUrl = UiResourceUrlConnection.class.getClassLoader().getResource(directoryPath + filePath);
                if (nativeUrl != null)
                {
                    return nativeUrl;
                } else
                {
                    logger.error("Resource not found: '{}/{}'", directory, filePath);
                }
            }
        } catch (MalformedURLException e)
        {
            logger.error("Error getting URL for {} in {}, returning default image URL: {}", filePath, directoryPath, defaultOnError, e);
        }

        return defaultOnError ? DEFAULT_IMAGE_URL : nativeUrl;
    }
}
