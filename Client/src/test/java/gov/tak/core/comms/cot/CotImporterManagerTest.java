package gov.tak.core.comms.cot;

import gov.tak.api.cot.CoordinatedTime;
import gov.tak.api.cot.CotConstants;
import gov.tak.api.cot.CotTypes;
import gov.tak.api.cot.detail.ICotDetailManager;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.AbstractCotEventTypeImporter;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.blacksmith.BlacksmithExtension;
import gov.tak.blacksmith.mock.Mocked;
import gov.tak.platform.cot.detail.CotDetailManager;
import gov.tak.platform.cot.importer.MarkerImporter;
import gov.tak.platform.cot.importer.RectangleImporter;
import gov.takx.api.annotation.SuppressLogging;
import gov.takx.test.AContainerTest;
import gov.takx.test.utils.CdiTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Validate {@link CotImporterManager} functionality.
 *
 * @since 3.0
 */
public class CotImporterManagerTest extends AContainerTest
{
    private static final String TYPE_FRIENDLY = "a-f-G";
    private static final String TYPE_HOSTILE = "a-h-G";

    private final CoordinatedTime now = new CoordinatedTime();

    private final CotEvent standardEvent = new CotEvent("uid", TYPE_FRIENDLY, "2.0", GeoPoint.ZERO_POINT,
            now, now, now, CotConstants.HOW_HUMAN_GARBAGE_IN_GARBAGE_OUT, null, null, null, null);

    private final CotImporterManager importerManager;

    @Mocked(parentTypes = ICotDetailManager.class)
    private CotDetailManager detailManager;

    public CotImporterManagerTest()
    {
        CotImporterManager bean = getBean(CotImporterManager.class);
        Object instance = CdiTestUtils.getInstance(bean);
        BlacksmithExtension.setBeansToUpdate(instance);

        importerManager = spy(bean);

        // Original 'when' statement:
        //  when(importerManager.runImporter(any(MarkerImporter.class), any())).thenReturn(ImportResult.SUCCESS)
        // Using the 'doAnswer' form to prevent NPE on 'runImporter(...)' as it is a spy and the above would also call the real implementation.
        doAnswer(invocation -> ImportResult.SUCCESS).when(importerManager).runImporter(any(MarkerImporter.class), any());
    }

    @BeforeEach
    public void startClean()
    {
        stopService(CotImporterManager.class);
        clearInvocations(detailManager, importerManager);
    }

    @AfterEach
    public void stopClean()
    {
        stopService(CotImporterManager.class);
    }

    @Test
    public void test_detail_manager_is_started_when_importer_manager_is_started()
    {
        startService(CotImporterManager.class);

        verify(detailManager, times(1)).startService();
    }

    @SuppressLogging({CotImporterManager.class})
    @Test
    public void test_default_importer_used_when_none_registered()
    {
        importerManager.importEvent(standardEvent);

        verify(importerManager, times(1)).runImporter(any(MarkerImporter.class), eq(standardEvent));
    }

    @SuppressLogging({CotImporterManager.class})
    @Test
    public void test_type_specific_importer_matching()
    {
        final TestImporter noMatchImporter = new TestImporter(false, TYPE_HOSTILE);
        importerManager.registerImporter(noMatchImporter);

        importerManager.importEvent(standardEvent);

        verify(importerManager, times(1)).runImporter(any(MarkerImporter.class), eq(standardEvent));
        clearInvocations(importerManager);

        importerManager.unregisterImporter(noMatchImporter);

        // Now test a match
        final TestImporter matchingImporter = new TestImporter(false, TYPE_FRIENDLY);
        importerManager.registerImporter(matchingImporter);

        importerManager.importEvent(standardEvent);

        verify(importerManager, times(1)).runImporter(eq(matchingImporter), eq(standardEvent));
    }

    @Test
    public void test_specific_type_takes_precedence()
    {
        final TestImporter typeImporter = new TestImporter(false, TYPE_FRIENDLY);
        final TestImporter prefixImporter = new TestImporter(true, TYPE_FRIENDLY);

        importerManager.registerImporter(prefixImporter);
        importerManager.registerImporter(typeImporter);

        importerManager.importEvent(standardEvent);

        verify(importerManager, times(1)).runImporter(eq(typeImporter), eq(standardEvent));
        clearInvocations(importerManager);
        // Now remove the type-specific importer and test again
        importerManager.unregisterImporter(typeImporter);

        importerManager.importEvent(standardEvent);

        verify(importerManager, times(1)).runImporter(eq(prefixImporter), eq(standardEvent));
    }

    @Test
    public void test_prefix_importer_matching()
    {
        final TestImporter importer = new TestImporter(true, TYPE_HOSTILE);
        importerManager.registerImporter(importer);

        // Force IGNORE for our importer
        when(importerManager.runImporter(importer, standardEvent)).thenReturn(ImportResult.IGNORE);

        importerManager.importEvent(standardEvent);

        // Should run default importer
        verify(importerManager, times(1)).runImporter(any(MarkerImporter.class), eq(standardEvent));

        // Next, a real invocation (no Expectations set) with a matching
        final CotEvent matchingEvent = new CotEvent(standardEvent);
        matchingEvent.setType(TYPE_HOSTILE + "-x");

        assertThat(importerManager.importEvent(matchingEvent)).isEqualTo(ImportResult.SUCCESS);
        assertThat(importer.called).isTrue();
    }

    @Test
    public void defaultImporter_isNotCalled_WhenMatchingPrefixImporter_isInstalled()
    {
        final TestImporter importer = new TestImporter(true, TYPE_HOSTILE);
        importerManager.registerImporter(importer);

        final CotEvent matchingEvent = new CotEvent(standardEvent);
        matchingEvent.setType(TYPE_HOSTILE + "-x");

        // return SUCCESS
        when(importerManager.runImporter(importer, matchingEvent)).thenReturn(ImportResult.SUCCESS);

        importerManager.importEvent(matchingEvent);

        verify(importerManager, times(1)).runImporter(any(), eq(matchingEvent));
    }

    @Test
    public void test_ignored_uses_next_importer()
    {
        final TestImporter matchingImporter = new TestImporter(false, TYPE_FRIENDLY);
        final TestImporter ignoringImporter = new TestImporter(false, TYPE_FRIENDLY);
        ignoringImporter.forceResult = ImportResult.IGNORE;

        importerManager.registerImporter(ignoringImporter);
        importerManager.registerImporter(matchingImporter);

        importerManager.importEvent(standardEvent);

        assertThat(ignoringImporter.called).isTrue();
        assertThat(matchingImporter.called).isTrue();
    }

    @Test
    @SuppressLogging({RectangleImporter.class, CotImporterManager.class})
    public void test_default_importers()
    {
        startService(CotImporterManager.class); // Get core/default importers registered

        final CotEvent rectangleEvent = new CotEvent(standardEvent);
        rectangleEvent.setType(CotTypes.SHAPE_RECTANGLE);

        importerManager.importEvent(rectangleEvent);

        verify(importerManager, times(1)).runImporter(any(RectangleImporter.class), eq(rectangleEvent)); // Should run Rectangle importer
    }

    /**
     * Test importer to help validate execution.
     */
    private static class TestImporter extends AbstractCotEventTypeImporter
    {
        boolean called = false;
        ImportResult forceResult = null;

        protected TestImporter(boolean prefixOnly, String... types)
        {
            super(prefixOnly, types);
        }

        @Override
        public ImportResult importData(CotEvent event)
        {
            called = true;
            return forceResult != null ? forceResult : super.importData(event);
        }

        @Override
        public String toString()
        {
            return "TestImporter{pfx=" + isPrefixOnly()
                    + "; types=" + getSupportedCotTypes()
                    + "; force=" + forceResult
                    + "}";
        }
    }
}
