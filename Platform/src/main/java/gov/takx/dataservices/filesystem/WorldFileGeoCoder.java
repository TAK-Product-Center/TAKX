package gov.takx.dataservices.filesystem;

import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.formats.worldfile.WorldFile;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.invoke.MethodHandles;

/**
 * GeoCoder that attempts to read geo-coordinates from a World File. A World File is a text file in the same
 * directory as the image.
 *
 */
public class WorldFileGeoCoder extends AFileGeoCoder
{
    Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public ILocation geoDecode(File file)
    {
        try
        {
            Double lat, lon;

            File[] worldFiles = WorldFile.getWorldFiles(file);
            if (worldFiles != null)
            {
                AVList worldFileParams = WorldFile.decodeWorldFiles(worldFiles, null);

                lon = (Double) worldFileParams.getValue(WorldFile.WORLD_FILE_X_LOCATION);
                lat = (Double) worldFileParams.getValue(WorldFile.WORLD_FILE_Y_LOCATION);

                if (lat != null && lon != null)
                {
                    return new RaptorLocation(lat, lon);
                }
            }
        } catch (FileNotFoundException ignored)
        {
        } catch (Exception e)
        {
            logger.warn("Exception geodecoding image: {}", e.toString());
        }

        return null;
    }
}
