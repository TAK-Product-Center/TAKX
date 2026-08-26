package gov.takx.core.globe.tak.elevation.wcs;

import com.atakmap.map.layer.feature.geometry.Envelope;
import gov.nasa.worldwind.ogc.gml.GMLPos;
import gov.nasa.worldwind.ogc.wcs.wcs100.WCS100Capabilities;
import gov.nasa.worldwind.ogc.wcs.wcs100.WCS100Capability;
import gov.nasa.worldwind.ogc.wcs.wcs100.WCS100ContentMetadata;
import gov.nasa.worldwind.ogc.wcs.wcs100.WCS100CoverageOffering;
import gov.nasa.worldwind.ogc.wcs.wcs100.WCS100CoverageOfferingBrief;
import gov.nasa.worldwind.ogc.wcs.wcs100.WCS100DescribeCoverage;
import gov.nasa.worldwind.ogc.wcs.wcs100.WCS100LonLatEnvelope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

public class WcsServiceTest
{
    private static final String MOCK_URL = "url://mock/path";
    private static final String WCS_VERSION = "1.0.0";
    private static final String OFFERING_NAME_1 = "ASTER_NGL";
    private static final String OFFERING_NAME_2 = "NGA Earth";
    private static final Envelope COVERAGE_AREA = new Envelope(10, 20, 0, 40, 60, 0);

    private MockedStatic<WCS100Capabilities> mockedWCS100Capabilities;
    private MockedStatic<WCS100DescribeCoverage> mockedWCS100DescribeCoverage;

    WCS100CoverageOffering coverageOffering1;
    WCS100CoverageOffering coverageOffering2;
    WCS100DescribeCoverage describeCoverage1;
    WCS100DescribeCoverage describeCoverage2;
    WCS100CoverageOfferingBrief coverageOfferingBrief1;
    WCS100CoverageOfferingBrief coverageOfferingBrief2;

    @BeforeEach
    void setUp()
    {
        GMLPos sw = mock(GMLPos.class);
        when(sw.getPos2()).thenReturn(new double[]{COVERAGE_AREA.minX, COVERAGE_AREA.minY});
        GMLPos ne = mock(GMLPos.class);
        when(ne.getPos2()).thenReturn(new double[]{COVERAGE_AREA.maxX, COVERAGE_AREA.maxY});
        WCS100LonLatEnvelope lonLatEnvelope = mock(WCS100LonLatEnvelope.class);
        when(lonLatEnvelope.getPositions()).thenReturn(List.of(sw, ne));

        coverageOffering1 = mock(WCS100CoverageOffering.class);
        when(coverageOffering1.getName()).thenReturn(OFFERING_NAME_1);
        when(coverageOffering1.getLonLatEnvelope()).thenReturn(lonLatEnvelope);
        coverageOffering2 = mock(WCS100CoverageOffering.class);
        when(coverageOffering2.getName()).thenReturn(OFFERING_NAME_2);

        describeCoverage1 = mock(WCS100DescribeCoverage.class);
        when(describeCoverage1.getCoverageOfferings()).thenReturn(List.of(coverageOffering1));
        describeCoverage2 = mock(WCS100DescribeCoverage.class);
        when(describeCoverage2.getCoverageOfferings()).thenReturn(List.of(coverageOffering2));

        mockedWCS100DescribeCoverage = mockStatic(WCS100DescribeCoverage.class);
        mockedWCS100DescribeCoverage.when(() -> WCS100DescribeCoverage.retrieve(any(), eq(OFFERING_NAME_1))).thenReturn(describeCoverage1);
        mockedWCS100DescribeCoverage.when(() -> WCS100DescribeCoverage.retrieve(any(), eq(OFFERING_NAME_2))).thenReturn(describeCoverage2);

        coverageOfferingBrief1 = mock(WCS100CoverageOfferingBrief.class);
        when(coverageOfferingBrief1.getName()).thenReturn(OFFERING_NAME_1);
        coverageOfferingBrief2 = mock(WCS100CoverageOfferingBrief.class);
        when(coverageOfferingBrief2.getName()).thenReturn(OFFERING_NAME_2);

        WCS100ContentMetadata contentMetadata = mock(WCS100ContentMetadata.class);
        when(contentMetadata.getCoverageOfferings()).thenReturn(List.of(coverageOfferingBrief1, coverageOfferingBrief2));

        WCS100Capability capability = mock(WCS100Capability.class);
        when(capability.getGetOperationAddress(any())).thenReturn(MOCK_URL);

        WCS100Capabilities capabilities = mock(WCS100Capabilities.class);
        when(capabilities.getContentMetadata()).thenReturn(contentMetadata);
        when(capabilities.getCapability()).thenReturn(capability);
        when(capabilities.getVersion()).thenReturn(WCS_VERSION);

        mockedWCS100Capabilities = mockStatic(WCS100Capabilities.class);
        mockedWCS100Capabilities.when(() -> WCS100Capabilities.retrieve(any())).thenReturn(capabilities);
    }

    @AfterEach
    void tearDown()
    {
        if (mockedWCS100Capabilities != null) mockedWCS100Capabilities.close();
        if (mockedWCS100DescribeCoverage != null) mockedWCS100DescribeCoverage.close();
    }

    @Test
    public void test_wcs_retrieve_and_get_capabilities() throws Exception
    {
        WcsService service = new WcsService(MOCK_URL);
        service.retrieveCapabilities();
        assertThat(service.getCapabilities()).isNotNull();
    }

    @Test
    public void test_get_capabilities_without_retrieve_throws() throws Exception
    {
        WcsService service = new WcsService(MOCK_URL);
        assertThatIllegalStateException().isThrownBy(service::getCapabilities);
    }

    @Test
    public void test_get_version() throws Exception
    {
        WcsService service = new WcsService(MOCK_URL);
        service.retrieveCapabilities();
        assertThat(service.getVersion()).isEqualTo(WCS_VERSION);
    }

    @Test
    public void test_get_version_without_retrieve_throws() throws Exception
    {
        WcsService service = new WcsService(MOCK_URL);
        assertThatIllegalStateException().isThrownBy(service::getVersion);
    }

    @Test
    public void test_wcs_retrieve_and_get_coverage() throws Exception
    {
        WcsService service = new WcsService(MOCK_URL);
        service.retrieveCapabilities();
        List<WCS100CoverageOfferingBrief> coverages = service.getCoverageOfferingBriefs();
        assertThat(coverages.size()).isEqualTo(2);
        service.retrieveCoverage(coverages.get(0).getName());
        assertThat(service.getCoverageDescription(coverages.get(0).getName())).isEqualTo(describeCoverage1);
        service.retrieveCoverage(coverages.get(1).getName());
        assertThat(service.getCoverageDescription(coverages.get(0).getName())).isEqualTo(describeCoverage1);
        assertThat(service.getCoverageDescription(coverages.get(1).getName())).isEqualTo(describeCoverage2);
    }

    @Test
    public void test_get_coverage_without_retrieve_throws() throws Exception
    {
        WcsService service = new WcsService(MOCK_URL);
        service.retrieveCapabilities();
        List<WCS100CoverageOfferingBrief> coverages = service.getCoverageOfferingBriefs();
        assertThatIllegalStateException().isThrownBy(() -> service.getCoverageDescription(coverages.get(0).getName()));
    }

    @Test
    public void test_get_coverage_and_get_coverage_wrong_name_throws() throws Exception
    {
        WcsService service = new WcsService(MOCK_URL);
        service.retrieveCapabilities();
        List<WCS100CoverageOfferingBrief> coverages = service.getCoverageOfferingBriefs();
        service.retrieveCoverage(coverages.get(0).getName());
        assertThatIllegalStateException().isThrownBy(() -> service.getCoverageDescription(coverages.get(1).getName()));
    }

    @Test
    public void test_get_coverageoffering() throws Exception
    {
        WcsService service = new WcsService(MOCK_URL);
        service.retrieveCapabilities();
        List<WCS100CoverageOfferingBrief> coverages = service.getCoverageOfferingBriefs();
        service.retrieveCoverage(coverages.get(0).getName());
        WCS100DescribeCoverage coverageDescription = service.getCoverageDescription(coverages.get(0).getName());
        assertThat(service.getCoverageOffering(coverageDescription, coverageOffering1.getName())).isEqualTo(coverageOffering1);
    }

    @Test
    public void test_get_coverageoffering_wrong_name_throws() throws Exception
    {
        WcsService service = new WcsService(MOCK_URL);
        service.retrieveCapabilities();
        List<WCS100CoverageOfferingBrief> coverages = service.getCoverageOfferingBriefs();
        service.retrieveCoverage(coverages.get(0).getName());
        service.retrieveCoverage(coverages.get(1).getName());
        WCS100DescribeCoverage coverageDescription1 = service.getCoverageDescription(coverages.get(0).getName());
        WCS100DescribeCoverage coverageDescription2 = service.getCoverageDescription(coverages.get(1).getName());
        assertThatIllegalStateException().isThrownBy(() -> service.getCoverageOffering(coverageDescription1, coverageOffering2.getName()));
        assertThatIllegalStateException().isThrownBy(() -> service.getCoverageOffering(coverageDescription2, coverageOffering1.getName()));
    }

    @Test
    public void test_get_coverageofferingArea() throws Exception
    {
        WcsService service = new WcsService(MOCK_URL);
        service.retrieveCapabilities();
        List<WCS100CoverageOfferingBrief> coverages = service.getCoverageOfferingBriefs();
        service.retrieveCoverage(coverages.get(0).getName());
        WCS100DescribeCoverage coverageDescription = service.getCoverageDescription(coverages.get(0).getName());
        WCS100CoverageOffering coverageOffering = service.getCoverageOffering(coverageDescription, coverageOffering1.getName());
        assertThat(service.getCoverageArea(coverageOffering)).isEqualTo(COVERAGE_AREA);
    }
}
