package gov.takx.core.geofence;

import gov.tak.blacksmith.Blacksmith;
import gov.tak.blacksmith.BlacksmithExtension;
import gov.tak.blacksmith.mock.Mocked;
import gov.tak.blacksmith.mock.strategy.MockStrategy;
import gov.tak.blacksmith.mock.strategy.MockitoStrategy;
import gov.takx.api.event.mapobject.MapObjectRemoved;
import gov.takx.api.geofence.GeoFence;
import gov.takx.api.geofence.Trigger;
import gov.takx.api.geofence.event.GeoFenceAdded;
import gov.takx.api.geofence.event.GeoFenceRemoved;
import gov.takx.api.geofence.event.GeoFenceUpdated;
import gov.takx.api.persist.IMapObjectFinder;
import gov.takx.api.persist.IMapObjectPersist;
import gov.takx.core.geofence.monitor.GeoFenceMonitorFactory;
import gov.takx.core.geofence.monitor.GeoFenceSpatialCalculator;
import gov.takx.core.notification.NotificationService;
import gov.takx.core.persist.MapObjectPersist;
import gov.takx.dataservices.persist.ShapeCircleMapObject;
import jakarta.enterprise.event.Event;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @since 5.3
 */
@ExtendWith(BlacksmithExtension.class)
public class GeoFenceManagerTest
{
    static
    {
        MockStrategy mockStrategy = new MockitoStrategy().cached();
        BlacksmithExtension.setBlacksmithFactory(classes -> new Blacksmith(classes).withMockStrategy(mockStrategy));
    }

    private static final String CIRCLE_UID = "circle-uid";

    @Mocked
    private GlobalLocationTracker tracker;

    @Mocked
    private GeoFenceSpatialCalculator calculator;

    @Mocked
    private GeoFenceMonitorFactory monitorFactory;

    @Mocked
    private NotificationService notificationService;

    @Mocked
    private AlertManager alertManager;

    @Mocked(parentTypes = {IMapObjectPersist.class, IMapObjectFinder.class})
    private MapObjectPersist mapObjectPersist;

    @Mocked
    private ShapeCircleMapObject circle;

    @Mocked
    private Event<GeoFenceAdded> geoFenceAddedEvent;
    @Mocked
    private Event<GeoFenceUpdated> geoFenceUpdatedEvent;
    @Mocked
    private Event<GeoFenceRemoved> geoFenceRemovedEvent;

    private GeoFenceManager geoFenceManager;

    @BeforeEach
    public void beforeEach()
    {
        reset(circle, mapObjectPersist, geoFenceAddedEvent, geoFenceUpdatedEvent, geoFenceRemovedEvent);

        when(circle.getUid()).thenReturn(CIRCLE_UID);
        when(mapObjectPersist.findMapObjectByUid(CIRCLE_UID)).thenReturn(circle);

        geoFenceManager = Mockito.spy(new GeoFenceManager(mapObjectPersist, monitorFactory, tracker, notificationService,
                alertManager, geoFenceAddedEvent, geoFenceUpdatedEvent, geoFenceRemovedEvent));
    }

    @Test
    void addOrUpdateGeoFence_withNewGeoFence_createsMonitor()
    {
        final GeoFence geoFence = GeoFence.builder().shapeUid(circle.getUid()).build();
        when(circle.getGeoFence()).thenReturn(null, geoFence);

        geoFenceManager.addOrUpdateGeoFence(geoFence);

        verify(circle).setGeoFence(geoFence);
        verify(geoFenceManager).deleteMonitor(CIRCLE_UID);
        verify(geoFenceManager).createMonitor(circle);
        verify(monitorFactory).create(circle, geoFence);

        final var geoFenceAddedCaptor = ArgumentCaptor.forClass(GeoFenceAdded.class);
        verify(geoFenceAddedEvent).fire(geoFenceAddedCaptor.capture());

        final var geoFenceAdded = geoFenceAddedCaptor.getValue();
        Assertions.assertThat(geoFenceAdded.geoFence()).isSameAs(geoFence);
        Assertions.assertThat(geoFenceAdded.shapeMapObject()).isSameAs(circle);
    }

    @Test
    void addOrUpdateGeoFence_withExistingGeoFence_updatesMonitor()
    {
        final GeoFence geoFence = GeoFence.builder().shapeUid(circle.getUid()).build();
        final GeoFence updatedGeoFence = GeoFence.builder().shapeUid(circle.getUid()).trigger(Trigger.Both).build();

        when(circle.getGeoFence()).thenReturn(null, geoFence);
        geoFenceManager.addOrUpdateGeoFence(geoFence);

        reset(geoFenceManager, circle);
        when(circle.getGeoFence()).thenReturn(geoFence, updatedGeoFence);
        when(circle.getUid()).thenReturn(CIRCLE_UID);
        geoFenceManager.addOrUpdateGeoFence(updatedGeoFence);

        verify(circle).setGeoFence(updatedGeoFence);
        verify(geoFenceManager).deleteMonitor(CIRCLE_UID);
        verify(geoFenceManager).createMonitor(circle);

        final var geoFenceUpdatedCaptor = ArgumentCaptor.forClass(GeoFenceUpdated.class);
        verify(geoFenceUpdatedEvent).fire(geoFenceUpdatedCaptor.capture());

        final var geoFenceUpdated = geoFenceUpdatedCaptor.getValue();
        Assertions.assertThat(geoFenceUpdated.geoFence()).isSameAs(updatedGeoFence);
        Assertions.assertThat(geoFenceUpdated.shapeMapObject()).isSameAs(circle);
    }

    @Test
    void addOrUpdateGeoFence_withSameGeoFence_doesNothing()
    {
        final GeoFence geoFence = GeoFence.builder().shapeUid(circle.getUid()).build();
        when(circle.getGeoFence()).thenReturn(geoFence);

        geoFenceManager.addOrUpdateGeoFence(geoFence);

        // verify no calls
        verify(circle, never()).setGeoFence(any());
        verify(geoFenceManager, never()).deleteMonitor(any());
        verify(geoFenceManager, never()).createMonitor(any());
    }

    @Test
    void addOrUpdateGeoFence_withNotTrackingGeoFence_doesNotCreateMonitor()
    {
        final GeoFence geoFence = GeoFence.builder().shapeUid(circle.getUid()).tracking(false).build();
        when(circle.getGeoFence()).thenReturn(null);

        geoFenceManager.addOrUpdateGeoFence(geoFence);

        // verify setGeoFence is called, but no creation
        verify(circle).setGeoFence(geoFence);
        verify(geoFenceManager).deleteMonitor(CIRCLE_UID);
        verify(geoFenceManager, never()).createMonitor(any());
    }

    @Test
    void removeGeoFence_deletesMonitor()
    {
        final GeoFence geoFence = GeoFence.builder().shapeUid(circle.getUid()).build();
        when(circle.getGeoFence()).thenReturn(null);

        geoFenceManager.removeGeoFence(geoFence);

        verify(circle).setGeoFence(null);
        verify(geoFenceManager).deleteMonitor(CIRCLE_UID);

        final var geoFenceRemovedCaptor = ArgumentCaptor.forClass(GeoFenceRemoved.class);
        verify(geoFenceRemovedEvent).fire(geoFenceRemovedCaptor.capture());

        final var geoFenceRemoved = geoFenceRemovedCaptor.getValue();
        Assertions.assertThat(geoFenceRemoved.geoFence()).isSameAs(geoFence);
        Assertions.assertThat(geoFenceRemoved.shapeMapObject()).isSameAs(circle);
    }

    @Test
    void mapObjectRemovedEvent_deletesMonitor()
    {
        final GeoFence geoFence = GeoFence.builder().shapeUid(CIRCLE_UID).build();
        when(circle.requireGeoFence()).thenReturn(geoFence);
        when(circle.hasGeoFence()).thenReturn(true);

        geoFenceManager.onMapObjectRemoved(new MapObjectRemoved(circle));

        verify(geoFenceManager).removeGeoFence(geoFence);
    }
}
