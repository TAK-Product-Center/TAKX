package gov.tak.platform.cot.detail;

import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.util.AttributeSet;
import gov.tak.api.util.StandardAttributeNames;
import gov.tak.core.comms.cot.CotImporterManager;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.SuppressLogging;
import gov.takx.api.emergency.EmergencyAlertType;
import gov.takx.api.emergency.IEmergencyAlertManager;
import gov.takx.api.emergency.event.EmergencyAlertAdded;
import gov.takx.api.emergency.event.EmergencyAlertRemoved;
import gov.takx.api.persist.IMapObjectFinder;
import gov.takx.core.globe.emergency.EmergencyAlertAnimator;
import gov.takx.test.AIntegrationTest;
import jakarta.enterprise.event.Observes;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 5.7
 */
@PseudoBean
public class EmergencyDetailHandlerTest extends AIntegrationTest
{
    private static final String MARKER_UID = "S-1-5-21-451637131-3102226802-3310873526-1001-9-1-1";
    private static final String MARKER_NAME = "DUCK-Alert";
    private static final String MARKER_TYPE = EmergencyAlertType.NineOneOne.getCotType();
    private static final String ALERT_NAME = "DUCK";

    private static final Offset<Double> EPSILON = Offset.offset(1e-6); // Comparison tolerance
    public static final String ALERT_TYPE = "911 Alert";

    private final CotImporterManager importerManager = getBean(CotImporterManager.class);
    private final IEmergencyAlertManager alertManager = getBean(IEmergencyAlertManager.class);
    private final IMapObjectFinder mapObjectFinder = getBean(IMapObjectFinder.class);

    private EmergencyAlertAdded lastEmergencyAlertAdded;
    private EmergencyAlertRemoved lastEmergencyAlertRemoved;

    @AfterEach
    void afterEach()
    {
        deleteAllMapObjects();
        alertManager.getAllAlerts().forEach(alertManager::removeAlert);
    }

    @Test
    @SuppressLogging(EmergencyAlertAnimator.class)
    void emergencyCot_createsEmergencyAlert() throws IOException
    {
        final var event = CotEvent.parse(getResourceAsString("/cot/emergency.cot"));
        assertThat(event.isValid()).isTrue();

        assertThat(importerManager.importEvent(event)).isEqualTo(ImportResult.SUCCESS);

        assertThat(lastEmergencyAlertRemoved).isNull();
        assertThat(lastEmergencyAlertAdded).isNotNull();

        final var alert = lastEmergencyAlertAdded.alert();
        assertThat(alert).isNotNull();
        assertThat(alert.callsign()).isEqualTo(ALERT_NAME);
        assertThat(alert.type()).isEqualTo(ALERT_TYPE);
        assertThat(alert.message()).isEqualTo("DUCK is in trouble");
        assertThat(alert.mapItem()).isNotNull();

        assertThat(alertManager.getAllAlerts()).hasSize(1).contains(alert);

        final var mapObject = mapObjectFinder.findMapObjectByUid(MARKER_UID);
        assertThat(mapObject).isNotNull();
        assertThat(mapObject).isSameAs(alert.mapItem());

        assertThat(mapObject.getLatestCotEvent()).isEqualTo(event);
        assertThat(mapObject.getTitle()).isEqualTo(MARKER_NAME);
        assertThat(mapObject.getCotType()).isEqualTo(MARKER_TYPE);
        assertThat(mapObject.isTransient()).isTrue();

        final var location = mapObject.getLocation();
        assertThat(location.getLatitude()).isCloseTo(41, EPSILON);
        assertThat(location.getLongitude()).isCloseTo(-91, EPSILON);

        final AttributeSet attrs = mapObject.getAttributeSet();
        assertThat(attrs.getStringAttribute(StandardAttributeNames.EMERGENCY)).isEqualTo(ALERT_TYPE);
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
