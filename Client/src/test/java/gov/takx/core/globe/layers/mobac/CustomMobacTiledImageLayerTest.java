package gov.takx.core.globe.layers.mobac;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.geom.LatLon;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validate the creation of {@link CustomMobacTiledImageLayer}.  Primary purpose is to ensure that the
 * reflection operations it performs during initialization work (in case a change to the WW library changes).
 *
 * @since 3.0
 */
public class CustomMobacTiledImageLayerTest
{
    private static final String LAYER_NAME = "Open Topo Map";

    private static final String CONFIG_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<customMapSource>\n" +
            "<name>" + LAYER_NAME + "</name>\n" +
            "<minZoom>3</minZoom>\n" +
            "<maxZoom>18</maxZoom>\n" +
            "<tileType>PNG</tileType>\n" +
            "<tileUpdate>IfNoneMatch</tileUpdate>\n" +
            "<serverParts>a b c</serverParts>\n" +
            "<url>https://{$serverpart}.tile.opentopomap.org/{$z}/{$x}/{$y}.png</url>\n" +
            "</customMapSource>\n";

    @Test
    public void test_layer_creation()
    {
        final CustomMobacTiledImageLayer layer = new CustomMobacTiledImageLayer(CONFIG_XML);

        assertThat(layer.getName()).isEqualTo(LAYER_NAME);

        final AVList params = (AVList) layer.getValue(AVKey.CONSTRUCTION_PARAMETERS);
        assertThat(params).isNotNull();

        assertThat(params.getValue(AVKey.SERVICE)).isEqualTo("CustomMobac");
        assertThat(params.getValue(AVKey.TILE_WIDTH)).isEqualTo(256);
        assertThat(params.getValue(AVKey.TILE_HEIGHT)).isEqualTo(256);
        assertThat(params.getValue(AVKey.FORMAT_SUFFIX)).isEqualTo(".PNG");
        assertThat(params.getValue(AVKey.NUM_LEVELS)).isEqualTo(16);

        final Integer minZoom = (Integer) params.getValue(CustomMobacTiledImageLayer.MIN_ZOOM_KEY);
        assertThat(minZoom).isEqualTo(3);

        // The minZoom determines the level 0 tile delta
        final int nTiles = 1 << minZoom;
        final LatLon tileDelta = LatLon.fromDegrees(180.0 / nTiles, 360.0 / nTiles);
        assertThat(params.getValue(AVKey.LEVEL_ZERO_TILE_DELTA)).isEqualTo(tileDelta);
    }
}
