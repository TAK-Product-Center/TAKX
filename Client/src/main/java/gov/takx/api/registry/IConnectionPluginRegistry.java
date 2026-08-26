package gov.takx.api.registry;

import gov.takx.api.plugin.connection.IConnectionPlugin;
import gov.takx.api.plugin.connection.IConnectionPluginDefinition;
import gov.takx.api.plugin.connection.PreconfiguredConnection;

import java.util.function.Consumer;

/**
 * Maintains a registry of all known IConnectionPlugin instances.
 *
 */
public interface IConnectionPluginRegistry extends IPluginRegistry<IConnectionPlugin, IConnectionPluginDefinition>
{
    /**
     * Add a preconfigured connection definition to the registry.
     *
     * @param preconfiguredConnection Definition to add
     * @since 2.20
     */
    void addPreconfiguredConnection(PreconfiguredConnection preconfiguredConnection);

    /**
     * Apply a processor to each preconfigured connection definition.  Simplified visitor pattern.
     *
     * @since 2.20
     */
    void applyToPreconfiguredConnections(Consumer<PreconfiguredConnection> processor);
}
