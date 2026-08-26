package gov.takx.test.mock.mapobject;

import gov.takx.api.event.mapobject.MapObjectAdded;
import gov.takx.api.event.mapobject.MapObjectRemoved;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.core.mapobject.MapObjectChangeMonitor;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;

import static jakarta.interceptor.Interceptor.Priority.PLATFORM_BEFORE;

/**
 * Alternative Monitor to be used during test execution.  Performs immediate persistence instead of queuing.
 *
 * @since 3.2
 */
@Alternative
@ApplicationScoped
public class TestMapObjectChangeMonitor implements PropertyChangeListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final IMapObjectManager mapObjectManager;
    private final IPersistServiceProvider persistServiceProvider;

    @Inject
    TestMapObjectChangeMonitor(IMapObjectManager mapObjectManager, IPersistServiceProvider persistServiceProvider)
    {
        this.mapObjectManager = mapObjectManager;
        this.persistServiceProvider = persistServiceProvider;
    }

    @Override
    public void propertyChange(PropertyChangeEvent pce)
    {
        if (MapObjectChangeMonitor.isLocationPropertyChange(pce)) return; // Skip changes associated with location data

        if (logger.isDebugEnabled()) logger.debug("PCE: source={} prop={}", pce.getSource(), pce.getPropertyName());

        final IMapObject mapObject = (IMapObject) pce.getSource();

        if (mapObject.isTransient()) return; // Don't try to persist transient MOs

        if (mapObjectManager.keyExists(mapObject.getKey())) // Ensure it hasn't been deleted
        {
            logger.debug("Persist MO: {}", mapObject);

            try
            {
                persistServiceProvider.getPersistService().forceUpdate(mapObject);
            } catch (Exception e)
            {
                logger.error("Failed to persist MO: {}", mapObject, e);
            }
        }
    }

    /**
     * Add listener to the Map Object.
     */
    private void onMapObjectAdded(@Observes @Priority(PLATFORM_BEFORE) MapObjectAdded event)
    {
        logger.debug("{}", event);
        event.getMapObject().addPropertyChangeListener(this);
    }

    /**
     * Remove our listener from the Map Object.
     */
    private void onMapObjectRemoved(@Observes MapObjectRemoved event)
    {
        logger.debug("{}", event);
        event.getMapObject().removePropertyChangeListener(this);
    }
}
