package gov.takx.core.emergency;

import com.atakmap.commoncommo.CommoException;
import gov.tak.api.comms.commo.ICommonCommoManager;
import gov.tak.api.cot.detail.DetailConstants;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.maps.PointMapItem;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.util.StandardAttributeNames;
import gov.tak.blacksmith.BlacksmithExtension;
import gov.tak.blacksmith.mock.Mocked;
import gov.tak.core.comms.commo.CommonCommoManager;
import gov.tak.core.location.LocationTracker;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.SuppressLogging;
import gov.takx.api.emergency.EmergencyAlert;
import gov.takx.api.emergency.EmergencyAlertType;
import gov.takx.api.emergency.IEmergencyAlertManager;
import gov.takx.api.emergency.event.EmergencyAlertAdded;
import gov.takx.api.emergency.event.EmergencyAlertRemoved;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.core.globe.emergency.EmergencyAlertAnimator;
import gov.takx.test.AIntegrationTest;
import gov.takx.test.utils.CdiTestUtils;
import jakarta.enterprise.event.Observes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static gov.takx.platform.preferences.PreferenceEntry.CALL_SIGN;
import static gov.takx.platform.preferences.PreferenceEntry.NODE_UUID;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 5.7
 */
@PseudoBean
public class EmergencyAlertManagerTest extends AIntegrationTest
{
    private static final GeoPoint LOCATION = new GeoPoint(30, -120);

    private final IEmergencyAlertManager emergencyAlertManager = getBean(IEmergencyAlertManager.class);
    private final IMapObjectManager mapObjectManager = getBean(IMapObjectManager.class);

    private PointMapItem pointMapItem;
    private PointMapItem pointMapItem2;
    private PointMapItem pointMapItem3;

    private EmergencyAlertAdded lastEmergencyAlertAdded;
    private EmergencyAlertRemoved lastEmergencyAlertRemoved;

    @SuppressWarnings("unused")
    @Mocked(parentTypes = ICommonCommoManager.class)
    private CommonCommoManager commonCommoManager;

    public EmergencyAlertManagerTest()
    {
        commonCommoManager = getBean(CommonCommoManager.class);
        BlacksmithExtension.setBeansToUpdate(CdiTestUtils.getInstance(emergencyAlertManager));
    }

    @BeforeEach
    void beforeEach()
    {
        getBean(LocationTracker.class).createMarker(); // Ensure we have a "self location" marker

        Mockito.reset(commonCommoManager);

        var spec = MapObjectSpec.builder("TEST", "TEST").name("Duck").location(new GeoPoint(30, -110)).build();
        pointMapItem = (PointMapItem) mapObjectManager.getOrCreateMapObject(spec);

        spec = MapObjectSpec.builder("TEST", "TEST").name("Swan").location(new GeoPoint(60, -10)).build();
        pointMapItem2 = (PointMapItem) mapObjectManager.getOrCreateMapObject(spec);

        spec = MapObjectSpec.builder("TEST", "TEST").name("Goose").location(new GeoPoint(10, -50)).build();
        pointMapItem3 = (PointMapItem) mapObjectManager.getOrCreateMapObject(spec);

        lastEmergencyAlertAdded = null;
        lastEmergencyAlertRemoved = null;
    }

    @AfterEach
    void afterEach()
    {
        deleteAllMapObjects();
    }

    @Test
    @SuppressLogging(EmergencyAlertAnimator.class)
    void addAlert_createsAlert()
    {
        final var alert = emergencyAlertManager.addAlert(pointMapItem, "911 Alert", "the message");
        assertThat(alert).isNotNull();
        assertThat(alert.mapItem()).isSameAs(pointMapItem);
        assertThat(alert.callsign()).isEqualTo("Duck");
        assertThat(alert.type()).isEqualTo("911 Alert");
        assertThat(alert.message()).isEqualTo("the message");
        assertThat(lastEmergencyAlertAdded).isNotNull();
        assertThat(lastEmergencyAlertAdded.alert()).isEqualTo(alert);
    }

    @Test
    @SuppressLogging(EmergencyAlertAnimator.class)
    void removeAlertByValues_removesAlert()
    {
        final var alert = emergencyAlertManager.addAlert(pointMapItem, "911 Alert", "message");
        emergencyAlertManager.removeAlert(pointMapItem);
        assertThat(lastEmergencyAlertRemoved).isNotNull();
        assertThat(lastEmergencyAlertRemoved.alert()).isEqualTo(alert);
        assertThat(lastEmergencyAlertRemoved.alert().mapItem()).isSameAs(pointMapItem);
    }

    @Test
    @SuppressLogging(EmergencyAlertAnimator.class)
    void removeAlertByAlert_removesAlert()
    {
        final var alert = emergencyAlertManager.addAlert(pointMapItem, "911 Alert", "message");
        emergencyAlertManager.removeAlert(alert);
        assertThat(lastEmergencyAlertRemoved).isNotNull();
        assertThat(lastEmergencyAlertRemoved.alert()).isEqualTo(alert);
        assertThat(lastEmergencyAlertRemoved.alert().mapItem()).isSameAs(pointMapItem);
    }

    @Test
    @SuppressLogging(EmergencyAlertAnimator.class)
    void getAllAlerts_returnsAllAlerts()
    {
        final var alert1 = emergencyAlertManager.addAlert(pointMapItem, "Alert 1", "message");
        final var alert2 = emergencyAlertManager.addAlert(pointMapItem2, "Alert 2", "message");
        final var alert3 = emergencyAlertManager.addAlert(pointMapItem3, "Alert 3", "message");

        assertThat(emergencyAlertManager.getAllAlerts()).containsExactlyInAnyOrder(alert1, alert2, alert3);

        emergencyAlertManager.removeAlert(alert1);
        assertThat(emergencyAlertManager.getAllAlerts()).containsExactlyInAnyOrder(alert2, alert3);

        emergencyAlertManager.removeAlert(alert2);
        assertThat(emergencyAlertManager.getAllAlerts()).containsExactlyInAnyOrder(alert3);

        emergencyAlertManager.removeAlert(alert3);
        assertThat(emergencyAlertManager.getAllAlerts()).isEmpty();
    }

    @Test
    void whenValidSelfLocation_getRangeAndBearing_returnsDirection()
    {
        // place our self location so the alert is East of it
        mapObjectManager.getSelfMarker().setLocation(LOCATION, true);
        final var alert = new EmergencyAlert(pointMapItem, "callsign", "911 Alert", "message");
        final String rangeAndBearing = emergencyAlertManager.getRangeAndBearing(alert);
        assertThat(rangeAndBearing).contains("km").endsWith("E");
    }

    @Test
    void whenInvalidSelfLocation_getRangeAndBearing_returnsNoGps()
    {
        mapObjectManager.getSelfMarker().setLocation(GeoPoint.UNKNOWN_POINT, true);
        final var alert = new EmergencyAlert(pointMapItem, "callsign", "911 Alert", "message");
        final String rangeAndBearing = emergencyAlertManager.getRangeAndBearing(alert);
        assertThat(rangeAndBearing).isEqualTo("(No GPS)");
    }

    @Test
    void broadcastAlert_hasCorrectEventContent() throws CommoException
    {
        mapObjectManager.getSelfMarker().setLocation(LOCATION, true);
        final String specialMessage = "special message";

        emergencyAlertManager.broadcastAlert(EmergencyAlertType.NineOneOne, specialMessage);

        final var cotEventCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(commonCommoManager).broadcastCoT(cotEventCaptor.capture());

        final var cotEvent = CotEvent.parse(cotEventCaptor.getValue());
        assertThat(cotEvent.getUID()).isEqualTo(NODE_UUID.getStringValue() + "-9-1-1");
        assertThat(cotEvent.getType()).isEqualTo(EmergencyAlertType.NineOneOne.getCotType());

        final var emergency = cotEvent.getDetail().getFirstChildByName(StandardAttributeNames.EMERGENCY);
        assertThat(emergency.getAttribute(DetailConstants.ATTR_TYPE)).isEqualTo(EmergencyAlertType.NineOneOne.getDescription());
        assertThat(emergency.getInnerText()).isEqualTo(specialMessage);

        final var contact = cotEvent.getDetail().getFirstChildByName(DetailConstants.CONTACT);
        assertThat(contact.getAttribute(DetailConstants.ATTR_CALLSIGN)).isEqualTo(CALL_SIGN.getStringValue() + "-Alert");
    }

    @Test
    void cancelAlert_hasCorrectEventContent() throws CommoException
    {
        mapObjectManager.getSelfMarker().setLocation(LOCATION, true);
        emergencyAlertManager.cancelAlert(EmergencyAlertType.NineOneOne);

        final var cotEventCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(commonCommoManager).broadcastCoT(cotEventCaptor.capture());

        final var cotEvent = CotEvent.parse(cotEventCaptor.getValue());
        assertThat(cotEvent.getType()).isEqualTo(EmergencyAlertType.Cancel.getCotType());

        final var emergency = cotEvent.getDetail().getFirstChildByName(StandardAttributeNames.EMERGENCY);
        assertThat(emergency.getAttribute("cancel")).isEqualTo("true");
    }

    private void onAlertAdded(@Observes EmergencyAlertAdded emergencyAlertAdded)
    {
        lastEmergencyAlertAdded = emergencyAlertAdded;
    }

    private void onAlertRemoved(@Observes EmergencyAlertRemoved emergencyAlertRemoved)
    {
        lastEmergencyAlertRemoved = emergencyAlertRemoved;
    }
}
