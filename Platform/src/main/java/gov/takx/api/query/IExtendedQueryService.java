package gov.takx.api.query;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.messages.IRaptorDataMessage;

/**
 * This query service offers an extended constraint model for use with plugins that do not have the security
 * restrictions of a device plugin (apps and IO plugins, for example).  The query service will be able to access
 * all {@link IRaptorDataMessage} instances in the database.
 *
 */
@NoPluginImplementations
public interface IExtendedQueryService extends IQueryService
{
    /**
     * Create a new, empty {@link IExtendedQueryConstraints} object.  Note that an empty constraints object
     * matches all messages in the Database, there is no query domain restriction as there is for a
     * standard (restricted) query constraint.
     *
     * @return Newly created object
     */
    @Override
    IExtendedQueryConstraints createEmptyConstraints();
}
