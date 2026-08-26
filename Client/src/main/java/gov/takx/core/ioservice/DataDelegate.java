package gov.takx.core.ioservice;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.dataportal.IRaptorDataPortal;
import gov.takx.api.mapobject.CreationFailedException;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.messages.IMapEntity;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.persist.IMapObjectPersist;
import gov.takx.api.platform.IDataMonitorServices;
import gov.takx.api.plugin.IDataDelegate;
import gov.takx.api.plugin.PluginCategory;
import gov.takx.api.plugin.device.IDataStructureFactory;
import gov.takx.api.plugin.device.IDevicePlugin;
import gov.takx.core.platform.APluginDelegate;
import gov.takx.core.registry.PluginRegistryManager;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Data operations implemented in multiple delegates.  Originally part of IO Plugin Delegate, but refactored out
 * so that multiple delegates could extend and use.
 *
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public abstract class DataDelegate extends APluginDelegate implements IDataDelegate
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private IMapObjectManager mapObjectManager;

    @Inject
    private IRaptorDataPortal dataPortal;

    @Inject
    private IMapObjectPersist mapObjectPersist;

    protected DataDelegate(String family, String type)
    {
        super(family, type);
    }

    @Override
    public IMapEntity getMapEntity(String family, String type)
    {
        return mapObjectPersist.findMapEntity(family, type);
    }

    @Override
    public IMapEntity getOrCreateMapEntity(String family, String type, List<IRaptorDataStructure> dataStructures)
    {
        return mapObjectManager.getOrCreateMapEntity(family, type, dataStructures);
    }

    @Override
    @Nonnull
    public IMapObject getOrCreateMapObject(@Nonnull MapObjectSpec mapObjectSpec) throws CreationFailedException
    {
        return mapObjectManager.getOrCreateMapObject(mapObjectSpec);
    }

    @Override
    public void sendRaptorDataMessage(IPrePersistRaptorDataMessage rdm)
    {
        dataPortal.sendToGateway(rdm);
    }

    @Override
    public Collection<? extends IMapObject> getMapObjects()
    {
        return mapObjectManager.getMapObjects();
    }

    @Override
    public Collection<? extends IMapObjectController> getMapObjectControllers()
    {
        return mapObjectManager.getMapObjectControllers();
    }

    @Override
    public IMapObject getMapObject(String key)
    {
        return mapObjectManager.getMapObject(key);
    }

    @Override
    public boolean doesPluginExist(PluginCategory category, String family, String type)
    {
        return getPluginRegistryManager().getRegistry(category).exists(family, type);
    }

    @Override
    public Map<IMapObject, IDataMonitorServices> getDataMonitorServices()
    {
        return mapObjectManager.getMapObjectControllers().stream()
                .collect(Collectors.toMap(IMapObjectController::getMapObject, Function.identity()));
    }

    @Override
    public void truncateMapObjectRdmsByTimestamp(IMapObject mapObject, long timestampOfDeletionMillis)
    {
        mapObjectManager.partialDeleteMapObject(mapObject, timestampOfDeletionMillis);
    }

    @Override
    public void deleteMapObject(IMapObject mapObject)
    {
        mapObjectManager.deleteMapObject(mapObject);
    }

    @Override
    public String getNextUnitID(String family, String type)
    {
        return mapObjectManager.getNextUnitID(family, type);
    }

    @Override
    public String getUniqueDataStructureName(String family, String type, String name)
    {
        if (name == null) return null;

        final IMapEntity mapEntity = mapObjectPersist.findMapEntity(family, type);
        if (mapEntity == null) return name;

        String uniqueName = name;
        int count = 2;

        while (mapEntity.getRaptorDataStructure(uniqueName) != null)
        {
            logger.debug("Data structure exists for type {}", uniqueName);
            uniqueName = name + " " + count++;
        }

        logger.debug("Returning {}", uniqueName);

        return uniqueName;
    }

    @Override
    public IDataStructureFactory getDataStructureFactory(String family, String type)
    {
        IDevicePlugin plugin = getPluginRegistryManager().getDevicePluginRegistry().getPlugin(family, type);
        return null != plugin ? plugin.getDataStructureFactory() : null;
    }

    /**
     * @return The plugin registry manager
     */
    protected PluginRegistryManager getPluginRegistryManager()
    {
        return getBean(PluginRegistryManager.class);
    }
}
