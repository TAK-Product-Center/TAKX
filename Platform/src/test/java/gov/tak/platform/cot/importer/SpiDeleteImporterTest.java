package gov.tak.platform.cot.importer;

import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.core.comms.cot.CotImporterManager;
import gov.takx.api.annotation.SuppressLogging;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.test.AIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test class targeting {@link SpiDeleteImporter}.
 *
 * @since 5.0
 */
class SpiDeleteImporterTest extends AIntegrationTest
{
    private final IMapObjectManager mapObjectManager = getBean(IMapObjectManager.class);
    private final CotImporterManager importerManager = getBean(CotImporterManager.class);

    @AfterEach
    public void cleanupAfterMethod()
    {
        deleteAllMapObjects();
    }

    @Test
    public void test_valid_digital_pointer_delete() throws IOException
    {
        final CotEvent dpAddEvent = CotEvent.parse(getResourceAsString("/cot/good_dp_add.cot"));
        assertThat(dpAddEvent.isValid()).isTrue();
        assertThat(importerManager.importEvent(dpAddEvent)).isEqualTo(ImportResult.SUCCESS);
        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(1);

        // Import a DP "heartbeat" and assert that the map object is not modified
        dpAddEvent.setTime(dpAddEvent.getTime().addSeconds(5));
        dpAddEvent.setStart(dpAddEvent.getStart().addSeconds(5));
        dpAddEvent.setStale(dpAddEvent.getStale().addSeconds(5));
        assertThat(dpAddEvent.isValid()).isTrue();
        assertThat(importerManager.importEvent(dpAddEvent)).isEqualTo(ImportResult.SUCCESS);
        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(1);

        final CotEvent dpDeleteEvent = CotEvent.parse(getResourceAsString("/cot/good_dp_delete.cot"));
        assertThat(dpDeleteEvent.isValid()).isTrue();
        assertThat(importerManager.importEvent(dpDeleteEvent)).isEqualTo(ImportResult.SUCCESS);
        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(0);
    }

    @SuppressLogging(CotImporterManager.class)
    @Test
    public void test_invalid_digital_pointer_delete() throws IOException
    {
        final CotEvent dpAddEvent = CotEvent.parse(getResourceAsString("/cot/good_dp_add.cot"));
        assertThat(dpAddEvent.isValid()).isTrue();
        assertThat(importerManager.importEvent(dpAddEvent)).isEqualTo(ImportResult.SUCCESS);
        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(1);

        final CotEvent dpDeleteEvent = CotEvent.parse(getResourceAsString("/cot/bad_dp_delete.cot"));
        assertThat(dpDeleteEvent.isValid()).isTrue();
        assertThat(importerManager.importEvent(dpDeleteEvent)).isEqualTo(ImportResult.FAILURE);
        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(1);
    }
}
