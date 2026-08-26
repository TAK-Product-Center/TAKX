package gov.takx.core.globe.layers.worldwind;

import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.layers.mercator.BasicMercatorTiledImageLayer;
import gov.nasa.worldwind.layers.mercator.MercatorSector;
import gov.nasa.worldwind.util.DataConfigurationUtils;
import gov.nasa.worldwind.util.LevelSet;
import gov.nasa.worldwind.util.Tile;
import gov.nasa.worldwind.util.TileUrlBuilder;
import gov.nasa.worldwind.util.WWXML;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import javax.xml.xpath.XPath;

import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Tiled image layer that supports Mercator projections. This class will be used for layers with a serviceName of
 * "WWMercatorTileService". Layers that use this class must provide the following elements in the layer config xml:
 * <ul>
 *     <li>DisplayName</li>
 *     <li>Service/URL</li>
 *     <li>DatasetName</li>
 *     <li>DataCacheName</li>
 *     <li>NumLevels</li>
 *     <li>LevelZeroTileDelta</li>
 *     <li>TileSize</li>
 * </ul>
 * <p>
 * Here is an example of a valid config file that will utilize this class for a Mercator tile layer:
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <Layer version="1" layerType="TiledImageLayer">
 *     <DisplayName>Waves</DisplayName>
 *     <Service serviceName="WWMercatorTileService">
 *         <URL>http://tiles.mydataservice.com/</URL>
 *     </Service>
 *     <DatasetName>waves</DatasetName>
 *     <DataCacheName>Earth/Mercator/Waves</DataCacheName>
 *     <FormatSuffix>.png</FormatSuffix>
 *     <NumLevels count="16" numEmpty="0"/>
 *     <LevelZeroTileDelta>
 *         <LatLon units="degrees" latitude="22.5" longitude="45.0"/>
 *     </LevelZeroTileDelta>
 *     <TileSize>
 *         <Dimension width="256" height="256"/>
 *     </TileSize>
 * </Layer>
 * }</pre>
 *
 * @since 2.38 (renamed from RaptorMercatorTiledImageLayer in 3.4)
 */
public class ApplicationMercatorTiledImageLayer extends BasicMercatorTiledImageLayer
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Constructor.
     *
     * @param domElement The document element of the XML config file
     * @param params     Any additional parameters (may be {@code null})
     */
    public ApplicationMercatorTiledImageLayer(Element domElement, AVList params)
    {
        this(getParams(domElement, params));
    }

    /**
     * Constructor.
     *
     * @param params The list of parameters for defining the layer
     */
    public ApplicationMercatorTiledImageLayer(AVList params)
    {
        super(new LevelSet(params));
        logger.debug("Creating Mercator Tiled Image Layer: {}", params);
    }

    /**
     * Gets the list of attribute/value parameters for defining the layer.
     *
     * @param domElement The document element of the XML config file
     * @param params     Any additional parameters (may be {@code null})
     * @return The list of parameters to define the layer.
     */
    private static AVList getParams(@Nonnull Element domElement, AVList params)
    {
        if (params == null)
        {
            params = new AVListImpl();
        }

        getMercatorTiledImageLayerConfigParams(domElement, params);

        params.setValue("gov.nasa.worldwind.avKey.Sector", new MercatorSector(-1.0D, 1.0D, Angle.NEG180, Angle.POS180));
        params.setValue("gov.nasa.worldwind.avkey.TileURLBuilder", new URLBuilder());

        return params;
    }

    /**
     * Gets the list of attribute/value parameters from the provided document.
     *
     * @param domElement The document element of the XML config file
     * @param params     Any additional parameters (or an empty params list if no additional params were provided)
     * @return The list of parameters from the document (or the additional params if any were provided).
     */
    protected static AVList getMercatorTiledImageLayerConfigParams(Element domElement, AVList params)
    {
        XPath xpath = WWXML.makeXPath();
        AbstractLayer.getLayerConfigParams(domElement, params);
        DataConfigurationUtils.getLevelSetConfigParams(domElement, params);
        WWXML.checkAndSetBooleanParam(domElement, params, "gov.nasa.worldwind.avkey.RetrievePropertiesFromService", "RetrievePropertiesFromService", xpath);
        WWXML.checkAndSetStringParam(domElement, params, "gov.nasa.worldwind.avkey.ImageFormat", "ImageFormat", xpath);
        WWXML.checkAndSetStringParam(domElement, params, "gov.nasa.worldwind.avkey.TextureFormat", "TextureFormat", xpath);
        WWXML.checkAndSetUniqueStringsParam(domElement, params, "gov.nasa.worldwind.avkey.AvailableImageFormats", "AvailableImageFormats/ImageFormat", xpath);
        WWXML.checkAndSetBooleanParam(domElement, params, "gov.nasa.worldwind.avkey.ForceLevelZeroLoads", "ForceLevelZeroLoads", xpath);
        WWXML.checkAndSetBooleanParam(domElement, params, "gov.nasa.worldwind.avkey.RetainLevelZeroTiles", "RetainLevelZeroTiles", xpath);
        WWXML.checkAndSetBooleanParam(domElement, params, "gov.nasa.worldwind.avkey.UseMipMaps", "UseMipMaps", xpath);
        WWXML.checkAndSetBooleanParam(domElement, params, "gov.nasa.worldwind.avkey.UseTransparentTextures", "UseTransparentTextures", xpath);
        WWXML.checkAndSetDoubleParam(domElement, params, "gov.nasa.worldwind.avkey.DetailHint", "DetailHint", xpath);
        WWXML.checkAndSetColorArrayParam(domElement, params, "gov.nasa.worldwind.avkey.TransparencyColors", "TransparencyColors/Color", xpath);
        WWXML.checkAndSetTimeParamAsInteger(domElement, params, "gov.nasa.worldwind.avkey.URLConnectTimeout", "RetrievalTimeouts/ConnectTimeout/Time", xpath);
        WWXML.checkAndSetTimeParamAsInteger(domElement, params, "gov.nasa.worldwind.avkey.URLReadTimeout", "RetrievalTimeouts/ReadTimeout/Time", xpath);
        WWXML.checkAndSetTimeParamAsInteger(domElement, params, "gov.nasa.worldwind.avkey.RetrievalStaleRequestLimit", "RetrievalTimeouts/StaleRequestLimit/Time", xpath);

        return params;
    }

    /**
     * URL Builder for specifying the correct tile using the format "{ServiceURLKey}{z}/{y}/{x}.{FormatSuffixKey}".
     */
    private static class URLBuilder implements TileUrlBuilder
    {
        private URLBuilder()
        {
        }

        @Override
        public URL getURL(Tile tile, String imageFormat) throws MalformedURLException
        {
            // This logic was copied from WorldWind's OSMMapnikLayer class so unfortunately I don't know the
            // justification behind the numbers used here.
            return new URL(tile.getLevel().getService()
                    + (tile.getLevelNumber() + 3) + "/"
                    + tile.getColumn() + "/"
                    + ((1 << tile.getLevelNumber() + 3) - 1 - tile.getRow())
                    + tile.getLevel().getFormatSuffix());
        }
    }
}
