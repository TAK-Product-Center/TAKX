package gov.takx.core.globe.tak.elevation;

import com.atakmap.coremap.maps.coords.Ellipsoid;
import com.atakmap.map.elevation.ElevationSource;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.LevelSet;
import gov.takx.commons.file.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * An implementation of {@link gov.nasa.worldwind.globes.ElevationModel} that queries a DTED {@link ElevationSource} for elevation data.
 *
 * @since 5.3
 */
public class DtedElevationModel extends ATakElevationModel
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // Represent a reasonably small grid of 1000x1000 tiles.
    private static final int TILE_GRID_SIZE = 1000;

    private final File dtedPath;
    private final String elevationSetName;

    Sector sector = Sector.FULL_SPHERE;
    double resolution = Double.MAX_VALUE;
    LevelSet levelSet;

    public DtedElevationModel(ElevationSource elevationSource, File dtedPath, String elevationSetName, String displayName)
    {
        super(elevationSource, displayName);
        this.dtedPath = dtedPath;
        this.elevationSetName = elevationSetName;
        inspectElevationSource();
    }

    private void inspectElevationSource()
    {
        walkDtedDirectory();

        AVList avList = new AVListImpl();
        avList.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, new LatLon(sector.getDeltaLat(), sector.getDeltaLon()));
        avList.setValue(AVKey.SECTOR, sector);
        avList.setValue(AVKey.NUM_LEVELS, 1);
        avList.setValue(AVKey.TILE_WIDTH, TILE_GRID_SIZE);
        avList.setValue(AVKey.TILE_HEIGHT, TILE_GRID_SIZE);
        avList.setValue(AVKey.FORMAT_SUFFIX, ".bil");
        avList.setValue(AVKey.DATA_CACHE_NAME, elevationSetName);
        avList.setValue(AVKey.DATASET_NAME, elevationSetName);
        avList.setValue(AVKey.DISPLAY_NAME, getName());
        levelSet = new LevelSet(avList);
    }

    @Override
    public double getBestResolution(Sector sector)
    {
        return resolution;
    }

    @Override
    public LevelSet getLevels()
    {
        return levelSet;
    }

    @Override
    public Sector getSector()
    {
        return sector;
    }

    private void setSector(Sector sector)
    {
        this.sector = sector;
        ((AVList) getValue(AVKey.CONSTRUCTION_PARAMETERS)).setValue(AVKey.SECTOR, sector);
    }

    /**
     * Iterate the file path containing the source DTED data and parse the pathing structure to discover the bounding box and resolution.
     * The DtedElevationModel will then reflect the discovered attributes.
     */
    private void walkDtedDirectory()
    {
        int minLat = Integer.MAX_VALUE;
        int maxLat = -Integer.MAX_VALUE;
        int minLon = Integer.MAX_VALUE;
        int maxLon = -Integer.MAX_VALUE;
        double resolution = Double.MAX_VALUE;
        try (var stream = Files.walk(dtedPath.toPath()))
        {
            List<File> dtedFiles = stream.map(Path::toFile).toList();
            for (File dtedFile : dtedFiles)
            {
                // DTED filename spec: http://www.pancroma.com/downloads/MIL-PDF-89020B.pdf
                // page 12, 3.10.7.2 Directories defines the structure and naming, page 13 has an image depicting it
                String fileName = dtedFile.isDirectory() ? dtedFile.getName() : FileUtils.getNameWithoutExtension(dtedFile);
                char direction = Character.toLowerCase(fileName.charAt(0));
                int value;
                try
                {
                    value = Integer.parseInt(fileName.substring(1));
                } catch (NumberFormatException ignored)
                {
                    continue;
                }
                if ('w' == direction || 's' == direction) value *= -1;
                if ('w' == direction || 'e' == direction)
                {
                    minLon = Math.min(value, minLon);
                    maxLon = Math.max(value + 1, maxLon); // DTED files contain a 1x1 degree of lat/long data starting in SW corner
                } else
                {
                    minLat = Math.min(value, minLat);
                    maxLat = Math.max(value + 1, maxLat);
                }

                String extension = FileUtils.getExtension(dtedFile);
                resolution = resolution == Double.MAX_VALUE ? getResolutionForDtedExtension(extension != null ? extension : "") : resolution;
            }
        } catch (IOException e)
        {
            logger.error("Error walking dted directory.", e);
        }
        if (resolution != Double.MAX_VALUE)
        {
            this.resolution = resolution / Ellipsoid.WGS_84.getSemiMajorAxis();
            setSector(new Sector(Angle.fromDegrees(minLat), Angle.fromDegrees(maxLat), Angle.fromDegrees(minLon), Angle.fromDegrees(maxLon)));
        }
    }

    private double getResolutionForDtedExtension(String extention)
    {
        return switch (extention.toLowerCase())
        {
            case "dt0" -> 900;
            case "dt1" -> 90;
            case "dt2" -> 30;
            case "dt3" -> 10;
            default -> Double.MAX_VALUE;
        };
    }
}
