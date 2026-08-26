package gov.takx.core.globe.layers.worldwind;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.exception.WWUnrecognizedException;
import gov.nasa.worldwind.layers.BasicLayerFactory;
import gov.nasa.worldwind.layers.BasicTiledImageLayer;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LocalRasterServerLayer;
import gov.nasa.worldwind.ogc.OGCCapabilities;
import gov.nasa.worldwind.ogc.OGCConstants;
import gov.nasa.worldwind.ogc.wms.WMSCapabilities;
import gov.nasa.worldwind.ogc.wms.WMSLayerCapabilities;
import gov.nasa.worldwind.util.WWXML;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.core.globe.layers.mobac.CustomMobacTiledImageLayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.util.List;

/**
 * @since 1.43 (renamed from RaptorLayerFactory in 3.4)
 */
@PseudoBean
public class LayerFactory extends BasicLayerFactory
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String OFFLINE_SERVICE_NAME = "Offline";
    private static final String TILE_SERVICE_NAME = "WWTileService";
    private static final String MERCATOR_TILE_SERVICE_NAME = "WWMercatorTileService";

    public static final String CUSTOM_MAP_SOURCE = "customMapSource";

    /**
     * Overridden to add support for custom map sources defined in XML, assuming MOBAC compliant tile servers.
     * <p>
     * {@inheritDoc}
     *
     * @since 3.0
     */
    @Override
    protected Object doCreateFromElement(Element domElement, AVList params) throws Exception
    {
        final Object layer = super.doCreateFromElement(domElement, params);

        if (layer != null) return layer;

        // Support for simplified "custom" map sources XML as supported by ATAK.

        return CUSTOM_MAP_SOURCE.equals(domElement.getLocalName())
                ? new CustomMobacTiledImageLayer(getStringFromDocument(domElement), domElement, params)
                : null;
    }

    @Override
    protected Layer doCreateFromCapabilities(OGCCapabilities caps, AVList params)
    {
        String serviceName = caps.getServiceInformation().getServiceName();
        if (serviceName == null || !(serviceName.equalsIgnoreCase(OGCConstants.WMS_SERVICE_NAME) || serviceName.contains("WMS")))
        {
            throw new IllegalArgumentException("The service is not a WMS service: " + (serviceName != null ? serviceName : "null"));
        }

        if (params == null) params = new AVListImpl();

        if (params.getStringValue(AVKey.LAYER_NAMES) == null)
        {
            // Use the first named layer since no other guidance given
            List<WMSLayerCapabilities> namedLayers = ((WMSCapabilities) caps).getNamedLayers();

            if (namedLayers == null || namedLayers.isEmpty() || namedLayers.get(0) == null)
            {
                throw new IllegalStateException("WMS capabilities document contains no named layers");
            }

            params.setValue(AVKey.LAYER_NAMES, namedLayers.get(0).getName());
        }

        return new ApplicationWmsTiledImageLayer((WMSCapabilities) caps, params);
    }

    @Override
    protected Layer createTiledImageLayer(Element domElement, AVList params)
    {
        String serviceName = WWXML.getText(domElement, "Service/@serviceName");

        Layer layer = switch (serviceName)
        {
            case OFFLINE_SERVICE_NAME, TILE_SERVICE_NAME -> new BasicTiledImageLayer(domElement, params);
            case MERCATOR_TILE_SERVICE_NAME -> new ApplicationMercatorTiledImageLayer(domElement, params);
            case OGCConstants.WMS_SERVICE_NAME -> new ApplicationWmsTiledImageLayer(domElement, params);
            case AVKey.SERVICE_NAME_LOCAL_RASTER_SERVER -> new LocalRasterServerLayer(domElement, params);
            default -> throw new WWUnrecognizedException("Unrecognized service type " + serviceName);
        };

        String actuate = WWXML.getText(domElement, "@actuate");
        layer.setEnabled("onLoad".equals(actuate));

        return layer;
    }

    /**
     * Convert a given DOM element back into XML.
     * <p>
     * By the time our code is executed, the original "source" is lost and all we have is an Element.
     * Some of our layers need to store the actual XML as their restorable state, so we need to reconstitute it.
     *
     * @param domElement Main element from document
     * @return XML String
     * @since 3.0
     */
    private String getStringFromDocument(Element domElement)
    {
        try
        {
            final StringWriter writer = new StringWriter();
            final DOMSource domSource = new DOMSource(domElement.getOwnerDocument());

            TransformerFactory.newInstance().newTransformer().transform(domSource, new StreamResult(writer));
            return writer.toString();
        } catch (TransformerException ex)
        {
            logger.error("Failed to reconstitute XML: {}", domElement.getTextContent());
            return "";
        }
    }
}
