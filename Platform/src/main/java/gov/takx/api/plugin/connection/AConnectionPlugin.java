package gov.takx.api.plugin.connection;

import gov.takx.api.plugin.ARaptorPlugin;
import gov.takx.api.plugin.annotations.ConnectionPlugin;

/**
 * Abstract base implementation for Connection plugins.  Developers are encouraged to extend this
 * base class instead of implementing all the required methods directly.
 *
 * @since 2.0.0
 */
public abstract class AConnectionPlugin extends ARaptorPlugin<ConnectionPlugin> implements IConnectionPlugin
{
    protected AConnectionPlugin()
    {
        super(ConnectionPlugin.class);
    }
}
