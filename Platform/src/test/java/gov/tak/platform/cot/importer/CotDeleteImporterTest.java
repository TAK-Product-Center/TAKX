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

/**
 * Validate {@link CotDeleteImporter} functionality.
 *
 * @since 5.0
 */
public class CotDeleteImporterTest extends AIntegrationTest
{
    private final IMapObjectManager mapObjectManager = getBean(IMapObjectManager.class);
    private final CotImporterManager importerManager = getBean(CotImporterManager.class);

    @AfterEach
    public void cleanupAfterMethod()
    {
        deleteAllMapObjects();
    }

    @Test
    public void test_valid_event() throws IOException
    {
        final CotEvent event = CotEvent.parse(getResourceAsString("/cot/good_rectangle.cot"));
        assertThat(event.isValid()).isTrue();
        assertThat(importerManager.importEvent(event)).isEqualTo(ImportResult.SUCCESS);
        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(1);

        final CotEvent delete = CotEvent.parse(getResourceAsString("/cot/good_delete.cot"));
        assertThat(delete.isValid()).isTrue();
        assertThat(importerManager.importEvent(delete)).isEqualTo(ImportResult.SUCCESS);
        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(0);
    }

    @SuppressLogging(CotImporterManager.class)
    @Test
    public void test_invalid_event() throws IOException
    {
        final CotEvent delete = CotEvent.parse(getResourceAsString("/cot/bad_delete.cot"));
        assertThat(delete.isValid()).isTrue();
        assertThat(importerManager.importEvent(delete)).isEqualTo(ImportResult.FAILURE);
    }
}
