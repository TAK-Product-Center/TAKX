package gov.takx.api.terrain.experimental.proposed;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globes.ElevationModel;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.terrain.CompoundElevationModel;
import gov.nasa.worldwind.util.Tile;
import gov.nasa.worldwind.util.UnitsFormat;
import gov.nasa.worldwind.util.WWIO;
import gov.nasa.worldwind.util.WWMath;
import gov.tak.api.annotation.DeprecatedApi;
import gov.takx.api.annotation.Experimental;
import gov.takx.api.terrain.experimental.ITakElevationModel;
import gov.takx.api.terrain.experimental.proposed.tile.ElevationAttributes;
import gov.takx.api.terrain.experimental.proposed.tile.ElevationTile;
import gov.takx.api.terrain.experimental.proposed.tile.TileCollection;
import gov.takx.platform.utils.ILengthMeasurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 */
@Experimental
public class TerrainDataUtils
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    public static final float PI = (float) Math.PI;
    public static final float PI2 = PI * 2;
    public static final float PI_HALF = PI / 2;
    public static final float ALTITUDE_DEG = 45;
    public static final float AZIMUTH_DEG = 315;
    public static final float CELL_SIZE = 1;
    public static final float Z_FACTOR = 1;

    @Deprecated(since = "5.6", forRemoval = true)
    @DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
    public static final List<String> ELEVATION_FILE_EXTENSIONS = Arrays.asList(".bil", ".tif");
    public static final int MISSING_DATA_COLOR_VALUE = 0xFF000000; // black
    public static final int NOT_LOCAL_COLOR_VALUE = 0x00000000; // transparent

    // Each WW elevation model can have a unique Missing Data Signal.  Since we will be working with multiple elevation
    // models at the same time, replace each model's missing data values to our own (single) global missing data
    // signal.
    public static final float MISSING_DATA_SIGNAL = Float.POSITIVE_INFINITY;

    // A secondary signal value indicating that data is not available because it is not local.  If a region returns
    // out of bounds signals, it should mean that some necessary tiles may not be local.  If the missing tiles are
    // eventually retrieved, this value should no longer appear in that region.
    public static final float NOT_LOCAL_SIGNAL = Float.NEGATIVE_INFINITY;

    private static final long ROW_COL_MASK = 0x1FFFFFFFL;
    private static final long LEVEL_MASK = 0x3FL;
    private static final long COL_BIT_OFFSET = 29;
    private static final long LEVEL_BIT_OFFSET = 58;

    /**
     * @deprecated no replacement
     */
    @Deprecated(since = "5.6", forRemoval = true)
    @DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
    public static void readBIL(File bilFile,
                               String dataType,
                               String endianness,
                               float localMissingDataSignal,
                               float[] values)
    {
        ByteBuffer byteBuffer = readByteBuffer(bilFile, 5, 10);
        if (byteBuffer != null)
        {
            readBufferAsFloat(byteBuffer, dataType, endianness, localMissingDataSignal, values);
        } else
        {
            logger.warn("Unable to read BIL file: {}", bilFile.getName());
        }
    }

    /**
     * Attempt to read a file into a ByteBuffer.  If the file is unreadable (possibly due to the file being locked by
     * another thread), retry reading the file up to a give number of maximum retries.
     *
     * @param byteFile   file to read to a ByteBuffer.
     * @param maxRetries maximum number of retry attempts to read the file if it is locked.
     * @param sleepTime  time to sleep between retries in order to give other thread time to finish using the file.
     * @return a ByteBuffer of the contents of the file - unless the file was found to be unreadable after
     * (maxRetries + 1) attempts.
     * @deprecated no replacement
     */
    @Deprecated(since = "5.6", forRemoval = true)
    @DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
    public static ByteBuffer readByteBuffer(File byteFile, int maxRetries, long sleepTime)
    {
        // Prevent unusable retry values.
        if (maxRetries < 0)
        {
            maxRetries = 0;
        }

        // Maximum number of attempts to read the file is (retries + 1), since the first attempt is mandatory and not
        // considered to be a 'retry'.
        int maxAttempts = maxRetries + 1;

        // Attempt to read the file to a ByteBuffer.  If the file is locked, perhaps due to another thread using the
        // file, up to retries number of retry attempts will be made.
        for (int i = 0; i < maxAttempts; i++)
        {
            try
            {
                ByteBuffer byteBuffer = WWIO.readFileToBuffer(byteFile);
                if (logger.isTraceEnabled())
                {
                    logger.trace("Successfully read ByteBuffer for file: {} on attempt: {} of {}", byteFile.getName(), i, maxAttempts);
                }
                return byteBuffer;
            } catch (IOException e)
            {
                logger.warn("Exception reading ByteBuffer for file: {}.  Attempt {} of {}", byteFile.getName(), i, maxAttempts, e);
            }

            // If there are any retries left, go to sleep for a while to give other thread time to finish using this file.
            if (i < (maxAttempts - 1))
            {
                try
                {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e)
                {
                    logger.warn("Sleep interrupted waiting to read ByteBuffer for file: {} on attempt {} of {}", byteFile.getName(), i, maxAttempts);
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Read values from a ByteBuffer into a float array.  The ByteBuffer's data will be interpreted as short, int,
     * float or double depending on information provided in the parameters.
     *
     * @param byteBuffer             ByteBuffer of interest.
     * @param dataType               String specifying primitive used to interpret the buffer data.
     * @param endianness             String specifying big or little endianness.
     * @param localMissingDataSignal value to write into the float array for any values determined to be missing.
     * @param values                 float array to be filled with data.
     * @since 1.43
     * @deprecated no replacement
     */
    @Deprecated(since = "5.6", forRemoval = true)
    @DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
    public static void readBufferAsFloat(ByteBuffer byteBuffer,
                                         String dataType,
                                         String endianness,
                                         float localMissingDataSignal,
                                         float[] values)
    {
        try
        {
            byteBuffer = byteBuffer.order(endianness.equalsIgnoreCase("BigEndian")
                    ? ByteOrder.BIG_ENDIAN
                    : ByteOrder.LITTLE_ENDIAN);
            if (dataType.toLowerCase().endsWith("int8"))
            {
                readAsByteBuffer(byteBuffer, values, localMissingDataSignal);
            } else if (dataType.toLowerCase().endsWith("int16"))
            {
                readAsShortBuffer(byteBuffer, values, localMissingDataSignal);
            } else if (dataType.toLowerCase().endsWith("int32"))
            {
                readAsIntBuffer(byteBuffer, values, localMissingDataSignal);
            } else if (dataType.toLowerCase().endsWith("float32"))
            {
                readAsFloatBuffer(byteBuffer, values, localMissingDataSignal);
            } else if (dataType.toLowerCase().endsWith("float64"))
            {
                readAsDoubleBuffer(byteBuffer, values, localMissingDataSignal);
            } else
            {
                logger.warn("Unrecognized data type: {}", dataType);
            }
        } catch (Exception e)
        {
            logger.warn("Exception reading elevation tile", e);
        }
    }

    public static boolean isMissingDataSignal(float... values)
    {
        for (float f : values)
        {
            if (f == MISSING_DATA_SIGNAL)
            {
                return true;
            }
        }
        return false;
    }

    public static boolean isNotLocalSignal(float... values)
    {
        for (float f : values)
        {
            if (f == NOT_LOCAL_SIGNAL)
            {
                return true;
            }
        }
        return false;
    }

    public static boolean isMissingOrNotLocalSignal(float... values)
    {
        for (float f : values)
        {
            if (f == MISSING_DATA_SIGNAL || f == NOT_LOCAL_SIGNAL)
            {
                return true;
            }
        }
        return false;
    }

    public static float[] getMinMax(float[] values)
    {
        if (values == null)
        {
            logger.warn("Unable to get min/max: values are null");
            return null;
        }
        Float min = null;
        Float max = null;
        for (float f : values)
        {
            if (!isMissingOrNotLocalSignal(f))
            {
                if (min == null || min > f)
                {
                    min = f;
                }
                if (max == null || max < f)
                {
                    max = f;
                }
            }
        }

        if (min == null || max == null)
        {
            logger.debug("Unable to get min/max values from values");
            return null;
        }

        return new float[]{min, max};
    }

    public static double[] getSectorDimensionInMeters(Sector sector, ILengthMeasurer measurer)
    {
        if (measurer == null)
        {
            return null;
        }

        // From WW docs: Array of the four corner locations, in the order SW, SE, NE, NW
        LatLon[] corners = sector.getCorners();
        List<LatLon> widthPositions = Arrays.asList(corners[0], corners[1]);
        List<LatLon> heightPositions = Arrays.asList(corners[0], corners[3]);

        measurer.setMeasurementPositions(widthPositions, 0);
        double width = measurer.computeLength();

        measurer.setMeasurementPositions(heightPositions, 0);
        double height = measurer.computeLength();

        return new double[]{width, height};
    }

    public static List<TileCollection> createTileCollections(List<ITakElevationModel> elevationModels)
    {
        List<TileCollection> tileManagers = new ArrayList<>();
        for (ITakElevationModel model : elevationModels)
        {
            tileManagers.add(createTileCollection(model));
        }
        return tileManagers;
    }

    /**
     * @since 5.2
     */
    public static TileCollection createTileCollection(ITakElevationModel elevationModel)
    {
        return new TileCollection(new ElevationAttributes(elevationModel));
    }

    public static List<ElevationModel> getModels(Globe globe)
    {
        List<ElevationModel> models = new ArrayList<>();
        getModels(globe.getElevationModel(), models);
        return models;
    }

    public static void getModels(ElevationModel elevationModel, List<ElevationModel> models)
    {
        if (elevationModel instanceof CompoundElevationModel)
        {
            List<ElevationModel> cm = ((CompoundElevationModel) elevationModel).getElevationModels();
            for (ElevationModel m : cm)
            {
                getModels(m, models);
            }
        } else
        {
            models.add(elevationModel);
        }
    }

    /**
     * Get the expected path of a tile file.  This path does not include file extension.
     *
     * @since 1.43
     * @deprecated no replacement
     */
    @Deprecated(since = "5.6", forRemoval = true)
    @DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
    public static String getTilePathWithoutExtension(String dataSet, int row, int col, int level)
    {
        return dataSet + File.separator + getTileFilename(row, col, level);
    }

    /**
     * @deprecated no replacement
     */
    @Deprecated(since = "5.6", forRemoval = true)
    @DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
    public static String getTileFilename(int row, int col, int level)
    {
        return level + File.separator + row + File.separator + row + "_" + col;
    }

    /**
     * @deprecated no replacement
     */
    @Deprecated(since = "5.6", forRemoval = true)
    @DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
    public static String getRowPath(String dataSet, int row, int level)
    {
        return dataSet + File.separator + level + File.separator + row;
    }

    /**
     * @deprecated no replacement
     */
    @Deprecated(since = "5.6", forRemoval = true)
    @DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
    public static List<ElevationTile> getElevationTiles(Sector sector, ElevationAttributes attributes)
    {
        List<ElevationTile> elevationTiles = new ArrayList<>();
        for (int h = 0; h < attributes.getHeight(); h++)
        {
            for (int w = 0; w < attributes.getWidth(); w++)
            {
                elevationTiles.add(new ElevationTile(w, h, 0, attributes));
            }
        }
        return elevationTiles;
    }

    public static ElevationTile getElevationTile(double latitude,
                                                 double longitude,
                                                 int level,
                                                 ElevationAttributes attributes,
                                                 Set<String> ignoreFilenames)
    {
        int[] rowCol = getRowCol(latitude, longitude, attributes, level);
        return new ElevationTile(rowCol[0], rowCol[1], level, attributes);
    }

    /**
     * @deprecated no replacement
     */
    @Deprecated(since = "5.6", forRemoval = true)
    @DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
    public static ElevationTile getElevationTile(int row,
                                                 int col,
                                                 int level,
                                                 long expiryTime,
                                                 ElevationAttributes attributes)
    {
        return new ElevationTile(row, col, level, attributes);
    }

    /**
     * Update a tile collection with valid local tiles in a given sector.  Tiles already in the tile collection are not
     * overwritten.
     */
    public static void updateTileCollection(Sector sector, TileCollection tileCollection)
    {
        if (sector == null)
        {
            return;
        }

        ElevationAttributes attributes = tileCollection.getAttributes();
        sector = attributes.getSector().intersection(sector);

        if (sector == null)
        {
            return;
        }

        long time = System.currentTimeMillis();
        int levels = attributes.getLevels();

        // Iterate through the levels
        int addCount = 0;
        int totalCount = 0;

        for (int level = levels - 1; level >= 0; level--)
        {
            int[] minRowCol = getRowCol(
                    sector.getMinLatitude().degrees,
                    sector.getMinLongitude().degrees,
                    attributes,
                    level);
            int[] maxRowCol = getRowCol(
                    sector.getMaxLatitude().degrees,
                    sector.getMaxLongitude().degrees,
                    attributes,
                    level);

            int minRow = minRowCol[0];
            int maxRow = maxRowCol[0];
            int minCol = minRowCol[1];
            int maxCol = maxRowCol[1];

            // Iterate through the rows in this sector.
            for (int row = minRow; row <= maxRow; row++)
            {
                // Test for interruption
                if (Thread.currentThread().isInterrupted())
                {
                    return;
                }

                // Iterate through the columns in this sector.
                for (int col = minCol; col <= maxCol; col++)
                {
                    // Check the tile collection for the current tile.  If the tile does not exist, create it and add
                    // it to the tile collection.  By only creating new tiles when necessary and ignoring tiles already
                    // in the collection, unnecessary calls to File.exists() are avoided.
                    long key = getKey(row, col, level);
                    if (!tileCollection.containsKey(key))
                    {
                        tileCollection.addTile(new ElevationTile(row, col, level, attributes));
                        addCount++;
                    }
                    totalCount++;
                }
            }
        }

        if (logger.isDebugEnabled())
        {
            time = System.currentTimeMillis() - time;
            logger.debug("{} update time: {} ms.  Added {} tiles from a total of {}", tileCollection.getAttributes().getElevationModel().getName(), time, addCount, totalCount);
        }
    }

    public static long getTileKey(double latitude, double longitude, int level, ElevationAttributes attributes)
    {
        int[] rowCol = getRowCol(latitude, longitude, attributes, level);
        return getKey(rowCol[0], rowCol[1], level);
    }

    public static int[] getRowCol(
                                  double latitude,
                                  double longitude,
                                  ElevationAttributes attributes,
                                  int level)
    {
        LatLon levelDelta = attributes.getLevelDelta(level);
        LatLon origin = attributes.getTileOrigin();
        int row = Tile.computeRow(
                levelDelta.getLatitude(),
                Angle.fromDegreesLatitude(latitude),
                origin.getLatitude());
        int col = Tile.computeColumn(
                levelDelta.getLongitude(),
                Angle.fromDegreesLongitude(longitude),
                origin.getLongitude());
        return new int[]{row, col};
    }

    public static Iterable<int[]> getRowColsForSector(Sector sector, ElevationAttributes attributes, int level)
    {
        int[] minRowCol = getRowCol(
                sector.getMinLatitude().degrees,
                sector.getMinLongitude().degrees,
                attributes,
                level);
        int[] maxRowCol = getRowCol(
                sector.getMaxLatitude().degrees,
                sector.getMaxLongitude().degrees,
                attributes,
                level);

        if (logger.isDebugEnabled())
        {
            logger.debug("model: {}, min row col: {}", attributes.getElevationModel(), Arrays.toString(minRowCol));
            logger.debug("model: {}, max row col: {}", attributes.getElevationModel(), Arrays.toString(maxRowCol));
        }

        final int minRow = minRowCol[0];
        final int maxRow = maxRowCol[0];
        final int minCol = minRowCol[1];
        final int maxCol = maxRowCol[1];

        return new Iterable<int[]>()
        {
            int row = minRow;
            int col = minCol;

            @Override
            public Iterator<int[]> iterator()
            {
                return new Iterator<int[]>()
                {
                    @Override
                    public boolean hasNext()
                    {
                        return row <= maxRow;
                    }

                    @Override
                    public int[] next()
                    {
                        int[] rowCol = new int[]{row, col};
                        col++;
                        if (col > maxCol)
                        {
                            col = minCol;
                            row++;
                        }
                        return rowCol;
                    }

                    @Override
                    public void remove()
                    {
                        // no removals
                    }
                };
            }
        };
    }

    public static LatLon getLatLon(ElevationAttributes attributes, int level, int row, int col)
    {
        // Get the lat/lon deltas
        LatLon delta = attributes.getLevelDelta(level);
        Angle latDelta = delta.getLatitude();
        Angle lonDelta = delta.getLongitude();

        LatLon tileOrigin = attributes.getTileOrigin();

        // Calculate the base latitude and longitude
        double lat = Tile.computeRowLatitude(row, latDelta, tileOrigin.getLatitude()).degrees;
        double lon = Tile.computeColumnLongitude(col, lonDelta, tileOrigin.getLongitude()).degrees;
        return LatLon.fromDegrees(lat, lon);
    }

    public static LatLon getLatLon(ElevationAttributes attributes, int level, int row, int col, int x, int y)
    {
        LatLon latLon = getLatLon(attributes, level, row, col);

        // Get the tile's attributes
        Dimension tileDimension = attributes.getTileDimension();

        double maxX = tileDimension.width - 1;
        double maxY = tileDimension.height - 1;

        // Get the lat/lon deltas
        LatLon delta = attributes.getLevelDelta(level);
        Angle latDelta = delta.getLatitude();
        Angle lonDelta = delta.getLongitude();

        // Calculate the base latitude and longitude
        double lat = latLon.getLatitude().degrees;
        double lon = latLon.getLongitude().degrees;

        double ratioX = (x / maxX);
        double ratioY = ((maxY - y) / maxY);

        lat += ratioY * latDelta.degrees;
        lon += ratioX * lonDelta.degrees;

        return LatLon.fromDegrees(lat, lon);
    }

    public static Point toGridPoint(Sector sector, double lat, double lon, int width, int height)
    {
        if (!sector.contains(LatLon.fromDegrees(lat, lon)))
        {
            return null;
        }

        // Get the lat/lon deltas
        double latDelta = sector.getDeltaLat().getDegrees();
        double lonDelta = sector.getDeltaLon().getDegrees();

        double minLat = sector.getMinLatitude().getDegrees();
        double minLon = sector.getMinLongitude().getDegrees();

        double latOffset = lat - minLat;
        double lonOffset = lon - minLon;

        double latRatio = latOffset / latDelta;
        double lonRatio = lonOffset / lonDelta;

        return new Point((int) Math.round(lonRatio * (width - 1)), (int) Math.round((1 - latRatio) * (height - 1)));
    }

    public static Sector getSector(int row,
                                   int col,
                                   int level,
                                   ElevationAttributes attributes,
                                   boolean intersectWithModel)
    {
        LatLon delta = attributes.getLevelDelta(level);
        double latDelta = delta.getLatitude().degrees;
        double lonDelta = delta.getLongitude().degrees;

        LatLon tileOrigin = attributes.getTileOrigin();

        double lat = Tile.computeRowLatitude(row, Angle.fromDegrees(latDelta), tileOrigin.getLatitude()).degrees;
        double lon = Tile.computeColumnLongitude(col, Angle.fromDegrees(lonDelta), tileOrigin.getLongitude()).degrees;

        double latMax = lat + latDelta;
        double lonMax = lon + lonDelta;
        Sector sector = Sector.fromDegrees(lat, latMax, lon, lonMax);
        return intersectWithModel ? sector.intersection(attributes.getSector()) : sector;
    }

    public static LatLon getLatLon(int row, int col, int level, ElevationAttributes attributes)
    {
        LatLon delta = attributes.getLevelDelta(level);
        double latDelta = delta.getLatitude().degrees;
        double lonDelta = delta.getLongitude().degrees;

        LatLon tileOrigin = attributes.getTileOrigin();

        double lat = Tile.computeRowLatitude(row, Angle.fromDegrees(latDelta), tileOrigin.getLatitude()).degrees;
        double lon = Tile.computeColumnLongitude(col, Angle.fromDegrees(lonDelta), tileOrigin.getLongitude()).degrees;

        return LatLon.fromDegrees(lat, lon);
    }

    public static float[] makeTopographic(float[] values,
                                          float minValue,
                                          float maxValue,
                                          int numGroups)
    {
        float[] topographic = new float[values.length];

        // We want to group each of the values into one of numGroups.  This will be done by creating a group size
        // representing a fraction of the difference between minValue and maxValue.  The lowest group will be any
        // values between minValue and (minValue + groupSize).  The second will be between (minValue + groupSize) and
        // (minValue * (groupSize * 2) and so on.
        float[] limits = getThresholds(minValue, maxValue, numGroups);

        // We want to create as much visual contrast as possible.  This will be done by stretching out the group size
        // so that the lowest group == minValue and the highest group == maxValue.  If we were to use the values in
        // the limits array, the lowest group value would actually be equal to (minValue + groupSize) and the
        // contrast would not be as wide as possible.
        float[] stretched = getStretchedThresholds(minValue, maxValue, numGroups);

        for (int i = 0; i < values.length; i++)
        {
            float f = values[i];
            if (isMissingDataSignal(f))
            {
                topographic[i] = MISSING_DATA_COLOR_VALUE;
            } else if (isNotLocalSignal(f))
            {
                topographic[i] = NOT_LOCAL_COLOR_VALUE;
            } else
            {
                int j = 0;
                while (j < (numGroups - 1) && f > limits[j])
                {
                    j++;
                }
                topographic[i] = stretched[j];
            }
        }
        return topographic;
    }

    public static float[] getThresholds(float minValue, float maxValue, int numGroups)
    {
        float[] thresholds = new float[numGroups];
        float diff = maxValue - minValue;
        float groupSize = diff / numGroups;

        for (int i = 0; i < numGroups; i++)
        {
            float top = minValue + (groupSize * (i + 1));
            thresholds[i] = top;
        }

        return thresholds;
    }

    public static float[] getStretchedThresholds(float minValue, float maxValue, int numGroups)
    {
        float[] stretched = new float[numGroups];
        float diff = maxValue - minValue;
        float stretchedGroupSize = diff / (numGroups - 1);
        for (int i = 0; i < numGroups; i++)
        {
            stretched[i] = minValue + stretchedGroupSize * i;
        }
        return stretched;
    }

    public static int getRGB(double value, double min, double max, float low, float high)
    {
        max -= min;
        value -= min;
        value /= max;
        float hue = (float) WWMath.mixSmooth(value, low, high);
        return 0xFF000000 | Color.HSBtoRGB(hue, 1f, 1f);
    }

    public static float[] getHueGroups(float low, float high, int numGroups)
    {
        float[] hues = new float[numGroups];
        double max = numGroups - 1;
        for (int i = 0; i < numGroups; i++)
        {
            hues[i] = (float) WWMath.mixSmooth((i / max), low, high);
        }
        return hues;
    }

    public static int[] getRGB(float[] values,
                               double minValue,
                               double maxValue,
                               Color low,
                               Color high,
                               int[] rgb)
    {
        if (rgb == null)
        {
            rgb = new int[values.length];
        }

        float lowHue = getHue(low);
        float highHue = getHue(high);

        for (int i = 0; i < rgb.length; i++)
        {
            float value = values[i];
            if (isMissingDataSignal(value))
            {
                rgb[i] = MISSING_DATA_COLOR_VALUE;
            } else if (isNotLocalSignal(value))
            {
                rgb[i] = NOT_LOCAL_COLOR_VALUE;
            } else
            {
                rgb[i] = getRGB(value, minValue, maxValue, lowHue, highHue);
            }
        }
        return rgb;
    }

    public static int[] getTopographicRGB(float[] values,
                                          float minValue,
                                          float maxValue,
                                          int numGroups,
                                          Color low,
                                          Color high,
                                          int[] data)
    {
        return getRGB(
                makeTopographic(values, minValue, maxValue, numGroups),
                minValue,
                maxValue,
                low,
                high,
                data);
    }

    public static void copyImage(BufferedImage sourceImage, BufferedImage destinationImage)
    {
        int[] sourceRGB = getPixelData(sourceImage);
        int[] destinationRGB = getPixelData(destinationImage);
        System.arraycopy(sourceRGB, 0, destinationRGB, 0, sourceRGB.length);
    }

    public static int[] getPixelData(BufferedImage image)
    {
        return ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
    }

    public static Set<Long> getTileKeysInSector(Sector sector, int level, ElevationAttributes attributes)
    {
        return getTileKeysInSector(sector, level, attributes, new HashSet<Long>());
    }

    /**
     * @deprecated no replacement
     */
    @Deprecated(since = "5.6", forRemoval = true)
    @DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
    public static String formatDegrees(double valueInDegrees)
    {
        return String.format("%.3f\u00B0", valueInDegrees);
    }

    /**
     * @deprecated no replacement
     */
    @Deprecated(since = "5.6", forRemoval = true)
    @DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
    public static String getLegendLabelString(double meters)
    {
        boolean km = Math.abs(meters) >= 250;
        if (km)
        {
            return String.format("%.2f km", (meters / 1000));
        }
        return String.format("%d m", (int) meters);
    }

    /**
     * @deprecated no replacement
     */
    @Deprecated(since = "5.6", forRemoval = true)
    @DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
    public static String getElevationString(double meters, UnitsFormat unitsFormat)
    {
        switch (unitsFormat.getLengthUnits())
        {
            case UnitsFormat.KILOMETERS:
                if (Math.abs(meters) < 250)
                {
                    return String.format("%.2f m", meters);
                }
                break;
            case UnitsFormat.MILES:
                double miles = meters * unitsFormat.getAltitudeUnitsMultiplier();
                if (Math.abs(miles) < 0.25)
                {
                    return String.format("%.2f ft", meters * WWMath.METERS_TO_FEET);
                }
                break;
            default:
                break;
        }
        return getDefaultElevationString(meters, unitsFormat);
    }

    /**
     * @deprecated no replacement
     */
    @Deprecated(since = "5.6", forRemoval = true)
    @DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
    public static String getDefaultElevationString(double meters, UnitsFormat unitsFormat)
    {
        return String.format("%.2f ", meters * unitsFormat.getLengthUnitsMultiplier()) +
                unitsFormat.getLengthUnitsSymbol();
    }

    /**
     * @deprecated no replacement
     */
    @Deprecated(since = "5.6", forRemoval = true)
    @DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
    public static String getLabelString(double low, double high, UnitsFormat unitsFormat)
    {
        switch (unitsFormat.getLengthUnits())
        {
            case UnitsFormat.KILOMETERS:
                if (Math.abs(low) < 250 && Math.abs(high) < 250)
                {
                    return String.format("%.2f m - %.2f m", low, high);
                }
                break;
            case UnitsFormat.MILES:
                double lowMiles = low * unitsFormat.getAltitudeUnitsMultiplier();
                double highMiles = high * unitsFormat.getAltitudeUnitsMultiplier();
                if (Math.abs(lowMiles) < 0.25 && Math.abs(highMiles) < 0.25)
                {
                    return String.format("%.2f ft - %.2f ft",
                            low * WWMath.METERS_TO_FEET,
                            high * WWMath.METERS_TO_FEET);
                }
                break;
            default:
                break;
        }
        return getDefaultElevationsString(low, high, unitsFormat);
    }

    /**
     * @deprecated no replacement
     */
    @Deprecated(since = "5.6", forRemoval = true)
    @DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
    public static String getDefaultElevationsString(double low, double high, UnitsFormat unitsFormat)
    {
        return String.format("%.2f %s - %.2f %s",
                low * unitsFormat.getLengthUnitsMultiplier(),
                unitsFormat.getLengthUnitsSymbol(),
                high * unitsFormat.getLengthUnitsMultiplier(),
                unitsFormat.getLengthUnitsSymbol());
    }

    /**
     * @deprecated no replacement
     */
    @Deprecated(since = "5.6", forRemoval = true)
    @DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
    public static int getLevel(File tileFile)
    {
        return Integer.parseInt(tileFile.getParentFile().getParentFile().getName());
    }

    public static long getKey(int row, int col, int level)
    {
        long maskedRow = ROW_COL_MASK & row; //2^29 max rows
        long maskedCol = ROW_COL_MASK & col; //2^29 max cols
        long maskedLevel = LEVEL_MASK & level; //2^6 max levels
        return (maskedLevel << 58) | (maskedCol << COL_BIT_OFFSET) | maskedRow;
    }

    public static int getRowFromKey(long key)
    {
        return (int) (ROW_COL_MASK & key);
    }

    public static int getColFromKey(long key)
    {
        return (int) (ROW_COL_MASK & (key >>> COL_BIT_OFFSET));
    }

    public static int getLevelFromKey(long key)
    {
        return (int) (LEVEL_MASK & (key >>> LEVEL_BIT_OFFSET));
    }

    public static int blendHillshade(int rgb, double hillshade)
    {
        if (rgb == 0 || hillshade == 0)
        {
            return 0;
        }

        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        r = Math.min(255, (int) (r * hillshade));
        g = Math.min(255, (int) (g * hillshade));
        b = Math.min(255, (int) (b * hillshade));

        return 0xFF000000 | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    /**
     * A modified version of BasicElevationModel.isTileLocalOrAbsent(), which relies on a cache of previously seen
     * tiles whenever possible.  Using the cache (really just a hash map) is a major speed optimization for terrain
     * discovery.
     *
     * @return {@code true} if the tile can be found on disk (or is absent), or {@code false} if not.
     * @deprecated no replacement
     */
    @Deprecated(since = "5.6", forRemoval = true)
    @DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
    public static boolean isTileLocal(File tileFile, long expiryTime)
    {
        if (tileFile == null || !tileFile.exists())
        {
            return false;
        }
        return tileFile.lastModified() >= expiryTime;
    }

    public static BufferedImage createImage(float[] elevations,
                                            int width,
                                            int height,
                                            boolean topographic,
                                            boolean hillshade,
                                            Color low,
                                            Color high,
                                            int numberOfColorGroups,
                                            BufferedImage image)
    {
        if (image == null || image.getWidth() != width || image.getHeight() != height)
        {
            image = createBufferedImage(width, height);
        }

        int[] imageData = getPixelData(image);
        float[] minMax = getMinMax(elevations);

        if (Thread.currentThread().isInterrupted())
        {
            return null;
        }

        if (minMax == null)
        {
            for (int i = 0; i < imageData.length; i++)
            {
                imageData[i] = MISSING_DATA_COLOR_VALUE;
            }
            return image;
        }

        imageData = topographic
                ? getTopographicRGB(elevations, minMax[0], minMax[1], numberOfColorGroups, low, high, imageData)
                : getRGB(elevations, minMax[0], minMax[1], low, high, imageData);

        if (Thread.currentThread().isInterrupted())
        {
            return null;
        }

        if (hillshade)
        {
            blendHillshade(imageData, getHillshade(elevations, width, height));
        }

        return image;
    }

    public static Iterable<LatLon> getLocations(final Sector sector, final int width, final int height)
    {
        return new Iterable<LatLon>()
        {
            @Override
            public Iterator<LatLon> iterator()
            {
                return new Iterator<LatLon>()
                {
                    private final double minLon = sector.getMinLongitude().degrees;
                    private final double maxLat = sector.getMaxLatitude().degrees;
                    private final double deltaLat = sector.getDeltaLatDegrees();
                    private final double pxLat = deltaLat / height;
                    private final double deltaLon = sector.getDeltaLonDegrees();
                    private final double pxLon = deltaLon / width;
                    private int x = 0;
                    private int y = 0;
                    private boolean hasNext = true;
                    private double latitude = maxLat;

                    @Override
                    public boolean hasNext()
                    {
                        return hasNext;
                    }

                    @Override
                    public LatLon next()
                    {
                        LatLon location = LatLon.fromDegrees(latitude, minLon + pxLon * x);

                        x++;
                        if (x >= width)
                        {
                            x = 0;
                            y++;
                            if (y >= height)
                            {
                                hasNext = false;
                            } else
                            {
                                latitude = maxLat - pxLat * y;
                            }
                        }
                        return location;
                    }

                    @Override
                    public void remove()
                    {
                    }
                };
            }
        };
    }

    public static BufferedImage createBufferedImage(int width, int height)
    {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    public static void blendHillshade(int[] rgb, List<Float> hillshade)
    {
        for (int i = 0; i < rgb.length; i++)
        {
            rgb[i] = blendHillshade(rgb[i], hillshade.get(i));
        }
    }

    public static List<Float> getHillshade(float[] values, int width, int height)
    {
        return getHillshade(values, null, width, height);
    }

    public static float getHue(Color c)
    {
        return Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null)[0];
    }

    public static Dimension getImageDimension(Sector sector, int maxDimension, ILengthMeasurer lengthMeasurer)
    {
        double[] originalDimension = getSectorDimensionInMeters(sector, lengthMeasurer);
        return getScaledDimension(originalDimension[0], originalDimension[1], maxDimension, maxDimension);
    }

    /**
     * This code was taken from:
     * http://stackoverflow.com/questions/10245220/java-image-resize-maintain-aspect-ratio
     * <p/>
     * Get a scaled dimension for a buffered image, respecting the image's aspect ratio.
     *
     * @param boundWidth  bounding width (largest possible scaled width).
     * @param boundHeight bounding height (largest possible scaled height).
     * @return a scaled dimension for the buffered image, respecting the image's aspect ratio.
     */
    public static Dimension getScaledDimension(double originalWidth,
                                               double originalHeight,
                                               double boundWidth,
                                               double boundHeight)
    {
        double newWidth = originalWidth;
        double newHeight = originalHeight;

        if (originalWidth <= boundWidth && originalHeight <= boundHeight)
        {
            return new Dimension((int) originalWidth, (int) originalHeight);
        }

        // first check if we need to scale width
        if (originalWidth > boundWidth)
        {
            //scale width to fit
            newWidth = boundWidth;
            //scale height to maintain aspect ratio
            newHeight = (newWidth * originalHeight) / originalWidth;
        }

        // then check if we need to scale even with the new height
        if (newHeight > boundHeight)
        {
            //scale height to fit instead
            newHeight = boundHeight;
            //scale width to maintain aspect ratio
            newWidth = (newHeight * originalWidth) / originalHeight;
        }

        return new Dimension((int) newWidth, (int) newHeight);
    }

    /**
     * Hillshade algorithm adapted from ESRI Hillshade documentation found at:
     * http://edndoc.esri.com/arcobjects/9.2/net/shared/geoprocessing/spatial_analyst_tools/how_hillshade_works.htm
     *
     * @return a list of values representing pixel hillshading.  These values range between 0 and 1, where 0 is black
     * and 1 is unshaded.
     */
    public static List<Float> getHillshade(float[] values, CellValues cellValues, int width, int height)
    {
        float altitudeDeg = ALTITUDE_DEG;
        float azimuthDeg = AZIMUTH_DEG;
        float zenithDeg = 90 - altitudeDeg;
        float zenithRad = zenithDeg * PI / 180.0f;
        float azimuthMath = 360 - azimuthDeg + 90;
        float azimuthRad = azimuthMath * PI / 180;
        float cellSize = CELL_SIZE;
        float zFactor = Z_FACTOR;

        List<Float> hillshadeValues = new ArrayList<>();
        for (int y = 0; y < height; y++)
        {
            for (int x = 0; x < width; x++)
            {
                NeighborPixels np = new NeighborPixels(x, y, width, height, values, cellValues);
                float dzDx = dzDx(np, cellSize);
                float dzDy = dzDy(np, cellSize);
                float slopeRad = getSlopeRad(zFactor, dzDx, dzDy);
                float aspectRad = getAspectRad(dzDx, dzDy);
                float hillshade = getHillshade(zenithRad, slopeRad, azimuthRad, aspectRad);
                hillshadeValues.add(hillshade);
            }
        }
        return hillshadeValues;
    }

    private static Set<Long> getTileKeysInSector(Sector sector,
                                                 int level,
                                                 ElevationAttributes attributes,
                                                 Set<Long> keys)
    {
        LatLon origin = attributes.getTileOrigin();
        LatLon delta = attributes.getLevelDelta(level);
        int minCol = Tile.computeColumn(delta.getLongitude(), sector.getMinLongitude(), origin.getLongitude());
        int maxCol = Tile.computeColumn(delta.getLongitude(), sector.getMaxLongitude(), origin.getLongitude());

        int minRow = Tile.computeRow(delta.getLatitude(), sector.getMinLatitude(), origin.getLatitude());
        int maxRow = Tile.computeRow(delta.getLatitude(), sector.getMaxLatitude(), origin.getLatitude());

        for (int row = minRow; row <= maxRow; row++)
        {
            for (int col = minCol; col <= maxCol; col++)
            {
                keys.add(getKey(row, col, level));
            }
        }
        return keys;
    }

    private static float dzDx(NeighborPixels np, float cellSize)
    {
        return ((np.c + 2 * np.f + np.i) - (np.a + 2 * np.d + np.g)) / (8 * cellSize);
    }

    private static float dzDy(NeighborPixels np, float cellSize)
    {
        return ((np.g + 2 * np.h + np.i) - (np.a + 2 * np.b + np.c)) / (8 * cellSize);
    }

    private static float getSlopeRad(float zFactor, float dzDx, float dzDy)
    {
        return (float) Math.atan(zFactor * Math.sqrt((dzDx * dzDx + dzDy * dzDy)));
    }

    private static float getAspectRad(float dzDx, float dzDy)
    {
        float aspectRad;
        if (dzDx != 0)
        {
            aspectRad = (float) Math.atan2(dzDy, -dzDx);
            if (aspectRad < 0)
            {
                aspectRad += PI2;
            }
        } else if (dzDy > 0)
        {
            aspectRad = PI_HALF;
        } else if (dzDy < 0)
        {
            aspectRad = PI2 - PI_HALF;
        } else
        {
            aspectRad = (float) Math.atan2(dzDy, -dzDx);
        }
        return aspectRad;
    }

    private static float getHillshade(double zenithRad, double slopeRad, double azimuthRad, double aspectRad)
    {
        return (float) (1 + ((Math.cos(zenithRad) * Math.cos(slopeRad)) +
                (Math.sin(zenithRad) * Math.sin(slopeRad) * Math.cos(azimuthRad - aspectRad)))) / 2;
    }

    private static void readAsByteBuffer(ByteBuffer byteBuffer, float[] values, float localMissingDataSignal)
    {
        int length = values.length;
        int i = 0;
        while (byteBuffer.hasRemaining() && i < length)
        {
            setValue(values, i++, byteBuffer.get(), localMissingDataSignal);
        }
    }

    private static void readAsShortBuffer(ByteBuffer byteBuffer, float[] values, float localMissingDataSignal)
    {
        int length = values.length;
        int i = 0;
        while (byteBuffer.hasRemaining() && i < length)
        {
            setValue(values, i++, byteBuffer.getShort(), localMissingDataSignal);
        }
    }

    private static void readAsIntBuffer(ByteBuffer byteBuffer, float[] values, float localMissingDataSignal)
    {
        int length = values.length;
        int i = 0;
        while (byteBuffer.hasRemaining() && i < length)
        {
            setValue(values, i++, (float) byteBuffer.getInt(), localMissingDataSignal);
        }
    }

    private static void readAsFloatBuffer(ByteBuffer byteBuffer, float[] values, float localMissingDataSignal)
    {
        int length = values.length;
        int i = 0;
        while (byteBuffer.hasRemaining() && i < length)
        {
            setValue(values, i++, byteBuffer.getFloat(), localMissingDataSignal);
        }
    }

    private static void readAsDoubleBuffer(ByteBuffer byteBuffer, float[] values, float localMissingDataSignal)
    {
        int length = values.length;
        int i = 0;
        while (byteBuffer.hasRemaining() && i < length)
        {
            setValue(values, i++, (float) byteBuffer.getDouble(), localMissingDataSignal);
        }
    }

    private static void setValue(float[] values, int index, float value, float localMissingDataSignal)
    {
        if (value == localMissingDataSignal)
        {
            value = MISSING_DATA_SIGNAL;
        }
        values[index] = value;
    }

    private enum CellLocation
    {
        TOP_LEFT(-1, -1),
        TOP(0, -1),
        TOP_RIGHT(1, -1),
        LEFT(-1, 0),
        RIGHT(1, 0),
        BOTTOM_LEFT(-1, 1),
        BOTTOM(0, 1),
        BOTTOM_RIGHT(1, 1);

        public final int x;
        public final int y;

        CellLocation(int x, int y)
        {
            this.x = x;
            this.y = y;
        }

        public int getX(int originalX)
        {
            return originalX + x;
        }

        public int getY(int originalY)
        {
            return originalY + y;
        }
    }

    public static class NeighborPixels
    {
        public final int x;
        public final int y;
        public final int width;
        public final int height;

        public final float a;
        public final float b;
        public final float c;
        public final float d;
        public final float e;
        public final float f;
        public final float g;
        public final float h;
        public final float i;

        public NeighborPixels(int x, int y, int width, int height, float[] values, CellValues cellValues)
        {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;

            e = values[(y * width) + x];
            a = get(CellLocation.TOP_LEFT, e, values, cellValues);
            b = get(CellLocation.TOP, e, values, cellValues);
            c = get(CellLocation.TOP_RIGHT, e, values, cellValues);
            d = get(CellLocation.LEFT, e, values, cellValues);
            f = get(CellLocation.RIGHT, e, values, cellValues);
            g = get(CellLocation.BOTTOM_LEFT, e, values, cellValues);
            h = get(CellLocation.BOTTOM, e, values, cellValues);
            i = get(CellLocation.BOTTOM_RIGHT, e, values, cellValues);
        }

        private float get(CellLocation cellLocation, float defaultValue, float[] values, CellValues cellValues)
        {
            int x1 = cellLocation.getX(x);
            int y1 = cellLocation.getY(y);

            if (x1 >= width || y1 >= height || x1 < 0 || y1 < 0)
            {
                if (cellValues != null)
                {
                    return convertPoint(x1, y1, cellValues, defaultValue);
                }
                return defaultValue;
            }
            return values[(y1 * width) + x1];
        }

        /**
         * Convert a neighbor pixel coordinate outside the boundaries of the tile into a value from one of the
         * surrounding tiles.  This will occur when calculating neighbor pixels of edge and corner points.
         */
        private float convertPoint(int x,
                                   int y,
                                   CellValues cellValues,
                                   float defaultValue)
        {
            ElevationTile tile;
            if (y < 0)
            {
                y = height - 1;

                // top left
                if (x < 0)
                {
                    x = width - 1;
                    tile = cellValues.topLeft;
                }

                // top right
                else if (x >= width)
                {
                    x = 0;
                    tile = cellValues.topRight;
                }

                // top
                else
                {
                    tile = cellValues.top;
                }
            } else if (y >= height)
            {
                y = 0;

                // bottom left
                if (x < 0)
                {
                    x = width - 1;
                    tile = cellValues.bottomLeft;
                }

                // bottom right
                else if (x >= width)
                {
                    x = 0;
                    tile = cellValues.bottomRight;
                }

                // bottom
                else
                {
                    tile = cellValues.bottom;
                }
            }

            // left
            else if (x < 0)
            {
                x = width - 1;
                tile = cellValues.left;
            }

            // right
            else if (x >= width)
            {
                x = 0;
                tile = cellValues.right;
            }

            // center
            else
            {
                tile = cellValues.center;
            }
            return get(x, y, tile, defaultValue);
        }

        private float get(int x, int y, ElevationTile tile, float defaultValue)
        {
            if (tile == null)
            {
                return defaultValue;
            }
            return tile.getValues()[y * width + x];
        }
    }

    public static class CellValues
    {
        public final ElevationTile topLeft;
        public final ElevationTile top;
        public final ElevationTile topRight;
        public final ElevationTile left;
        public final ElevationTile center;
        public final ElevationTile right;
        public final ElevationTile bottomLeft;
        public final ElevationTile bottom;
        public final ElevationTile bottomRight;

        public CellValues(ElevationTile topLeft,
                          ElevationTile top,
                          ElevationTile topRight,
                          ElevationTile left,
                          ElevationTile center,
                          ElevationTile right,
                          ElevationTile bottomLeft,
                          ElevationTile bottom,
                          ElevationTile bottomRight)
        {
            this.topLeft = topLeft;
            this.top = top;
            this.topRight = topRight;
            this.left = left;
            this.center = center;
            this.right = right;
            this.bottomLeft = bottomLeft;
            this.bottom = bottom;
            this.bottomRight = bottomRight;
        }
    }

    /**
     * @deprecated no replacement
     */
    @Deprecated(since = "5.6", forRemoval = true)
    @DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
    public static void main(String[] args)
    {
        for (int row = 0; row < 1000000; row += 1000)
        {
            for (int col = 0; col < 1000000; col += 1000)
            {
                for (int level = 0; level < 32; level++)
                {
                    long key = getKey(row, col, level);
                    if (row != getRowFromKey(key))
                    {
                        System.out.println("row: " + row + ", extracted row: " + row);
                    }
                    if (col != getColFromKey(key))
                    {
                        System.out.println("col: " + col + ", extracted col: " + col);
                    }
                    if (level != getLevelFromKey(key))
                    {
                        System.out.println("level: " + level + ", extracted level: " + level);
                    }
                }
            }
        }
        System.out.println("done");
    }
}
