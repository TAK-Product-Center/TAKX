package gov.takx.api.event;

import gov.takx.api.plugin.connection.PreconfiguredConnection;

/**
 * Event fired when a new {@link PreconfiguredConnection} is added at runtime, i.e., hot deployment.
 * <p/>
 * Note that this event is never fired during service startup.  It is only fired when a plugin jar is hot deployed.
 *
 * @param preconfiguredConnection The preconfigured connection that was added
 * @since 5.7
 * @see gov.takx.core.view.gui.maps.importers.PluginJarImporter
 */
public record PreconfiguredConnectionAdded(PreconfiguredConnection preconfiguredConnection)
{
}
