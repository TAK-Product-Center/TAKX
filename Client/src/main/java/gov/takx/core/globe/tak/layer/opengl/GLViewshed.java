
package gov.takx.core.globe.tak.layer.opengl;

import android.util.Pair;
import com.atakmap.coremap.maps.coords.GeoCalculations;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.lang.Objects;
import com.atakmap.map.MapRenderer;
import com.atakmap.map.layer.Layer;
import com.atakmap.map.layer.opengl.GLLayer2;
import com.atakmap.map.layer.opengl.GLLayerFactory;
import com.atakmap.map.layer.opengl.GLLayerSpi2;
import com.atakmap.map.opengl.GLMapView;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.takx.core.globe.tak.layer.HorizonUtilities;
import gov.takx.core.globe.tak.layer.ViewshedLayer;
import jakarta.annotation.Nonnull;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

/**
 * Adds viewshed functionality to {@link GLHeatMap}.
 *
 * @since 5.6 (from ATAK)
 */
@SuppressWarnings("deprecation")
public class GLViewshed extends GLHeatMap implements ViewshedLayer.OnPointOfInterestChangedListener,
        ViewshedLayer.OnRadiusChangedListener, ViewshedLayer.OnOpacityChangedListener
{
    public static final GLLayerSpi2 SPI = new GLLayerSpi2()
    {
        @Override
        public int getPriority()
        {
            return 1;
        }

        @Override
        public GLLayer2 create(Pair<MapRenderer, Layer> arg)
        {
            return arg.second instanceof ViewshedLayer viewShedLayer
                    ? GLLayerFactory.adapt(new GLViewshed(arg.first, viewShedLayer))
                    : null;
        }
    };

    private static final int SEEN = 0x00FF0000; // rgba green
    private static final int UNSEEN = 0xFF000000; // rgba red
    private static final int OUTSIDE_RANGE = 0x00000000;

    private IGeoPoint pointOfInterest;
    private double radius;
    private int opacitySeen;
    private int opacityUnseen;

    private boolean showCircle = true;

    private double calculatedRadius;
    private IGeoPoint calculatedPOI;
    private int calculatedOpacityUnseen = -1;
    private int calculatedOpacitySeen = -1;

    private int columnCount;
    private int rowCount;
    private int startX;
    private int startY;
    private double xSampleDist;
    private double ySampleDist;
    private float[] elevGrid;
    private float[] slopeGrid;
    private float startAlt;

    public GLViewshed(@Nonnull MapRenderer surface, @Nonnull ViewshedLayer layer)
    {
        super(surface, layer);

        layer.addOnPointOfInterestChangedListener(this);
        layer.addOnRadiusChangedListener(this);
        layer.addOnOpacityChangedListener(this);

        pointOfInterest = layer.getPointOfInterest();
        radius = layer.getRadius();
        opacitySeen = layer.getOpacitySeen();
        opacityUnseen = layer.getOpacityUnseen();
    }

    @Override
    public void onPointOfInterestChanged(@Nonnull ViewshedLayer layer)
    {
        final IGeoPoint pointOfInterest = layer.getPointOfInterest();

        executeOnRenderThread(() -> {
            this.pointOfInterest = pointOfInterest;
            invalidate();
        });
    }

    @Override
    public void onRadiusChanged(@Nonnull ViewshedLayer layer)
    {
        final double radius = layer.getRadius();

        executeOnRenderThread(() -> {
            this.radius = radius;
            invalidate();
        });
    }

    @Override
    public void onOpacityChanged(@Nonnull ViewshedLayer layer)
    {
        final int opacitySeen = layer.getOpacitySeen();
        final int opacityUnseen = layer.getOpacityUnseen();

        executeOnRenderThread(() -> {
            this.opacitySeen = opacitySeen;
            this.opacityUnseen = opacityUnseen;
            invalidate();
        });
    }

    @Override
    protected boolean checkState()
    {
        if (invalid) return true;

        // Only need to query when the point of interest or radius changes
        final var preparedState = (ViewshedState) this.preparedState;
        final var targetState = (ViewshedState) this.targetState;

        return !Objects.equals(preparedState.pointOfInterest, targetState.pointOfInterest)
                || !((Double.isNaN(preparedState.radius) && Double.isNaN(targetState.radius))
                        || (preparedState.radius == targetState.radius));
    }

    @Override
    protected ViewState newViewStateInstance()
    {
        return new ViewshedState();
    }

    @Override
    protected void updateRGBA(@Nonnull HeatMapState heatMapState, @Nonnull HeatMapParams result)
    {
        //skip finding the low res view
        if (result.quick) return;

        //only update if radius, or POI have changed
        final var stateImpl = (ViewshedState) heatMapState;

        result.rgbaData = new byte[result.elevationData.length * 4];
        final var buffer = ByteBuffer.wrap(result.rgbaData).asIntBuffer();
        calculateVSD(stateImpl, result, buffer);

        result.valid = true;
        calculatedRadius = radius;
        calculatedPOI = pointOfInterest;
        calculatedOpacitySeen = opacitySeen;
        calculatedOpacityUnseen = opacityUnseen;
    }

    /**
     * Set up the variables so that the viewshed can be calculated, calculate the viewshed, then convert the resulting 
     * 2d array color map back to a single dimensional array.
     * 
     * @param state the viewshed info 
     * @param result parameters for calculating the viewshed overlay and storing the result
     * @param resultGrid the resulting array to be filled with color values
     */
    private void calculateVSD(@Nonnull ViewshedState state, @Nonnull HeatMapParams result, @Nonnull IntBuffer resultGrid)
    {
        elevGrid = result.elevationData;

        columnCount = result.sampleResolutionX;
        rowCount = result.sampleResolutionY;

        //wrap the result array that will hold the color values associated with the point being seen or unseen
        xSampleDist = (state.radius * 2) / result.sampleResolutionX;
        ySampleDist = (state.radius * 2) / result.sampleResolutionY;

        startX = result.sampleResolutionX / 2;
        startY = result.sampleResolutionY / 2;
        startAlt = (float) state.pointOfInterest.getAltitude() + elevGrid[getIndex(startX, startY)];

        calculateSlopeGrid();
        doLevelCalc(resultGrid);
    }

    /**
     * Calculates the slope for each point in the elevation grid.
     */
    private void calculateSlopeGrid()
    {
        slopeGrid = new float[elevGrid.length];
        int numStages = Math.min(startX, startY);

        for (int stage = 1; stage <= numStages; stage++)
        {
            int x = startX - stage;
            int y = startY - stage;

            //top quarter
            for (int i = 0; i < stage * 2; i++)
            {
                slopeGrid[getIndex(x, y)] = getSlope(x, y);
                x++;
            }

            //right quarter
            for (int i = 0; i < stage * 2; i++)
            {
                slopeGrid[getIndex(x, y)] = getSlope(x, y);
                y++;
            }

            //bottom quarter
            for (int i = 0; i < stage * 2; i++)
            {
                slopeGrid[getIndex(x, y)] = getSlope(x, y);
                x--;
            }

            //left quarter
            for (int i = 0; i < stage * 2; i++)
            {
                slopeGrid[getIndex(x, y)] = getSlope(x, y);
                y--;
            }
        }
    }

    /**
     * @param x The x coordinate
     * @param y The y coordinate
     * @return The slope at the specified coordinates
     */
    private float getSlope(final int x, final int y)
    {
        float pointElev = elevGrid[getIndex(x, y)];
        final double run = getSampleDist(x, y);

        // The tested point elevation needs to take into account the curvature of the earth
        pointElev -= (float) HorizonUtilities.getCurvatureDrop(run);

        double currentAngle = Math.atan(Math.abs(startAlt - pointElev) / run);
        currentAngle = Math.toDegrees(currentAngle);

        return (float) (pointElev < startAlt ? 360 - currentAngle : 360 + currentAngle);
    }

    /**
     * @param x The x coordinate
     * @param y The y coordinate
     * @return The 2d array index
     */
    private int getIndex(final double x, final double y)
    {
        return (int) y * columnCount + (int) x;
    }

    /**
     * Use a gridded approach to calculate the viewshed. Starting at the center point, test every point in the elevation 
     * grid to see if it is visible using the sightline ray approach. 
     * 
     * @param results a 2d array of the color values corresponding to each point's visibility
     */
    private void doLevelCalc(IntBuffer results)
    {
        int y = 0;
        int x;

        for (int i = 0; i < columnCount - 1; i++) testPoint(true, i, y, results);

        x = columnCount - 1;

        //right quarter
        for (int i = 0; i < rowCount - 1; i++) testPoint(false, x, i, results);

        //bottom quarter
        y = rowCount - 1;
        for (int i = columnCount - 1; i > 0; i--) testPoint(true, i, y, results);

        //left quarter
        x = 0;
        for (int i = rowCount - 1; i > 0; i--) testPoint(false, x, i, results);

        //fill the center point
        testPoint(true, startX, startY, results);
    }

    /**
     * Tests a point at a given stage to see if it is visible or not.
     * 
     * @param xAxis {@code true} if sampling at x-axis intersections
     * @param x the x loc of the point to be tested on the elevation grid
     * @param y the y loc of the point to be tested on the elevation grid
     */
    private void testPoint(final boolean xAxis, final int x, final int y, @Nonnull final IntBuffer results)
    {
        final int stage = Math.max(Math.abs(startX - x), Math.abs(startY - y));
        final int seen = SEEN | (0x000000FF & opacitySeen);
        final int unseen = UNSEEN | (0x000000FF & opacityUnseen);

        final double xxDist = Math.abs(startX - x) * xSampleDist;
        final double yyDist = Math.abs(startY - y) * ySampleDist;
        final double angle = Math.toDegrees(Math.atan(yyDist / xxDist));

        final double tanAngle = Math.tan(Math.toRadians(angle));
        float maxSlope = Float.NaN;

        for (int j = 1; j < stage; j++)
        {
            if (xAxis)
            {
                //now that we have the angle, get the x value at the given level
                final double rise = ySampleDist * j;
                final double run = rise / tanAngle;
                int yLevel = startY - j;

                if (y > startY) yLevel = startY + j;

                int diff = (int) Math.round(run / xSampleDist);
                if (x < startX) diff *= -1;

                final int idx = getIndex(startX + diff, yLevel);

                if (showCircle)
                {
                    final double xDist = diff * xSampleDist;
                    final double yDist = (yLevel - startY) * ySampleDist;
                    final double dist = Math.sqrt(xDist * xDist + yDist * yDist);

                    if (dist > radius) return;
                }

                if (Float.isNaN(maxSlope) || slopeGrid[idx] >= maxSlope)
                {
                    results.put(idx, seen);
                    maxSlope = slopeGrid[idx];
                } else
                {
                    results.put(idx, unseen);
                }
            } else
            {
                //now that we have the angle, get the y value at the given level
                final double run = xSampleDist * j;
                final double rise = run * tanAngle;

                int xLevel = startX + j;
                if (x < startX) xLevel = startX - j;

                int diff = (int) Math.round(rise / ySampleDist);
                if (y < startY) diff *= -1;

                final int idx = getIndex(xLevel, startY + diff);

                if (showCircle)
                {
                    double xDist = (xLevel - startX) * xSampleDist;
                    double yDist = diff * ySampleDist;
                    double dist = Math.sqrt(xDist * xDist + yDist * yDist);
                    if (dist > radius) return;
                }

                if (Float.isNaN(maxSlope) || slopeGrid[idx] >= maxSlope)
                {
                    results.put(idx, seen);
                    maxSlope = slopeGrid[idx];
                } else
                {
                    results.put(idx, unseen);
                }
            }
        }

        final int idx = getIndex(x, y);

        if (showCircle)
        {
            results.put(idx, OUTSIDE_RANGE);
            return;
        }

        results.put(idx, Float.isNaN(maxSlope) || slopeGrid[idx] >= maxSlope ? seen : unseen);
    }

    /**
     * Get the distance between the sampled points. 
     * 
    
     * @param x the x position of the dest point in the elevation grid
     * @param y the y position of the dest point in the elevation grid
     * @return the distance in meters between the sampled points along the vector from the start point to the end point
     */
    private double getSampleDist(int x, int y)
    {
        final double xDist = Math.abs(startX - x) * xSampleDist;
        final double yDist = Math.abs(startY - y) * ySampleDist;

        if (xDist == 0 || yDist == 0) return xDist + yDist;

        return Math.sqrt(xDist * xDist + yDist * yDist);
    }

    @Override
    protected void query(ViewState viewState, HeatMapParams result)
    {
        final var viewShedState = (ViewshedState) viewState;

        if (viewShedState.pointOfInterest == null || Double.isNaN(viewShedState.radius))
        {
            // if point of interest or radius are undefined, transparent fill
            result.sampleResolutionX = quickResolutionX;
            result.sampleResolutionY = quickResolutionY;

            if (result.rgbaData == null || result.rgbaData.length < (4 * result.sampleResolutionX * result.sampleResolutionY))
            {
                result.rgbaData = new byte[(4 * result.sampleResolutionX * result.sampleResolutionY)];
            }

            Arrays.fill(result.rgbaData, (byte) 0);
        } else
        {
            final var viewShedLayer = (ViewshedLayer) layer;
            final var transPercentageSeen = viewShedLayer.getOpacitySeen() / 100d;
            final var transPercentageUnseen = viewShedLayer.getOpacityUnseen() / 100d;

            opacitySeen = (int) Math.round(255d * transPercentageSeen) & (byte) 0xFF;
            opacityUnseen = (int) Math.round(255d * transPercentageUnseen) & (byte) 0xFF;
            showCircle = viewShedLayer.isCircle();

            boolean rbgaUpdateNeeded = (result.rgbaData == null || result.rgbaData.length < (4 * result.sampleResolutionX * result.sampleResolutionY));

            rbgaUpdateNeeded = rbgaUpdateNeeded || radius != calculatedRadius || calculatedPOI == null || calculatedPOI != pointOfInterest;

            if (viewShedState.elevationModel == result.elevationModel)
            {
                if (!rbgaUpdateNeeded && (opacitySeen != calculatedOpacitySeen || opacityUnseen != calculatedOpacityUnseen))
                {
                    for (int i = 3; i < result.rgbaData.length; i += 4)
                    {
                        // 0x00 is missing and should not be used as an alpha value
                        if (result.rgbaData[i] != (byte) 0x00)
                        {
                            result.rgbaData[i] = result.rgbaData[i - 3] == (byte) 0xFF ? (byte) opacityUnseen : (byte) opacitySeen;
                        }
                    }

                    return;
                }
            }

            // set up some defaults
            result.minElev = DEFAULT_MIN_ELEVATION;
            result.maxElev = DEFAULT_MAX_ELEVATION;
            result.numSamples = 0;
            result.drawVersion = viewShedState.drawVersion;
            result.sampleResolutionX = fullResolutionX;
            result.sampleResolutionY = fullResolutionY;
            result.elevationModel = elevationModel;

            // record the ROI that we are querying
            if (viewShedState.crossesIDL)
            {
                int hemi = viewShedState.drawLng < 0d ? GeoCalculations.HEMISPHERE_WEST : GeoCalculations.HEMISPHERE_EAST;

                result.upperLeft.set(GeoCalculations.wrapLongitude(viewShedState.upperLeft, hemi));
                result.upperRight.set(GeoCalculations.wrapLongitude(viewShedState.upperRight, hemi));
                result.lowerRight.set(GeoCalculations.wrapLongitude(viewShedState.lowerRight, hemi));
                result.lowerLeft.set(GeoCalculations.wrapLongitude(viewShedState.lowerLeft, hemi));
            } else
            {
                result.upperLeft.set(viewShedState.upperLeft);
                result.upperRight.set(viewShedState.upperRight);
                result.lowerRight.set(viewShedState.lowerRight);
                result.lowerLeft.set(viewShedState.lowerLeft);
            }

            // obtain the elevation data
            if (result.elevationData == null || result.elevationData.length < (result.sampleResolutionX * result.sampleResolutionY))
            {
                result.elevationData = new float[(result.sampleResolutionX * result.sampleResolutionY)];
            }

            if (viewShedState.drawMapResolution > 1000)
            {
                Arrays.fill(result.elevationData, Float.NaN);
            } else
            {
                queryGridImpl(result);
            }

            // generate the RGBA data from the elevation values
            updateRGBA(viewShedState, result);
        }
    }

    private void executeOnRenderThread(@Nonnull Runnable runnable)
    {
        if (renderContext.isRenderThread())
        {
            runnable.run();
        } else
        {
            renderContext.queueEvent(runnable);
        }
    }

    private class ViewshedState extends HeatMapState
    {
        private GeoPoint pointOfInterest;
        private double radius;

        @Override
        public void set(GLMapView view)
        {
            super.set(view);

            IGeoPoint igeoPoint = GLViewshed.this.pointOfInterest;
            radius = GLViewshed.this.radius;

            if (igeoPoint != null && !Double.isNaN(radius))
            {
                pointOfInterest = new GeoPoint(igeoPoint.getLatitude(), igeoPoint.getLongitude(), igeoPoint.getAltitude(), GeoPoint.AltitudeReference.AGL);
                // compute our ROI as the minimum bounding box of the radius around the point of interest
                final var north = GeoCalculations.pointAtDistance(pointOfInterest, 0, radius);
                final var east = GeoCalculations.pointAtDistance(pointOfInterest, 90, radius);
                final var south = GeoCalculations.pointAtDistance(pointOfInterest, 180, radius);
                final var west = GeoCalculations.pointAtDistance(pointOfInterest, 270, radius);

                upperLeft.set(north.getLatitude(), west.getLongitude());
                upperRight.set(north.getLatitude(), east.getLongitude());
                lowerRight.set(south.getLatitude(), east.getLongitude());
                lowerLeft.set(south.getLatitude(), west.getLongitude());
            } else
            {
                upperLeft.set(Double.NaN, Double.NaN);
                upperRight.set(Double.NaN, Double.NaN);
                lowerRight.set(Double.NaN, Double.NaN);
                lowerLeft.set(Double.NaN, Double.NaN);
            }
        }

        @Override
        public void copy(ViewState view)
        {
            super.copy(view);

            final var state = (ViewshedState) view;
            pointOfInterest = state.pointOfInterest;
            radius = state.radius;
        }
    }
}
