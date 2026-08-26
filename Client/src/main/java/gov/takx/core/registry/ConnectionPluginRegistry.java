package gov.takx.core.registry;

import gov.takx.api.plugin.PluginCategory;
import gov.takx.api.plugin.connection.IConnectionPlugin;
import gov.takx.api.plugin.connection.IConnectionPluginDefinition;
import gov.takx.api.plugin.connection.PreconfiguredConnection;
import gov.takx.api.registry.IConnectionPluginRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Registry for all Connection plugins.
 *
 */
@ApplicationScoped
public class ConnectionPluginRegistry extends APluginRegistry<IConnectionPlugin, IConnectionPluginDefinition> implements IConnectionPluginRegistry
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final List<PreconfiguredConnection> preconfiguredConnections = Collections.synchronizedList(new ArrayList<>());

    /**
     * Construct the connection plugin registry.
     */
    public ConnectionPluginRegistry()
    {
        super(PluginCategory.ConnectionPlugin);
    }

    @Override
    public void addPreconfiguredConnection(PreconfiguredConnection preconfiguredConnection)
    {
        if (logger.isDebugEnabled()) logger.debug("Add: {}", preconfiguredConnection);

        preconfiguredConnections.add(preconfiguredConnection);
    }

    @Override
    public void applyToPreconfiguredConnections(Consumer<PreconfiguredConnection> processor)
    {
        preconfiguredConnections.forEach(processor);
    }
}
