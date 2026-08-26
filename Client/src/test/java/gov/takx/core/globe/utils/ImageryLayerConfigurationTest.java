package gov.takx.core.globe.utils;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.render.ScreenCredit;
import gov.nasa.worldwind.render.ScreenCreditImage;
import gov.nasa.worldwind.util.LevelSet;
import gov.nasa.worldwind.util.WWIO;
import gov.nasa.worldwind.util.WWXML;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.awt.Color;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for layer configuration file parser.
 *
 */
public class ImageryLayerConfigurationTest
{
    @Test
    public void testParsingFile() throws IOException
    {
        URL url = getClass().getClassLoader().getResource("LayerTest.xml");
        ImageryLayerConfiguration conf = new ImageryLayerConfiguration(url);

        assertExpected(conf);
    }

    @Test
    public void testParsingString() throws IOException, SAXException
    {
        URL url = getClass().getClassLoader().getResource("LayerTest.xml");
        String xml = WWIO.readURLContentToString(url, null);
        ImageryLayerConfiguration conf = new ImageryLayerConfiguration(xml);

        assertExpected(conf);
    }

    private void assertExpected(ImageryLayerConfiguration conf) throws MalformedURLException
    {
        assertThat(conf.getDisplayName()).isEqualTo("Earth at Night");
        assertThat(conf.getServiceName()).isEqualTo("OGC:WMS");
        assertThat(conf.getWMSVersion()).isEqualTo("1.3");
        assertThat(conf.getCapabilitiesURL()).isEqualTo(new URL("http://worldwind25.arc.nasa.gov/wms"));
        assertThat(conf.getMapURL()).isEqualTo(new URL("http://worldwind25.arc.nasa.gov/wms"));
        assertThat(conf.getLayerNames()).isEqualTo(new String[]{"earthatnight"});
        assertThat(conf.isRetrievePropertiesFromService()).isTrue();
        assertThat(conf.getDataCacheName()).isEqualTo("Earth/EarthAtNight");
        assertThat(conf.getImageFormat()).isEqualTo("image/jpeg");
        assertThat(conf.getAvailableImageFormats()).isEqualTo(new String[]{"image/jpeg", "image/png"});
        assertThat(conf.getFormatSuffix()).isEqualTo(".dds");
        assertThat((int) conf.getNumLevels()).isEqualTo(6);
        assertThat((int) conf.getNumEmptyLevels()).isEqualTo(0);
        assertThat(conf.getTileOrigin()).isEqualTo(LatLon.fromDegrees(-90, -180));
        assertThat(conf.getLevelZeroTileDelta()).isEqualTo(LatLon.fromDegrees(36, 36));
        assertThat((int) conf.getTileWidth()).isEqualTo(512);
        assertThat((int) conf.getTileHeight()).isEqualTo(1024);
        assertThat((Iterable<? extends LatLon>) conf.getSector()).isEqualTo(Sector.fromDegrees(-90, 90, -180, 180));
        assertThat(conf.isUseTransparentTextures()).isFalse();
        assertThat((int) conf.getMaxAbsentTileAttempts()).isEqualTo(2);
        assertThat((int) conf.getMinAbsentTileCheckInterval()).isEqualTo(1000);
        assertThat(conf.getMapScale()).isEqualTo(24000.0);
        assertThat(conf.getDetailHint()).isEqualTo(0.3);
        assertThat(conf.getTransparencyColors()).isEqualTo(new Color[]{Color.WHITE});
        assertThat((int) conf.getReadTimeout()).isEqualTo(10000);
        assertThat((int) conf.getConnectTimeout()).isEqualTo(20000);
        assertThat((int) conf.getStaleRequestLimit()).isEqualTo(30000);

        ScreenCredit sc = conf.getScreenCredit();
        assertThat(sc).isNotNull();
        assertThat(sc.getLink()).isEqualTo("http://www.testserver.com/screencredit/");
    }

    @Test
    public void testSerializeAndParse() throws IOException, SAXException
    {
        final String DATA_SET_NAME = "earthatnight";
        final String DISPLAY_NAME = "Earth at Night";
        final String SERVICE_NAME = "OGC:WMS";
        final String WMS_VERSION = "1.3";
        final URL GET_CAPABILITIES_URL = new URL("http://worldwind25.arc.nasa.gov/wms");
        final URL GET_MAP_URL = new URL("http://worldwind25.arc.nasa.gov/wms");
        final String[] LAYER_NAMES = {"earthatnight"};
        final String[] STYLE_NAMES = {"style1", "style2"};
        final boolean RETRIEVE_PROPERTIES_FROM_SERVICE = true;
        final String DATA_CACHE_NAME = "Earth/EarthAtNight";
        final String IMAGE_FORMAT = "image/jpeg";
        final String[] AVAILABLE_IMAGE_FORMATS = {"image/jpeg", "image/png"};
        final String FORMAT_SUFFIX = ".dds";
        final int NUM_LEVELS = 6;
        final int NUM_EMPTY_LEVELS = 0;
        final String[] INACTIVE = {"layer1", "layer2", "layer3"};
        final LatLon TILE_ORIGIN = LatLon.fromDegrees(-90, -180);
        final LatLon LEVEL_ZERO_TILE_DELTA = LatLon.fromDegrees(36, 36);
        final int TILE_WIDTH = 512;
        final int TILE_HEIGHT = 1024;
        final Sector SECTOR = Sector.fromDegrees(-90, 90, -180, 180);
        final int READ_TIMEOUT = 10000;
        final int CONNECT_TIMEOUT = 20000;
        final int STALE_REQUEST_LIMIT = 30000;
        final int MAX_ABSENT_TILE_ATTEMPTS = 2;
        final int MIN_ABSENT_TILE_CHECK_INTERVAL = 1000;
        final double MAP_SCALE = 24e3;
        final double DETAIL_HINT = 0.3;
        final Color[] TRANSPARENCY_COLORS = {Color.WHITE};
        final boolean USE_MIP_MAPS = true;
        final boolean FORCE_LEVEL_ZERO = true;
        final boolean RETAIN_LEVEL_ZERO = false;
        final boolean TRANSPARENT_TEXTURES = true;
        final String SCREEN_CREDIT_FILE = "files/TestScreenCredit.png";
        final String SCREEN_CREDIT_LINK = "http://testserver.com/screencredit";
        final String TEXTURE_FORMAT = "image/dds";
        final long EXPIRY_TIME = 1397252204000L;
        final double MIN_ACTIVE_ALTITUDE = 10000;
        final double MAX_ACTIVE_ALTITUDE = 100000;

        final LevelSet.SectorResolution[] RESOLUTION_LIMITS = {
                new LevelSet.SectorResolution(Sector.fromDegrees(30, 40, -120, -100), 5),
                new LevelSet.SectorResolution(Sector.fromDegrees(50, 60, -130, -120), 6),
                new LevelSet.SectorResolution(Sector.fromDegrees(-90, 90, -180, 180), 8)
        };

        // Create a configuration document and populate the fields
        ImageryLayerConfiguration conf_orig = new ImageryLayerConfiguration();
        conf_orig.setDataSetName(DATA_SET_NAME);
        conf_orig.setDisplayName(DISPLAY_NAME);
        conf_orig.setServiceName(SERVICE_NAME);
        conf_orig.setWMSVersion(WMS_VERSION);
        conf_orig.setCapabilitiesURL(GET_CAPABILITIES_URL);
        conf_orig.setMapURL(GET_MAP_URL);
        conf_orig.setLayerNames(LAYER_NAMES);
        conf_orig.setStyleNames(STYLE_NAMES);
        conf_orig.setRetrievePropertiesFromService(RETRIEVE_PROPERTIES_FROM_SERVICE);
        conf_orig.setDataCacheName(DATA_CACHE_NAME);
        conf_orig.setImageFormat(IMAGE_FORMAT);
        conf_orig.setAvailableImageFormats(AVAILABLE_IMAGE_FORMATS);
        conf_orig.setFormatSuffix(FORMAT_SUFFIX);
        conf_orig.setNumLevels(NUM_LEVELS);
        conf_orig.setNumEmptyLevels(NUM_EMPTY_LEVELS);
        conf_orig.setInactiveLevels(INACTIVE);
        conf_orig.setTileOrigin(TILE_ORIGIN);
        conf_orig.setLevelZeroTileDelta(LEVEL_ZERO_TILE_DELTA);
        conf_orig.setTileWidth(TILE_WIDTH);
        conf_orig.setTileHeight(TILE_HEIGHT);
        conf_orig.setSector(SECTOR);
        conf_orig.setReadTimeout(READ_TIMEOUT);
        conf_orig.setConnectTimeout(CONNECT_TIMEOUT);
        conf_orig.setStaleRequestLimit(STALE_REQUEST_LIMIT);
        conf_orig.setMaxAbsentTileAttempts(MAX_ABSENT_TILE_ATTEMPTS);
        conf_orig.setMinAbsentTileCheckInterval(MIN_ABSENT_TILE_CHECK_INTERVAL);
        conf_orig.setMapScale(MAP_SCALE);
        conf_orig.setDetailHint(DETAIL_HINT);
        conf_orig.setTransparencyColors(TRANSPARENCY_COLORS);
        conf_orig.setUseMipMaps(USE_MIP_MAPS);
        conf_orig.setExpiryTime(EXPIRY_TIME);
        conf_orig.setForceLevelZeroLoads(FORCE_LEVEL_ZERO);
        conf_orig.setRetainLevelZeroTiles(RETAIN_LEVEL_ZERO);
        conf_orig.setUseTransparentTextures(TRANSPARENT_TEXTURES);
        conf_orig.setSectorResolutionLimits(RESOLUTION_LIMITS);
        conf_orig.setTextureFormat(TEXTURE_FORMAT);
        conf_orig.setMinActiveAltitude(MIN_ACTIVE_ALTITUDE);
        conf_orig.setMaxActiveAltitude(MAX_ACTIVE_ALTITUDE);

        ScreenCredit screenCredit = new ScreenCreditImage("ScreenCredit", SCREEN_CREDIT_FILE);
        screenCredit.setLink(SCREEN_CREDIT_LINK);
        conf_orig.setScreenCredit(screenCredit);

        // Serialize the document and parse into a new instance. Ensure that the fields match the expected values.
        String xml = conf_orig.toXml();
        ImageryLayerConfiguration conf = new ImageryLayerConfiguration(xml);

        assertThat(conf.getDataSetName()).isEqualTo(DATA_SET_NAME);
        assertThat(conf.getDisplayName()).isEqualTo(DISPLAY_NAME);
        assertThat(conf.getServiceName()).isEqualTo(SERVICE_NAME);
        assertThat(conf.getWMSVersion()).isEqualTo(WMS_VERSION);
        assertThat(conf.getCapabilitiesURL()).isEqualTo(GET_CAPABILITIES_URL);
        assertThat(conf.getMapURL()).isEqualTo(GET_MAP_URL);
        assertThat(conf.getLayerNames()).isEqualTo(LAYER_NAMES);
        assertThat(conf.getStyleNames()).isEqualTo(STYLE_NAMES);
        assertThat((boolean) conf.isRetrievePropertiesFromService()).isEqualTo(RETRIEVE_PROPERTIES_FROM_SERVICE);
        assertThat(conf.getDataCacheName()).isEqualTo(DATA_CACHE_NAME);
        assertThat(conf.getImageFormat()).isEqualTo(IMAGE_FORMAT);
        assertThat(conf.getAvailableImageFormats()).isEqualTo(AVAILABLE_IMAGE_FORMATS);
        assertThat(conf.getFormatSuffix()).isEqualTo(FORMAT_SUFFIX);
        assertThat((int) conf.getNumLevels()).isEqualTo(NUM_LEVELS);
        assertThat((int) conf.getNumEmptyLevels()).isEqualTo(NUM_EMPTY_LEVELS);
        assertThat(conf.getInactiveLevels()).isEqualTo(INACTIVE);
        assertThat(conf.getTileOrigin()).isEqualTo(TILE_ORIGIN);
        assertThat(conf.getLevelZeroTileDelta()).isEqualTo(LEVEL_ZERO_TILE_DELTA);
        assertThat((int) conf.getTileWidth()).isEqualTo(TILE_WIDTH);
        assertThat((int) conf.getTileHeight()).isEqualTo(TILE_HEIGHT);
        assertThat((Iterable<? extends LatLon>) conf.getSector()).isEqualTo(SECTOR);
        assertThat((int) conf.getReadTimeout()).isEqualTo(READ_TIMEOUT);
        assertThat((int) conf.getConnectTimeout()).isEqualTo(CONNECT_TIMEOUT);
        assertThat((int) conf.getStaleRequestLimit()).isEqualTo(STALE_REQUEST_LIMIT);
        assertThat((int) conf.getMaxAbsentTileAttempts()).isEqualTo(MAX_ABSENT_TILE_ATTEMPTS);
        assertThat((int) conf.getMinAbsentTileCheckInterval()).isEqualTo(MIN_ABSENT_TILE_CHECK_INTERVAL);
        assertThat(conf.getMapScale()).isEqualTo(MAP_SCALE);
        assertThat(conf.getDetailHint()).isEqualTo(DETAIL_HINT);
        assertThat(conf.getTransparencyColors()).isEqualTo(TRANSPARENCY_COLORS);
        assertThat((boolean) conf.isUseMipMaps()).isEqualTo(USE_MIP_MAPS);
        assertThat((long) conf.getExpiryTime()).isEqualTo(EXPIRY_TIME);
        assertThat((boolean) conf.isForceLevelZeroLoads()).isEqualTo(FORCE_LEVEL_ZERO);
        assertThat((boolean) conf.isRetainLevelZeroTiles()).isEqualTo(RETAIN_LEVEL_ZERO);
        assertThat((boolean) conf.isUseTransparentTextures()).isEqualTo(TRANSPARENT_TEXTURES);
        assertThat(conf.getTextureFormat()).isEqualTo(TEXTURE_FORMAT);
        assertThat(conf.getMinActiveAltitude()).isEqualTo(MIN_ACTIVE_ALTITUDE);
        assertThat(conf.getMaxActiveAltitude()).isEqualTo(MAX_ACTIVE_ALTITUDE);

        ScreenCredit sc = conf.getScreenCredit();
        assertThat(sc).isNotNull();
        assertThat(((ScreenCreditImage) sc).getImageSource()).isEqualTo(SCREEN_CREDIT_FILE);
        assertThat(sc.getLink()).isEqualTo(SCREEN_CREDIT_LINK);

        // LevelSet.SectorResolution does not override equals, so we can't use array comparison methods.
        LevelSet.SectorResolution[] limits = conf.getSectorResolutionLimits();
        assertThat(limits).isNotNull();
        assertThat(limits).hasSize(RESOLUTION_LIMITS.length);
        for (int i = 0; i < limits.length; i++)
        {
            assertThat(limits[i].getLevelNumber()).isEqualTo(RESOLUTION_LIMITS[i].getLevelNumber());
            assertThat((Iterable<? extends LatLon>) limits[i].getSector()).isEqualTo(RESOLUTION_LIMITS[i].getSector());
        }
    }

    @Test
    public void testAVList() throws IOException
    {
        URL url = getClass().getClassLoader().getResource("LayerTest.xml");
        ImageryLayerConfiguration conf = new ImageryLayerConfiguration(url);

        AVList list = conf.toAVList();
        assertThat(list.getStringValue(AVKey.DISPLAY_NAME)).isEqualTo("Earth at Night");
        assertThat(list.getStringValue(AVKey.SERVICE_NAME)).isEqualTo("OGC:WMS");
        assertThat(list.getStringValue(AVKey.WMS_VERSION)).isEqualTo("1.3");
        assertThat(list.getStringValue(AVKey.DATA_CACHE_NAME)).isEqualTo("Earth/EarthAtNight");
    }

    /**
     * Test to ensure that workaround for a WW XML serialization bug is still necessary. ImageryLayerConfiguration
     * includes code to add the transparency colors to the serialized XML. World Wind should do this, but does not.
     * This test will fail when the WW bug (WWJ-458) is fixed, at which point the workaround code in
     * ImageryLayerConfiguration should be removed, along with this test.
     */
    @Test
    public void singleTransparentColorsElement() throws IOException
    {
        ImageryLayerConfiguration conf = new ImageryLayerConfiguration();
        conf.setTransparencyColors(new Color[]{Color.WHITE});

        String xml = conf.toXml();

        // Parse the serialized XML and confirm that there is only a single TransparencyColor tag
        try
        {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xml)));

            Element[] elements = WWXML.getElements(document.getDocumentElement(), "TransparencyColors", null);

            // We should find exactly one element. If there are more than WW and TAKX are both adding TransparencyColor tags.
            assertThat(elements).hasSize(1);
        } catch (ParserConfigurationException | SAXException e)
        {
            throw new IOException(e);
        }
    }
}
