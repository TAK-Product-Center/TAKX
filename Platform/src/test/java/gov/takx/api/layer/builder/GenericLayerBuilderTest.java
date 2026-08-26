package gov.takx.api.layer.builder;

import com.atakmap.map.layer.Layer;
import com.atakmap.map.layer.MultiLayer;
import gov.takx.api.layer.LayerConfiguration;
import gov.takx.api.layer.LayerGroup;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @since 5.4
 */
public class GenericLayerBuilderTest
{
    private static final String NAME = "name";
    private static final String DISPLAY_NAME = "display name";
    private static final LayerGroup LAYER_GROUP = LayerGroup.EXTRAS;
    private static final Layer LAYER = new MultiLayer("multilayer");

    @Test
    void builderDefaults_asExpected()
    {
        final LayerConfiguration layerConfiguration = GenericLayerBuilder.builder().setName(NAME).setLayerGroup(LAYER_GROUP).setLayer(LAYER).build();
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
        assertThat(layerConfiguration.layerGroup()).isEqualTo(LAYER_GROUP);
        assertThat(layerConfiguration.layer()).isEqualTo(LAYER);
    }

    @Test
    void missingValues_throws()
    {
        assertThatIllegalStateException()
                .isThrownBy(() -> GenericLayerBuilder.builder().build())
                .withMessageContaining("layer")
                .withMessageContaining("layerGroup");
    }

    private LayerConfiguration makeLayerConfigurationWithAllValuesSet()
    {
        return GenericLayerBuilder.builder()
                .setName(NAME)
                .setDisplayName(DISPLAY_NAME)
                .setVisible(false)
                .setLayerGroup(LAYER_GROUP)
                .setLayer(LAYER)
                .build();
    }
}
