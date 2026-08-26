package gov.takx.core.connection;

import gov.takx.api.annotation.ProgramPreference;
import gov.takx.platform.preferences.PreferenceEntry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;

/**
 * Static helper methods for determining auto re-connect support and configuration for plugins in the
 * ProgramProperties.xml file.
 *
 */
@ApplicationScoped
public class AutoReconnectionManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String KEY_CONNECTION = "connectionKey";

    @Inject
    @ProgramPreference(PreferenceEntry.AUTO_RECONNECT_CONNECTIONS)
    private boolean autoReconnectEnabled;

    AutoReconnectionManager()
    {
    }

    /**
     * Check to see if auto-reconnect enabled in the program properties.
     *
     * @return true if specified so in program properties, false otherwise.
     */
    public boolean isAutoReconnectionEnabled()
    {
        logger.debug("Auto Reconnection Connections Flag: {}", autoReconnectEnabled);

        return autoReconnectEnabled;
    }

    /**
     * Check to see if there is a configuration for a specific connection type to auto-reconnect specified in the
     * program properties
     *
     * @param connectionKey connection plugin key
     * @return true if config exists, false otherwise
     */
    public boolean hasConfiguration(String connectionKey)
    {
        final List<String> connectionKeys = PreferenceEntry.AUTO_RECONNECT_SERVICE_NODE.node().valuesFromChildren(KEY_CONNECTION);

        final boolean hasKey = connectionKeys.contains(connectionKey);

        logger.debug("hasConfiguration() for key {}: {}", connectionKey, hasKey);

        return hasKey;
    }
}
