package gov.takx.core.persist;

import gov.takx.api.persist.ICustomDataMonitorPersist;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.api.query.param.HQLParameter;
import gov.takx.dataservices.persist.CustomDataMonitor;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.List;

/**
 * Persist Service Extension class for handling CRUD operations for the CustomDataMonitors
 *
 */
@Singleton
public class CustomDataMonitorPersist implements ICustomDataMonitorPersist
{
    @Inject
    private IPersistServiceProvider persistServiceProvider;

    @Override
    public void persistDataMonitor(CustomDataMonitor cdm)
    {
        persistServiceProvider.getPersistService().persistEntity(cdm);
    }

    @Override
    public List<CustomDataMonitor> query(String key)
    {
        String hql = "from CustomDataMonitor where moKey = :key";

        return persistServiceProvider.getPersistService().query(hql,
                new HQLParameter("key", key));
    }

    @Override
    public List<CustomDataMonitor> queryAll()
    {
        return persistServiceProvider.getPersistService().query("from CustomDataMonitor");
    }

    @Override
    public void deleteDataMonitor(CustomDataMonitor cdm)
    {
        persistServiceProvider.getPersistService().deleteEntity(cdm);
    }
}
