package gov.tak.platform.cot.detail;

import com.google.common.io.Resources;
import gov.tak.api.cot.detail.DetailConstants;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.blacksmith.mock.Mocked;
import gov.takx.api.geofence.GeoFence;
import gov.takx.api.geofence.GeoFenceDetailConstants;
import gov.takx.api.geofence.MonitoredType;
import gov.takx.api.geofence.Trigger;
import gov.takx.api.mapobject.IShapeCircleMapObject;
import gov.takx.api.shapes.IShapeService;
import gov.takx.api.shapes.ShapeAlreadyExistsException;
import gov.takx.core.geofence.GeoFenceManager;
import gov.takx.core.geofence.monitor.GeoFenceSpatialCalculator;
import gov.takx.core.shapes.ShapeServiceFactory;
import gov.takx.dataservices.persist.IconMapObject;
import gov.takx.dataservices.shapes.Circle;
import gov.takx.test.AIntegrationTest;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 5.3
 */
public class GeofenceDetailHandlerTest extends AIntegrationTest
{
    private IShapeService shapeService;
    private GeofenceDetailHandler detailHandler;

    @Mocked
    private IconMapObject nonShapeMapObject;

    @Override
    protected void postProjectOpen()
    {
        shapeService = getBean(ShapeServiceFactory.class).getOrCreateCoreShapeService();
        detailHandler = new GeofenceDetailHandler();
    }

    @AfterEach
    public void afterEach()
    {
        deleteAllMapObjects();
        stopService(GeoFenceManager.class);
        stopService(GeoFenceSpatialCalculator.class);
    }

    @Test
    void geoFenceCot_withNoPreviousGeoFence_createsGeoFence() throws IOException, ShapeAlreadyExistsException
    {
        final var circleMapObject = makeCircle("/cot/geofence.cot");

        // <__geofence elevationMonitored="true" monitor="Friendly" tracking="True" trigger="Both" minElevation="1000" maxElevation="2000"/>
        final GeoFence expected = GeoFence.builder().shapeUid("SHAPE UID")
                .tracking(true)
                .trigger(Trigger.Both)
                .monitoredType(MonitoredType.Friendly)
                .elevationRange(1000, 2000)
                .build();

        assertThat(circleMapObject.hasGeoFence()).isTrue();
        assertThat(circleMapObject.getGeoFence()).isEqualTo(expected);
    }

    @Test
    void geoFenceCot_withPreviousGeoFence_updatesGeoFence() throws IOException, ShapeAlreadyExistsException
    {
        final var circle = new Circle("SHAPE UID", GeoPoint.ZERO_POINT, 1000);
        final var circleMapObject = shapeService.createCircleMapObject(circle);

        final var initial = GeoFence.builder().shapeUid("SHAPE UID")
                .tracking(true)
                .trigger(Trigger.Both)
                .monitoredType(MonitoredType.Friendly)
                .elevationRange(1000, 2000)
                .build();

        circleMapObject.setGeoFence(initial);
        assertThat(circleMapObject.hasGeoFence()).isTrue();

        final CotEvent event = getCotEvent("/cot/geofence_update.cot");
        final CotDetail geoFenceDetail = event.getDetail().getFirstChildByName(DetailConstants.GEOFENCE);
        assertThat(detailHandler.toItemMetadata(circleMapObject, event, geoFenceDetail)).isEqualTo(ImportResult.SUCCESS);
        assertThat(circleMapObject.hasGeoFence()).isTrue();

        // <__geofence elevationMonitored="false" monitor="All" tracking="True" trigger="Exit"/>
        final GeoFence expected = GeoFence.builder().shapeUid("SHAPE UID")
                .tracking(true)
                .trigger(Trigger.Exit)
                .monitoredType(MonitoredType.All)
                .build();

        assertThat(circleMapObject.getGeoFence()).isEqualTo(expected);
    }

    @Test
    void geoFenceCot_withCustomUids_importsCorrectly() throws ShapeAlreadyExistsException, IOException
    {
        final var circleMapObject = makeCircle("/cot/geofence_with_custom_uids.cot");

        // <__geofence elevationMonitored="false" monitor="Custom" tracking="True" trigger="Both">
        //     <monitor uid="UID1"/>
        //     <monitor uid="UID2"/>
        //     <monitor uid="UID3"/>
        // </__geofence>
        final GeoFence expected = GeoFence.builder().shapeUid("SHAPE UID")
                .tracking(true)
                .trigger(Trigger.Both)
                .monitoredType(MonitoredType.Custom)
                .customUids(Set.of("UID1", "UID2", "UID3"))
                .build();

        assertThat(circleMapObject.hasGeoFence()).isTrue();
        assertThat(circleMapObject.getGeoFence()).isEqualTo(expected);
    }

    @Test
    void nonShape_isNotProcessed()
    {
        assertThat(detailHandler.isSupported(nonShapeMapObject, null, null)).isFalse();
    }

    @Test
    void shape_withNoGeofence_exportsNothing() throws ShapeAlreadyExistsException
    {
        final var circle = new Circle("SHAPE UID", GeoPoint.ZERO_POINT, 1000);
        final var circleMapObject = shapeService.createCircleMapObject(circle);
        final var cotEvent = new CotEvent();
        final var detail = new CotDetail("detail");

        assertThat(detailHandler.toCotDetail(circleMapObject, cotEvent, detail)).isFalse();
    }

    @Test
    void shape_withBasicGeoFence_exportsCorrectly() throws ShapeAlreadyExistsException, IOException
    {
        final var circleMapObject = makeCircle("/cot/geofence.cot");
        final var cotEvent = new CotEvent();
        final var detail = new CotDetail("detail");

        final boolean handled = detailHandler.toCotDetail(circleMapObject, cotEvent, detail);
        assertThat(handled).isTrue();

        final CotDetail geoFenceDetail = detail.getFirstChildByName(DetailConstants.GEOFENCE);

        assertThat(geoFenceDetail).isNotNull();
        assertThat(geoFenceDetail.childCount()).isEqualTo(0);
        assertThat(Boolean.valueOf(geoFenceDetail.getAttribute(GeoFenceDetailConstants.TRACKING))).isTrue();
        assertThat(geoFenceDetail.getAttribute(GeoFenceDetailConstants.TRIGGER)).isEqualTo(Trigger.Both.name());
        assertThat(geoFenceDetail.getAttribute(GeoFenceDetailConstants.MONITOR)).isEqualTo(MonitoredType.Friendly.name());
        assertThat(Boolean.valueOf(geoFenceDetail.getAttribute(GeoFenceDetailConstants.ELEVATION_MONITORED))).isTrue();
        assertThat(Double.valueOf(geoFenceDetail.getAttribute(GeoFenceDetailConstants.ELEVATION_MIN))).isEqualTo(1000);
        assertThat(Double.valueOf(geoFenceDetail.getAttribute(GeoFenceDetailConstants.ELEVATION_MAX))).isEqualTo(2000);
    }

    @Test
    void shape_withCustomUidsGeoFence_exportsCorrectly() throws ShapeAlreadyExistsException, IOException
    {
        final var circleMapObject = makeCircle("/cot/geofence_with_custom_uids.cot");
        final var cotEvent = new CotEvent();
        final var detail = new CotDetail("detail");

        final boolean handled = detailHandler.toCotDetail(circleMapObject, cotEvent, detail);
        assertThat(handled).isTrue();

        final CotDetail geoFenceDetail = detail.getFirstChildByName(DetailConstants.GEOFENCE);

        assertThat(geoFenceDetail).isNotNull();
        assertThat(Boolean.valueOf(geoFenceDetail.getAttribute(GeoFenceDetailConstants.TRACKING))).isTrue();
        assertThat(geoFenceDetail.getAttribute(GeoFenceDetailConstants.TRIGGER)).isEqualTo(Trigger.Both.name());
        assertThat(geoFenceDetail.getAttribute(GeoFenceDetailConstants.MONITOR)).isEqualTo(MonitoredType.Custom.name());
        assertThat(Boolean.valueOf(geoFenceDetail.getAttribute(GeoFenceDetailConstants.ELEVATION_MONITORED))).isFalse();

        final int childCount = geoFenceDetail.childCount();
        assertThat(childCount).isEqualTo(3);

        final Set<String> customUids = new HashSet<>();
        for (int i = 0; i < childCount; i++)
        {
            final var monitorDetail = geoFenceDetail.getChild(i);
            assertThat(monitorDetail.getElementName()).isEqualTo(GeoFenceDetailConstants.MONITOR);

            customUids.add(monitorDetail.getAttribute(DetailConstants.ATTR_UID));
        }

        assertThat(customUids).containsOnly("UID1", "UID2", "UID3");
    }

    /**
     * Create a circle and import the given CoT resource.
     */
    @Nonnull
    private IShapeCircleMapObject makeCircle(@Nonnull String resourcePath) throws ShapeAlreadyExistsException, IOException
    {
        final var circle = new Circle("SHAPE UID", GeoPoint.ZERO_POINT, 1000);
        final var circleMapObject = shapeService.createCircleMapObject(circle);
        assertThat(circleMapObject.hasGeoFence()).isFalse();

        final var event = getCotEvent(resourcePath);
        final var geoFenceDetail = event.getDetail().getFirstChildByName(DetailConstants.GEOFENCE);
        assertThat(detailHandler.toItemMetadata(circleMapObject, event, geoFenceDetail)).isEqualTo(ImportResult.SUCCESS);

        return circleMapObject;
    }

    @Nonnull
    private CotEvent getCotEvent(@Nonnull String resourcePath) throws IOException
    {
        final URL url = Objects.requireNonNull(getClass().getResource(resourcePath));
        return CotEvent.parse(Resources.toString(url, StandardCharsets.UTF_8));
    }
}
