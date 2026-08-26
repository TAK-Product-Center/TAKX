package gov.takx.api.layer.builder;

import gov.takx.api.layer.LayerConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @since 5.4
 */
public class LocalImageryLayerBuilderTest
{
    private static final String NAME = "name";
    private static final String DISPLAY_NAME = "display name";
    private static final String FILE1_NAME = "path" + File.separator + "file1name";
    private static final File FILE1 = new File(FILE1_NAME);
    private static final String FILE2_NAME = "path" + File.separator + "file2name";
    private static final File FILE2 = new File(FILE2_NAME);
    private static final List<File> FILES = List.of(FILE1, FILE2);

    @Test
    void builderDefaults_asExpected()
    {
        final LayerConfiguration layerConfiguration = LocalImageryLayerBuilder.builder().setName(NAME).setFiles(FILES).build();
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
        assertThat(layerConfiguration.restorableXMLString()).isNotNull();
        assertThat(layerConfiguration.restorableXMLString().split(LocalImageryLayerBuilder.ESCAPED_PATH_SEPARATOR))
                .containsExactlyInAnyOrder(FILE1.getAbsolutePath(), FILE2.getAbsolutePath());
    }

    @Test
    void missingValues_throws()
    {
        assertThatIllegalStateException()
                .isThrownBy(() -> LocalImageryLayerBuilder.builder().build())
                .withMessageContaining("name")
                .withMessageContaining("file");
    }

    private LayerConfiguration makeLayerConfigurationWithAllValuesSet()
    {
        return LocalImageryLayerBuilder.builder()
                .setName(NAME)
                .setDisplayName(DISPLAY_NAME)
                .setVisible(false)
                .setFiles(FILES)
                .build();
    }
}
