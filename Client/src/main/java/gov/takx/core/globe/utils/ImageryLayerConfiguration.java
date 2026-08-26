package gov.takx.core.globe.utils;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.layers.TiledImageLayer;
import gov.nasa.worldwind.render.ScreenCredit;
import gov.nasa.worldwind.util.DataConfigurationUtils;
import gov.nasa.worldwind.util.WWXML;
import jakarta.annotation.Nonnull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;

/**
 * Wrapper around a WorldWind layer configuration document. This class provides a uniform interface to layer
 * configuration settings, methods to parse the settings from an XML document, and methods to serialize back into XML.
 *
 */
public class ImageryLayerConfiguration extends ALayerConfiguration
{
    /**
     * Create an empty configuration.
     */
    public ImageryLayerConfiguration()
    {
        // Do nothing
    }

    /**
     * Read a configuration document from a file.
     *
     * @param file A file containing a layer configuration document in World Wind's layer configuration XML format.
     * @throws IOException          if the configuration cannot be read from the URL.
     */
    public ImageryLayerConfiguration(@Nonnull File file) throws IOException
    {
        super(file);
    }

    /**
     * Read a configuration document from a URL.
     *
     * @param url An XML file containing a layer configuration document in World Wind's layer configuration XML format.
     * @throws IOException          if the configuration cannot be read from the URL.
     */
    public ImageryLayerConfiguration(@Nonnull URL url) throws IOException
    {
        super(url);
    }

    /**
     * Parse a configuration document from an XML string.
     *
     * @param xml An XML layer configuration document in World Wind's layer configuration XML format.
     * @throws SAXException         if an error occurs while parsing the XML.
     */
    public ImageryLayerConfiguration(@Nonnull String xml) throws SAXException
    {
        super(xml);
    }

    /**
     * Create a configuration from a list of key/value pairs.
     *
     * @param list List of key/value pairs.
     */
    public ImageryLayerConfiguration(@Nonnull AVList list)
    {
        fields = list.copy();
    }

    public ImageryLayerConfiguration(@Nonnull Element element)
    {
        init(element);
    }

    @Override
    protected void init(Element element)
    {
        DataConfigurationUtils.getWMSLayerConfigParams(element, fields);
        TiledImageLayer.getTiledImageLayerConfigParams(element, fields);
    }

    @Override
    public String toXml()
    {
        try
        {
            Document document = TiledImageLayer.createTiledImageLayerConfigDocument(fields);
            DataConfigurationUtils.createWMSLayerConfigElements(fields, document.getDocumentElement());

            // Add transparency colors to the generated document.
            // TODO: WW should add these elements but does not. Remove this code when WW bug is fixed. There is a unit
            // TODO: test in ImageryLayerConfigurationTest to detect if the element is added twice. See WWJ-458.
            Color[] colors = getTransparencyColors();
            if (colors != null && colors.length > 0)
            {
                Element listEl = WWXML.appendElementPath(document.getDocumentElement(), "TransparencyColors");
                for (Color color : colors)
                {
                    Element colorEl = WWXML.appendElement(listEl, "Color");
                    WWXML.setIntegerAttribute(colorEl, "red", color.getRed());
                    WWXML.setIntegerAttribute(colorEl, "green", color.getGreen());
                    WWXML.setIntegerAttribute(colorEl, "blue", color.getBlue());
                }
            }
            // TODO: End of TransparencyColor workaround

            StringWriter writer = new StringWriter();
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.transform(new DOMSource(document), new StreamResult(writer));

            return writer.toString();
        } catch (TransformerException e)
        {
            throw new UnsupportedOperationException(e);
        }
    }

    ///////////////////////////////////////////////////////
    // Configuration elements specific to elevation models
    ///////////////////////////////////////////////////////

    /**
     * Indicates the format used to store images in texture memory. May be {@code null} to indicate that images should be stored
     * in their native format.
     *
     * @return image format (e.g. "image/dds") or {@code null}.
     */
    public String getTextureFormat()
    {
        return fields.getStringValue(AVKey.TEXTURE_FORMAT);
    }

    /**
     * Specifies the format used to store images in texture memory. May be {@code null} to indicate that images should be stored
     * in their native format.
     *
     * @param format an image format (e.g. "image/dds") or {@code null}.
     */
    public void setTextureFormat(String format)
    {
        fields.setValue(AVKey.TEXTURE_FORMAT, format);
    }

    /**
     * Retrieves the layer opacity.
     *
     * @return Layer opacity as a floating point value between zero (fully transparent) and one (fully opaque),
     * or {@code null}, indicating default opacity (fully opaque).
     */
    public Double getOpacity()
    {
        return AVListImpl.getDoubleValue(fields, AVKey.OPACITY);
    }

    /**
     * Specifies the layer opacity.
     *
     * @param opacity A value between zero and one. Zero indicates complete transparency, and one indicates a fully
     *                opaque image. Specify {@code null} to indicate default opacity.
     */
    public void setOpacity(Double opacity)
    {
        fields.setValue(AVKey.OPACITY, opacity);
    }

    /**
     * Indicates the minimum altitude at which this layer is visible.
     *
     * @return The minimum altitude (in meters), or {@code null} if there is no minimum.
     */
    public Double getMinActiveAltitude()
    {
        return AVListImpl.getDoubleValue(fields, AVKey.MIN_ACTIVE_ALTITUDE);
    }

    /**
     * Specifies the minimum altitude at which this layer is visible.
     *
     * @param altitude Altitude in meters, or {@code null} to specify that there is no minimum.
     */
    public void setMinActiveAltitude(Double altitude)
    {
        fields.setValue(AVKey.MIN_ACTIVE_ALTITUDE, altitude);
    }

    /**
     * Indicates the maximum altitude at which this layer is visible.
     *
     * @return The maximum altitude (in meters), or {@code null} if there is no maximum.
     */
    public Double getMaxActiveAltitude()
    {
        return AVListImpl.getDoubleValue(fields, AVKey.MAX_ACTIVE_ALTITUDE);
    }

    /**
     * Specifies the maximum altitude at which this layer is visible.
     *
     * @param altitude Altitude in meters, or {@code null} to specify that there is no maximum.
     */
    public void setMaxActiveAltitude(Double altitude)
    {
        fields.setValue(AVKey.MAX_ACTIVE_ALTITUDE, altitude);
    }

    /**
     * Indicates the scale of the map contained in this layer. The map scale is given as a ratio 1:X, where X is the
     * number returned by this method (e.g., 1:24000).
     *
     * @return The denominator of the map scale ratio (the X in 1:X), or {@code null} if the map scale is not set.
     */
    public Double getMapScale()
    {
        return AVListImpl.getDoubleValue(fields, AVKey.MAP_SCALE);
    }

    /**
     * Specifies the scale of the map contained in this layer.
     *
     * @param scale The denominator of the map scale ratio (the X in 1:X), or {@code null} to the map scale.
     */
    public void setMapScale(Double scale)
    {
        fields.setValue(AVKey.MAP_SCALE, scale);
    }

    /**
     * Indicates a caption or image that should be displayed on the screen when this layer is visible.
     *
     * @return Screen credit or {@code null}.
     */
    public ScreenCredit getScreenCredit()
    {
        Object o = fields.getValue(AVKey.SCREEN_CREDIT);
        return (o instanceof ScreenCredit) ? (ScreenCredit) o : null;
    }

    /**
     * Specifies a caption or image that should be displayed on the screen when this layer is visible.
     *
     * @param credit New screen credit, or {@code null}.
     */
    public void setScreenCredit(ScreenCredit credit)
    {
        fields.setValue(AVKey.SCREEN_CREDIT, credit);
    }

    /**
     * Indicates whether or not this layer should be rendered using mipmaps. Mipmaps are precomputed versions of a
     * texture at different sizes (i.e. full size, half size, quarter size), and their use often improves rendering
     * performance.
     *
     * @return True if mipmaps should be used to render the layer, false if not, and {@code null} if the property is not set.
     */
    public Boolean isUseMipMaps()
    {
        return AVListImpl.getBooleanValue(fields, AVKey.USE_MIP_MAPS);
    }

    /**
     * Specifies whether or not mipmaps should be used to render this layer.
     *
     * @param useMipMaps True if mipmaps should be used, false if they should not be used, or {@code null} to clear the property.
     * @see #isUseMipMaps()
     */
    public void setUseMipMaps(Boolean useMipMaps)
    {
        fields.setValue(AVKey.USE_MIP_MAPS, useMipMaps);
    }

    /**
     * Indicates whether or not transparent textures should be used for this layer. Must be enabled if the layer
     * includes transparency.
     *
     * @return true if transparent textures will be used to render this layer.
     */
    public Boolean isUseTransparentTextures()
    {
        return AVListImpl.getBooleanValue(fields, AVKey.USE_TRANSPARENT_TEXTURES);
    }

    /**
     * Specifies whether or not transparent textures should be used for this layer. Must be enabled if the layer
     * includes transparency.
     *
     * @param useTransparentTextures true if transparent textures will be used to render this layer.
     */
    public void setUseTransparentTextures(Boolean useTransparentTextures)
    {
        fields.setValue(AVKey.USE_TRANSPARENT_TEXTURES, useTransparentTextures);
    }

    /**
     * Indicates colors in the layer imagery that represent transparent areas.
     *
     * @return colors that represent transparency.
     */
    public Color[] getTransparencyColors()
    {
        // WW AVList representation uses int[] to represent colors.
        Object o = fields.getValue(AVKey.TRANSPARENCY_COLORS);
        if (o instanceof int[])
        {
            int[] rgb = (int[]) o;
            Color[] colors = new Color[rgb.length];
            for (int i = 0; i < rgb.length; i++)
            {
                colors[i] = new Color(rgb[i]);
            }
            return colors;
        }
        return null;
    }

    /**
     * Specifies colors in the layer imagery that represent transparent areas.
     *
     * @param colors Array of colors that represent transparency.
     */
    public void setTransparencyColors(Color[] colors)
    {
        // WW AVList representation uses int[] to represent colors.
        if (colors != null)
        {
            int rgb[] = new int[colors.length];
            for (int i = 0; i < colors.length; i++)
            {
                rgb[i] = colors[i].getRGB();
            }
            fields.setValue(AVKey.TRANSPARENCY_COLORS, rgb);
        } else
        {
            fields.setValue(AVKey.TRANSPARENCY_COLORS, null);
        }
    }

    /**
     * Indicates whether or not level zero tiles (lowest resolution tiles) will always be loaded.
     *
     * @return true of level zero tiles will be forced to load.
     */
    public Boolean isForceLevelZeroLoads()
    {
        return AVListImpl.getBooleanValue(fields, AVKey.FORCE_LEVEL_ZERO_LOADS);
    }

    /**
     * Specifies whether or not level zero tiles (lowest resolution tiles) will always be loaded.
     *
     * @param forceLoads true of level zero tiles will be forced to load.
     */
    public void setForceLevelZeroLoads(boolean forceLoads)
    {
        fields.setValue(AVKey.FORCE_LEVEL_ZERO_LOADS, forceLoads);
    }

    /**
     * Indicates whether or not level zero tiles (lowest resolution tiles) should be kept in the in-memory tile cache.
     *
     * @return True if level zero tiles should be kept in the cache, false if not, or {@code null} if the property is not set.
     */
    public Boolean isRetainLevelZeroTiles()
    {
        return AVListImpl.getBooleanValue(fields, AVKey.RETAIN_LEVEL_ZERO_TILES);
    }

    /**
     * Specifies whether or not level zero tiles (lowest resolution tiles) should be kept in the in-memory tile cache.
     *
     * @param retainTiles True if level zero tiles should be kept in the cache, false if not, or {@code null} to clear the
     *                    property.
     */
    public void setRetainLevelZeroTiles(Boolean retainTiles)
    {
        fields.setValue(AVKey.RETAIN_LEVEL_ZERO_TILES, retainTiles);
    }

    /**
     * Indicates a timeout value to apply when connecting to resources for this layer.
     *
     * @return Timeout value in milliseconds, or {@code null} to indicate no timeout.
     */
    public Integer getConnectTimeout()
    {
        return AVListImpl.getIntegerValue(fields, AVKey.URL_CONNECT_TIMEOUT);
    }

    /**
     * Specifies a timeout value to apply to when connecting to resources for this layer.
     *
     * @param timeout Timeout value in milliseconds, or {@code null} to indicate no timeout.
     */
    public void setConnectTimeout(Integer timeout)
    {
        fields.setValue(AVKey.URL_CONNECT_TIMEOUT, timeout);
    }

    /**
     * Indicates a timeout value to apply to when reading resources for this layer.
     *
     * @return Timeout value in milliseconds, or {@code null} to indicate no timeout.
     */
    public Integer getReadTimeout()
    {
        return AVListImpl.getIntegerValue(fields, AVKey.URL_READ_TIMEOUT);
    }

    /**
     * Specifies a timeout value to apply to when reading resources for this layer.
     *
     * @param timeout Timeout value in milliseconds, or {@code null} to indicate no timeout.
     */
    public void setReadTimeout(Integer timeout)
    {
        fields.setValue(AVKey.URL_READ_TIMEOUT, timeout);
    }

    /**
     * Indicates how long a request may stay in the request queue. If a retrieval task for this layer is left in the
     * request queue for longer than the timeout, then the request is considered stale and is discarded.
     *
     * @return The stale request timeout, in milliseconds, or {@code null} if no timeout is set.
     */
    public Integer getStaleRequestLimit()
    {
        return AVListImpl.getIntegerValue(fields, AVKey.RETRIEVAL_QUEUE_STALE_REQUEST_LIMIT);
    }

    /**
     * Specifies how long a request may stay in the request queue. If a retrieval task for this layer is left in the
     * request queue for longer than the timeout, then the request is considered stale and is discarded.
     *
     * @param limit The stale request timeout, in milliseconds, or {@code null} to indicate no timeout.
     */
    public void setStaleRequestLimit(Integer limit)
    {
        fields.setValue(AVKey.RETRIEVAL_QUEUE_STALE_REQUEST_LIMIT, limit);
    }
}
