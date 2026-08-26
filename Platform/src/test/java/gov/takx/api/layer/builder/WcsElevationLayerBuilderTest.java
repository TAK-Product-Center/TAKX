package gov.takx.api.layer.builder;

import gov.takx.api.layer.LayerConfiguration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @since 5.4
 */
public class WcsElevationLayerBuilderTest
{
    private static final String NAME = "name";
    private static final String DISPLAY_NAME = "display name";
    private static final String URI = "https://host/path";
    private static final String COVERAGE_NAME = "coverage name";
    private static final String COVERAGE_OFFERING_NAME = "coverage offering name";
    private static final String XML = WcsElevationLayerBuilder.buildXml(URI, COVERAGE_NAME, COVERAGE_OFFERING_NAME);

    @Test
    void builderDefaults_asExpected()
    {
        final LayerConfiguration layerConfiguration = WcsElevationLayerBuilder.builder().setName(NAME).setUri(URI).build();
        assertThat(layerConfiguration).isNotNull();
        assertThat(layerConfiguration.displayName()).isNull();
        assertThat(layerConfiguration.visible()).isTrue();
    }

    @Test
    void builderSetValues_asSpecified()
    {
        final LayerConfiguration layerConfiguration = makeLayerConfigurationWithAllValuesSet();
        assertThat(layerConfiguration).isNotNull();
        assertThat(layerConfiguration.name()).isEqualTo(NAME);
        assertThat(layerConfiguration.displayName()).isEqualTo(DISPLAY_NAME);
        assertThat(layerConfiguration.visible()).isFalse();
        assertThat(layerConfiguration.restorableXMLString()).isEqualTo(XML);
    }

    @Test
    void missingValues_throws()
    {
        assertThatIllegalStateException()
                .isThrownBy(() -> WcsElevationLayerBuilder.builder().build())
                .withMessageContaining("name")
                .withMessageContaining("uri");
    }

    private LayerConfiguration makeLayerConfigurationWithAllValuesSet()
    {
        return WcsElevationLayerBuilder.builder()
                .setName(NAME)
                .setDisplayName(DISPLAY_NAME)
                .setVisible(false)
                .setUri(URI)
                .setCoverageName(COVERAGE_NAME)
                .setCoverageOfferingName(COVERAGE_OFFERING_NAME)
                .build();
    }
}
