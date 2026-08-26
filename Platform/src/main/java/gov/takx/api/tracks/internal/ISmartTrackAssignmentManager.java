package gov.takx.api.tracks.internal;

import gov.takx.api.annotation.Internal;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.tracks.ITrackStyleDefinition;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Contract for the Smart Track Assignment Manager, which provides a centralized service for dealing with assignments
 * of Track Style Definitions to RDMs.
 * <p/>
 * This allows for RDMs to leave memory and still let interested parties know when their TSDs are replaced.  A
 * standard property change model won't work since the RDM listener list can't be persisted.
 *
 */
@Internal
@NoPluginImplementations
public interface ISmartTrackAssignmentManager
{
    /**
     * Registers a SmartTrack assignment listener.
     *
     * @param listener Listener
     */
    void addAssignmentListener(@Nonnull ISmartTrackAssignmentListener listener);

    /**
     * Unregisters a SmartTrack assignment listener.
     *
     * @param listener Listener
     */
    void removeAssignmentListener(@Nonnull ISmartTrackAssignmentListener listener);

    /**
     * Get the TSD assigned to the given RDM (by ID).  This will be the "override" TSD if
     * one has been set, or the permanent TSD otherwise.  If neither TSD is set, then return {@code null}.
     *
     * @param rdmId ID of the RDM in question
     * @return The assigned TSD (override or permanent), or {@code null} if none
     */
    ITrackStyleDefinition getActiveTSDforRDM(Integer rdmId);

    /**
     * Get a TrackStyleDefinition given its database Id.
     *
     * @param tsdId Id of TSD to retrieve
     * @return The TSD, or {@code null} if none with that Id
     */
    ITrackStyleDefinition getTrackStyleDefinitionById(Integer tsdId);

    /**
     * Process an assignment of a TSD (which may be {@code null}) to an RDM.  Fire the appropriate event to all listeners.
     * Note that if an "override" TSD is in effect for this RDM, then no event will be fired (since a change
     * to the permanent TSD while overridden has no effect on the visible rendering).
     *
     * @param oldTsdId The Id of the previous TSD assignment on the RDM, may be {@code null}
     * @param tsd      The new TSD assigned to the RDM, may be {@code null}
     * @param rdm      The RDM that was affected
     */
    void assignTSDtoRDM(Integer oldTsdId, ITrackStyleDefinition tsd, IRaptorDataMessage rdm);

    /**
     * Process an assignment of an "override" TSD (which may be {@code null}) to an RDM.  Fire the appropriate event to all listeners.
     *
     * @param oldTsdId The Id of the previous TSD assignment on the RDM, may be {@code null}
     * @param tsd      The new TSD assigned to the RDM, may be {@code null}
     * @param rdm      The RDM that was affected
     */
    void assignOverrideTSDtoRDM(Integer oldTsdId, ITrackStyleDefinition tsd, IRaptorDataMessage rdm);

    /**
     * Bulk assign a given TSD Id to the given list of RDMs (by Id).  This operation does not pull the RDMs back
     * into memory, it updates their state in persistence and fires appropriate notifications to registered listeners.
     *
     * @param tsdId  The TSD Id to assign to the RDMs, may be {@code null} to clear the active TSD assignment
     * @param rdmIds The list of RDM Ids indicating the RDMs to be updated
     * @since 1.48
     */
    void assignTSDtoRDMs(Integer tsdId, List<Integer> rdmIds);
}
