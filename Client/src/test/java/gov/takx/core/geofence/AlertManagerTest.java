package gov.takx.core.geofence;

import gov.tak.api.cot.CoordinatedTime;
import gov.tak.api.maps.PointMapItem;
import gov.tak.blacksmith.mock.Mocked;
import gov.takx.api.geofence.GeoFence;
import gov.takx.api.geofence.GeoFenceAlert;
import gov.takx.api.geofence.Trigger;
import gov.takx.api.geofence.event.GeoFenceAlertAdded;
import gov.takx.api.geofence.event.GeoFenceAlertRemoved;
import gov.takx.api.notification.INotificationService;
import gov.takx.dataservices.persist.ShapeCircleMapObject;
import gov.takx.dataservices.persist.ShapeEllipseMapObject;
import gov.takx.dataservices.persist.ShapeSquareMapObject;
import gov.takx.dataservices.shapes.Circle;
import gov.takx.dataservices.shapes.Ellipse;
import gov.takx.dataservices.shapes.Square;
import gov.takx.test.AContainerTest;
import jakarta.enterprise.event.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @since 5.3
 */
public class AlertManagerTest extends AContainerTest
{
    private static final String CIRCLE_NAME = "Circle 1";
    private static final String CIRCLE_UID = "CIRCLE_UID";
    private static final String SQUARE_NAME = "Square 1";
    private static final String SQUARE_UID = "SQUARE_UID";
    private static final String MAP_ITEM_NAME = "PMI 1";

    @Mocked
    private INotificationService notificationService;
    @Mocked
    private Event<GeoFenceAlertAdded> alertAddedEvent;
    @Mocked
    private Event<GeoFenceAlertRemoved> alertRemovedEvent;

    @Mocked
    private PointMapItem pointMapItem;
    @Mocked
    private Circle circle;
    @Mocked
    private ShapeCircleMapObject circleMapObject;
    @Mocked
    private Square square;
    @Mocked
    private ShapeSquareMapObject squareMapObject;

    private final GeoFence circleGeoFence = GeoFence.builder().shapeUid(CIRCLE_UID).build();
    private final GeoFence squareGeoFence = GeoFence.builder().shapeUid(SQUARE_UID).build();

    private AlertManager alertManager;

    @BeforeEach
    public void beforeEach()
    {
        when(circle.getShapeMapObject()).thenReturn(circleMapObject);
        when(circle.getName()).thenReturn(CIRCLE_NAME);

        when(circleMapObject.getUid()).thenReturn(CIRCLE_UID);
        when(circleMapObject.getName()).thenReturn(CIRCLE_NAME);
        when(circleMapObject.getGeoFence()).thenReturn(circleGeoFence);

        when(square.getShapeMapObject()).thenReturn(squareMapObject);
        when(square.getName()).thenReturn(SQUARE_NAME);

        when(squareMapObject.getUid()).thenReturn(SQUARE_UID);
        when(squareMapObject.getName()).thenReturn(SQUARE_NAME);
        when(squareMapObject.getGeoFence()).thenReturn(squareGeoFence);

        when(pointMapItem.getName()).thenReturn(MAP_ITEM_NAME);

        reset(notificationService, alertAddedEvent, alertRemovedEvent);

        alertManager = Mockito.spy(new AlertManager(notificationService, alertAddedEvent, alertRemovedEvent));
    }

    @Test
    void whenAlertAdded_correctEventsFired()
    {
        final var expectedAlert = alertManager.alert(circle, pointMapItem, Trigger.Entry);

        assertThat(alertManager.getAlertCount(CIRCLE_UID)).isEqualTo(1);
        assertThat(alertManager.getAlerts(CIRCLE_UID)).containsExactly(expectedAlert);

        assertThat(alertManager.getTotalAlertCount()).isEqualTo(1);
        assertThat(alertManager.getAllAlerts()).containsExactly(expectedAlert);

        assertThat(expectedAlert.shape()).isSameAs(circle);
        assertThat(expectedAlert.geoFence()).isSameAs(circleGeoFence);
        assertThat(expectedAlert.mapItem()).isSameAs(pointMapItem);
        assertThat(expectedAlert.crossingType()).isEqualTo(Trigger.Entry);

        final var alertAddedCaptor = ArgumentCaptor.forClass(GeoFenceAlertAdded.class);
        verify(alertAddedEvent).fire(alertAddedCaptor.capture());

        assertThat(alertAddedCaptor.getValue().alert()).isSameAs(expectedAlert);

        final var geoFenceAlertCaptor = ArgumentCaptor.forClass(GeoFenceAlert.class);
        verify(alertManager).addNotification(geoFenceAlertCaptor.capture());

        final GeoFenceAlert alert = geoFenceAlertCaptor.getValue();
        assertThat(alert.message()).contains(CIRCLE_NAME).contains(MAP_ITEM_NAME).contains("Entered");
    }

    @Test
    void whenAlertRemoved_correctEventsFired()
    {
        final var alert = alertManager.alert(circle, pointMapItem, Trigger.Entry);

        alertManager.removeAlert(alert);

        assertThat(alertManager.getAlertCount(CIRCLE_UID)).isEqualTo(0);
        assertThat(alertManager.getAlerts(CIRCLE_UID)).isEmpty();

        assertThat(alertManager.getTotalAlertCount()).isEqualTo(0);
        assertThat(alertManager.getAllAlerts()).isEmpty();

        final var alertRemovedCaptor = ArgumentCaptor.forClass(GeoFenceAlertRemoved.class);
        verify(alertRemovedEvent).fire(alertRemovedCaptor.capture());
        assertThat(alertRemovedCaptor.getValue().alert()).isSameAs(alert);
    }

    @Test
    void whenAlertsAdded_alertCountIsCorrect()
    {
        final int count = 5;
        for (int i = 0; i < count; i++) alertManager.alert(circle, pointMapItem, Trigger.Entry);

        verify(alertAddedEvent, times(count)).fire(any(GeoFenceAlertAdded.class));
        verify(alertManager, times(count)).addNotification(any(GeoFenceAlert.class));

        assertThat(alertManager.getAlertCount(CIRCLE_UID)).isEqualTo(count);
        assertThat(alertManager.getAlerts(CIRCLE_UID)).hasSize(count);

        assertThat(alertManager.getTotalAlertCount()).isEqualTo(count);
        assertThat(alertManager.getAllAlerts()).hasSize(count);
    }

    @Test
    void whenAlertsCleared_correctEventsFired()
    {
        final int count = 5;
        for (int i = 0; i < count; i++) alertManager.alert(circle, pointMapItem, Trigger.Entry);

        alertManager.clearAlerts(CIRCLE_UID);

        verify(alertRemovedEvent, times(count)).fire(any(GeoFenceAlertRemoved.class));

        assertThat(alertManager.getTotalAlertCount()).isEqualTo(0);
        assertThat(alertManager.getAllAlerts()).isEmpty();
    }

    @Test
    void whenAlertsAddedWithDifferentUids_alertsAreTrackedUniquely()
    {
        final var circleAlert1 = alertManager.alert(circle, pointMapItem, Trigger.Entry);
        final var squareAlert1 = alertManager.alert(square, pointMapItem, Trigger.Entry);
        final var circleAlert2 = alertManager.alert(circle, pointMapItem, Trigger.Entry);
        final var squareAlert2 = alertManager.alert(square, pointMapItem, Trigger.Entry);

        verify(alertManager, times(4)).addNotification(any());

        final var alertAddedCaptor = ArgumentCaptor.forClass(GeoFenceAlertAdded.class);
        verify(alertAddedEvent, times(4)).fire(alertAddedCaptor.capture());

        assertThat(alertManager.getTotalAlertCount()).isEqualTo(4);
        assertThat(alertManager.getAllAlerts()).containsExactlyInAnyOrder(circleAlert1, squareAlert1, circleAlert2, squareAlert2);

        assertThat(alertAddedCaptor.getAllValues()).hasSize(4)
                .extracting(GeoFenceAlertAdded::alert)
                .containsExactly(circleAlert1, squareAlert1, circleAlert2, squareAlert2);

        assertThat(alertManager.getAlertCount(CIRCLE_UID)).isEqualTo(2);
        assertThat(alertManager.getAlerts(CIRCLE_UID)).containsExactly(circleAlert1, circleAlert2);

        assertThat(alertManager.getAlertCount(SQUARE_UID)).isEqualTo(2);
        assertThat(alertManager.getAlerts(SQUARE_UID)).containsExactly(squareAlert1, squareAlert2);

        // Clearing one should leave the other untouched
        alertManager.clearAlerts(CIRCLE_UID);

        assertThat(alertManager.getAlertCount(CIRCLE_UID)).isEqualTo(0);
        assertThat(alertManager.getAlerts(CIRCLE_UID)).isEmpty();

        assertThat(alertManager.getAlertCount(SQUARE_UID)).isEqualTo(2);
        assertThat(alertManager.getAlerts(SQUARE_UID)).containsExactly(squareAlert1, squareAlert2);

        final var alertRemovedCaptor = ArgumentCaptor.forClass(GeoFenceAlertRemoved.class);
        verify(alertRemovedEvent, times(2)).fire(alertRemovedCaptor.capture());

        assertThat(alertRemovedCaptor.getAllValues()).hasSize(2)
                .extracting(GeoFenceAlertRemoved::alert)
                .containsExactly(circleAlert1, circleAlert2);
    }

    @Test
    void getAlerts_withUnknownUid_returnsZeroAndEmpty()
    {
        alertManager.alert(circle, pointMapItem, Trigger.Entry);
        alertManager.alert(square, pointMapItem, Trigger.Entry);

        final Random random = new Random();
        for (int i = 0; i < 5; i++)
        {
            final String uid = "RANDOM_UID_" + random.nextInt();
            assertThat(alertManager.getAlertCount(uid)).isEqualTo(0);
            assertThat(alertManager.getAlerts(uid)).isEmpty();
        }
    }

    @Test
    void removeAlert_withUnknownUid_doesNothing()
    {
        when(circleMapObject.getUid()).thenReturn("UNKNOWN_UID");
        final var alert = new GeoFenceAlert(circle, circleGeoFence, pointMapItem, Trigger.Exit, new CoordinatedTime());

        alertManager.removeAlert(alert);
        verify(alertRemovedEvent, never()).fire(any(GeoFenceAlertRemoved.class));
    }

    @Test
    void clearAlerts_withUnknownUid_doesNothing()
    {
        alertManager.alert(circle, pointMapItem, Trigger.Entry);
        alertManager.alert(square, pointMapItem, Trigger.Entry);

        alertManager.clearAlerts("UNKNOWN_UID");
        verify(alertRemovedEvent, never()).fire(any(GeoFenceAlertRemoved.class));
    }

    @Test
    void onProjectClosing_alertsAreCleared()
    {
        alertManager.alert(circle, pointMapItem, Trigger.Entry);
        alertManager.alert(square, pointMapItem, Trigger.Entry);

        assertThat(alertManager.getAlertCount(CIRCLE_UID)).isEqualTo(1);
        assertThat(alertManager.getAlertCount(SQUARE_UID)).isEqualTo(1);

        alertManager.onProjectClosing(null);

        assertThat(alertManager.getTotalAlertCount()).isEqualTo(0);
        assertThat(alertManager.getAllAlerts()).isEmpty();

        assertThat(alertManager.getAlertCount(CIRCLE_UID)).isEqualTo(0);
        assertThat(alertManager.getAlertCount(SQUARE_UID)).isEqualTo(0);
    }

    @Test
    void whenAlertIsGivenInvalidArguments_exceptionIsThrown(@Mocked Ellipse ellipse, @Mocked ShapeEllipseMapObject ellipseMapObject)
    {
        when(ellipse.getShapeMapObject()).thenReturn(null);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> alertManager.alert(ellipse, pointMapItem, Trigger.Exit))
                .withMessageContaining("ShapeMapObject is null");

        when(ellipse.getShapeMapObject()).thenReturn(ellipseMapObject);
        when(ellipseMapObject.getGeoFence()).thenReturn(null);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> alertManager.alert(ellipse, pointMapItem, Trigger.Exit))
                .withMessageContaining("geoFence is null");
    }
}
