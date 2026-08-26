package gov.tak.platform.cot.detail;

import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.util.StandardAttributeNames;
import gov.tak.platform.cot.importer.CotImportHelper;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.breadcrumb.IBreadcrumbManager;
import gov.takx.api.persist.IMapObjectPersist;
import gov.takx.test.AIntegrationTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validate {@link BreadCrumbsDetailHandler}.
 * @since 5.6
 */
@SuppressWarnings("DataFlowIssue")
@PseudoBean
public class BreadcrumbsDetailHandlerTest extends AIntegrationTest
{
    private static final String MARKER_UID = "21c57f66-d135-4180-a771-f2ede16629dc";
    private static final String BREADCRUMBS_ENABLED = "<detail>\n<__bread_crumbs enabled=\"1\"/>";
    private static final String BREADCRUMBS_DISABLED = "<detail>\n<__bread_crumbs enabled=\"false\"/>";

    @Inject
    private CotImportHelper cotImportHelper;

    private final IBreadcrumbManager breadcrumbManager = getBean(IBreadcrumbManager.class);
    private final IMapObjectPersist mapObjectPersist = getBean(IMapObjectPersist.class);

    @AfterEach
    public void afterEach()
    {
        deleteAllMapObjects();
    }

    @Test
    void cotWithNoBreadcrumbs_createsMarkerWithBreadcrumbsDisabled() throws IOException
    {
        final String cotMessage = getResourceAsString("/cot/marker.cot");

        assertThat(cotImportHelper.parseAndImportCot(cotMessage).join()).isEqualTo(ImportResult.SUCCESS);

        final var mapObject = mapObjectPersist.findMapObjectByUid(MARKER_UID);

        final boolean breadcrumbs_on = mapObject.getMetaBoolean(StandardAttributeNames.BREADCRUMBS_ENABLED, false);
        assertThat(breadcrumbs_on).isFalse();
        assertThat(breadcrumbManager.getCurrentTrack(MARKER_UID)).isNull();
    }

    @Test
    void cotEnablingBreadcrumbs_createsTrack() throws IOException
    {
        final String cotMessage = getResourceAsString("/cot/marker.cot").replace("<detail>", BREADCRUMBS_ENABLED);
        assertThat(cotImportHelper.parseAndImportCot(cotMessage).join()).isEqualTo(ImportResult.SUCCESS);

        final var mapObject = mapObjectPersist.findMapObjectByUid(MARKER_UID);

        assertThat(mapObject.hasMetaValue(StandardAttributeNames.BREADCRUMBS_ENABLED)).isFalse();
        assertThat(breadcrumbManager.areBreadcrumbsEnabled(MARKER_UID)).isTrue();
        assertThat(breadcrumbManager.getCurrentTrack(MARKER_UID)).isNotNull();
    }

    @Test
    void cotDisablingBreadcrumbs_stopsRecording() throws IOException
    {
        String cotMessage = getResourceAsString("/cot/marker.cot").replace("<detail>", BREADCRUMBS_ENABLED);
        assertThat(cotImportHelper.parseAndImportCot(cotMessage).join()).isEqualTo(ImportResult.SUCCESS);

        cotMessage = getResourceAsString("/cot/marker.cot").replace("<detail>", BREADCRUMBS_DISABLED);
        assertThat(cotImportHelper.parseAndImportCot(cotMessage).join()).isEqualTo(ImportResult.SUCCESS);

        final var mapObject = mapObjectPersist.findMapObjectByUid(MARKER_UID);

        assertThat(mapObject.hasMetaValue(StandardAttributeNames.BREADCRUMBS_ENABLED)).isFalse();
        assertThat(breadcrumbManager.areBreadcrumbsEnabled(MARKER_UID)).isFalse();
        assertThat(breadcrumbManager.getCurrentTrack(MARKER_UID)).isNotNull();
    }
}
