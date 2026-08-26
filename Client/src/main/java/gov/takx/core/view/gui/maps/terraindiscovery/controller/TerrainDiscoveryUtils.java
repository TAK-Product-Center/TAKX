package gov.takx.core.view.gui.maps.terraindiscovery.controller;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globes.ElevationModel;
import gov.nasa.worldwind.util.WWMath;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.shapes.IRectangle;
import gov.takx.api.terrain.experimental.ITakElevationModel;
import gov.takx.api.terrain.experimental.TerrainGrid;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 */
public class TerrainDiscoveryUtils
{
    public static final float HUE_BLUE = getHue(Color.BLUE);
    public static final float HUE_RED = getHue(Color.RED);

    public static LatLon toLatLon(ILocation location)
    {
        return LatLon.fromDegrees(location.getLatitude(), location.getLongitude());
    }

    public static Sector rectangleToSector(IRectangle rectangle)
    {
        if (rectangle == null)
        {
            return null;
        }

        ILocation minCorner = rectangle.getMinCorner();
        ILocation maxCorner = rectangle.getMaxCorner();
        if (minCorner == null || maxCorner == null)
        {
            return null;
        }

        return Sector.boundingSector(toLatLon(minCorner), toLatLon(maxCorner));
    }

    public static boolean contains(IRectangle rectangle, double latitude, double longitude)
    {
        if (rectangle == null)
        {
            return false;
        }

        Sector s = rectangleToSector(rectangle);
        return s != null && s.containsDegrees(latitude, longitude);
    }

    public static String getResolutionString(double[] resolutions, double multiplier)
    {
        if (resolutions.length == 1)
        {
            return String.format("%.2f m", resolutions[0] * multiplier);
        } else
        {
            double min = resolutions[0] * multiplier;
            double max = resolutions[resolutions.length - 1] * multiplier;

            return String.format("%.2f m - %.2f m", min, max);
        }
    }

    public static double[] getResolutions(WorldWindow wwd, ITakElevationModel model, IRectangle region)
    {
        if (region == null)
        {
            return new double[]{model.getBestResolution(null)};
        }

        double width = region.getWidth();
        double length = region.getHeight();

        double sampleDistance = Math.max(Math.sqrt(width * length / 1000), 1.0);

        double[] resolutions = getResolutions(model, makeTerrainGrid(wwd, region, sampleDistance));
        if (resolutions.length >= 1)
        {
            return resolutions;
        }
        return new double[]{model.getBestResolution(null)};
    }

    public static double[] getResolutions(ITakElevationModel model, TerrainGrid gridData)
    {
        double[] minMaxLatLon = getMinMaxLatLon(gridData);
        return getResolutions(
                gridData,
                List.of(model),
                minMaxLatLon[0],
                minMaxLatLon[1],
                minMaxLatLon[2],
                minMaxLatLon[3]);
    }

    public static double[] getResolutions(TerrainGrid gridData,
                                          List<ITakElevationModel> selectedModels,
                                          double minLatitude,
                                          double minLongitude,
                                          double maxLatitude,
                                          double maxLongitude)
    {
        double[][] resolutionsMatrix = getResolutionsMatrix(
                gridData,
                selectedModels,
                minLatitude,
                minLongitude,
                maxLatitude,
                maxLongitude);
        return matrixToSortedArray(resolutionsMatrix);
    }

    private static double[] getMinMaxLatLon(TerrainGrid gridData)
    {
        ILocation corner1 = gridData.getMinLocation();
        ILocation corner2 = gridData.getMaxLocation();

        double lat1 = corner1.getLatitude();
        double lat2 = corner2.getLatitude();
        double lon1 = corner1.getLongitude();
        double lon2 = corner2.getLongitude();

        double minLatitude = Math.min(lat1, lat2);
        double minLongitude = Math.min(lon1, lon2);
        double maxLatitude = Math.max(lat1, lat2);
        double maxLongitude = Math.max(lon1, lon2);

        return new double[]{minLatitude, minLongitude, maxLatitude, maxLongitude};
    }

    public static TerrainGrid makeTerrainGrid(WorldWindow wwd, IRectangle region, Double stepSize)
    {
        final ILocation corner1 = region.getMinCorner();
        final ILocation corner2 = region.getMaxCorner();
        return new TerrainGrid(wwd.getModel().getGlobe(), corner1, corner2, stepSize);
    }

    public static double[] matrixToSortedArray(double[][] dataMatrix)
    {
        // Keep track of the different resolutions in the model. Each distinct resolution will get a contour line
        // in the elevation bounds visualization.
        Set<Double> resolutions = new HashSet<>();
        for (double[] row : dataMatrix)
        {
            for (double d : row)
            {
                resolutions.add(d);
            }
        }

        // Convert the resolution set to an array, and sort the array. The result is a sorted array of thresholds
        // to use to create resolution contour lines.
        // PLUGIN-2112:
        //
        // Fixing potential source of terrain discovery bugs with invalid resolutions
        List<Double> resolutionList = new ArrayList<>();
        for (Double d : resolutions)
        {
            if (d != null && !Double.isInfinite(d) && !Double.isNaN(d) && d != Double.MAX_VALUE)
            {
                resolutionList.add(d);
            }
        }
        Collections.sort(resolutionList);
        double[] resolutionAry = new double[resolutionList.size()];
        for (int i = 0; i < resolutionAry.length; i++)
        {
            resolutionAry[i] = resolutionList.get(i);
        }
        return resolutionAry;
    }

    public static double[][] getResolutionsMatrix(TerrainGrid gridData,
                                                  Iterable<ITakElevationModel> selectedModels,
                                                  double minLatitude,
                                                  double minLongitude,
                                                  double maxLatitude,
                                                  double maxLongitude)
    {
        final int gridPointsX = gridData.getNumberOfGridPointsX();
        final int gridPointsY = gridData.getNumberOfGridPointsY();

        // Create a matrix to hold the elevation resolutions at each grid point.
        double[][] dataMatrix = new double[gridPointsY][gridPointsX];

        double lonStep = Math.abs((maxLongitude - minLongitude) / gridPointsX);
        double latStep = Math.abs((maxLatitude - minLatitude) / gridPointsY);

        // Implementation note: this loop is similar to the one in createGridValues. The loops could be combined, but
        // finding the elevation bounds is much faster than determining local terrain availability. Having the operations
        // separate gives the user feedback on elevation bounds before the local terrain data is available.
        for (int j = gridPointsY - 1; j >= 0; j--)
        {
            for (int i = 0; i < gridPointsX; i++)
            {
                // Determine the center point of the grid cell
                double lon = gridData.getLonFromX(gridData.getWidthOfGrid() * i / gridPointsX);
                double lat = gridData.getLatFromY(gridData.getHeightOfGrid() * j / gridPointsY);

                Sector sector = getSubSector(lat, lon, latStep, lonStep);

                // Determine the terrain resolution for the cell
                // todo: if best resolution is unavailable, it MAY be possible that getBestResolution will
                // return Double.MAX_VALUE, which when multiplied with globeRadius, will become a NaN.
                double res = getBestResolution(selectedModels, sector);
                dataMatrix[j][i] = res;
            }
        }
        return dataMatrix;
    }

    /**
     * Get a subsection of the region of interest.
     *
     * @param lat     center latitude
     * @param lon     center longitude
     * @param latStep latitude step size
     * @param lonStep longitude step size
     * @return the Sector subsection
     */
    public static Sector getSubSector(double lat, double lon, double latStep, double lonStep)
    {
        // Find the min and max corners of the cell
        double minLon = lon - lonStep / 2;
        double maxLon = lon + lonStep / 2;

        double minLat = lat - latStep / 2;
        double maxLat = lat + latStep / 2;

        // Compute the sector that covers this grid cell
        return Sector.fromDegrees(minLat, maxLat, minLon, maxLon);
    }

    /**
     * Determine the best possible resolution of a list of elevation models, within a particular sector.  This
     * resolution value does not necessarily represent the data that is currently available on disk or in memory.  It
     * only specifies the best possible resolution of the models assuming no data is missing.  Smaller values indicate
     * higher resolutions.
     *
     * @param elevationModels list of elevation models to search for best possible resolution.
     * @param sector          the sector of interest.
     * @return the best possible resolution in the list of models and sector.
     */
    public static double getBestResolution(Iterable<ITakElevationModel> elevationModels, Sector sector)
    {
        double res = Double.MAX_VALUE;

        for (ElevationModel em : elevationModels)
        {
            if (em == null || !em.isEnabled())
            {
                continue;
            }

            if (sector != null && em.intersects(sector) < 0) // sector does not intersect elevation model
            {
                continue;
            }

            double r = em.getBestResolution(sector);
            if (r < res)
            {
                res = r;
            }
        }
        return res;
    }

    public static float getHue(Color color)
    {
        return Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null)[0];
    }

    public static Color hueColorFromRange(double value, double minValue, double maxValue, double minHue, double maxHue)
    {
        double ratio = (value - minValue) / (maxValue - minValue);
        float hue = (float) WWMath.mixSmooth(ratio, minHue, maxHue);
        if (hue < 0)
        {
            hue = 0;
        }
        if (hue > 1)
        {
            hue = 1;
        }
        return new Color(Color.HSBtoRGB(hue, 1, 1));
    }

    public static BufferedImage createResolutionColorLegend(String title, Map<Double, Color> resolutionColorMap)
    {
        if (resolutionColorMap == null || resolutionColorMap.isEmpty())
        {
            return null;
        }

        // Create image and prepare graphics
        BufferedImage image = new BufferedImage(400, 600, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();

        // Set up fonts
        Font basicFont = g.getFont();
        basicFont = basicFont.deriveFont(Font.BOLD, basicFont.getSize() + 3f);
        Font largeFont = basicFont.deriveFont(Font.BOLD, basicFont.getSize() + 3f);
        int titleHeight = g.getFontMetrics(largeFont).getHeight() + 2;
        int resolutionHeight = g.getFontMetrics(basicFont).getHeight() + 2;

        // Create a sorted ascending list of resolutions
        List<Double> resolutions = new ArrayList<>(resolutionColorMap.keySet());
        Collections.sort(resolutions);

        // Start drawing...
        final int margin = 4;
        int yOffset = margin + titleHeight;

        // Draw title
        drawString(title, g, margin, yOffset, largeFont, Color.BLACK, Color.WHITE);

        yOffset += 2 * margin;

        // Draw resolution color rectangles and resolution strings
        for (Double resolution : resolutions)
        {
            yOffset += resolutionHeight + margin;
            drawResolution(resolution,
                    resolutionColorMap.get(resolution),
                    resolutionHeight,
                    g,
                    margin,
                    margin,
                    yOffset,
                    basicFont,
                    Color.BLACK, Color.WHITE);
        }

        int newY = yOffset + (2 * margin) + resolutionHeight + 4;

        BufferedImage cropped = new BufferedImage(400, newY, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = cropped.createGraphics();
        g2.drawImage(image, 0, 0, null);
        return cropped;
    }

    private static void drawString(String s,
                                   Graphics2D g,
                                   int x,
                                   int y,
                                   Font f,
                                   Color outlineColor,
                                   Color interiorColor)
    {
        g.setFont(f);

        // draw 1 pixel outline boundary
        g.setColor(outlineColor);
        g.drawString(s, x, y);
        g.drawString(s, x + 1, y);
        g.drawString(s, x + 2, y);
        g.drawString(s, x, y + 1);
        g.drawString(s, x + 2, y + 1);
        g.drawString(s, x, y + 2);
        g.drawString(s, x + 1, y + 2);
        g.drawString(s, x + 2, y + 2);

        // draw string inside boundary
        g.setColor(interiorColor);
        g.drawString(s, x + 1, y + 1);
    }

    private static void drawResolution(double resolution,
                                       Color resolutionColor,
                                       int resolutionHeight,
                                       Graphics2D g,
                                       int margin,
                                       int x,
                                       int y,
                                       Font f,
                                       Color outlineColor,
                                       Color interiorColor)
    {
        g.setColor(resolutionColor);
        int rectWidth = resolutionHeight * 2;
        g.fillRect(x, y + margin - resolutionHeight, rectWidth, resolutionHeight);
        g.setColor(resolutionColor.darker().darker());
        g.drawRect(x, y + margin - resolutionHeight, rectWidth, resolutionHeight);
        x += rectWidth + margin;
        drawString(String.format("%.2f m", resolution), g, x, y, f, outlineColor, interiorColor);
    }
}
