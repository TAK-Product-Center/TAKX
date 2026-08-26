package gov.takx.core.messagequery;

import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.query.IExtendedQueryConstraints;
import gov.takx.api.query.IExtendedQueryService;
import gov.takx.platform.query.QueryConstraints;

/**
 * This query service offers and extended constraint model for use with plugins that do not have the security
 * restrictions of a device plugin (apps and IO plugins, for example).  The query service will be able to access
 * all {@link IRaptorDataMessage} instances in the database.
 *
 */
public class ExtendedQueryService extends AQueryService implements IExtendedQueryService
{

    @Override
    public IExtendedQueryConstraints createEmptyConstraints()
    {
        final QueryConstraints qc = new QueryConstraints();
        qc.setExtendedClear(true);
        return qc;
    }
}
