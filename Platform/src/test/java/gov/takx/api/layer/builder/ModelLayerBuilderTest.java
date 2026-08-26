package gov.takx.api.layer.builder;

import gov.takx.api.layer.LayerConfiguration;
import gov.takx.api.layer.LayerConfiguration.LayerClassType;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @since 5.4
 */
public class ModelLayerBuilderTest
{
    private static final String NAME = "name";
    private static final String DISPLAY_NAME = "display name";
    private static final String FILE_NAME = "path" + File.separator + "filename";
    private static final File FILE = new File(FILE_NAME);

    @Test
    void builderDefaults_asExpected()
    {
        final LayerConfiguration layerConfiguration = ModelLayerBuilder.builder().setName(NAME).setFile(FILE).build();
        assertThat(layerConfiguration).isNotNull();
        assertThat(layerConfiguration.displayName()).isNull();
        assertThat(layerConfiguration.visible()).isTrue();
        assertThat(layerConfiguration.layerClassType()).isEqualTo(LayerClassType.Model);
    }

    @Test
    void builderSetValues_asSpecified()
    {
        final LayerConfiguration layerConfiguration = makeLayerConfigurationWithAllValuesSet();
        assertThat(layerConfiguration).isNotNull();
        assertThat(layerConfiguration.name()).isEqualTo(NAME);
        assertThat(layerConfiguration.displayName()).isEqualTo(DISPLAY_NAME);
        assertThat(layerConfiguration.visible()).isFalse();
        assertThat(layerConfiguration.layerClassType()).isEqualTo(LayerClassType.Model);
        assertThat(layerConfiguration.restorableXMLString()).isEqualTo(FILE.getAbsolutePath());
    }

    @Test
    void missingValues_throws()
    {
        assertThatIllegalStateException()
                .isThrownBy(() -> ModelLayerBuilder.builder().build())
                .withMessageContaining("name")
                .withMessageContaining("file");
    }

    private LayerConfiguration makeLayerConfigurationWithAllValuesSet()
    {
        return ModelLayerBuilder.builder()
                .setName(NAME)
                .setDisplayName(DISPLAY_NAME)
                .setVisible(false)
                .setFile(FILE)
                .build();
    }
}
