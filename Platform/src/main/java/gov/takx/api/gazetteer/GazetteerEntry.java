package gov.takx.api.gazetteer;

import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import jakarta.annotation.Nonnull;

/**
 * A single entry from the gazetteer database.
 *
 * @see IGazetteerService#search(String)
 * @since 3.0
 */
@SuppressWarnings("ClassCanBeRecord")
public class GazetteerEntry
{
    private final String name;
    private final double latitude;
    private final double longitude;

    public GazetteerEntry(@Nonnull String name, double latitude, double longitude)
    {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * @return The full name from the gazetteer entry
     */
    @Nonnull
    public String getName()
    {
        return name;
    }

    /**
     * @return Latitude from the gazetteer entry
     */
    public double getLatitude()
    {
        return latitude;
    }

    /**
     * @return Longitude from the gazetteer entry
     */
    public double getLongitude()
    {
        return longitude;
    }

    /**
     * @return Location constructed from the latitude and longitude from the gazetteer entry
     */
    public ILocation getLocation()
    {
        return new RaptorLocation(latitude, longitude);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GazetteerEntry that = (GazetteerEntry) o;
        return Double.compare(latitude, that.latitude) == 0 && Double.compare(longitude, that.longitude) == 0 && name.equals(that.name);
    }

    @Override
    public int hashCode()
    {
        int result = name.hashCode();
        result = 31 * result + Double.hashCode(latitude);
        result = 31 * result + Double.hashCode(longitude);
        return result;
    }
}
