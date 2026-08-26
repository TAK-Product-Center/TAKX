package gov.tak.platform.cot.exporter;

import gov.tak.api.cot.CoordinatedTime;
import gov.tak.api.cot.CotTypes;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.cot.route.RouteDirection;
import gov.tak.api.cot.route.RouteMethod;
import gov.tak.api.cot.route.RouteOrder;
import gov.tak.api.cot.route.RouteType;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.tak.api.util.StandardAttributeNames;
import gov.tak.core.comms.cot.CotImporterManager;
import gov.tak.platform.cot.importer.RouteImporterTest;
import gov.tak.test.utils.ExporterTestUtils;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.IRouteMapObject;
import gov.takx.test.AIntegrationTest;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static gov.tak.api.cot.detail.DetailConstants.ATTR_CALLSIGN;
import static gov.tak.api.cot.detail.DetailConstants.ATTR_COLOR;
import static gov.tak.api.cot.detail.DetailConstants.ATTR_DIRECTION;
import static gov.tak.api.cot.detail.DetailConstants.ATTR_METHOD;
import static gov.tak.api.cot.detail.DetailConstants.ATTR_ORDER;
import static gov.tak.api.cot.detail.DetailConstants.ATTR_PLANNING_METHOD;
import static gov.tak.api.cot.detail.DetailConstants.ATTR_POINT;
import static gov.tak.api.cot.detail.DetailConstants.ATTR_PREFIX;
import static gov.tak.api.cot.detail.DetailConstants.ATTR_RELATION;
import static gov.tak.api.cot.detail.DetailConstants.ATTR_REMARKS;
import static gov.tak.api.cot.detail.DetailConstants.ATTR_ROUTE_TYPE;
import static gov.tak.api.cot.detail.DetailConstants.ATTR_STROKE;
import static gov.tak.api.cot.detail.DetailConstants.ATTR_TYPE;
import static gov.tak.api.cot.detail.DetailConstants.ATTR_UID;
import static gov.tak.api.cot.detail.DetailConstants.LINK;
import static gov.tak.api.cot.detail.DetailConstants.LINK_ATTR;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validate {@link RouteExporter} functionality.
 *
 * @since 3.0
 */
public class RouteExporterTest extends AIntegrationTest
{
    private final CotImporterManager importerManager = getBean(CotImporterManager.class);
    private final IMapObjectManager mapObjectManager = getBean(IMapObjectManager.class);
    private final RouteExporter routeExporter = new RouteExporter();

    @Test
    public void test_route_export() throws IOException
    {
        final CotEvent sourceEvent = CotEvent.parse(getResourceAsString("/cot/good_route.cot"));
        assertThat(importerManager.importEvent(sourceEvent)).isEqualTo(ImportResult.SUCCESS);

        final var routeMO = (IRouteMapObject) mapObjectManager.getMapObject(RouteImporterTest.ROUTE_KEY);
        assertThat(routeMO).isNotNull();

        // Now convert the Map Object back to a CoT event
        final long now = CoordinatedTime.currentTimeMillis();

        CotEvent event = routeExporter.export(routeMO);
        ExporterTestUtils.verifyCommonEventValues(event, now, routeMO);

        //noinspection ConstantConditions (verified by ExporterTestUtils)
        assertThat(event.getCallsign()).isEqualTo(RouteImporterTest.ROUTE_NAME);

        final CotDetail detail = event.getDetail();
        CotDetail linkAttrDetail = detail.getFirstChildByName(LINK_ATTR);
        assertThat(linkAttrDetail).isNotNull();
        assertThat(linkAttrDetail.getAttribute(ATTR_COLOR)).isEqualTo(String.valueOf(RouteImporterTest.ROUTE_COLOR_RGBA));
        assertThat(linkAttrDetail.getAttribute(ATTR_DIRECTION)).isEqualTo(RouteDirection.EXFILTRATION.getDisplayText());

        // Intentionally comparing to direction here, since the value is deprecated and is the same as direction now
        assertThat(linkAttrDetail.getAttribute(ATTR_PLANNING_METHOD)).isEqualTo(RouteDirection.EXFILTRATION.getDisplayText());
        assertThat(linkAttrDetail.getAttribute(ATTR_METHOD)).isEqualTo(RouteMethod.WALKING.getDisplayText());
        assertThat(linkAttrDetail.getAttribute(ATTR_ORDER)).isEqualTo(RouteOrder.DESCENDING.getDisplayText());
        assertThat(linkAttrDetail.getAttribute(ATTR_PREFIX)).isEqualTo("WP");
        assertThat(linkAttrDetail.getAttribute(ATTR_ROUTE_TYPE)).isEqualTo(RouteType.SECONDARY.getDisplayText());
        assertThat(linkAttrDetail.getAttribute(ATTR_STROKE)).isEqualTo(Integer.toString(1));
        assertThat(linkAttrDetail.getAttribute(ATTR_TYPE)).isEqualTo(RouteMethod.WALKING.getTransportationType());

        List<GeoPointMetaData> controlPoints = routeMO.getControlPoints();
        List<CotDetail> linkDetails = detail.getChildrenByName(LINK);
        assertThat(linkDetails).hasSize(5);
        verifyLinkDetail(linkDetails.get(0), controlPoints.get(0), CotTypes.WAYPOINT, RouteImporterTest.WAYPOINT_1_NAME, RouteImporterTest.WAYPOINT_1_REMARKS);
        verifyLinkDetail(linkDetails.get(1), controlPoints.get(1), CotTypes.CONTROL_POINT, "", "");
        verifyLinkDetail(linkDetails.get(2), controlPoints.get(2), CotTypes.WAYPOINT, RouteImporterTest.WAYPOINT_2_NAME, "");
        verifyLinkDetail(linkDetails.get(3), controlPoints.get(3), CotTypes.CONTROL_POINT, "", "");
        verifyLinkDetail(linkDetails.get(4), controlPoints.get(4), CotTypes.WAYPOINT, RouteImporterTest.WAYPOINT_3_NAME, "");
    }

    /**
     * Verify the link detail contains the expected values.
     *
     * @param link         The detail to verify
     * @param controlPoint The associated control point
     * @param type         The CoT type
     * @param callsign     The expected callsign
     * @param remarks      The expected remarks
     */
    private void verifyLinkDetail(@Nonnull CotDetail link, @Nonnull GeoPointMetaData controlPoint, @Nonnull String type,
                                  @Nonnull String callsign, @Nonnull String remarks)
    {
        final GeoPoint point = GeoPoint.parseGeoPoint(link.getAttribute(ATTR_POINT));
        assertThat(point).isEqualTo(controlPoint.get());

        assertThat(link.getAttribute(ATTR_UID)).isEqualTo(controlPoint.getMetaData().getStringAttribute(StandardAttributeNames.UID));
        assertThat(link.getAttribute(ATTR_TYPE)).isEqualTo(type);
        assertThat(link.getAttribute(ATTR_CALLSIGN)).isEqualTo(callsign);
        assertThat(link.getAttribute(ATTR_REMARKS)).isEqualTo(remarks);
        assertThat(link.getAttribute(ATTR_RELATION)).isEqualTo("c");
    }
}
