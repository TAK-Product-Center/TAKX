package gov.takx.api.network;

import gov.tak.api.comms.commo.ConnectionConfiguration;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.preferences.PreferencesNode;
import gov.takx.test.AContainerTest;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests the {@link NetworkPreferencesService}.
 *
 * @since 5.8
 */
public class NetworkPreferencesServiceTest extends AContainerTest
{
    private static final URI TCP_URI = getUri("tcp://0.0.0.0:8080");
    private static final URI UDP_URI = getUri("udp://0.0.0.0:8080");
    private static final NetworkEntry TCP_ENTRY = getEntry("name", TCP_URI, true);
    private static final NetworkEntry UDP_ENTRY = getEntry("name", UDP_URI, true);

    private final NetworkPreferencesService service;
    private final PreferencesNode inputsNode;
    private final PreferencesNode outputsNode;

    public NetworkPreferencesServiceTest()
    {
        service = getBean(NetworkPreferencesService.class);
        inputsNode = PreferenceEntry.COMMON_COMMO_DEFAULT_INPUTS_NODE.node();
        outputsNode = PreferenceEntry.COMMON_COMMO_DEFAULT_OUTPUTS_NODE.node();
    }

    @BeforeEach
    public void beforeEach()
    {
        service.getInputs().forEach(service::removeInput);
        service.getOutputs().forEach(service::removeOutput);
    }

    @Test
    public void addInput_updatesPreferenceNode()
    {
        service.addInput(TCP_ENTRY);
        validateNode(TCP_ENTRY, inputsNode, TCP_URI);
    }

    @Test
    public void addOutput_updatesPreferenceNode()
    {
        service.addOutput(UDP_ENTRY);
        validateNode(UDP_ENTRY, outputsNode, UDP_URI);
    }

    @Test
    public void getInput_returnsCorrectValues()
    {
        service.addInput(TCP_ENTRY);

        final var result = service.getInput(TCP_ENTRY.name());
        validateEntry(TCP_ENTRY, result);
    }

    @Test
    public void getOutput_returnsCorrectValues()
    {
        service.addOutput(UDP_ENTRY);

        final var result = service.getOutput(UDP_ENTRY.name());
        validateEntry(UDP_ENTRY, result);
    }

    @Test
    public void getInputs_returnsCorrectValues()
    {
        final var entry1 = getEntry("name1", TCP_URI, true);
        final var entry2 = getEntry("name2", TCP_URI, true);

        service.addInput(entry1);
        service.addInput(entry2);

        final var result = service.getInputs();
        assertThat(result).containsExactlyInAnyOrderElementsOf(List.of(entry1, entry2));
    }

    @Test
    public void getOutputs_returnsCorrectValues()
    {
        final var entry1 = getEntry("name1", UDP_URI, true);
        final var entry2 = getEntry("name2", UDP_URI, true);

        service.addOutput(entry1);
        service.addOutput(entry2);

        final var result = service.getOutputs();
        assertThat(result).containsExactlyInAnyOrderElementsOf(List.of(entry1, entry2));
    }

    @Test
    public void removeInput_removesInput()
    {
        service.addInput(UDP_ENTRY);
        assertThat(service.getInputs()).hasSize(1);

        service.removeInput(UDP_ENTRY);
        assertThat(service.getInputs()).isEmpty();
    }

    @Test
    public void removeOutput_removesOutput()
    {
        service.addOutput(UDP_ENTRY);
        assertThat(service.getOutputs()).hasSize(1);

        service.removeOutput(UDP_ENTRY);
        assertThat(service.getOutputs()).isEmpty();
    }

    @Test
    public void removeInput_updatesPreferenceNode()
    {
        service.addInput(UDP_ENTRY);
        assertThat(inputsNode.nodeExists(UDP_ENTRY.name())).isTrue();

        service.removeInput(UDP_ENTRY);
        assertThat(inputsNode.nodeExists(UDP_ENTRY.name())).isFalse();
    }

    @Test
    public void removeOutput_updatesPreferenceNode()
    {
        service.addOutput(UDP_ENTRY);
        assertThat(outputsNode.nodeExists(UDP_ENTRY.name())).isTrue();

        service.removeOutput(UDP_ENTRY);
        assertThat(outputsNode.nodeExists(UDP_ENTRY.name())).isFalse();
    }

    @Test
    public void addInput_whenDuplicateName_throwsException()
    {
        service.addInput(UDP_ENTRY);
        assertThatThrownBy(() -> service.addInput(UDP_ENTRY)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void addOutput_whenDuplicateName_throwsException()
    {
        service.addOutput(UDP_ENTRY);
        assertThatThrownBy(() -> service.addOutput(UDP_ENTRY)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void addOutput_whenUnsupportedScheme_throwsException()
    {
        assertThatThrownBy(() -> service.addOutput(TCP_ENTRY)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void addInput_whenEnabled_createsEnabledEntry()
    {
        final var entry = getEntry("name", TCP_URI, true);
        service.addInput(entry);

        //noinspection DataFlowIssue
        assertThat(service.getInput(entry.name()).enabled()).isTrue();
    }

    @Test
    public void addOutput_whenEnabled_createsEnabledEntry()
    {
        final var entry = getEntry("name", UDP_URI, true);
        service.addOutput(entry);

        //noinspection DataFlowIssue
        assertThat(service.getOutput(entry.name()).enabled()).isTrue();
    }

    @Test
    public void addInput_whenDisabled_createsDisabledEntry()
    {
        final var entry = getEntry("name", TCP_URI, false);
        service.addInput(entry);

        //noinspection DataFlowIssue
        assertThat(service.getInput(entry.name()).enabled()).isFalse();
    }

    @Test
    public void addOutput_whenDisabled_createsDisabledEntry()
    {
        final var entry = getEntry(UDP_ENTRY.name(), UDP_URI, false);
        service.addOutput(entry);

        //noinspection DataFlowIssue
        assertThat(service.getOutput(entry.name()).enabled()).isFalse();
    }

    private void validateNode(NetworkEntry entry, PreferencesNode node, URI uri)
    {
        //noinspection DataFlowIssue
        final var config = new ConnectionConfiguration(node.node(entry.name()));
        assertThat(config.getDisplayName()).isEqualTo(entry.name());
        assertThat(config.getUri()).isEqualTo(uri);
        assertThat(config.isEnabled()).isEqualTo(entry.enabled());
    }

    private void validateEntry(NetworkEntry added, NetworkEntry returned)
    {
        assertThat(returned).isNotNull();
        assertThat(added.name()).isEqualTo(returned.name());
        assertThat(added.scheme()).isEqualTo(returned.scheme());
        assertThat(added.host()).isEqualTo(returned.host());
        assertThat(added.port()).isEqualTo(returned.port());
        assertThat(added.enabled()).isEqualTo(returned.enabled());
    }

    private static NetworkEntry getEntry(String name, URI uri, boolean enabled)
    {
        return new NetworkEntry(name, NetworkEntry.Scheme.valueOf(uri.getScheme()), uri.getHost(), uri.getPort(), enabled);
    }

    @Nonnull
    private static URI getUri(String uriString)
    {
        try
        {
            return URI.create(uriString);
        } catch (Exception e)
        {
            //noinspection DataFlowIssue should never happen
            return null;
        }
    }
}
