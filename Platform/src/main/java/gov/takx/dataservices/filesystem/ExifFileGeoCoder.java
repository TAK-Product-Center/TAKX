package gov.takx.dataservices.filesystem;

import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.lang.Rational;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.GpsDirectory;
import gov.nasa.worldwind.geom.Angle;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;

/**
 * Geocoder to handle files with GPS data embedded in the EXIF portion of image. This geocoder pulls out latitude,
 * longitude, and elevation, but ignores other GPS fields.
 * <p>
 * See http://www.exif.org for specification.
 *
 */
public class ExifFileGeoCoder extends AFileGeoCoder
{
    private static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public ILocation geoDecode(File file)
    {
        try
        {
            Metadata metadata = JpegMetadataReader.readMetadata(file);

            Directory gpsDir = metadata.getFirstDirectoryOfType(GpsDirectory.class);
            Angle lat = null, lon = null;
            Rational[] ary;
            String ref;
            double degrees, elevation = 0.0;
            boolean hasElevation = false;
            int sign;

            if (gpsDir.containsTag(GpsDirectory.TAG_LATITUDE))
            {
                ary = gpsDir.getRationalArray(GpsDirectory.TAG_LATITUDE);
                ref = gpsDir.getString(GpsDirectory.TAG_LATITUDE_REF);
                sign = ("N".equals(ref)) ? 1 : -1;
                degrees = (ary[0].doubleValue() + (ary[1].doubleValue() / 60) + (ary[2].doubleValue() / 3600)) * sign;
                lat = Angle.fromDegrees(degrees);
            }

            if (gpsDir.containsTag(GpsDirectory.TAG_LONGITUDE))
            {
                ary = gpsDir.getRationalArray(GpsDirectory.TAG_LONGITUDE);
                ref = gpsDir.getString(GpsDirectory.TAG_LONGITUDE_REF);
                sign = ("E".equals(ref)) ? 1 : -1;
                degrees = (ary[0].doubleValue() + (ary[1].doubleValue() / 60) + (ary[2].doubleValue() / 3600)) * sign;
                lon = Angle.fromDegrees(degrees);
            }

            if (gpsDir.containsTag(GpsDirectory.TAG_ALTITUDE))
            {
                Rational el = gpsDir.getRational(GpsDirectory.TAG_ALTITUDE);
                int ref_ = gpsDir.getInt(GpsDirectory.TAG_ALTITUDE_REF);
                sign = (ref_ == 0) ? 1 : -1;
                elevation = el.doubleValue() * sign;
                hasElevation = true;
            }

            logger.debug("lat = {}, lon = {}", lat, lon);

            if (lat != null && lon != null)
            {
                ILocation location;
                if (hasElevation)
                {
                    location = new RaptorLocation(lat.getDegrees(), lon.getDegrees());
                } else
                {
                    location = new RaptorLocation(lat.getDegrees(), lon.getDegrees(), elevation);
                }

                return location;
            }
        } catch (Exception e)
        {
            logger.debug("Can't geocode file: ", e);
        }
        return null;
    }
}
