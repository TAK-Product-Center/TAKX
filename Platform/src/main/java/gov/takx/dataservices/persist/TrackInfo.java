package gov.takx.dataservices.persist;

import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.ITrackInfo;
import gov.takx.api.messages.IRaptorDataMessage;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;

/**
 * Stores track "metadata" for an instance of an {@link IMapObject}, including the track count
 * and the geographic and time boundaries for those tracks.
 *
 * @since 1.13
 */
@Embeddable
public class TrackInfo implements ITrackInfo
{
    /**
     * The total number of tracks associated with this ITrackInfo's {@link IMapObject}.
     */
    private int numberOfTracks;

    /**
     * The date (relevant time) of the oldest track associated with this ITrackInfo's
     * {@link IMapObject}.
     */
    private long trackStartDate = Long.MAX_VALUE;

    /**
     * The date (relevant time) of the newest track associated with this ITrackInfo's
     * {@link IMapObject}.
     */
    private long trackEndDate = Long.MIN_VALUE;

    /**
     * The corners of the {@link Sector} defined in an array of degrees. The {@link Sector}
     * can be recomputed using {@link Sector#fromDegrees(double[])}.
     */
    private double[] corners;

    /**
     * The geographic boundaries of the tracks associated with this ITrackInfo's
     * {@link IMapObject}. The {@link Sector} object is
     * maintained to perform calculations (contains, union, etc.) but its data is
     * stored in the database in the form of {@link #corners}.
     */
    @Transient
    private Sector trackExtents;

    protected TrackInfo()
    {
    }

    @Override
    public int getNumberOfTracks()
    {
        return numberOfTracks;
    }

    @Override
    public long getTrackStartDate()
    {
        return trackStartDate;
    }

    @Override
    public long getTrackEndDate()
    {
        return trackEndDate;
    }

    @Override
    public Sector getTrackExtents()
    {
        // Check to see if the Sector needs to be updated with
        // data restored from the database.
        if (corners != null && trackExtents == null)
        {
            trackExtents = Sector.fromDegrees(corners);
        }

        return trackExtents;
    }

    @Override
    public void updateForRdm(IRaptorDataMessage rdm)
    {
        numberOfTracks += 1;

        final long time = rdm.getRelevantTime();
        if (time < trackStartDate) trackStartDate = time;
        if (time > trackEndDate) trackEndDate = time;

        if (rdm.hasLocation())
        {
            updateExtents(rdm.getLatitude(), rdm.getLongitude());
        }
    }

    /**
     * Update the track extents with a given location.  If the given location is outside of the current extent,
     * then the extent will be expanded to include the given location.
     *
     * @param latitude  latitude of location to add
     * @param longitude longitude of location to add
     * @since 3.0
     */
    private void updateExtents(double latitude, double longitude)
    {
        final LatLon latLon = LatLon.fromDegrees(latitude, longitude);
        final Sector sector = getTrackExtents();

        // No sector created yet: create a bounding sector from the given location
        if (sector == null)
        {
            trackExtents = Sector.boundingSector(latLon, latLon);
        } else if (!sector.containsDegrees(latitude, longitude))
        {
            // Sector exists but does not contain the location: union the location with the current sector.
            trackExtents = sector.union(latLon.latitude, latLon.longitude);
        }

        corners = trackExtents.asDegreesArray(); // And update our persistent representation of the sector
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName() + ": num tracks=" + numberOfTracks;
    }
}
