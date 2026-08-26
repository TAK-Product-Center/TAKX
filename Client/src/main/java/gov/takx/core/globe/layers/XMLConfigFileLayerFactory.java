package gov.takx.core.globe.layers;

import com.atakmap.map.layer.Layer;
import com.atakmap.map.layer.raster.DatasetDescriptor;
import gov.nasa.worldwind.Factory;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.exception.WWUnrecognizedException;
import gov.nasa.worldwind.util.WWXML;
import gov.tak.api.annotation.DeprecatedApi;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.core.globe.TakLayerFactory;
import gov.takx.core.globe.tak.ImageryGlobeLayer;
import gov.takx.core.globe.utils.ImageryLayerConfiguration;
import gov.takx.core.view.gui.maps.exporters.dataset.MapDataWrapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * Factory service to generate {@link GlobeLayer} instances.
 */
@ApplicationScoped
public class XMLConfigFileLayerFactory
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final TakLayerFactory takLayerFactory;

    @Inject
    XMLConfigFileLayerFactory(TakLayerFactory takLayerFactory)
    {
        this.takLayerFactory = takLayerFactory;
    }

    /**
     * Factory method to build an {@link GlobeLayer} given a {@link IRaptorLayer} and the config file path
     * defining the layer to generate.
     *
     * @param raptorLayer Raptor Layer to use
     * @param configFile  Path to XML config file defining the layer to create
     * @return new instance of GlobeLayer
     */
    public GlobeLayer makeLayerFromConfigFile(IRaptorLayer raptorLayer, String configFile)
    {
        Document doc = WWXML.openDocument(configFile);
        if (doc != null)
        {
            Layer layer = createFromLayerDocument(doc.getDocumentElement());
            if (layer != null)
            {
                ImageryGlobeLayer globeLayer = new ImageryGlobeLayer(raptorLayer, layer);
                DatasetDescriptor[] descriptors = TakLayerFactory.queryForDatasetDescriptors(layer);
                if (descriptors.length > 0) globeLayer.setValue(MapDataWrapper.DATASET_DESCRIPTORS, descriptors);
                return globeLayer;
            }
        }
        return null;
    }

    protected Layer createFromLayerDocument(Element domElement)
    {
        String layerType = WWXML.getText(domElement, "@layerType");
        Layer layer = null;
        if ("TiledImageLayer".equals(layerType))
        {
            layer = createTiledImageLayer(domElement);
        } else if ("ShapefileLayer".equals(layerType))
        {
            final String displayName = WWXML.getText(domElement, "DisplayName");
            logger.warn("Creating ShapefileLayer not supported for layer: {}", displayName);
        }

        if (layer != null)
        {
            String actuate = WWXML.getText(domElement, "@actuate");
            layer.setVisible("onLoad".equals(actuate));
            WWXML.invokePropertySetters(layer, domElement);
        }

        return layer;
    }

    private Layer createTiledImageLayer(Element domElement)
    {
        String serviceName = WWXML.getText(domElement, "Service/@serviceName");
        Layer layer;
        String msg;
        if ("Offline".equals(serviceName))
        {
            // Not currently supported in TAK
            layer = null;
        } else if ("WWTileService".equals(serviceName))
        {
            // Not currently supported in TAK
            layer = null;
        } else if ("OGC:WMS".equals(serviceName))
        {
            try
            {
                layer = takLayerFactory.createWmsImageryLayer(new ImageryLayerConfiguration(domElement));
            } catch (IOException e)
            {
                logger.warn("Error creating a WMS imagery layer.", e);
                layer = null;
            }
        } else
        {
            if (!"LocalRasterServer".equals(serviceName))
            {
                msg = "Unrecognized service type " + serviceName;
                throw new WWUnrecognizedException(msg);
            }

            // Not currently supported in TAK
            layer = null;
        }

        return layer;
    }

    /**
     * Factory method to build an {@link XMLConfigFileLayer} given a {@link IRaptorLayer} and the config file path
     * defining the WW layer to generate.
     *
     * @param raptorLayer Raptor Layer to use
     * @param configFile  Path to XML config file defining the WW layer to create
     * @return new instance of XMLConfigFileLayer
     * @deprecated to be removed
     */
    @Deprecated(since = "5.5", forRemoval = true)
    @DeprecatedApi(since = "5.5", forRemoval = true)
    public XMLConfigFileLayer makeLayerFromWWConfigFile(IRaptorLayer raptorLayer, String configFile)
    {
        try
        {
            Factory factory = (Factory) WorldWind.createConfigurationComponent(AVKey.LAYER_FACTORY);
            gov.nasa.worldwind.layers.Layer layer = (gov.nasa.worldwind.layers.Layer) factory.createFromConfigSource(configFile, null);

            if (layer != null)
            {
                layer.setEnabled(true);
                return new XMLConfigFileLayer(raptorLayer, layer, configFile);
            } else
            {
                logger.error("Failed to create WW layer from config file: {}", configFile);
            }
        } catch (Exception e)
        {
            logger.error("Failed to create layer from XML config: {}", configFile, e);
        }

        return null;
    }
}
