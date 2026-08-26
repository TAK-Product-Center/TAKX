package gov.takx.core.persist.breadcrumb;

import gov.takx.api.breadcrumb.ITrack;
import gov.takx.api.persist.IPersistService;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.api.query.param.HQLParameter;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

/**
 * Persistence helpers for {@link ITrack tracks}.
 *
 * @since 5.3
 */
@ApplicationScoped
public class TrackPersist
{
    private static final String TRACK_TABLE = Track.class.getSimpleName();
    private static final String TRACKS_BY_MARKER_UID_QUERY = "FROM " + TRACK_TABLE + " WHERE markerUid = :markerUid";
    private static final String COUNT_QUERY = "WHERE markerUid = :markerUid";

    private final IPersistServiceProvider persistServiceProvider;
    private final BreadcrumbPersist breadcrumbPersist;

    @Inject
    TrackPersist(IPersistServiceProvider persistServiceProvider, BreadcrumbPersist breadcrumbPersist)
    {
        this.persistServiceProvider = persistServiceProvider;
        this.breadcrumbPersist = breadcrumbPersist;
    }

    /**
     * @param track The track to persist
     */
    public void persist(@Nonnull ITrack track)
    {
        getPersistService().persistEntity(track);
    }

    /**
     * @return All persisted tracks
     */
    public List<ITrack> getAllTracks()
    {
        return getPersistService().query("FROM " + TRACK_TABLE);
    }

    /**
     * Returns a {@link gov.tak.api.maps.Marker marker's} persisted tracks, limited to tracks with start and end times
     * that fall between {@code minStartTime} and {@code maxEndTime}.
     *
     * @param markerUid The {@link gov.tak.api.maps.Marker marker's} UID
     * @param minStartTime The min start time to include in the results, or 0 if unbounded
     * @param maxEndTime The max end time to include in the results, or 0 if unbounded
     * @return The list of tracks that fall within the specified time boundaries
     */
    public List<ITrack> getTracks(@Nonnull String markerUid, long minStartTime, long maxEndTime)
    {
        String query = TRACKS_BY_MARKER_UID_QUERY;
        final List<HQLParameter> parameters = new ArrayList<>();

        parameters.add(new HQLParameter("markerUid", markerUid));

        if (minStartTime > 0)
        {
            query += " AND startTime >= :startTime";
            parameters.add(new HQLParameter("startTime", minStartTime));
        }

        if (maxEndTime > 0)
        {
            query += "AND endTime <= :endTime";
            parameters.add(new HQLParameter("endTime", maxEndTime));
        }

        return getPersistService().query(query, parameters.toArray(new HQLParameter[0]));
    }

    /**
     * @param track The track to delete; the tracks {@link gov.takx.api.breadcrumb.IBreadcrumb breadcrumbs} will also be deleted.
     */
    public void delete(@Nonnull ITrack track)
    {
        breadcrumbPersist.deleteTrack(track.getUid());

        getPersistService().deleteEntity(track);
    }

    /**
     * Deletes all {@link ITrack tracks} associated with the specified {@link gov.tak.api.maps.Marker}.
     * 
     * @param markerUid The {@link gov.tak.api.maps.Marker marker's} UID
     * @see BreadcrumbPersist#deleteTrack(String)
     */
    public void delete(@Nonnull String markerUid)
    {
        // Apparently, the query to pull in Track instances is complex, and even if there are no actual instances to
        // reify, it takes a fair amount of CPU time to process it.  So it is faster (by about 5 times) to do a
        // quick count check before doing the actual query to pull them in.
        final long count = getPersistService().countSQL(TRACK_TABLE, COUNT_QUERY, new HQLParameter("markerUid", markerUid));
        if (count > 0) getTracks(markerUid, 0, 0).forEach(this::delete);
    }

    private IPersistService getPersistService()
    {
        return persistServiceProvider.getPersistService();
    }
}
