package gov.takx.api.persist;

import gov.takx.api.tracks.IConnectingLineDefinition;
import gov.takx.api.tracks.ITrackStyleDefinition;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.List;

/**
 */
public interface ISmartTrackPersist
{
    /**
     * Query for a specific track style definition
     *
     * @param id The database ID of the TSD
     * @return Track style definition with the given ID, or {@code null} if no match is found
     */
    @Nullable
    ITrackStyleDefinition getTrackStyleDefinition(int id);

    /**
     * Persist the given track style definition
     *
     * @param trackStyleDefinition track style definition
     */
    void persistTrackStyleDefinition(ITrackStyleDefinition trackStyleDefinition);

    /**
     * Query for all the track style definitions in the system.
     *
     * @return non-null list of track style definitions
     */
    @Nonnull
    List<ITrackStyleDefinition> queryAllTrackStyleDefinitions();

    /**
     * Query for all the track style definitions in a given family.
     *
     * @param family Family to filter on.
     * @return non-null list of matching track style definitions
     */
    @Nonnull
    List<ITrackStyleDefinition> queryTrackStylesFamily(@Nonnull String family);

    /**
     * Query for a specific track style definition
     *
     * @param uniqueKey Exact match (case-sensitive) unique key.
     * @return Track style definition with the given unique key, or {@code null} if no match is found
     */
    @Nullable
    ITrackStyleDefinition queryTrackStyleByUniqueKey(@Nonnull String uniqueKey);

    /**
     * Delete the specified track style definition from the database.
     *
     * @param trackStyleDefinition Track style definition to delete
     */
    void deleteTrackStyleDefinition(ITrackStyleDefinition trackStyleDefinition);

    /**
     * Persist the given connecting line definition.
     *
     * @param connectingLineDefinition Connecting line definition to persist
     */
    void persistConnectingLineDefinition(IConnectingLineDefinition connectingLineDefinition);

    /**
     * Query for all the connecting line definitions in the database.
     *
     * @return list of connecting line definitions
     */
    @Nonnull
    List<IConnectingLineDefinition> queryAllConnectingLineDefinitions();

    /**
     * Query for all the connecting line definitions in a given family.
     *
     * @param family Family to filter on.
     * @return non-null list of connecting line definitions in the given family
     */
    @Nonnull
    List<IConnectingLineDefinition> queryConnectingLinesByFamily(@Nonnull String family);

    /**
     * Query for a specific track style definition
     *
     * @param uniqueKey Exact match (case-sensitive) unique key.
     * @return Connecting line definition with the given unique key, or {@code null} if no match is found
     */
    @Nullable
    IConnectingLineDefinition queryConnectingLineByUniqueKey(@Nonnull String uniqueKey);

    /**
     * Delete the specified connecting line definition from the database.
     *
     * @param connectingLineDefinition Connecting line definition to delete
     */
    void deleteConnectingLineDefinition(IConnectingLineDefinition connectingLineDefinition);

    /**
     * Set the permanent TSD on the given RDM.  If you are updating a large number of RDMs, you should
     * consider using {@link #batchUpdateRDMTSD(Integer, java.util.Collection)} instead.  It will provide
     * far superior performance.
     *
     * @param rdmId Id of RDM to update
     * @param tsdId Id of TSD to assign to the RDM
     * @since 1.25
     */
    void updateRDMTSD(int rdmId, @Nullable Integer tsdId);

    /**
     * Assign the given TSD Id to a batch of RDMs, indicated by Id.  This operation is carried out
     * directly on the current thread.  You should not call it from threads like the EDT.
     *
     * @param tsdId  The TSD Id to assign, use {@code null} to clear any previous assignment
     * @param rdmIds The Ids of the RDMs to operate upon
     * @since 1.48
     */
    void batchUpdateRDMTSD(@Nullable Integer tsdId, @Nonnull Collection<Integer> rdmIds);

    /**
     * Set the override TSD on the given RDM.
     *
     * @param rdmId Id of RDM to update
     * @param tsdId Id of TSD to assign to the RDM
     * @since 1.25
     */
    void updateOverrideRDMTSD(int rdmId, @Nullable Integer tsdId);

    /**
     * Get the current, active TSD assigned to the RDM with the given Id.  If there is an override TSD assigned,
     * then that will be returned.  If not, and a permanent TSD is assigned, then that will be returned.  If
     * neither are assigned, then {@code null} is returned.
     *
     * @param rdmId Id of the RDM to query against
     * @return TSD in effect for the RDM, {@code null} if none
     */
    @Nullable
    ITrackStyleDefinition getActiveTSDforRDM(@Nullable Integer rdmId);

    /**
     * Get the (shared instance) TrackStyleDefinition with the given Id, if any.
     *
     * @param tsdId ID of the TSD to retrieve
     * @return The TSD, or {@code null} if not found
     */
    @Nullable
    ITrackStyleDefinition getSharedTrackStyleDefinition(@Nullable Integer tsdId);

    /**
     * Query for the unique pairs of MO Id and RDM TSD Ids across all RDM-level TSD assignments.  Teh return value
     * is a list of tuples, MO ID and RDM TSD ID.
     *
     * @return list of tuples, MO ID and RDM TSD ID
     */
    List<Object[]> queryRDMTSDMapObjectIds();

    /**
     * Query the TSD assignment data for the given list of RDM Id.  The return data consists of the RDM Id,
     * the Map Object Id (the owner of the RDM), the current TSD Id (which may be {@code null}),
     * and override TSD Id (which may be {@code null}).
     *
     * @param rdmIds List of RDM Ids of interest
     * @return tuple: {RDM Id, Map Object Id, TSD Id, Override TSD Id}
     */
    List<Object[]> queryRDMTSDAssignments(@Nonnull List<Integer> rdmIds);
}
