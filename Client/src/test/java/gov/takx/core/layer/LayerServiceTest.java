package gov.takx.core.layer;

import com.atakmap.map.layer.MultiLayer;
import gov.takx.api.layer.ILayerService;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.layer.LayerConfiguration;
import gov.takx.api.layer.LayerGroup;
import gov.takx.api.layer.builder.GenericLayerBuilder;
import gov.takx.api.layer.builder.LocalImageryLayerBuilder;
import gov.takx.test.AIntegrationTest;
import gov.takx.test.utils.SwingTestUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 5.4
 */
public class LayerServiceTest extends AIntegrationTest
{
    private final ILayerService layerService;
    private final RaptorLayerManager raptorLayerManager;

    public LayerServiceTest()
    {
        raptorLayerManager = getBean(RaptorLayerManager.class);
        layerService = getBean(ILayerService.class);
    }

    @Test
    public void test_addLayer()
    {
        final var layerConfigurations = createSampleLayerConfigurations();
        final var layerNames = layerConfigurations.stream().map(LayerConfiguration::name).collect(Collectors.toSet());
        final var listener = new TestRaptorLayerManagerListener(false);
        raptorLayerManager.addRaptorLayerManagerListener(listener);

        final var raptorLayers = new ArrayList<IRaptorLayer>();
        for (var layerConfiguration : layerConfigurations)
        {
            var raptorLayer = layerService.addLayer(layerConfiguration);
            assertThat(raptorLayer).isNotNull();
            raptorLayers.add(raptorLayer);
        }

        SwingTestUtils.waitForEdtEvents(); // listener callbacks happen on the EDT

        assertThat(listener.getLayerNamesAdded()).containsExactlyInAnyOrderElementsOf(layerNames);
        assertThat(raptorLayerManager.getLayers()).containsAll(raptorLayers);

        raptorLayerManager.removeRaptorLayerManagerListener(listener);
    }

    private List<LayerConfiguration> createSampleLayerConfigurations()
    {
        ArrayList<LayerConfiguration> layerConfigurations = new ArrayList<>();

        layerConfigurations.add(LocalImageryLayerBuilder.builder()
                .setName("LocalImageryLayerBuilder")
                .setFiles(List.of(new File("samplefile.tif")))
                .build());

        layerConfigurations.add(GenericLayerBuilder.builder()
                .setName("multilayer")
                .setLayerGroup(LayerGroup.EXTRAS)
                .setLayer(new MultiLayer("multilayer"))
                .build());

        return layerConfigurations;
    }
}
