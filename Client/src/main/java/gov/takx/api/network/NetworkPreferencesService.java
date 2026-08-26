package gov.takx.api.network;

import gov.tak.api.comms.commo.ConnectionConfiguration;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.api.preferences.IPreferenceEntry;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.preferences.PreferencesNode;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Objects;

/**
 * Provides simplified management of network-related {@link IPreferenceEntry preferences}.
 *
 * @since 5.8
 */
@ApplicationScoped
public class NetworkPreferencesService implements INetworkPreferencesService
{
    private final List<NetworkEntry.Scheme> INPUT_TYPES = List.of(NetworkEntry.Scheme.tcp, NetworkEntry.Scheme.udp);
    private final List<NetworkEntry.Scheme> OUTPUT_TYPES = List.of(NetworkEntry.Scheme.udp);

    private final ICoreExecutorService executorService;

    @Inject
    NetworkPreferencesService(ICoreExecutorService executorService)
    {
        this.executorService = executorService;
    }

    @Override
    public void addInput(@Nonnull NetworkEntry entry) throws IllegalArgumentException
    {
        add(PreferenceEntry.COMMON_COMMO_DEFAULT_INPUTS_NODE, entry, INPUT_TYPES);
    }

    @Override
    public void setInputEnabled(@Nonnull NetworkEntry entry, boolean enabled) throws IllegalArgumentException
    {
        enable(PreferenceEntry.COMMON_COMMO_DEFAULT_INPUTS_NODE, entry, enabled);
    }

    @Override
    @Nullable
    public NetworkEntry getInput(@Nonnull String name)
    {
        return getEntry(PreferenceEntry.COMMON_COMMO_DEFAULT_INPUTS_NODE, name);
    }

    @Override
    @Nonnull
    public List<NetworkEntry> getInputs()
    {
        return list(PreferenceEntry.COMMON_COMMO_DEFAULT_INPUTS_NODE);
    }

    @Override
    public void removeInput(@Nonnull NetworkEntry entry)
    {
        remove(PreferenceEntry.COMMON_COMMO_DEFAULT_INPUTS_NODE, entry);
    }

    @Override
    public void addOutput(@Nonnull NetworkEntry entry) throws IllegalArgumentException
    {
        add(PreferenceEntry.COMMON_COMMO_DEFAULT_OUTPUTS_NODE, entry, OUTPUT_TYPES);
    }

    @Override
    public void setOutputEnabled(@Nonnull NetworkEntry entry, boolean enabled) throws IllegalArgumentException
    {
        enable(PreferenceEntry.COMMON_COMMO_DEFAULT_OUTPUTS_NODE, entry, enabled);
    }

    @Override
    @Nullable
    public NetworkEntry getOutput(@Nonnull String name)
    {
        return getEntry(PreferenceEntry.COMMON_COMMO_DEFAULT_OUTPUTS_NODE, name);
    }

    @Override
    @Nonnull
    public List<NetworkEntry> getOutputs()
    {
        return list(PreferenceEntry.COMMON_COMMO_DEFAULT_OUTPUTS_NODE);
    }

    @Override
    public void removeOutput(@Nonnull NetworkEntry entry)
    {
        remove(PreferenceEntry.COMMON_COMMO_DEFAULT_OUTPUTS_NODE, entry);
    }

    private void add(@Nonnull PreferenceEntry preferenceEntry, @Nonnull NetworkEntry networkEntry, @Nonnull List<NetworkEntry.Scheme> validTypes)
            throws IllegalArgumentException
    {
        final var scheme = networkEntry.scheme();
        final var name = networkEntry.name();

        if (!validTypes.contains(scheme)) throw new IllegalArgumentException(scheme + " is not currently supported");

        final var node = preferenceEntry.node();
        final var exists = node.nodeExists(name);

        if (exists) throw new IllegalArgumentException(String.format("Entry already exists with name %s", name));

        executeAndSave(preferenceEntry, () -> {
            final var newNode = requireNode(node, name);
            newNode.put("uri", scheme + "://" + networkEntry.host() + ':' + networkEntry.port());
            newNode.put("enabled", networkEntry.enabled());
        });
    }

    private void enable(@Nonnull PreferenceEntry preferenceEntry, @Nonnull NetworkEntry networkEntry, boolean enabled)
    {
        final var node = preferenceEntry.node();
        final var name = networkEntry.name();

        if (!node.nodeExists(name)) throw new IllegalArgumentException(String.format("Entry does not exist for %s", name));

        executeAndSave(preferenceEntry, () -> new ConnectionConfiguration(requireNode(node, name)).setEnabled(enabled));
    }

    @Nullable
    private NetworkEntry getEntry(@Nonnull PreferenceEntry preferenceEntry, @Nonnull String name)
    {
        final var node = preferenceEntry.node();
        final var entryNode = node.nodeExists(name) ? node.node(name) : null;

        return entryNode != null ? toNetworkEntry(entryNode) : null;
    }

    @Nonnull
    private List<NetworkEntry> list(@Nonnull PreferenceEntry entry)
    {
        return entry.node()
                .childrenToMap(this::toNetworkEntry)
                .values()
                .stream()
                .filter(Objects::nonNull)
                .toList();
    }

    private void remove(@Nonnull PreferenceEntry preferenceEntry, @Nonnull NetworkEntry networkEntry)
    {
        final var node = preferenceEntry.node().node(networkEntry.name());
        if (node != null) executeAndSave(preferenceEntry, node::removeNode);
    }

    @Nonnull
    private PreferencesNode requireNode(@Nonnull PreferencesNode node, @Nonnull String pathName)
    {
        //noinspection DataFlowIssue should never be null
        return node.node(pathName);
    }

    @Nonnull
    private NetworkEntry toNetworkEntry(@Nonnull PreferencesNode node)
    {
        final var config = new ConnectionConfiguration(node);
        final var uri = config.getUri();

        //noinspection DataFlowIssue
        return new NetworkEntry(config.name(), NetworkEntry.Scheme.valueOf(uri.getScheme()), uri.getHost(), uri.getPort(), config.isEnabled());
    }

    private void executeAndSave(@Nonnull PreferenceEntry preferenceEntry, @Nonnull Runnable runnable)
    {
        executorService.execute(() -> {
            Thread.currentThread().setName(getClass().getName() + " Updater");
            runnable.run();
            preferenceEntry.getPreferenceManager().savePreferences();
            preferenceEntry.firePreferenceEntryChangeEvent();
        });
    }
}
