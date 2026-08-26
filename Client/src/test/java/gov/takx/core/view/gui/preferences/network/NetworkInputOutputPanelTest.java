package gov.takx.core.view.gui.preferences.network;

import gov.tak.blacksmith.Blacksmith;
import gov.tak.blacksmith.BlacksmithExtension;
import gov.tak.blacksmith.mock.Mocked;
import gov.tak.blacksmith.mock.strategy.MockStrategy;
import gov.tak.blacksmith.mock.strategy.MockitoStrategy;
import gov.takx.platform.preferences.PreferencesNode;
import gov.takx.test.AIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.function.Consumer;
import java.util.function.Function;

import static gov.takx.platform.preferences.PreferenceEntry.COMMON_COMMO_DEFAULT_INPUTS_NODE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Tests {@link NetworkInputOutputPanel}.
 * 
 * @since 5.0
 */
@ExtendWith(BlacksmithExtension.class)
public class NetworkInputOutputPanelTest extends AIntegrationTest
{
    private static final String URI_KEY = "uri";
    private static final String DEFAULT_URI = "udp://0.0.0.0:4242";
    private static final String ENABLED_KEY = "enabled";
    private static final String DEFAULT_ENABLED = "true";

    static
    {
        MockStrategy mockStrategy = new MockitoStrategy().cached();
        BlacksmithExtension.setBlacksmithFactory(classes -> new Blacksmith(classes)
                .withMockStrategy(mockStrategy));
    }

    @Mocked
    private Consumer<NetworkInputOutputPanel> deleteConsumer;

    @Mocked
    private Function<String, Boolean> duplicateDescriptorChecker;

    private final PreferencesNode defaultPreferencesNode = COMMON_COMMO_DEFAULT_INPUTS_NODE.node().node("Default");

    private NetworkInputOutputPanel panel;

    @BeforeEach
    void beforeEach()
    {
        when(duplicateDescriptorChecker.apply(anyString())).thenReturn(false);
        defaultPreferencesNode.put(ENABLED_KEY, DEFAULT_ENABLED);
        defaultPreferencesNode.put(URI_KEY, DEFAULT_URI);
        panel = new NetworkInputOutputPanel(COMMON_COMMO_DEFAULT_INPUTS_NODE, defaultPreferencesNode, "Input", deleteConsumer,
                duplicateDescriptorChecker, false);
    }

    @Test
    public void okay_updatesPreferenceNode()
    {
        assertThat(defaultPreferencesNode.get(URI_KEY, "")).isEqualTo(DEFAULT_URI);

        panel.editingPanel.setProtocol("tcp");
        panel.editingPanel.setHost("0.0.0.0");
        panel.editingPanel.setPort("1");

        panel.okay();

        assertThat(defaultPreferencesNode.get(URI_KEY, "")).isEqualTo("tcp://0.0.0.0:1");
    }

    @Test
    public void enableCheckBox_updatesPreferenceNodeImmediately()
    {
        assertThat(defaultPreferencesNode.get(ENABLED_KEY, "")).isEqualTo(DEFAULT_ENABLED);

        panel.enableCheckBox.doClick();

        assertThat(defaultPreferencesNode.get(ENABLED_KEY, "")).isEqualTo("false");
    }

    @Test
    public void restorePreferenceValues_restoresPanelValues()
    {
        assertThat(defaultPreferencesNode.get(URI_KEY, "")).isEqualTo(DEFAULT_URI);

        panel.editingPanel.setProtocol("tcp");
        panel.editingPanel.setHost("0.0.0.0");
        panel.editingPanel.setPort("1");

        panel.cancel();

        assertThat(defaultPreferencesNode.get(URI_KEY, "")).isEqualTo(DEFAULT_URI);
    }
}