package gov.takx.api.layer.builder;

import gov.takx.api.layer.LayerConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @since 5.4
 */
public class OnlineImageryLayerBuilderTest
{
    private static final String NAME = "name";
    private static final String DISPLAY_NAME = "display name";
    private static final String XML = "<doc><el>element</el></doc>";

    @Test
    void builderDefaults_asExpected()
    {
        final LayerConfiguration layerConfiguration = OnlineImageryLayerBuilder.builder().setName(NAME).setConfigurationXml(XML).build();
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
    void setConfigurationFile() throws IOException
    {
        File tempFile = File.createTempFile("test", null);
        tempFile.deleteOnExit();
        Files.writeString(tempFile.toPath(), XML);
        LayerConfiguration layerConfiguration = OnlineImageryLayerBuilder.builder()
                .setName(NAME)
                .setConfigurationFile(tempFile)
                .build();
        assertThat(layerConfiguration.restorableXMLString()).isEqualTo(XML);
    }

    @Test
    void missingValues_throws()
    {
        assertThatIllegalStateException()
                .isThrownBy(() -> OnlineImageryLayerBuilder.builder().build())
                .withMessageContaining("name")
                .withMessageContaining("configurationXml");
    }

    private LayerConfiguration makeLayerConfigurationWithAllValuesSet()
    {
        return OnlineImageryLayerBuilder.builder()
                .setName(NAME)
                .setDisplayName(DISPLAY_NAME)
                .setVisible(false)
                .setConfigurationXml(XML)
                .build();
    }
}
