package gov.takx.core.globe.layers.worldwind;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.layers.TextureTile;
import gov.nasa.worldwind.ogc.wms.WMSCapabilities;
import gov.nasa.worldwind.retrieve.Retriever;
import gov.nasa.worldwind.retrieve.URLRetriever;
import gov.nasa.worldwind.util.RestorableSupport;
import gov.nasa.worldwind.wms.WMSTiledImageLayer;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @since 1.43 (renamed from RaptorWMSTiledImageLayer in 3.4)
 */
public class ApplicationWmsTiledImageLayer extends WMSTiledImageLayer
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public ApplicationWmsTiledImageLayer(AVList params)
    {
        super(params);
        logger.debug("Creating WMS Tiled Image Layer: {}", params);
    }

    public ApplicationWmsTiledImageLayer(Document dom, AVList params)
    {
        super(dom, params);
        logger.debug("Creating WMS Tiled Image Layer: {}", params);
    }

    public ApplicationWmsTiledImageLayer(Element domElement, AVList params)
    {
        super(domElement, params);
        logger.debug("Creating WMS Tiled Image Layer: {}", params);
    }

    public ApplicationWmsTiledImageLayer(WMSCapabilities caps, AVList params)
    {
        super(caps, params);
        logger.debug("Creating WMS Tiled Image Layer: {}", params);
    }

    public static AVList wmsRestorableStateToExtendedParams(@Nonnull String stateInXml)
    {
        RestorableSupport rs;
        try
        {
            rs = RestorableSupport.parse(stateInXml);
        } catch (Exception e)
        {
            // Parsing the document specified by stateInXml failed.
            final String message = "Exception attempting to parse state XML " + stateInXml;
            logger.error("{}", message);
            throw new IllegalArgumentException(message, e);
        }

        AVList params = new AVListImpl();
        wmsRestoreStateToParams(rs, null, params);

        String coordinateSystem = rs.getStateValueAsString(null, AVKey.COORDINATE_SYSTEM);
        if (coordinateSystem != null)
        {
            params.setValue(AVKey.COORDINATE_SYSTEM, coordinateSystem);
            URLBuilder builder = new URLBuilder(params);
            params.setValue(AVKey.TILE_URL_BUILDER, builder);
        }

        return params;
    }

    public ApplicationWmsTiledImageLayer(String stateInXml)
    {
        this(wmsRestorableStateToExtendedParams(stateInXml));
        logger.debug("Creating WMS Tiled Image Layer: {}", stateInXml);

        RestorableSupport rs;
        try
        {
            rs = RestorableSupport.parse(stateInXml);
        } catch (Exception e)
        {
            // Parsing the document specified by stateInXml failed.
            final String message = "Exception attempting to parse state XML " + stateInXml;
            logger.error("{}", message);
            throw new IllegalArgumentException(message, e);
        }

        doRestoreState(rs, null);
    }

    @Override
    protected void doRestoreState(RestorableSupport rs, RestorableSupport.StateObject context)
    {
        super.doRestoreState(rs, context);
    }

    @Override
    protected void retrieveRemoteImage(final TextureTile tile, String mimeType, int timeout) throws Exception
    {
        // TODO: apply retriever-factory pattern for remote retrieval case.
        final URL resourceURL = tile.getResourceURL(mimeType);
        if (resourceURL == null)
        {
            return;
        }

        Retriever retriever;

        String protocol = resourceURL.getProtocol();

        if ("http".equalsIgnoreCase(protocol) || "https".equalsIgnoreCase(protocol))
        {
            retriever = new ApplicationHttpRetriever(resourceURL, new CompositionRetrievalPostProcessor(tile));
            retriever.setValue(URLRetriever.EXTRACT_ZIP_ENTRY, "true"); // supports legacy layers
        } else
        {
            throw new RuntimeException("Unrecognized retrieval protocol for texture URL " + resourceURL);
        }

        retriever.setConnectTimeout(10000);
        retriever.setReadTimeout(timeout);
        retriever.call();
    }

    @Override
    protected void retrieveRemoteTexture(TextureTile tile, DownloadPostProcessor postProcessor)
    {
        if (!isNetworkRetrievalEnabled())
        {
            getLevels().markResourceAbsent(tile);
            return;
        }

        if (!WorldWind.getRetrievalService().isAvailable())
        {
            return;
        }

        URL url;
        try
        {
            url = tile.getResourceURL();
            if (url == null)
            {
                return;
            }

            if (WorldWind.getNetworkStatus().isHostUnavailable(url))
            {
                getLevels().markResourceAbsent(tile);
                return;
            }
        } catch (MalformedURLException e)
        {
            logger.error("Exception creating texture URL for {}", tile, e);
            return;
        }

        Retriever retriever;

        if (postProcessor == null)
        {
            postProcessor = createDownloadPostProcessor(tile);
        }

        retriever = ApplicationHttpRetriever.createRetriever(url, postProcessor);
        retriever.setValue(URLRetriever.EXTRACT_ZIP_ENTRY, "true"); // supports legacy layers

        // Apply any overridden timeouts.
        Integer cto = AVListImpl.getIntegerValue(this, AVKey.URL_CONNECT_TIMEOUT);
        if (cto != null && cto > 0)
        {
            retriever.setConnectTimeout(cto);
        }
        Integer cro = AVListImpl.getIntegerValue(this, AVKey.URL_READ_TIMEOUT);
        if (cro != null && cro > 0)
        {
            retriever.setReadTimeout(cro);
        }
        Integer srl = AVListImpl.getIntegerValue(this, AVKey.RETRIEVAL_QUEUE_STALE_REQUEST_LIMIT);
        if (srl != null && srl > 0)
        {
            retriever.setStaleRequestLimit(srl);
        }

        WorldWind.getRetrievalService().runRetriever(retriever, tile.getPriority());
    }
}
