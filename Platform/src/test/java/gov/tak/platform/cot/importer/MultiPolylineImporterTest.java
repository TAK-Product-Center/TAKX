package gov.tak.platform.cot.importer;

import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.maps.MapItem;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.core.comms.cot.CotImporterManager;
import gov.takx.api.annotation.SuppressLogging;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.IMultiPolylineMapObject;
import gov.takx.api.mapobject.IShapePathMapObject;
import gov.takx.platform.KeyUtils;
import gov.takx.test.AIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static gov.takx.api.mapobject.IMultiPolylineMapObject.MULTI_POLYLINE_TYPE;
import static gov.takx.platform.KeyUtils.COMPOUND_FAMILY;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validate {@link MultiPolylineImporter} functionality.
 *
 * @since 3.0
 */
public class MultiPolylineImporterTest extends AIntegrationTest
{
    public static final String MULTI_POLYLINE_UID = "8add4fbf-6d3f-474f-97c9-d0122d667222";
    public static final String MULTI_POLYLINE_NAME = "Freehand 1";
    public static final String MULTI_POLYLINE_KEY = KeyUtils.getUniqueKey(COMPOUND_FAMILY, MULTI_POLYLINE_TYPE, MULTI_POLYLINE_UID);

    public static final String POLYLINE_1_UID = "ad5fe7b0-2d10-4199-8666-f16d6dacdf45";
    public static final String POLYLINE_1_NAME = "Freehand Red";
    public static final List<ILocation> POLYLINE_1_POINTS = Arrays.asList(
            new GeoPoint(33, -87),
            new GeoPoint(34, -87),
            new GeoPoint(35, -87));
    public static final int POLYLINE_1_COLOR_RGBA = -65536;

    public static final String POLYLINE_2_UID = "90b4204b-17f8-4ddf-b080-fc0fab519ad2";
    public static final String POLYLINE_2_NAME = "Freehand Blue";
    public static final List<ILocation> POLYLINE_2_POINTS = Arrays.asList(
            new GeoPoint(34, -88),
            new GeoPoint(35, -88),
            new GeoPoint(36, -88));
    public static final int POLYLINE_2_COLOR_RGBA = -16776961;

    public static final String POLYLINE_3_UID = "39ca3e0b-4ad0-45b5-b4dc-7f4db6f80d27";
    public static final String POLYLINE_3_NAME = "Freehand Green";
    public static final List<ILocation> POLYLINE_3_POINTS = Arrays.asList(
            new GeoPoint(35, -89),
            new GeoPoint(36, -89),
            new GeoPoint(37, -89));
    public static final int POLYLINE_3_COLOR_RGBA = -16711936;

    private final CotImporterManager importerManager = getBean(CotImporterManager.class);
    private final IMapObjectManager mapObjectManager = getBean(IMapObjectManager.class);

    @AfterEach
    public void cleanupAfterMethod()
    {
        deleteAllMapObjects();
    }

    @SuppressLogging({CotImporterManager.class})
    @Test
    public void test_invalid_multi_polyline() throws IOException
    {
        final CotEvent event = CotEvent.parse(getResourceAsString("/cot/bad_multi_polyline.cot"));
        assertThat(event.isValid()).isTrue();

        assertThat(importerManager.importEvent(event)).isEqualTo(ImportResult.FAILURE);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    public void test_valid_multi_polyline() throws IOException
    {
        final CotEvent event = CotEvent.parse(getResourceAsString("/cot/good_multi_polyline.cot"));
        assertThat(event.isValid()).isTrue();
        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(0);

        assertThat(importerManager.importEvent(event)).isEqualTo(ImportResult.SUCCESS);

        // One MO for the multi-polyline and two for the polylines
        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(3);

        IMapObject mapObject = mapObjectManager.getMapObject(MULTI_POLYLINE_KEY);
        assertThat(mapObject).isNotNull().isInstanceOf(IMultiPolylineMapObject.class);

        IMultiPolylineMapObject multiPolylineMO = (IMultiPolylineMapObject) mapObject;
        assertThat(multiPolylineMO.getLatestCotEvent()).isEqualTo(event);
        assertThat(multiPolylineMO.getHeightMeters()).isEqualTo(1.23);
        assertThat(multiPolylineMO.getName()).isEqualTo(MULTI_POLYLINE_NAME);

        final List<IShapePathMapObject<?>> polylines = multiPolylineMO.getPolylines();
        assertThat(polylines).hasSize(2).extracting(MapItem::getUid).containsOnly(POLYLINE_1_UID, POLYLINE_2_UID);

        IShapePathMapObject<?> polyline1 = polylines.stream().filter(mo -> mo.getUid().equals(POLYLINE_1_UID)).findFirst().get();
        assertThat(polyline1.getLineColor()).isEqualTo(new Color(POLYLINE_1_COLOR_RGBA, true));
        assertThat(polyline1.getName()).isEqualTo(POLYLINE_1_NAME);
        assertThat(polyline1.getVertices()).usingElementComparatorOnFields("latitude", "longitude").containsOnlyElementsOf(POLYLINE_1_POINTS);

        IShapePathMapObject<?> polyline2 = polylines.stream().filter(mo -> mo.getUid().equals(POLYLINE_2_UID)).findFirst().get();
        assertThat(polyline2.getLineColor()).isEqualTo(new Color(POLYLINE_2_COLOR_RGBA, true));
        assertThat(polyline2.getName()).isEqualTo(POLYLINE_2_NAME);
        assertThat(polyline2.getVertices()).usingElementComparatorOnFields("latitude", "longitude").containsOnlyElementsOf(POLYLINE_2_POINTS);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    public void test_update_multi_polyline() throws IOException
    {
        final CotEvent event = CotEvent.parse(getResourceAsString("/cot/good_multi_polyline.cot"));
        final CotEvent updateEvent = CotEvent.parse(getResourceAsString("/cot/multi_polyline_update.cot"));
        assertThat(updateEvent.isValid()).isTrue();
        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(0);

        assertThat(importerManager.importEvent(event)).isEqualTo(ImportResult.SUCCESS);
        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(3);

        // The updated multi-polyline has:
        //  - Polyline removed (Red)
        //  - Polyline added (Green)
        //  - Polyline renamed (Blue)
        //  - Height and name changed
        assertThat(importerManager.importEvent(updateEvent)).isEqualTo(ImportResult.SUCCESS);

        // One new polyline MO was added (and the MO for the removed polyline was deleted automatically)
        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(3);

        IMapObject mapObject = mapObjectManager.getMapObject(MULTI_POLYLINE_KEY);
        assertThat(mapObject).isNotNull().isInstanceOf(IMultiPolylineMapObject.class);

        IMultiPolylineMapObject multiPolylineMO = (IMultiPolylineMapObject) mapObject;
        assertThat(multiPolylineMO.getHeightMeters()).isEqualTo(2.34);
        assertThat(multiPolylineMO.getName()).isEqualTo(MULTI_POLYLINE_NAME + " updated");

        final List<IShapePathMapObject<?>> polylines = multiPolylineMO.getPolylines();
        assertThat(polylines).hasSize(2).extracting(MapItem::getUid).containsOnly(POLYLINE_2_UID, POLYLINE_3_UID);

        IShapePathMapObject<?> polyline2 = polylines.stream().filter(mo -> mo.getUid().equals(POLYLINE_2_UID)).findFirst().get();
        assertThat(polyline2.getLineColor()).isEqualTo(new Color(POLYLINE_2_COLOR_RGBA, true));
        assertThat(polyline2.getName()).isEqualTo(POLYLINE_2_NAME + " updated");
        assertThat(polyline2.getVertices()).usingElementComparatorOnFields("latitude", "longitude").containsOnlyElementsOf(POLYLINE_2_POINTS);

        IShapePathMapObject<?> polyline3 = polylines.stream().filter(mo -> mo.getUid().equals(POLYLINE_3_UID)).findFirst().get();
        assertThat(polyline3.getLineColor()).isEqualTo(new Color(POLYLINE_3_COLOR_RGBA, true));
        assertThat(polyline3.getName()).isEqualTo(POLYLINE_3_NAME);
        assertThat(polyline3.getVertices()).usingElementComparatorOnFields("latitude", "longitude").containsOnlyElementsOf(POLYLINE_3_POINTS);
    }
}
