package gov.takx.api.query;

import gov.takx.api.annotation.NoPluginImplementations;

/**
 * This interface defines the <em>extended</em> constraint operations available to plugins with lower security
 * restrictions than a device plugin (like IO and App plugins).
 * <p>
 * As of 2.0, this interface is essentially deprecated (subsumed by IQueryConstraints), but the number of places
 * in which it is used within plugins really makes it impossible to deprecate it or delete it.  It remains
 * as an empty interface.
 *
 */
@NoPluginImplementations
public interface IExtendedQueryConstraints extends IQueryConstraints
{
}
