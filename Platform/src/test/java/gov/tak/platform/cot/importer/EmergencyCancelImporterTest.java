package gov.tak.platform.cot.importer;

import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.core.comms.cot.CotImporterManager;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.SuppressLogging;
import gov.takx.api.emergency.IEmergencyAlertManager;
import gov.takx.api.emergency.event.EmergencyAlertAdded;
import gov.takx.api.emergency.event.EmergencyAlertRemoved;
import gov.takx.api.persist.IMapObjectFinder;
import gov.takx.core.globe.emergency.EmergencyAlertAnimator;
import gov.takx.test.AIntegrationTest;
import jakarta.enterprise.event.Observes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 5.7
 */
@PseudoBean
public class EmergencyCancelImporterTest extends AIntegrationTest
{
    private final CotImporterManager importerManager = getBean(CotImporterManager.class);
    private final IEmergencyAlertManager alertManager = getBean(IEmergencyAlertManager.class);
    private final IMapObjectFinder mapObjectFinder = getBean(IMapObjectFinder.class);

    private EmergencyAlertAdded lastEmergencyAlertAdded;
    private EmergencyAlertRemoved lastEmergencyAlertRemoved;

    @AfterEach
    void afterEach()
    {
        deleteAllMapObjects();
    }

    @Test
    @SuppressLogging(EmergencyAlertAnimator.class)
    void cancelEmergencyCot_removesAlert() throws IOException
    {
        assertThat(alertManager.getAllAlerts()).isEmpty();
        assertThat(mapObjectFinder.findAllMapObjects()).isEmpty();

        final var event = CotEvent.parse(getResourceAsString("/cot/emergency.cot"));
        assertThat(event.isValid()).isTrue();

        final var cancelEvent = CotEvent.parse(getResourceAsString("/cot/emergency_cancel.cot"));
        assertThat(cancelEvent.isValid()).isTrue();

        assertThat(importerManager.importEvent(event)).isEqualTo(ImportResult.SUCCESS);
        assertThat(alertManager.getAllAlerts()).hasSize(1);
        assertThat(mapObjectFinder.findAllMapObjects()).hasSize(1);

        // Now import the cancel
        assertThat(importerManager.importEvent(cancelEvent)).isEqualTo(ImportResult.SUCCESS);
        assertThat(lastEmergencyAlertRemoved).isNotNull();
        assertThat(lastEmergencyAlertRemoved.alert().mapItem()).isEqualTo(lastEmergencyAlertAdded.alert().mapItem());
        assertThat(alertManager.getAllAlerts()).isEmpty();
        assertThat(mapObjectFinder.findAllMapObjects()).isEmpty();
    }

    @SuppressLogging({EmergencyCancelImporter.class, CotImporterManager.class})
    @Test
    public void invalidCancelCot_resultsInFailure() throws IOException
    {
        // Remove the "cancel" attribute and verify that import fails
        final var cotXml = getResourceAsString("/cot/emergency_cancel.cot").replace("cancel=\"true\"", "");
        final var deleteEvent = CotEvent.parse(cotXml);
        assertThat(deleteEvent.isValid()).isTrue();
        assertThat(importerManager.importEvent(deleteEvent)).isEqualTo(ImportResult.FAILURE);
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
