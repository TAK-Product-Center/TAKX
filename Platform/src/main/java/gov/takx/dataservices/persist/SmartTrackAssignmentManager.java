package gov.takx.dataservices.persist;

import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.persist.ISmartTrackPersist;
import gov.takx.api.tracks.ITrackStyleDefinition;
import gov.takx.api.tracks.internal.ISmartTrackAssignmentListener;
import gov.takx.api.tracks.internal.ISmartTrackAssignmentManager;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.swing.event.EventListenerList;

import java.util.List;

/**
 * This singleton provides event notifications when RDMs have their TSD assignment changed.  This
 * allows for RDMs to leave memory and still let interested parties know when their TSDs are replaced.  A
 * standard property change model won't work since the RDM listener list can't be persisted.
 * <p>
 * For now, this class is implemented as a standard singleton.  At some point we may want to look at making this
 * a more pluggable service.
 *
 * @since 1.12
 */
@ApplicationScoped
public class SmartTrackAssignmentManager implements ISmartTrackAssignmentManager
{
    private final EventListenerList eventListeners = new EventListenerList();

    private final ISmartTrackPersist smartTrackPersist;

    @Inject
    SmartTrackAssignmentManager(ISmartTrackPersist smartTrackPersist)
    {
        this.smartTrackPersist = smartTrackPersist;
    }

    @Override
    public void addAssignmentListener(@Nonnull ISmartTrackAssignmentListener listener)
    {
        eventListeners.add(ISmartTrackAssignmentListener.class, listener);
    }

    @Override
    public void removeAssignmentListener(@Nonnull ISmartTrackAssignmentListener listener)
    {
        eventListeners.remove(ISmartTrackAssignmentListener.class, listener);
    }

    @Override
    public ITrackStyleDefinition getActiveTSDforRDM(Integer rdmId)
    {
        return smartTrackPersist.getActiveTSDforRDM(rdmId);
    }

    @Override
    public ITrackStyleDefinition getTrackStyleDefinitionById(Integer tsdId)
    {
        return smartTrackPersist.getSharedTrackStyleDefinition(tsdId);
    }

    @Override
    public void assignTSDtoRDM(Integer oldTsdId, ITrackStyleDefinition tsd, IRaptorDataMessage rdm)
    {
        smartTrackPersist.updateRDMTSD(rdm.getIdAsInt(), tsd != null ? tsd.getId() : null);

        // If there is an override TSD, then this current assignment will not generate an event.
        if (rdm.getOverrideTrackStyleDefinitionId() == null)
        {
            fireAssignmentEvent(oldTsdId, tsd, rdm); // Fire the event to all the listeners
        }
    }

    @Override
    public void assignOverrideTSDtoRDM(Integer oldTsdId, ITrackStyleDefinition tsd, IRaptorDataMessage rdm)
    {
        smartTrackPersist.updateOverrideRDMTSD(rdm.getIdAsInt(), tsd != null ? tsd.getId() : null);

        // If we are clearing the override TSD (tsd == null), and there is a permanent TSD recorded, then we need
        // to use the permanent TSD in the event we fire since it is the one now in effect.

        if (tsd == null)
        {
            final ITrackStyleDefinition permTsd = rdm.getTrackStyleDefinition();
            if (permTsd != null) tsd = permTsd;
        }

        fireAssignmentEvent(oldTsdId, tsd, rdm); // Fire the event to all the listeners
    }

    @Override
    public void assignTSDtoRDMs(Integer tsdId, List<Integer> rdmIds)
    {
        // First, we need to get the old TSD id and MO id for each affected RDM
        // We need this to fire the proper events after the update

        final List<Object[]> results = smartTrackPersist.queryRDMTSDAssignments(rdmIds);

        smartTrackPersist.batchUpdateRDMTSD(tsdId, rdmIds); // Perform the batch update

        // Now go through and fire the needed events

        for (Object[] result : results)
        {
            final Integer rdmId = (Integer) result[0];
            final Integer moId = (Integer) result[1];
            final Integer oldTsdId = (Integer) result[2];
            final Integer overrideTsdId = (Integer) result[3];

            // Determine the new Active TSD Id (from the new TSD Id and the overrideTsdId)
            Integer activeTsdId = overrideTsdId != null ? overrideTsdId : tsdId;

            fireAssignmentEvent(oldTsdId, activeTsdId, moId, rdmId);
        }
    }

    /**
     * Fire the TSD assignment event to all registered listeners.
     *
     * @param oldTsdId The Id of the old TSD, may be {@code null}
     * @param tsd      The new TSD
     * @param rdm      The RDM that has been affected
     */
    private void fireAssignmentEvent(Integer oldTsdId, ITrackStyleDefinition tsd, IRaptorDataMessage rdm)
    {
        Object[] listeners = eventListeners.getListenerList();

        for (int i = 0; i < listeners.length; i += 2)
        {
            if (listeners[i] == ISmartTrackAssignmentListener.class)
            {
                ISmartTrackAssignmentListener listener = ((ISmartTrackAssignmentListener) listeners[i + 1]);
                listener.onTSDAssignedToRDM(oldTsdId, tsd, rdm);
            }
        }
    }

    /**
     * Fire the TSD assignment event to all registered listeners.
     *
     * @param oldTsdId The Id of the old TSD, may be {@code null}
     * @param tsdId    The Id of the new TSD, may be {@code null}
     * @param moId     The Id of the Map Object that owns the RDM
     * @param rdmId    The Id of the RDM who's TSD assignment changed
     */
    private void fireAssignmentEvent(Integer oldTsdId, Integer tsdId, Integer moId, Integer rdmId)
    {
        Object[] listeners = eventListeners.getListenerList();

        for (int i = 0; i < listeners.length; i += 2)
        {
            if (listeners[i] == ISmartTrackAssignmentListener.class)
            {
                ISmartTrackAssignmentListener listener = ((ISmartTrackAssignmentListener) listeners[i + 1]);
                listener.onTSDAssignedToRDM(oldTsdId, tsdId, moId, rdmId);
            }
        }
    }
}
