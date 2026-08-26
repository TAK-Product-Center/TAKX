package gov.takx.core.persist;

import gov.takx.api.persist.IConnectionPersist;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.dataservices.persist.Connection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 */
@ApplicationScoped
public class ConnectionPersist implements IConnectionPersist
{
    @Inject
    private IPersistServiceProvider persistServiceProvider;

    @Override
    public void persistConnection(Connection connection)
    {
        persistServiceProvider.getPersistService().persistEntity(connection);
    }

    @Override
    public List<Connection> queryAll()
    {
        return persistServiceProvider.getPersistService().query("from Connection");
    }

    @Override
    public void deleteConnection(Connection connection)
    {
        persistServiceProvider.getPersistService().deleteEntity(connection);
    }
}
