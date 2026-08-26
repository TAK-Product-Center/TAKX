package gov.takx.core.persist;

import gov.takx.api.messages.IExtendedDataStructure;
import gov.takx.api.messages.IRaptorMessage;
import gov.takx.api.persist.IExtendedDataPersist;
import gov.takx.api.persist.IPersistServiceProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.sql.SQLException;

/**
 * Implementation of {@link IExtendedDataPersist}.
 *
 * @since 2.29
 */
@ApplicationScoped
public class ExtendedDataPersist implements IExtendedDataPersist
{
    private final IPersistServiceProvider persistServiceProvider;
    private final MessagePersistHelper messagePersistHelper;

    @Inject
    ExtendedDataPersist(IPersistServiceProvider persistServiceProvider, MessagePersistHelper messagePersistHelper)
    {
        this.persistServiceProvider = persistServiceProvider;
        this.messagePersistHelper = messagePersistHelper;
    }

    @Override
    public int generateRdmId()
    {
        return messagePersistHelper.generateRdmId();
    }

    @Override
    public IExtendedDataStructure loadExtendedData(IRaptorMessage rxm) throws SQLException
    {
        return persistServiceProvider.getPersistService().loadExtendedData(rxm);
    }
}
