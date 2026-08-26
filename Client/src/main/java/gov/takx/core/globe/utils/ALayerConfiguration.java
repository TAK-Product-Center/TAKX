package gov.takx.core.globe.utils;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.DataConfigurationUtils;
import gov.nasa.worldwind.util.LevelSet;
import gov.takx.commons.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Wrapper around an abstract WorldWind data configuration document. This class captures configuration properties that
 * are common to both imagery layer configuration and elevation model configuration.
 *
 */
public abstract class ALayerConfiguration
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Key-value map to store configuration properties.
     */
    protected AVList fields = new AVListImpl();

    /**
     * Create an empty configuration.
     */
    protected ALayerConfiguration()
    {
        // Do nothing
    }

    /**
     * Read a configuration document from a file.
     *
     * @param file A file containing a layer configuration document in World Wind's layer configuration XML format.
     * @throws java.io.IOException  if the configuration cannot be read from the URL.
     */
    protected ALayerConfiguration(@Nonnull File file) throws IOException
    {
        try
        {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = builder.parse(new InputSource(new FileReader(file)));

            init(document.getDocumentElement());
        } catch (ParserConfigurationException | SAXException e)
        {
            throw new IOException(e);
        }
    }

    /**
     * Read a configuration document from a URL.
     *
     * @param url An XML file containing a layer configuration document in World Wind's layer configuration XML format.
     * @throws IOException          if the configuration cannot be read from the URL.
     */
    protected ALayerConfiguration(@Nonnull URL url) throws IOException
    {
        try
        {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = builder.parse(url.openStream(), url.toString());

            init(document.getDocumentElement());
        } catch (ParserConfigurationException | SAXException e)
        {
            throw new IOException(e);
        }
    }

    /**
     * Parse a configuration document from an XML string.
     *
     * @param xml An XML layer configuration document in World Wind's layer configuration XML format.
     * @throws SAXException         if an error occurs while parsing the XML.
     */
    protected ALayerConfiguration(@Nonnull String xml) throws SAXException
    {
        try
        {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xml)));

            init(document.getDocumentElement());
        } catch (ParserConfigurationException | IOException e)
        {
            throw new SAXException(e);
        }
    }

    /**
     * Initialize the configuration from an XML document.
     *
     * @param element Root element of the XML configuration document.
     */
    protected abstract void init(Element element);

    /**
     * Serialize the configuration to an XML configuration document.
     *
     * @return XML string for the configuration.
     */
    public abstract String toXml();

    /**
     * Convert the configuration to a list of key/value pairs. AVLists are used by many of World Wind's layer and
     * elevation model implementations.
     *
     * @return A key/value list that contains the contents of this configuration.
     */
    public AVList toAVList()
    {
        return fields.copy();
    }

    ///////////////////////////
    // Common layer parameters
    ///////////////////////////

    /**
     * Indicates the name for this data set. This a name for the system to use, not for display.
     *
     * @return Layer display name.
     * @see #setDisplayName(String)
     */
    public String getDataSetName()
    {
        return fields.getStringValue(AVKey.DATASET_NAME);
    }

    /**
     * Specifies the name for this data set. This is a name for the system to use, not for display.
     *
     * @param name New display name.
     * @see #getDisplayName()
     */
    public void setDataSetName(String name)
    {
        fields.setValue(AVKey.DATASET_NAME, name);
    }

    /**
     * Indicates the display name for this layer.
     *
     * @return Layer display name.
     */
    public String getDisplayName()
    {
        return fields.getStringValue(AVKey.DISPLAY_NAME);
    }

    /**
     * Specifies the display name for this layer.
     *
     * @param name New display name.
     */
    public void setDisplayName(String name)
    {
        fields.setValue(AVKey.DISPLAY_NAME, name);
    }

    /**
     * Indicates whether or not images from this layer will be retrieved over the network.
     *
     * @return True if images for this layer should be retrieved from the network.
     */
    public Boolean isNetworkRetrievalEnabled()
    {
        return AVListImpl.getBooleanValue(fields, AVKey.NETWORK_RETRIEVAL_ENABLED);
    }

    /**
     * Specifies whether or not images from this layer should be retrieved over the network.
     *
     * @param enabled True if images for this layer should be retrieved from the network.
     */
    public void setNetworkRetrievalEnabled(Boolean enabled)
    {
        fields.setValue(AVKey.NETWORK_RETRIEVAL_ENABLED, enabled);
    }

    /**
     * Indicates whether or not non-tile resources (such as the layer expiration time) should be retrieved from the
     * layer service.
     *
     * @return True if non-tiles resources should be retrieved, false if not, or {@code null} if the property is not set.
     */
    @Nullable
    public Boolean isRetrievePropertiesFromService()
    {
        return AVListImpl.getBooleanValue(fields, AVKey.RETRIEVE_PROPERTIES_FROM_SERVICE);
    }

    /**
     * Specifies whether or not non-tile resources should be retrieved from the layer's service.
     *
     * @param val True if non-tile resources should be retrieved, false if not, or {@code null} to clear the property.
     */
    public void setRetrievePropertiesFromService(Boolean val)
    {
        fields.setValue(AVKey.RETRIEVE_PROPERTIES_FROM_SERVICE, val);
    }

    ///////////////////////////////
    // WMS parameters
    ///////////////////////////////

    /**
     * Indicates the type of service that provides data for this layer. For example, "OGC:WMS" in the case of a WMS layer.
     *
     * @return The service name or {@code null} if the property is not set.
     */
    @Nullable
    public String getServiceName()
    {
        return fields.getStringValue(AVKey.SERVICE_NAME);
    }

    /**
     * Specifies the type of service that provides data for this layer. For example, "OGC:WMS" in the case of a WMS layer.
     *
     * @param name New service name, or {@code null} to clear the property.
     */
    public void setServiceName(String name)
    {
        fields.setValue(AVKey.SERVICE_NAME, name);
    }

    /**
     * Indicates the service URL for this layer. In the case of a layer served by the World Wind tile server, this is
     * the URL used to fetch map tiles. In the case of a WMS server it is the GetMap URL.
     *
     * @return Service URL, or {@code null} if not set.
     */
    @Nullable
    public String getService()
    {
        return fields.getStringValue(AVKey.SERVICE);
    }

    /**
     * Specifies the service URL for this layer. In the case of a layer served by the World Wind tile server, this is
     * the URL used to fetch map tiles. In the case of a WMS server it is the GetMap URL.
     *
     * @param url New service URL, or {@code null} to clear the parameter.
     */
    public void setService(String url)
    {
        fields.setValue(AVKey.SERVICE, url);
    }

    /**
     * Indicates the version of the WMS protocol to use when communicating with for this layer's WMS server.
     *
     * @return The WMS version string.
     */
    public String getWMSVersion()
    {
        return fields.getStringValue(AVKey.WMS_VERSION);
    }

    /**
     * Specifies the version of the WMS protocol to use when communicating with for this layer's WMS server.
     *
     * @param version New WMS version string (e.g. "1.3").
     */
    public void setWMSVersion(String version)
    {
        fields.setValue(AVKey.WMS_VERSION, version);
    }

    /**
     * Indicates the WMS GetCapabilities URL for this layer.
     *
     * @return WMS GetCapabilities URL.
     */
    @Nullable
    public URL getCapabilitiesURL()
    {
        String s = fields.getStringValue(AVKey.GET_CAPABILITIES_URL);
        try
        {
            if (s != null)
            {
                return new URL(s);
            }
        } catch (MalformedURLException e)
        {
            logger.warn("Malformed GetCapabilities URL: {}", s);
        }
        return null;
    }

    /**
     * Specifies the WMS GetCapabilities URL for this layer.
     *
     * @param url new GetCapabilities URL.
     */
    public void setCapabilitiesURL(URL url)
    {
        fields.setValue(AVKey.GET_CAPABILITIES_URL, url != null ? url.toString() : null);
    }

    /**
     * Indicates the WMS GetMap URL for this layer.
     *
     * @return WMS GetMap URL.
     */
    @Nullable
    public URL getMapURL()
    {
        String s = fields.getStringValue(AVKey.GET_MAP_URL);
        try
        {
            if (s != null)
            {
                return new URL(s);
            }
        } catch (MalformedURLException e)
        {
            logger.warn("Malformed GetMap URL: {}", s);
        }
        return null;
    }

    /**
     * Specifies the WMS GetMap URL for this layer.
     *
     * @param url new GetMap URL.
     */
    public void setMapURL(URL url)
    {
        fields.setValue(AVKey.GET_MAP_URL, url != null ? url.toString() : null);
    }

    /**
     * Indicates the names of the WMS layers that will be retrieved by this layer. These names must match the named
     * layers configured in the WMS server.
     *
     * @return Array of layer names, or {@code null} if not set.
     */
    @Nullable
    public String[] getLayerNames()
    {
        return DataConfigurationUtils.getOGCLayerNames(fields);
    }

    /**
     * Specifies the names of the WMS layers that will be retrieved by this layer. These names must match the named
     * layers configured in the WMS server.
     *
     * @param names New layer names, or {@code null} to clear the property.
     */
    public void setLayerNames(String[] names)
    {
        // Layer names are stored as a comma separated string, for compatibility with World Wind's AVList representation.
        fields.setValue(AVKey.LAYER_NAMES, StringUtils.join(names));
    }

    /**
     * Indicates the names of WMS styles to request from the server when retrieving resources for this layer.
     *
     * @return Array of style names, or {@code null} if not set.
     */
    @Nullable
    public String[] getStyleNames()
    {
        String s = fields.getStringValue(AVKey.STYLE_NAMES);
        if (s == null || s.isEmpty())
        {
            return null;
        }

        return s.split(",");
    }

    /**
     * Specifies the names of WMS styles to request from the server when retrieving resources for this layer.
     *
     * @param styles New style names, or {@code null} to clear the property.
     */
    public void setStyleNames(String[] styles)
    {
        fields.setValue(AVKey.STYLE_NAMES, StringUtils.join(styles));
    }

    /////////////////////////
    // Data cache parameters
    /////////////////////////

    /**
     * Indicates the name of the data cache that will hold cached resources for this layer. The data cache name
     * identifies a folder in the World Wind file store. For example, if the data cache name is "MyCache", resources for
     * the layer will be stored in file-store/MyCache.
     *
     * @return The data cache name, or {@code null} if the property is not set.
     */
    @Nullable
    public String getDataCacheName()
    {
        return fields.getStringValue(AVKey.DATA_CACHE_NAME);
    }

    /**
     * Specifies the name of the data cache that will hold cached resources for this layer.
     *
     * @param name New cache name, or {@code null} to clear the property.
     * @see #getDataCacheName()
     */
    public void setDataCacheName(String name)
    {
        fields.setValue(AVKey.DATA_CACHE_NAME, name);
    }

    /**
     * Indicates the time at which the layer expires.
     *
     * @return expiration time, in milliseconds since the Epoch, or {@code null} if no time has been set.
     */
    @Nullable
    public Long getExpiryTime()
    {
        return AVListImpl.getLongValue(fields, AVKey.EXPIRY_TIME);
    }

    /**
     * Specifies the time at which the layer expires.
     *
     * @param time expiration time, in milliseconds since the Epoch, or {@code null} for no expiration.
     */
    public void setExpiryTime(Long time)
    {
        fields.setValue(AVKey.EXPIRY_TIME, time);
    }

    /**
     * Indicates the image format in which to store cached tiles for this layer.
     *
     * @return Image format suffix (e.g. ".png").
     */
    public String getFormatSuffix()
    {
        return fields.getStringValue(AVKey.FORMAT_SUFFIX);
    }

    /**
     * Specifies the image format in which to store cached tiles for this layer.
     *
     * @param suffix Image format suffix (e.g. ".png").
     */
    public void setFormatSuffix(String suffix)
    {
        fields.setValue(AVKey.FORMAT_SUFFIX, suffix);
    }

    /**
     * Indicates the number of levels in the imagery pyramid for this level.
     *
     * @return Number of levels, or {@code null} if not set.
     */
    @Nullable
    public Integer getNumLevels()
    {
        return AVListImpl.getIntegerValue(fields, AVKey.NUM_LEVELS);
    }

    /**
     * Specifies the number of levels in the imagery pyramid for this level.
     *
     * @param numLevels The number of levels in the pyramid, or {@code null} if the property is not set.
     */
    public void setNumLevels(Integer numLevels)
    {
        fields.setValue(AVKey.NUM_LEVELS, numLevels);
    }

    /**
     * Indicates the number of empty levels in the top (low resolution) layers of the pyramid.
     *
     * @return The number of empty layers at the top of the pyramid, or {@code null} if the property is not set.
     */
    @Nullable
    public Integer getNumEmptyLevels()
    {
        return AVListImpl.getIntegerValue(fields, AVKey.NUM_EMPTY_LEVELS);
    }

    /**
     * Specifies the number of empty levels in the top (low resolution) layers of the pyramid.
     *
     * @param numLevels The number of empty layers at the top of the pyramid, or {@code null} to clear the property.
     */
    public void setNumEmptyLevels(Integer numLevels)
    {
        fields.setValue(AVKey.NUM_EMPTY_LEVELS, numLevels);
    }

    /**
     * Indicates layers that are marked as inactive in this data set.
     *
     * @return Array of layer names, or {@code null} if not set.
     */
    @Nullable
    public String[] getInactiveLevels()
    {
        String s = fields.getStringValue(AVKey.INACTIVE_LEVELS);
        if (s == null || s.isEmpty())
        {
            return null;
        }

        return s.split(",");
    }

    /**
     * Specifies layers that are marked as inactive in this data set.
     *
     * @param names Names of inactive layers, or {@code null}.
     */
    public void setInactiveLevels(String[] names)
    {
        fields.setValue(AVKey.INACTIVE_LEVELS, StringUtils.join(names));
    }

    /**
     * Indicates an array of resolution limits that give the highest available resolution for certain sectors of the
     * globe. Each sector resolution object specifies a sector and the number of the highest resolution level that
     * contains data for that sector.
     *
     * @return Array of resolution limits, or {@code null} if not set.
     */
    @Nullable
    public LevelSet.SectorResolution[] getSectorResolutionLimits()
    {
        Object o = fields.getValue(AVKey.SECTOR_RESOLUTION_LIMITS);
        if (o instanceof LevelSet.SectorResolution[])
        {
            return (LevelSet.SectorResolution[]) o;
        }
        return null;
    }

    /**
     * Specifies an array of resolution limits that give the highest available resolution for certain sectors of the
     * globe.
     *
     * @param limits Array of resoltuion limits or {@code null} to clear the property.
     * @see #getSectorResolutionLimits
     */
    public void setSectorResolutionLimits(LevelSet.SectorResolution[] limits)
    {
        fields.setValue(AVKey.SECTOR_RESOLUTION_LIMITS, limits);
    }

    /**
     * Indicates the location used as the origin for the tile grid. For example, if the tile origin is 180 W, 90 N then
     * the upper left corner of the first tile in the grid falls at the point where the date line touches the north pole.
     *
     * @return Location of the origin of the tile grid, or {@code null} if the origin is not set.
     */
    @Nullable
    public LatLon getTileOrigin()
    {
        Object o = fields.getValue(AVKey.TILE_ORIGIN);
        return (o instanceof LatLon) ? (LatLon) o : null;
    }

    /**
     * Indicates the location used as the origin for the tile grid.
     *
     * @param origin Location of the origin of the tile grid, or {@code null} to clear the property.
     * @see #getTileOrigin()
     */
    public void setTileOrigin(LatLon origin)
    {
        fields.setValue(AVKey.TILE_ORIGIN, origin);
    }

    /**
     * Indicates the width in pixels of tiles in this layer.
     *
     * @return tile width in pixels.
     */
    public Integer getTileWidth()
    {
        return AVListImpl.getIntegerValue(fields, AVKey.TILE_WIDTH);
    }

    /**
     * Specifies the width, in pixels, of tiles in this layer.
     *
     * @param width new tile width.
     */
    public void setTileWidth(Integer width)
    {
        fields.setValue(AVKey.TILE_WIDTH, width);
    }

    /**
     * Indicates the height, in pixels, of tiles in this layer.
     *
     * @return tile height, in pixels.
     */
    public Integer getTileHeight()
    {
        return AVListImpl.getIntegerValue(fields, AVKey.TILE_HEIGHT);
    }

    /**
     * Specifies the height, in pixels, of tiles in this layer.
     *
     * @param height new tile height.
     */
    public void setTileHeight(Integer height)
    {
        fields.setValue(AVKey.TILE_HEIGHT, height);
    }

    /**
     * Indicates the angle covered by tiles at the first (lowest resolution) level of the pyramid. Tiles at the next
     * level will cover one quarter of the angle, below that one eighth, etc.
     *
     * @return The angles (in latitude and longitude) spanned by tiles at the lowest resolution level of the pyramid, or
     * {@code null} if the property is not set.
     */
    @Nullable
    public LatLon getLevelZeroTileDelta()
    {
        Object o = fields.getValue(AVKey.LEVEL_ZERO_TILE_DELTA);
        return (o instanceof LatLon) ? (LatLon) o : null;
    }

    /**
     * Specifies the angle covered by tiles at the first (lowest resolution) level of the pyramid. Tiles at the next
     * level will cover one quarter of the angle, below that one eighth, etc.
     *
     * @param delta The angles (in latitude and longitude) spanned by tiles at the lowest resolution level of the
     *              pyramid, or {@code null} to clear the property.
     */
    public void setLevelZeroTileDelta(LatLon delta)
    {
        fields.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, delta);
    }

    /**
     * Indicates the maximum number of times a resource is marked as absent before being marked as permanently absent.
     *
     * @return the maximum number of absent markings, or {@code null} if no maximum is set.
     */
    @Nullable
    public Integer getMaxAbsentTileAttempts()
    {
        return AVListImpl.getIntegerValue(fields, AVKey.MAX_ABSENT_TILE_ATTEMPTS);
    }

    /**
     * Specifies the maximum number of times a resource is marked as absent before being marked as permanently absent.
     *
     * @param max the number of max tries, or {@code null} for the default.
     */
    public void setMaxAbsentTileAttempts(Integer max)
    {
        fields.setValue(AVKey.MAX_ABSENT_TILE_ATTEMPTS, max);
    }

    /**
     * Indicates the time interval that must elapse before the resource is considered not absent if its max-tries has
     * not been reached.
     *
     * @return the interval, in milliseconds, or {@code null} to indicate the interval has not be set.
     */
    @Nullable
    public Integer getMinAbsentTileCheckInterval()
    {
        return AVListImpl.getIntegerValue(fields, AVKey.MIN_ABSENT_TILE_CHECK_INTERVAL);
    }

    /**
     * Specifies the time interval that must elapse before the resource is considered absent if its max-tries has not
     * been reached.
     *
     * @param interval the check interval, or {@code null} if no interval has been set.
     */
    public void setMinAbsentTileCheckInterval(Integer interval)
    {
        fields.setValue(AVKey.MIN_ABSENT_TILE_CHECK_INTERVAL, interval);
    }

    /**
     * Indicates the sector that bounds this layer.
     *
     * @return The geographic bounds of the layer.
     */
    public Sector getSector()
    {
        Object o = fields.getValue(AVKey.SECTOR);
        return (o instanceof Sector) ? (Sector) o : null;
    }

    /**
     * Specifies the sector that bounds this layer.
     *
     * @param sector The geographic bounds of the layer.
     */
    public void setSector(Sector sector)
    {
        fields.setValue(AVKey.SECTOR, sector);
    }

    /**
     * Indicates the image format to use when requesting tiles from a WMS server.
     *
     * @return the image format (e.g. "image/png").
     */
    public String getImageFormat()
    {
        return fields.getStringValue(AVKey.IMAGE_FORMAT);
    }

    /**
     * Specifies the image format to use when requesting tiles from a WMS server.
     *
     * @param format the image format (e.g. "image/png").
     */
    public void setImageFormat(String format)
    {
        fields.setValue(AVKey.IMAGE_FORMAT, format);
    }

    /**
     * Indicates the image formats available for this layer.
     *
     * @return Array of image formats.
     */
    public String[] getAvailableImageFormats()
    {
        Object o = fields.getValue(AVKey.AVAILABLE_IMAGE_FORMATS);
        if (o instanceof String[])
        {
            return (String[]) o;
        }
        return null;
    }

    /**
     * Specifies the image formats available for this layer.
     *
     * @param formats Array of image formats.
     */
    public void setAvailableImageFormats(String[] formats)
    {
        fields.setValue(AVKey.AVAILABLE_IMAGE_FORMATS, formats);
    }

    /**
     * Modifies the default relationship of image resolution to screen resolution as the viewing altitude changes.
     * Values greater than 0 cause imagery to appear at higher resolution at greater altitudes than normal, but at an
     * increased performance cost. Values less than 0 decrease the default resolution at any given altitude. The default
     * value is 0. Values typically range between -0.5 and 0.5.
     *
     * @return The detail hint value, or {@code null} is no hint is set.
     */
    @Nullable
    public Double getDetailHint()
    {
        return AVListImpl.getDoubleValue(fields, AVKey.DETAIL_HINT);
    }

    /**
     * Specifies the detail hint. See {@link #getDetailHint()} for a description of this parameter.
     *
     * @param hint new hint value, or {@code null} to indicate the default.
     */
    public void setDetailHint(Double hint)
    {
        fields.setValue(AVKey.DETAIL_HINT, hint);
    }
}
