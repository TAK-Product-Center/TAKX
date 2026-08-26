package gov.takx.platform.globe.renderer;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.event.project.ProjectClosing;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.IMapObjectManagerListener;
import gov.takx.api.platform.IProgressMonitor;
import gov.takx.api.platform.IRaptorShutdownListener;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.BitSet;

/**
 * Abstract base class for data models supporting rendering engines.
 *
 * @since 2.29
 */
@PseudoBean
public abstract class ARenderingModel implements IMapObjectManagerListener, IRaptorShutdownListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    protected IMapObjectManager mapObjectManager;

    // Track every MO that has been deleted
    protected final BitSet deletedMOIds = new BitSet();

    /**
     * Construct a model.
     */
    protected ARenderingModel()
    {
    }

    /**
     * Setup our world.  Add listeners, load in all the existing map object data.
     */
    protected void internalInitialize()
    {
        // Register as a map object manager listener (so we get notified of map object creation/deletion)
        mapObjectManager.addMapObjectManagerListener(this);
    }

    @Override
    public void onShutdown(IProgressMonitor progressMonitor)
    {
        mapObjectManager.removeMapObjectManagerListener(this);
    }

    protected boolean isMapObjectDeleted(int mapObjectId)
    {
        return deletedMOIds.get(mapObjectId);
    }

    @Override
    public void clientMapObjectAdded(@Nonnull IMapObject mapObject)
    {
        handleMapObjectAdded(mapObject);
    }

    @Override
    public void clientMapObjectRemoved(@Nonnull IMapObject mapObject)
    {
        if (!mapObject.isDevice() || mapObject.isTransient()) return; // Nothing to do if it has no data

        final Integer moId = mapObject.getId();
        logger.debug("deleteMapObjectData: moId = {}", moId);

        synchronized (deletedMOIds)
        {
            deletedMOIds.set(moId);
        }

        handleMapObjectRemoved(mapObject);
    }

    @Override
    public void mapObjectPartialDelete(@Nonnull IMapObject mapObject, long timeOfDeletion)
    {
        if (!mapObject.isDevice()) return; // Nothing to do if it has no data

        final Integer moId = mapObject.getId();
        logger.debug("mapObjectPartialDelete: moId = {} tod={}", moId, timeOfDeletion);

        handleMapObjectPartialDelete(mapObject, timeOfDeletion);
    }

    /**
     * Handle a Map Object being added to the system.
     */
    protected abstract void handleMapObjectAdded(IMapObject mapObject);

    /**
     * Handle a Map Object being removed from the system.
     */
    protected abstract void handleMapObjectRemoved(IMapObject mapObject);

    /**
     * Handle the partial deletion of a Map Object's data.
     */
    protected abstract void handleMapObjectPartialDelete(IMapObject mapObject, long timeOfDeletion);

    /**
     * The project is closing, quickly nuke the model. No reason to do it MO by MO.
     */
    protected abstract void projectClosing();

    private void onProjectClosing(@SuppressWarnings("unused") @Observes ProjectClosing event)
    {
        projectClosing();
    }
}
