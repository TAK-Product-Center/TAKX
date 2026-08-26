package gov.takx.core.persist;

import gov.takx.api.media.IMedia;
import gov.takx.api.persist.IMediaPersist;
import gov.takx.api.persist.IPersistServiceProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;

/**
 */
@ApplicationScoped
public class MediaPersist implements IMediaPersist
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private IPersistServiceProvider persistServiceProvider;

    @Override
    public void persist(IMedia media)
    {
        persistServiceProvider.getPersistService().persistEntity(media);
    }

    @Override
    public List<IMedia> queryAll()
    {
        String hql = "from AMedia";

        List<IMedia> results = persistServiceProvider.getPersistService().query(hql);

        if (logger.isDebugEnabled())
        {
            logger.debug("Query: {}", hql);
            logger.debug("    results size: {}", results.size());
        }

        return results;
    }

    @Override
    public void delete(IMedia media)
    {
        persistServiceProvider.getPersistService().deleteEntity(media);
    }
}
