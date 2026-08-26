package gov.takx.dataservices.shapes;

import gov.nasa.worldwind.geom.Box;
import gov.nasa.worldwind.geom.Cylinder;
import gov.nasa.worldwind.geom.Extent;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.platform.utils.WWUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class to calculate extents for various shapes.
 *
 * @since 5.5
 */
class ExtentFactory
{
    /**
     * Calculate an extent for a circular shape.
     * @param globe the globe
     * @param center the center of the circle
     * @param radius the radius of the circle
     * @return the extent
     */
    public static Extent createExtent(Globe globe, ILocation center, double radius)
    {
        Sector[] sectors = Sector.splitBoundingSectors(globe, WWUtils.locationToLatLon(center), radius);
        if (sectors.length == 1)
        {
            return Cylinder.computeVerticalBoundingCylinder(globe, 0, sectors[0]);
        }
        return computeExtent(globe, sectors);
    }

    /**
     * Calculate an extent for a set of locations.
     * @param globe the globe
     * @param locations the locations
     * @return the extent
     */
    @Nullable
    public static Extent createExtent(Globe globe, List<ILocation> locations)
    {
        List<LatLon> positions = locations.stream().map(WWUtils::locationToLatLon).toList();
        Sector[] sectors;
        if (LatLon.locationsCrossDateLine(positions))
        {
            sectors = Sector.splitBoundingSectors(positions);
        } else
        {
            Sector sector = Sector.boundingSector(positions);
            sectors = (sector != null && !sector.equals(Sector.EMPTY_SECTOR)) ? new Sector[]{sector} : null;
        }
        return computeExtent(globe, sectors);
    }

    @Nullable
    private static Extent computeExtent(@Nonnull Globe globe, @Nullable Sector[] sectors)
    {
        if (sectors == null || sectors.length == 0)
        {
            return null;
        }
        // This surface shape does not cross the international dateline, and therefore has a single bounding sector.
        // Return the box which contains that sector.
        else if (sectors.length == 1)
        {
            return Sector.computeBoundingBox(globe, 0, sectors[0]);
        }
        // This surface crosses the international dateline, and its bounding sectors are split along the dateline.
        // Return a box which contains the corners of the boxes bounding each sector.
        else
        {
            ArrayList<Vec4> boxCorners = new ArrayList<>();

            for (Sector s : sectors)
            {
                Box box = Sector.computeBoundingBox(globe, 0, s);
                boxCorners.addAll(Arrays.asList(box.getCorners()));
            }

            return Box.computeBoundingBox(boxCorners);
        }
    }
}
