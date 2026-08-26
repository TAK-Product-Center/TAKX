package gov.takx.core.view.gui.maps.exporters.dataset;

import gov.nasa.worldwind.geom.Sector;

/**
 * Utilities methods to normalize a sector.
 *
 * @since 1.43
 */
public class SectorNormalizer
{
    /**
     * Given a sector, perform normalization on it (if required).
     *
     * @param sector
     * @return a normalized sector.
     */
    public static Sector normalize(Sector sector)
    {

        Sector rv = null == sector ? Sector.EMPTY_SECTOR : sector;

        if (!isNormal(sector))
        {
            double minLat = Math.min(sector.getMinLatitude().degrees, sector.getMaxLatitude().degrees);
            double maxLat = Math.max(sector.getMinLatitude().degrees, sector.getMaxLatitude().degrees);
            double minLon = Math.min(sector.getMinLongitude().degrees, sector.getMaxLongitude().degrees);
            double maxLon = Math.max(sector.getMinLongitude().degrees, sector.getMaxLongitude().degrees);

            rv = Sector.fromDegrees(minLat, maxLat, minLon, maxLon);
        }

        return rv;
    }

    /**
     * Given a sector, compute if it is a properly normal sector.
     * A sector is normal if the min/max values are appropriately related
     * and the deltas are positive.
     *
     * @param sector
     * @return true if normal.
     */
    public static boolean isNormal(Sector sector)
    {
        return sector.getMinLatitude().degrees <= sector.getMaxLatitude().degrees &&
                sector.getMinLongitude().degrees <= sector.getMaxLongitude().degrees &&
                sector.getDeltaLatDegrees() >= 0 &&
                sector.getDeltaLonDegrees() >= 0;
    }
}
