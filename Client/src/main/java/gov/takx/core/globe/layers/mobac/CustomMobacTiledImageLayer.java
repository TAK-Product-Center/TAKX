package gov.takx.core.globe.layers.mobac;

import com.google.common.annotations.VisibleForTesting;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.layers.mercator.BasicMercatorTiledImageLayer;
import gov.nasa.worldwind.layers.mercator.MercatorSector;
import gov.nasa.worldwind.layers.mercator.MercatorTextureTile;
import gov.nasa.worldwind.layers.mercator.MercatorTiledImageLayer;
import gov.nasa.worldwind.retrieve.RetrievalPostProcessor;
import gov.nasa.worldwind.retrieve.Retriever;
import gov.nasa.worldwind.retrieve.URLRetriever;
import gov.nasa.worldwind.util.Tile;
import gov.nasa.worldwind.util.TileUrlBuilder;
import gov.nasa.worldwind.util.WWXML;
import gov.takx.core.globe.layers.worldwind.ApplicationHttpRetriever;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.xpath.XPath;

import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A tiled image layer that handles "custom" map sources (tile servers) from MOBAC compatible image sources.
 * <p>
 * The Mobile Atlas Creator (MOBAC) tools support ingestion of map data using a simple XML configuration file
 * (compared to the WW XML Layer definition).  This layer ingests the same form of configuration XML as MOBAC,
 * and pulls tiles from the configured tile server.
 * <p>
 * Here's an example XML configuration:
 *
 * <pre>{@code
 * <customMapSource>
 *     <name>Chartbundle IFR Low</name>
 *     <minZoom>4</minZoom>
 *     <maxZoom>13</maxZoom>
 *     <invertYCoordinate>false</invertYCoordinate>
 *     <tileType>png</tileType>
 *     <tileUpdate>None</tileUpdate>
 *     <url>http://wms.chartbundle.com/tms/v1.0/enrl/{$z}/{$x}/{$y}.png?type=google</url>
 *     <backgroundColor>#000000</backgroundColor>
 *     <serverParts>a b c</serverParts>
 * </customMapSource>
 * }</pre>
 * <p>
 * The URL format specified in the configuration supports the following parameter substitutions:
 * <p>
 * {@code {$z}} - Zoom level<br>
 * {@code {$x}} - Tile X coordinate<br>
 * {@code {$y}} - Tile Y coordinate<br>
 * {@code {$serverpart}} - Selected server part, as listed in {@code serverParts}, used in sequence<br>
 *
 * @see <a href="https://mobac.sourceforge.io/">mobac.sourceforge.io</a>
 * @since 3.0
 */
public class CustomMobacTiledImageLayer extends BasicMercatorTiledImageLayer
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @VisibleForTesting
    public static final String MIN_ZOOM_KEY = "minZoom";

    private static final String BASE_URL_KEY = "MobacBaseUrl";

    private static final String POST_PROCESSOR_CLASS_NAME =
            BasicMercatorTiledImageLayer.class.getName() + "$DownloadPostProcessor";

    private static final MercatorSector FULL_SECTOR = new MercatorSector(-1.0D, 1.0D, Angle.NEG180, Angle.POS180);

    private final String configXml;
    private final String baseServerUrl;
    private Field splitScaleField;
    private Constructor<? extends RetrievalPostProcessor> retrieverConstructor;

    /**
     * Construct from the given XML configuration.
     *
     * @param configXml The XML configuration to build from (our restorable state)
     */
    public CustomMobacTiledImageLayer(String configXml)
    {
        this(configXml, parseConfigXml(configXml), null);
    }

    /**
     * Construct from a parsed DOM Element.
     *
     * @param configXml  The XML configuration (our restorable state)
     * @param domElement DOM Element parsed from {@code configXml}
     * @param params     Pre-configured AVList
     */
    public CustomMobacTiledImageLayer(String configXml, Element domElement, AVList params)
    {
        this(configXml, getParams(domElement, params));
    }

    /**
     * Parse the given XML into a DOM Element.
     *
     * @param configXml XML to parse
     * @return DOM Element
     */
    private static Element parseConfigXml(String configXml)
    {
        final InputStream inputStream = IOUtils.toInputStream(configXml, StandardCharsets.UTF_8);
        final Document document = WWXML.openDocumentStream(inputStream);
        return document.getDocumentElement();
    }

    /**
     * Create the construction parameters from the XML definition.
     *
     * @param domElement The document element of the XML config file
     * @param params     Any additional parameters
     * @return construction parameters
     */
    private static AVList getParams(@Nonnull Element domElement, @Nullable AVList params)
    {
        if (params == null) params = new AVListImpl();

        AbstractLayer.getLayerConfigParams(domElement, params);

        final XPath xpath = WWXML.makeXPath();

        final String url = Objects.requireNonNull(WWXML.getText(domElement, "url", xpath));
        final String name = Objects.requireNonNull(WWXML.getText(domElement, "name", xpath));
        final String tileType = WWXML.getText(domElement, "tileType", xpath);
        final int minZoom = Objects.requireNonNull(WWXML.getInteger(domElement, "minZoom", xpath));
        final int maxZoom = Objects.requireNonNull(WWXML.getInteger(domElement, "maxZoom", xpath));

        params.setValue(BASE_URL_KEY, url);
        params.setValue(AVKey.DISPLAY_NAME, name);
        params.setValue(AVKey.TILE_WIDTH, 256);
        params.setValue(AVKey.TILE_HEIGHT, 256);
        params.setValue(AVKey.DATA_CACHE_NAME, "Earth/mobac/" + name);
        params.setValue(AVKey.SERVICE, "CustomMobac");
        params.setValue(AVKey.DATASET_NAME, "map");
        params.setValue(AVKey.FORMAT_SUFFIX, "." + (tileType != null ? tileType.toUpperCase() : "PNG"));
        params.setValue(AVKey.NUM_LEVELS, maxZoom - minZoom + 1);
        params.setValue(AVKey.NUM_EMPTY_LEVELS, 0);
        params.setValue(AVKey.SECTOR, FULL_SECTOR);
        params.setValue(MIN_ZOOM_KEY, minZoom);

        final int nTiles = 1 << minZoom;
        params.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, LatLon.fromDegrees(180.0 / nTiles, 360.0 / nTiles));

        WWXML.checkAndSetStringParam(domElement, params, AVKey.DISPLAY_NAME, "DisplayName", xpath);
        WWXML.checkAndSetDoubleParam(domElement, params, AVKey.OPACITY, "Opacity", xpath);

        params.setValue(AVKey.TILE_URL_BUILDER, new URLBuilder(domElement, xpath, minZoom));

        return params;
    }

    /**
     * Internal constructor.
     *
     * @param configXml The XML configuration (our restorable state)
     * @param params    Construction parameters
     */
    private CustomMobacTiledImageLayer(String configXml, AVList params)
    {
        super(params);

        baseServerUrl = makeBaseServerUrl(params.getStringValue(BASE_URL_KEY));

        setName(params.getStringValue(AVKey.DISPLAY_NAME));
        setPickEnabled(false); // Nothing pickable on these layer types
        setUseTransparentTextures(true);

        this.configXml = configXml; // Hold on to our restorable state

        loadReflectionHacks(); // I don't know the meaning of the word "private"

        // This may not be the best heuristic, but with the initial data sets I played with, the ones that had a
        // higher minZoom value needed to have a higher splitScale, or they were too blurry to read.  But, if I
        // just bump all of them up to, say 1.4 or 1.5, then those with lower minZoom's started having some odd
        // tile discrepancies, where more detailed tiles were loaded right next to less detailed, making it hard
        // to read.
        //
        // So, for now, try this.  If we get feedback that it is not working, we can take another look.

        final int minZoom = (int) params.getValue(MIN_ZOOM_KEY);
        setSplitScale(1.1 + (minZoom * .1));  // This helps make the images crisper
    }

    /**
     * @return protocol and host name from the input {@code sourceUrl} string
     * @since 3.4
     */
    private String makeBaseServerUrl(String sourceUrl)
    {
        try
        {
            final URL ourUrl = new URL(sourceUrl);
            return ourUrl.getProtocol() + "://" + ourUrl.getHost();
        } catch (MalformedURLException e)
        {
            return null;
        }
    }

    /**
     * @return The protocol + hostname of the tile server, {@code null} if original URL was malformed
     * @since 3.4
     */
    @Nullable
    public String getBaseServerUrl()
    {
        return baseServerUrl;
    }

    @Override
    public String getRestorableState()
    {
        return configXml;
    }

    /**
     * Overridden so we can add support for the "https" protocol.
     * <p>
     * {@inheritDoc}
     */
    @Override
    protected void downloadTexture(final MercatorTextureTile tile)
    {
        if (!WorldWind.getRetrievalService().isAvailable()) return;

        final java.net.URL url;
        try
        {
            url = tile.getResourceURL();
            if (url == null || WorldWind.getNetworkStatus().isHostUnavailable(url)) return;
        } catch (java.net.MalformedURLException e)
        {
            logger.error("Exception creating texture URL for {}", tile, e);
            return;
        }

        final String protocol = url.getProtocol();
        if ("http".equalsIgnoreCase(protocol) || "https".equalsIgnoreCase(protocol))
        {
            final Retriever retriever = new ApplicationHttpRetriever(url, makeRetrieverPostProcessor(tile));
            retriever.setValue(URLRetriever.EXTRACT_ZIP_ENTRY, "true"); // supports legacy layers

            WorldWind.getRetrievalService().runRetriever(retriever, tile.getPriority());
        } else
        {
            logger.error("Unrecognized retrieval protocol for texture URL {}", url);
        }
    }

    /**
     * Sadly, the WW code base is very closed, and it is not easy to extend basic classes, like the Mercator
     * tiled image layer.  So, instead of copying 500+ lines of code, I've decided to use some reflection hacks
     * to give me the controls I need.
     *
     * <ul>
     *     <li>Need to be able to set the splitScale field</li>
     *     <li>need to be able to create instances of a private inner class:
     *     {@code BasicMercatorTiledImageLayer$DownloadPostProcessor}</li>
     * </ul>
     */
    private void loadReflectionHacks()
    {
        try
        {
            // Find the splitScale field.
            splitScaleField = MercatorTiledImageLayer.class.getDeclaredField("splitScale");
            splitScaleField.setAccessible(true);

            // find the post-processor class and its constructor.
            final Class<? extends RetrievalPostProcessor> cls =
                    Class.forName(POST_PROCESSOR_CLASS_NAME).asSubclass(RetrievalPostProcessor.class);
            retrieverConstructor = cls.getDeclaredConstructor(MercatorTextureTile.class, BasicMercatorTiledImageLayer.class);
            retrieverConstructor.setAccessible(true);
        } catch (ReflectiveOperationException e)
        {
            logger.error("Failed to get needed fields/methods", e);
            throw new RuntimeException("Reflection failed", e);
        }
    }

    /**
     * @param splitScale new splitScale
     * @see #loadReflectionHacks()
     */
    private void setSplitScale(double splitScale)
    {
        try
        {
            splitScaleField.set(this, splitScale);
        } catch (IllegalAccessException e)
        {
            logger.error("Failed to set splitScale", e);
        }
    }

    /**
     * Construct the retrieval post-processor, using reflection.
     *
     * @param tile Tile being processed
     * @return The post-processor
     * @see #loadReflectionHacks()
     */
    private RetrievalPostProcessor makeRetrieverPostProcessor(MercatorTextureTile tile)
    {
        try
        {
            return retrieverConstructor.newInstance(tile, this);
        } catch (ReflectiveOperationException e)
        {
            logger.error("Failed to create Retriever", e);
            return null;
        }
    }

    /**
     * URL Builder for specifying the correct tile using the URL format specified in the configuration.  The
     * following parameters are substituted:
     * <p>
     * {@code {$z}} - Zoom level<br>
     * {@code {$x}} - Tile X coordinate<br>
     * {@code {$y}} - Tile Y coordinate<br>
     * {@code {$serverpart}} - Selected server part, as listed in {@code serverParts}, used in sequence<br>
     */
    private static class URLBuilder implements TileUrlBuilder
    {
        private final String urlPattern;
        private final int levelOffset;
        private final boolean invertYCoordinate;
        private final String[] serverParts;
        private final AtomicInteger serverPartIdx = new AtomicInteger(0);

        private URLBuilder(@Nonnull Element domElement, @Nonnull XPath xpath, int levelOffset)
        {
            this.levelOffset = levelOffset;

            urlPattern = Objects.requireNonNull(WWXML.getText(domElement, "url", xpath));
            invertYCoordinate = Boolean.TRUE.equals(WWXML.getBoolean(domElement, "invertYCoordinate", xpath));

            final String serverPartsConfig = WWXML.getText(domElement, "serverParts", xpath);
            serverParts = serverPartsConfig != null && !serverPartsConfig.isEmpty()
                    ? serverPartsConfig.split("\\s+")
                    : null;
        }

        @Override
        public URL getURL(Tile tile, String imageFormat) throws MalformedURLException
        {
            final int zoom = tile.getLevelNumber() + levelOffset;
            final int x = tile.getColumn();

            // Y-axis tile indices run bottom to top in WW, but top to bottom in MOBAC.
            // If invertYCoordinate is true, then we just use the WW index directly.
            final int row = tile.getRow();
            final int y = invertYCoordinate ? row : (1 << zoom) - 1 - row;

            String url = urlPattern;
            url = url.replace("{$z}", Integer.toString(zoom));
            url = url.replace("{$x}", Integer.toString(x));
            url = url.replace("{$y}", Integer.toString(y));

            if (serverParts != null)
            {
                // Rotate through the server parts
                final int index = serverPartIdx.getAndAccumulate(1, (idx, inc) -> (idx + inc) % serverParts.length);
                url = url.replace("{$serverpart}", serverParts[index]);
            }

            return new URL(url);
        }
    }
}
