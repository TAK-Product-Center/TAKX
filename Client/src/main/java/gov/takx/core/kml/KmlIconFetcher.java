package gov.takx.core.kml;

import de.micromata.opengis.kml.v_2_2_0.Icon;
import gov.takx.commons.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service to fetch images referenced in KML Screen Overlays and Ground Overlays.  The fetch operations are performed
 * in the background.
 *
 * @see #fetch(KmlModel, Icon, String)
 * @since 5.5
 */
@Singleton
public class KmlIconFetcher
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final List<String> HTTP_PREFIXES = List.of("http:", "https:");

    /**
     * Single thread executor to fetch images.  This is used in preference to the core executor service because of
     * special handling of the input stream obtained from jar URLs to ensure they are not held open after the input
     * stream is closed.  If these fetches are allowed to run in parallel, then two input streams from the same jar
     * file can encounter errors when one of them closes the jarFile associated with the URL stream.
     */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    KmlIconFetcher()
    {
    }

    /**
     * Fetch an image referenced in a kml {@link Icon}. The fetch happens in the background.
     * 
     * @param model {@link KmlModel} containing the {@code icon}
     * @param icon Icon to fetch (according to the {@code href} property
     * @param name Icon name, for use in error reporting
     * @return {@link CompletableFuture<BufferedImage>} for obtaining the fetched image
     * @see Icon#getHref()
     */
    public CompletableFuture<BufferedImage> fetch(@Nonnull KmlModel model, @Nonnull Icon icon, @Nullable String name)
    {
        return CompletableFuture.supplyAsync(() -> loadImage(model, icon, name), executor);
    }

    /**
     * Load the image from Icon's href.
     */
    private BufferedImage loadImage(KmlModel model, Icon icon, String name)
    {
        final var href = icon != null ? icon.getHref() : null;
        if (href == null)
        {
            logger.warn("No icon href for overlay {}", name);
            throw new NullPointerException("No icon href for overlay " + name);
        }

        try
        {
            // Construct a jar URL if the href is not a remote reference
            final URL url = resolveHref(model, href);

            logger.debug("{} URL = {}", name, url);

            try (var is = getImageInputStream(url))
            {
                return ImageIO.read(is);
            }
        } catch (IOException e)
        {
            // Google Earth and WW are both silent if an image can't be loaded
            logger.debug("Failed to read image", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Resolve the given {@code href} to a URL.  For http and https hrefs, the string is converted directly to a
     * {@link URL}.  For a href from a KMZ file, it is converted to a {@code jar:file:} URL using the href as the
     * resource path.  For a KML href, it is converted to a file resolved relative to the source file's directory.
     * 
     * @param href href to resolve
     * @return Corresponding URL for href, or an exception if the resulting URL is malformed
     */
    private URL resolveHref(@Nonnull KmlModel model, @Nonnull String href) throws MalformedURLException
    {
        if (StringUtils.startsWithAny(href.toLowerCase(), HTTP_PREFIXES)) return new URL(href);

        // Resolve KMZ links as jar resource URLs, and KML links as a file relative to the source file's parent dir.
        return model.isKmz()
                ? new URL("jar:file:" + model.sourceFile() + "!/" + href)
                : model.sourceFile().getParentFile().toPath().resolve(href).toFile().toURI().toURL();
    }

    /**
     * Construct a specialized input stream when dealing with jar URLs.  We need to ensure that the connection to the
     * jar file is also closed when the resource URL is closed.  Otherwise, there's an open reference to the KMZ file,
     * and we can't delete it if the user tries to remove it.
     *
     * @param url URL to process
     * @return Custom input stream for jar URLs, or a normal input stream otherwise
     */
    private InputStream getImageInputStream(URL url) throws IOException
    {
        final var connection = url.openConnection();
        final var inputStream = connection.getInputStream();
        if (connection instanceof JarURLConnection jarURLConnection)
        {
            // Wrap the InputStream to also close the underlying JarFile if from a JarURLConnection.
            return new InputStream()
            {
                @Override
                public int read() throws IOException
                {
                    return inputStream.read();
                }

                @Override
                public void close() throws IOException
                {
                    inputStream.close();
                    jarURLConnection.getJarFile().close();
                }
            };
        } else
        {
            return inputStream; // No special handling
        }
    }
}
