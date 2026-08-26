package gov.takx.api.tracks.internal;

import gov.takx.api.annotation.Internal;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.tracks.ITrackStyleDefinition;

import java.util.EventListener;

/**
 * Ability to receive assignment notifications from the
 * {@link ISmartTrackAssignmentManager} when a track style definition is assigned
 * to either a map object or an RDM.
 *
 * @since 1.12
 */
@Internal
public interface ISmartTrackAssignmentListener extends EventListener
{
    /**
     * Notification that a TSD setting has been made on an RDM.  The TSD may be {@code null} if the TSD assigned to the
     * RDM was cleared.
     *
     * @param oldTsdId The Id of the previous TSD assignment on the RDM, may be {@code null}
     * @param tsd      The TSD that was assigned to the RDM, may be {@code null} if it was cleared
     * @param rdm      The RDM who's TSD assignment changed
     * @see IRaptorDataMessage#setTrackStyleDefinition(ITrackStyleDefinition)
     */
    void onTSDAssignedToRDM(Integer oldTsdId, ITrackStyleDefinition tsd, IRaptorDataMessage rdm);

    /**
     * Notification that a TSD (by Id) setting has been made on an RDM.  The tsdId may be {@code null} if the TSD assigned to the
     * RDM was cleared.  This method is called when a bulk assignment of a TSD Id to a collection of RDMs is made.
     * The RDM instances are not available in memory, so the operation is strictly performed by Id.
     * <p/>
     * Note that the {@code tsdId} specified in this callback is the current active TSD Id.  This might be the
     * override TSD Id, if that has been set.  Otherwise, it is the permanent TSD Id.  {@code tsdId} will only be
     * {@code null} if both the permanent and override TSD Id are both {@code null}.
     *
     * @param oldTsdId The Id of the previous TSD assignment on the RDM, may be {@code null}
     * @param tsdId    The new active TSD that was assigned to the RDM, may be {@code null} if it was cleared
     * @param moId     The Id of the Map Object that owns the RDM
     * @param rdmId    The Id of the RDM who's TSD assignment changed
     * @see ISmartTrackAssignmentManager#assignTSDtoRDMs(Integer, java.util.List)
     */
    void onTSDAssignedToRDM(Integer oldTsdId, Integer tsdId, int moId, int rdmId);
}
