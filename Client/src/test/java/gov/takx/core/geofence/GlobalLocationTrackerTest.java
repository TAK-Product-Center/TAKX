package gov.takx.core.geofence;

import gov.tak.api.maps.PointMapItem;
import gov.tak.blacksmith.mock.Mocked;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.event.mapobject.MapObjectAdded;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.IMarkerMapObject;
import gov.takx.api.mapobject.MapObjectOptions;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.persist.IMapObjectFinder;
import gov.takx.core.geofence.monitor.GeoFenceSpatialCalculator;
import gov.takx.core.geofence.monitor.IGeoFenceMonitor;
import gov.takx.test.AIntegrationTest;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * @since 5.3
 */
public class GlobalLocationTrackerTest extends AIntegrationTest
{
    private static final ILocation LOCATION = new RaptorLocation(1, 1);
    private static final String FRIENDLY = "a-f-G";
    private static final String HOSTILE = "a-h-G";
    private static final String NOT_INVITED_TO_THE_PARTY = "whatever";

    private static final String NAME_BASE = "POINT_";
    private int nameCounter = 1;

    private GeoFenceSpatialCalculator calculator;
    private IMapObjectManager mapObjectManager;
    private GlobalLocationTracker tracker;

    @Mocked(allowCaching = false)
    private IGeoFenceMonitor monitor1;
    @Mocked(allowCaching = false)
    private IGeoFenceMonitor monitor2;

    @Override
    protected void postProjectOpen()
    {
        mapObjectManager = getBean(IMapObjectManager.class);
        calculator = getBean(GeoFenceSpatialCalculator.class);
    }

    @BeforeEach
    public void setup()
    {
        tracker = new GlobalLocationTracker(getBean(IMapObjectFinder.class), calculator);

        reset(monitor1, monitor2);
        when(monitor1.getShapeUid()).thenReturn("UID1");
        when(monitor2.getShapeUid()).thenReturn("UID2");
    }

    @AfterEach
    public void afterEach()
    {
        deleteAllMapObjects();
    }

    @AfterAll
    public void afterAll()
    {
        stopService(GeoFenceSpatialCalculator.class);
    }

    @Test
    void whenNotStarted_addingMonitors_doesNotCallSetMonitorState()
    {
        createPoint(FRIENDLY);

        tracker.addMonitor(monitor1);
        tracker.addMonitor(monitor2);

        verify(monitor1, never()).setMapItemState(any());
        verify(monitor2, never()).setMapItemState(any());
    }

    @Test
    void whenNotStarted_newPoint_doesNothing()
    {
        tracker.addMonitor(monitor1);

        reset(monitor1);

        final PointMapItem point = createPoint(FRIENDLY);
        tracker.onMapObjectAdded(new MapObjectAdded(point));

        verifyNoInteractions(monitor1);
    }

    @Test
    void whenStarted_addingMonitors_doesCallSetMonitorState()
    {
        final PointMapItem point1 = createPoint(FRIENDLY);
        final PointMapItem point2 = createPoint(NOT_INVITED_TO_THE_PARTY);

        when(monitor1.matchesMonitoredType(any())).thenReturn(true);
        when(monitor2.matchesMonitoredType(any())).thenReturn(true);

        tracker.start();

        tracker.addMonitor(monitor1);
        tracker.addMonitor(monitor2);

        verify(monitor1).setMapItemState(point1);
        verify(monitor2, never()).setMapItemState(point2);
    }

    @Test
    void whenStarted_newPoint_callsMapItemMoved()
    {
        tracker.start();
        tracker.addMonitor(monitor1);

        when(monitor1.matchesMonitoredType(any())).thenReturn(true);

        final PointMapItem point = createPoint(FRIENDLY);
        tracker.onMapObjectAdded(new MapObjectAdded(point));
        verify(monitor1).mapItemMoved(point);
    }

    @Test
    void whenStarted_removeMonitor_removesUnmatchedMapItems()
    {
        final PointMapItem point1 = createPoint(FRIENDLY);
        final PointMapItem point2 = createPoint(HOSTILE);

        when(monitor1.matchesMonitoredType(point1)).thenReturn(true);
        when(monitor2.matchesMonitoredType(point2)).thenReturn(true);

        tracker.addMonitor(monitor1);
        tracker.addMonitor(monitor2);

        tracker.start();

        assertThat(calculator.isPointTracked(point1.getUid())).isTrue();
        assertThat(calculator.isPointTracked(point2.getUid())).isTrue();

        tracker.removeMonitor(monitor2);

        assertThat(calculator.isPointTracked(point1.getUid())).isTrue();
        assertThat(calculator.isPointTracked(point2.getUid())).isFalse();
    }

    /**
     * Create a new {@link PointMapItem} at the given location and add it to the calculator.
     */
    private PointMapItem createPoint(@Nonnull String cotType)
    {
        final MapObjectSpec spec = MapObjectSpec.builder(IMarkerMapObject.MARKER_FAMILY, IMarkerMapObject.MARKER_TYPE)
                .name(NAME_BASE + ++nameCounter)
                .cotType(cotType)
                .mapObjectOptions(MapObjectOptions.STANDARD_RESOURCE_OPTIONS)
                .location(LOCATION)
                .build();

        final PointMapItem point = (PointMapItem) mapObjectManager.getOrCreateMapObject(spec);
        calculator.addOrUpdatePoint(point);
        return point;
    }
}
