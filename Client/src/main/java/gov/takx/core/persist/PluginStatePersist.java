package gov.takx.core.persist;

import gov.takx.api.messages.IPluginState;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.api.persist.IPersistableState;
import gov.takx.api.persist.IPluginStatePersist;
import gov.takx.api.query.param.HQLParameter;
import gov.takx.dataservices.persist.PluginState;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;

/**
 * Implementation of persistence of PluginState.
 *
 * @since 1.3
 */
@Singleton
public class PluginStatePersist implements IPluginStatePersist
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private IPersistServiceProvider persistServiceProvider;

    @Override
    public void saveOrUpdateState(String stateKey, IPersistableState persistableState)
    {
        try
        {
            String state = persistableState.store();

            if (state != null)
            {
                // See if we already have a row in the DB with this key
                IPluginState pluginState = query(stateKey);

                if (pluginState == null)
                {
                    // No, then create a new one
                    pluginState = new PluginState(stateKey, state);
                } else
                {
                    // Yes, update its state
                    pluginState.setState(state);
                }

                persistState(pluginState);  // Save/update
            }
        } catch (Throwable e)
        {
            logger.error("Error persisting plugin state for {}", stateKey, e);
        }
    }

    @Override
    public void persistState(@Nonnull IPluginState pluginState)
    {
        persistServiceProvider.getPersistService().persistEntity(pluginState);
    }

    @Override
    public IPluginState query(@Nonnull String uniqueKey)
    {
        String hql = "from PluginState where uniqueKey=:key";

        return persistServiceProvider.getPersistService().queryUnique(hql, new HQLParameter("key", uniqueKey));
    }

    @Override
    public List<IPluginState> queryAll()
    {
        return persistServiceProvider.getPersistService().query("from PluginState");
    }

    @Override
    public void deleteState(IPluginState pluginState)
    {
        persistServiceProvider.getPersistService().deleteEntity(pluginState);
    }

    @Override
    public void deleteState(String stateKey)
    {
        persistServiceProvider.getPersistService()
                .executeNativeUpdateSQL("delete from PluginState where uniqueKey = :stateKey",
                        new HQLParameter("stateKey", stateKey));
    }
}
