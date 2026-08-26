package gov.takx.core.persist;

import gov.takx.api.event.project.ProjectClosed;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.messages.IMapEntity;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.messages.IStructure;
import gov.takx.api.persist.IRaptorDataMessageFactory;
import gov.takx.dataservices.persist.RaptorDataMessage;
import gov.takx.platform.KeyUtils;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/**
 * This service is responsible for managing a registry of custom RDM factory functions that are used to construct
 * RDM instances based on a Structure.
 *
 * @since 2.29
 */
@Singleton
public class RaptorDataMessageFactory implements IRaptorDataMessageFactory
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final BiFunction<IStructure, IMapObject, IPrePersistRaptorDataMessage> NEW_RDM = RaptorDataMessage::new;

    private volatile boolean anyFactoriesRegistered = false;
    private final Map<String, BiFunction<IStructure, IMapObject, IPrePersistRaptorDataMessage>> factories = new ConcurrentHashMap<>();

    @Override
    public void registerStructure(@Nonnull IRaptorDataStructure structure,
                                  @Nonnull BiFunction<IStructure, IMapObject, IPrePersistRaptorDataMessage> factory)
    {
        final String factoryKey = getFactoryKey(structure);

        if (factories.containsKey(factoryKey))
        {
            logger.error("Structure [{}] registered more than once for: {}", structure, factoryKey);
        } else
        {
            factories.put(factoryKey, factory);
            anyFactoriesRegistered = true;
        }
    }

    @Override
    public IPrePersistRaptorDataMessage createRaptorDataMessage(@Nonnull IStructure structure, @Nonnull IMapObject mapObject)
    {
        return anyFactoriesRegistered
                ? factories.getOrDefault(getFactoryKey(mapObject.getMapEntity(), structure.getId()), NEW_RDM)
                        .apply(structure, mapObject)
                : new RaptorDataMessage(structure, mapObject);
    }

    /**
     * Clear out all our state when the project closes.
     *
     * @since 2.36
     */
    private void handleProjectClosing(@Observes ProjectClosed event)
    {
        factories.clear();
        anyFactoriesRegistered = false;
    }

    /**
     * @return Key to use for factory registration given a structure
     * @since 3.1
     */
    @Nonnull
    private String getFactoryKey(IRaptorDataStructure structure)
    {
        final IMapEntity mapEntity = structure.getMapEntity();
        final Integer structureId = structure.getId();

        if (mapEntity != null)
        {
            return getFactoryKey(mapEntity, structureId);
        } else
        {
            return KeyUtils.getUniqueKey(structure.getMapObjectFamily(), structure.getMapObjectType(), Integer.toString(structureId));
        }
    }

    /**
     * @return Key to use for factory registration given a MapEntity and a Structure Id
     * @since 3.0
     */
    @Nonnull
    private String getFactoryKey(IMapEntity mapEntity, int structureId)
    {
        return KeyUtils.getUniqueKey(mapEntity.getUniqueKey(), Integer.toString(structureId));
    }
}
