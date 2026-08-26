package gov.takx.dataservices.persist;

import gov.tak.api.maps.Marker;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.eventlog.IRaptorEventLogManager;
import gov.takx.api.mapobject.IIconMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.IRaptorPlugin;
import gov.takx.api.tracks.IConnectingLineDefinition;
import gov.takx.api.tracks.ISmartTrackManager;
import gov.takx.api.tracks.ITrackStyleDefinition;
import gov.takx.api.tracks.ITrackStylingContext;
import gov.takx.commons.constants.State;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 */
@PseudoBean
@Entity
public class IconMapObject extends MarkerMapObject implements IIconMapObject, PropertyChangeListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // Shared constant to indicate we have no contained Map Objects
    private static final List<IIconMapObject> EMPTY_CONTAINED_LIST = Collections.emptyList();

    protected volatile String containedMapObjectsIds;

    @Transient
    private volatile List<IIconMapObject> containedMapObjects = EMPTY_CONTAINED_LIST;

    protected Integer parentContainerId = null;

    @Transient
    protected IIconMapObject parentContainer;

    @ManyToOne(targetEntity = TrackStyleDefinition.class)
    protected ITrackStyleDefinition trackStyleDefinition;

    @ManyToOne(targetEntity = ConnectingLineDefinition.class)
    protected IConnectingLineDefinition connectingLineDefinition;

    @ManyToOne(targetEntity = TrackStyleDefinition.class)
    protected ITrackStyleDefinition stopRecordsTrackStyleDefinition;

    @Transient
    @Inject
    private ISmartTrackManager smartTrackManager;

    @Transient
    @Inject
    private IRaptorEventLogManager eventLogManager;

    protected IconMapObject()
    {
    }

    public IconMapObject(MapObjectSpec mapObjectSpec, IPluginDefinition<? extends IRaptorPlugin> pluginDefinition)
    {
        super(mapObjectSpec, pluginDefinition);

        // Override the default rendering mode for Markers
        setStyle(getStyle() & ~Marker.STYLE_ROTATE_HEADING_MASK);
    }

    /**
     * Reconnect our parent and contained Map Objects.
     */
    @Override
    public void onAllLoaded()
    {
        super.onAllLoaded();

        // Reattach our parent, if any
        parentContainer = findMapObjectById(parentContainerId, "parent container", IIconMapObject.class);

        // Reattach our contained MOs
        if (containedMapObjectsIds != null)
        {
            containedMapObjects = new CopyOnWriteArrayList<>(findMapObjectsByCsvIds(containedMapObjectsIds));
        }

        // Loop through and reattach ourselves as a listener on each of our contained objects
        for (IIconMapObject child : containedMapObjects)
        {
            // Add ourself as a listener so we can update when important values on our contained object change
            child.addPropertyChangeListener(HIGHEST_STATE, this);
        }
    }

    @Override
    public void showDeviceLog()
    {
        getController().showDeviceLog(null, null, null);
    }

    /**
     * Calculates which of this CIMOs contained objects has the highest state.  It then compares the result with this
     * CIMOs highest state and returns the result.
     *
     * @return the highest state of this CIMO and its children
     */
    @Override
    public State calculateContainedHighestState()
    {
        // Don't convert this back to a stream.collect and a call to State.getHighestState since that
        // is really wasteful on memory.

        State highestState = State.Unknown;
        if (!containedMapObjects.isEmpty())
        {
            for (IIconMapObject cmo : containedMapObjects)
            {
                final State state = cmo.getHighestState();
                if (state != null && state.ordinal() > highestState.ordinal())
                {
                    highestState = state;
                }
            }
        }
        return highestState;
    }

    @Override
    public synchronized void addMapObjectToContainer(@Nonnull IIconMapObject imo)
    {
        // Ensure that this map object is not already in our contained list
        if (containedMapObjects.contains(imo))
        {
            if (logger.isDebugEnabled()) logger.debug("MO {} already contained in MO {}", imo.getKey(), getKey());
            return;
        }

        // To conserve memory in the normal case where there are no contained MOs, the containedMapObjects list is
        // initialized to a shared, constant, empty list.  So, before we can add to it, we need to allocate a
        // real, mutable list.

        if (containedMapObjects == EMPTY_CONTAINED_LIST) containedMapObjects = new CopyOnWriteArrayList<>();

        imo.setParentContainer(this);

        containedMapObjects.add(imo);
        updateContainedMapObjectIds();

        // Update our highest alarm state
        setHighestState(calculateContainedHighestState());

        // Add ourself as a listener so we can update when important values on our contained object change
        imo.addPropertyChangeListener(HIGHEST_STATE, this);

        propertyChangeSupport.firePropertyChange(CONTAINED_ADDED, null, imo);

        // Now add the containment to the EventLog
        eventLogManager.logContainment(new Date(), imo, getName(), true, null);
    }

    @Override
    public synchronized void removeContainedMapObject(@Nonnull IIconMapObject imo)
    {
        // Ensure that this map object is actually in our contained list
        if (!containedMapObjects.contains(imo))
        {
            if (logger.isDebugEnabled()) logger.debug("MO {} not contained in MO {}", imo.getKey(), getKey());
            return;
        }

        imo.setParentContainer(null);

        containedMapObjects.remove(imo);
        updateContainedMapObjectIds();

        // Recalculate our highest state from our new set of contained objects
        setHighestState(calculateContainedHighestState());

        // Unregister as a listener on the map object that was removed
        imo.removePropertyChangeListener(HIGHEST_STATE, this);

        propertyChangeSupport.firePropertyChange(CONTAINED_REMOVED, null, imo);

        // Now add the de-containment to the EventLog
        eventLogManager.logContainment(new Date(), imo, getName(), false, null);
    }

    /**
     * Update the list of contained MO ids according to the current contents of the {@code containedMapObjects} list.
     *
     * @since 3.0
     */
    private void updateContainedMapObjectIds()
    {
        containedMapObjectsIds = encodeMapObjectsAsCsvIds(containedMapObjects);
    }

    @Override
    public boolean internalSetLocation(ILocation location, long time, boolean manualLocation)
    {
        boolean successful = super.internalSetLocation(location, time, manualLocation);
        if (successful)
        {
            if (parentContainer != null && !manualLocation) sendLocationChangeMessage(parentContainer, location);
            for (var contained : containedMapObjects) sendLocationChangeMessage(contained, location);
        }
        return successful;
    }

    private void sendLocationChangeMessage(IMapObject cmo, ILocation location)
    {
        getController().sendLocationChangeMessage(location);
    }

    @Override
    public void shutdown()
    {
        // Unregister as a listener on all the contained map objects
        for (IIconMapObject mapObject : containedMapObjects)
        {
            mapObject.removePropertyChangeListener(HIGHEST_STATE, this);
        }

        super.shutdown();
    }

    @Override
    public void setHighestState(State highestState)
    {
        final State containedHighestState = calculateContainedHighestState();

        if (containedHighestState.ordinal() > highestState.ordinal())
        {
            super.setHighestState(containedHighestState);
        } else
        {
            super.setHighestState(highestState);
        }
    }

    @Override
    public void setParentContainer(IIconMapObject parent)
    {
        propertyChangeSupport.updateAndFire(CONTAINED, isContained(), parent != null,
                () -> {
                    parentContainer = parent;
                    parentContainerId = parent != null ? parent.getId() : null;
                });
    }

    @Override
    public boolean isContained()
    {
        return parentContainer != null;
    }

    @Override
    public IIconMapObject getParentContainer()
    {
        return parentContainer;
    }

    @Override
    public List<IIconMapObject> getContainedMapObjects()
    {
        return Collections.unmodifiableList(containedMapObjects);
    }

    @Override
    public void propertyChange(PropertyChangeEvent propertyChangeEvent)
    {
        final String propName = propertyChangeEvent.getPropertyName();

        if (propName.equals(HIGHEST_STATE))
        {
            // The highest state of one of our contained objects has changed, update our state accordingly
            if (getHighestState() != null)
            {
                setHighestState(calculateContainedHighestState());
            } else
            {
                setHighestState((State) propertyChangeEvent.getNewValue());
            }
        }
    }

    @Override
    @Nullable
    public IConnectingLineDefinition getConnectingLineDefinition()
    {
        return connectingLineDefinition;
    }

    @Override
    public void setConnectingLineDefinition(IConnectingLineDefinition connectingLineDefinition)
    {
        propertyChangeSupport.updateAndFire(CONNECTING_LINE, this.connectingLineDefinition, connectingLineDefinition,
                () -> this.connectingLineDefinition = connectingLineDefinition);
    }

    @Override
    public void setTrackStyleDefinition(ITrackStyleDefinition trackStyleDefinition)
    {
        propertyChangeSupport.updateAndFire(TRACK_STYLE, this.trackStyleDefinition, trackStyleDefinition,
                () -> this.trackStyleDefinition = trackStyleDefinition);
    }

    @Override
    @Nullable
    public ITrackStyleDefinition getTrackStyleDefinition()
    {
        return trackStyleDefinition;
    }

    @Override
    public void setStopRecordsTrackStyleDefinition(ITrackStyleDefinition trackStyleDefinition)
    {
        stopRecordsTrackStyleDefinition = trackStyleDefinition;
    }

    @Override
    @Nullable
    public ITrackStyleDefinition getStopRecordsTrackStyleDefinition()
    {
        return stopRecordsTrackStyleDefinition;
    }

    @Override
    public ITrackStylingContext getTrackStylingContext(@Nonnull IRaptorDataMessage rdm)
    {
        return smartTrackManager.getTrackStylingContext(this);
    }
}
