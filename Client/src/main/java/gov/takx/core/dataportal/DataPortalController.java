package gov.takx.core.dataportal;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.dataportal.IRaptorDataListener;
import gov.takx.api.dataportal.IRaptorDataPortalController;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.messages.IMapEntity;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.messages.IStructure;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.dataservices.persist.RaptorDataStructure;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class DataPortalController implements IRaptorDataPortalController
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private IPersistServiceProvider persistServiceProvider;

    private final IMapObject mapObject;

    private final ConcurrentSkipListSet<PrioritizedEntry<IRaptorDataListener>> listeners = new ConcurrentSkipListSet<>();

    public DataPortalController(@Nonnull IMapObject mapObject)
    {
        this.mapObject = mapObject;
    }

    @Override
    public void sendDataMessage(@Nonnull IPrePersistRaptorDataMessage rdm)
    {
        // Attach the message to our map object to ensure that everyone in the system is looking at the same object.
        rdm.setMapObject(mapObject);

        final IRaptorDataStructure struct = prepareStructure(rdm);

        logger.trace("DataMessage: {}", rdm);

        // First persist the message
        try
        {
            // Skip persistence if it is not persistable, or if it has already been persisted
            if (rdm.isPersistable() && !rdm.hasId())
            {
                persistServiceProvider.getPersistService().persistRDM(rdm);
            }
        } catch (Exception e)
        {
            logger.error("Failed to persist RDM: {}", rdm, e);

            return; // Don't notify any other listeners since we didn't get the message persisted
        }

        // Then notify the data listeners
        final String mapObjectKey = mapObject.getKey();

        listeners.forEach(entry -> entry.safeApply(listener -> listener.onDataMessage(mapObjectKey, rdm, struct), "data message"));
    }

    /**
     * Find or create the structure for the RDM.
     *
     * @param rdm RDM to process
     * @return Proper structure for the RDM.
     * @since 3.0
     */
    @Nonnull
    private IRaptorDataStructure prepareStructure(@Nonnull IPrePersistRaptorDataMessage rdm)
    {
        final IMapEntity mapEntity = mapObject.getMapEntity();
        final IStructure messageStructure = rdm.getStructure();

        IRaptorDataStructure struct = mapEntity.getRaptorDataStructure(messageStructure.getMessageType());

        // If the MapEntity doesn't know about the message's structure, then add it to the MapEntity
        if (struct == null)
        {
            struct = new RaptorDataStructure(rdm.getFamily(), rdm.getType(), messageStructure);

            mapEntity.addStructure(messageStructure); // Update the MapEntity
        }
        return struct;
    }

    @Override
    public void addListener(@Nonnull IRaptorDataListener listener, int priority)
    {
        addListener(new PrioritizedEntry<>(listener, priority));
    }

    /**
     * Add a prioritized data listener.
     *
     * @param prioritizedEntry Entry to add to data listeners
     * @since 3.0
     */
    void addListener(PrioritizedEntry<IRaptorDataListener> prioritizedEntry)
    {
        final IRaptorDataListener listener = prioritizedEntry.delegate;

        if (!listener.matchMapObjectForData(mapObject)) return;

        // Don't add a listener twice
        if (listeners.stream().anyMatch(entry -> entry.delegate == listener)) return;

        logger.debug("Add listener {} on {}", prioritizedEntry, mapObject);

        listeners.add(prioritizedEntry);
    }

    @Override
    public void removeListener(@Nonnull IRaptorDataListener listener)
    {
        if (listeners.removeIf(entry -> entry.delegate == listener))
        {
            logger.debug("Removed listener: {} from {}", listener, mapObject);
        }
    }

    @Nonnull
    @Override
    public IMapObject getMapObject()
    {
        return mapObject;
    }

    /**
     * Stop the controller and notify all listeners that the controller is being closed.
     */
    protected void close()
    {
        listeners.forEach(entry -> entry.safeApply(l -> l.onDisconnect(this), "disconnect"));
        listeners.clear();
    }
}
