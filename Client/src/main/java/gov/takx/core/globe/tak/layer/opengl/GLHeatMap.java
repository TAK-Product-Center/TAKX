
package gov.takx.core.globe.tak.layer.opengl;

import com.atakmap.coremap.conversions.ConversionFactors;
import com.atakmap.coremap.maps.conversion.EGM96;
import com.atakmap.coremap.maps.coords.GeoCalculations;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.lang.Unsafe;
import com.atakmap.map.MapRenderer;
import com.atakmap.map.elevation.ElevationData;
import com.atakmap.map.elevation.ElevationManager;
import com.atakmap.map.elevation.ElevationSource;
import com.atakmap.map.elevation.ElevationSourceManager;
import com.atakmap.map.layer.opengl.GLAsynchronousLayer;
import com.atakmap.map.layer.raster.DatasetDescriptor;
import com.atakmap.map.layer.raster.DefaultDatasetProjection2;
import com.atakmap.map.layer.raster.tilereader.opengl.GLTileMesh;
import com.atakmap.map.opengl.GLMapRenderable;
import com.atakmap.map.opengl.GLMapView;
import com.atakmap.math.MathUtils;
import com.atakmap.math.Matrix;
import com.atakmap.math.NoninvertibleTransformException;
import com.atakmap.math.PointD;
import com.atakmap.opengl.GLES20FixedPipeline;
import com.atakmap.opengl.GLTexture;
import gov.takx.core.globe.tak.layer.HeatMapLayer;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.lang.invoke.MethodHandles;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Renders a heat map on the map. Ported from ATAK.
 *
 * @since 5.6 (from ATAK)
 */
@SuppressWarnings("deprecation")
public class GLHeatMap extends GLAsynchronousLayer<GLHeatMap.HeatMapParams>
        implements HeatMapLayer.OnHeatMapResolutionChangedListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int QUICK_FACTOR = 5;
    static final float DEFAULT_MIN_ELEVATION = (float) ConversionFactors.FEET_TO_METERS * -900;
    static final float DEFAULT_MAX_ELEVATION = (float) ConversionFactors.FEET_TO_METERS * 19000;

    final HeatMapLayer layer;

    int elevationModel;
    int fullResolutionX;
    int fullResolutionY;
    int quickResolutionX;
    int quickResolutionY;

    private final Collection<GLMapRenderable> renderables = new LinkedList<>();

    private final float alpha;
    private final float saturation;
    private final float value;

    private GLHeatMapTexture heatmap;
    private int terrainVersion = -1;

    GLHeatMap(@Nonnull MapRenderer renderContext, @Nonnull HeatMapLayer layer)
    {
        super(renderContext, layer);
        this.layer = layer;

        layer.addOnHeatMapResolutionChangedListener(this);

        elevationModel = layer.getElevationModel();
        saturation = layer.getSaturation();
        value = layer.getValue();
        alpha = layer.getAlpha();
        fullResolutionX = layer.getSampleResolutionX();
        fullResolutionY = layer.getSampleResolutionY();
        quickResolutionX = Math.max(fullResolutionX / QUICK_FACTOR, 14);
        quickResolutionY = Math.max(fullResolutionY / QUICK_FACTOR, 10);
    }

    @Override
    public void onHeatMapResolutionChanged(@Nonnull HeatMapLayer layer)
    {
        final int resolutionX = layer.getSampleResolutionX();
        final int resolutionY = layer.getSampleResolutionY();

        renderContext.queueEvent(() -> {
            fullResolutionX = resolutionX;
            fullResolutionY = resolutionY;

            elevationModel = this.layer.getElevationModel();

            quickResolutionX = Math.max(fullResolutionX / QUICK_FACTOR, 14);
            quickResolutionY = Math.max(fullResolutionY / QUICK_FACTOR, 10);

            invalidate();
        });
    }

    @Override
    public void draw(GLMapView view)
    {
        final int terrainVersion = view.getTerrainVersion();

        if (this.terrainVersion != terrainVersion)
        {
            this.terrainVersion = terrainVersion;
            invalid = true;
        }

        super.draw(view);
    }

    @Override
    protected Collection<GLMapRenderable> getRenderList()
    {
        return renderables;
    }

    @Override
    protected ViewState newViewStateInstance()
    {
        return new HeatMapState();
    }

    @Override
    protected void resetPendingData(HeatMapParams pendingData)
    {
    }

    @Override
    protected void releasePendingData(HeatMapParams pendingData)
    {
        pendingData.elevationData = null;
        pendingData.rgbaData = null;
        pendingData.hsvLut = null;
    }

    @Override
    protected HeatMapParams createPendingData()
    {
        return new HeatMapParams();
    }

    @Override
    protected boolean updateRenderableReleaseLists(HeatMapParams pendingData)
    {
        // Query results were invalid, there's nothing new to render
        if (!pendingData.valid) return false;

        // Create the texture if needed
        if (heatmap == null) heatmap = new GLHeatMapTexture();

        // Make sure the texture is in the renderable list
        if (renderables.isEmpty()) renderables.add(heatmap);

        // Update the texture with the results from the query
        heatmap.update(pendingData);

        return !invalid && !pendingData.needsRefresh;
    }

    @Override
    protected String getBackgroundThreadName()
    {
        return "HeatMap [" + layer.getName() + "] GL worker@" + Integer.toString(hashCode(), 16);
    }

    @Override
    protected void query(ViewState state, HeatMapParams result)
    {
        result.minElev = DEFAULT_MIN_ELEVATION;
        result.maxElev = DEFAULT_MAX_ELEVATION;
        result.numSamples = 0;
        result.elevationModel = elevationModel;

        // Update the resolution based on whether the map has settled. We'll use a lower resolution while the map is 
        // moving to hopefully see more frequent updates

        final boolean quickOnly = state.drawMapResolution > 30;

        if (state.settled)
        {
            if (result.drawVersion != state.drawVersion)
            {
                // The map is settled, but we haven't rendered for it yet. Obtain a quick view and mark ourselves as
                // needing a refresh
                result.needsRefresh = true;
                result.quick = true;
            } else
            {
                result.needsRefresh = false;
                result.quick = quickOnly;
            }
        } else
        {
            result.needsRefresh = false;
            result.quick = true;
        }

        result.drawVersion = state.drawVersion;

        if (!result.quick)
        {
            result.sampleResolutionX = fullResolutionX;
            result.sampleResolutionY = fullResolutionY;
        } else
        {
            result.sampleResolutionX = quickResolutionX;
            result.sampleResolutionY = quickResolutionY;
        }

        // Record the ROI that we are querying
        if (state.crossesIDL)
        {
            final int hemisphere = state.drawLng < 0d ? GeoCalculations.HEMISPHERE_WEST : GeoCalculations.HEMISPHERE_EAST;

            result.upperLeft.set(GeoCalculations.wrapLongitude(state.upperLeft, hemisphere));
            result.upperRight.set(GeoCalculations.wrapLongitude(state.upperRight, hemisphere));
            result.lowerRight.set(GeoCalculations.wrapLongitude(state.lowerRight, hemisphere));
            result.lowerLeft.set(GeoCalculations.wrapLongitude(state.lowerLeft, hemisphere));
        } else
        {
            result.upperLeft.set(state.upperLeft);
            result.upperRight.set(state.upperRight);
            result.lowerRight.set(state.lowerRight);
            result.lowerLeft.set(state.lowerLeft);
        }

        // Obtain the elevation data
        if (result.elevationData == null || result.elevationData.length < (result.sampleResolutionX * result.sampleResolutionY))
        {
            result.elevationData = new float[(result.sampleResolutionX * result.sampleResolutionY)];
        }

        if (state.drawMapResolution > 1000) Arrays.fill(result.elevationData, Float.NaN);

        queryGridImpl(result);

        // Generate the RGBA data from the elevation values
        if (result.rgbaData == null || result.rgbaData.length < (4 * result.sampleResolutionX * result.sampleResolutionY))
        {
            result.rgbaData = new byte[(4 * result.sampleResolutionX * result.sampleResolutionY)];
        }

        updateRGBA((HeatMapState) state, result);
    }

    /**
     * @param result The heat map params and query result data
     */
    protected final void queryGridImpl(@Nonnull HeatMapParams result)
    {
        try
        {
            final var gridCache = new GridCache(result);
            final var start = System.currentTimeMillis();

            gridCache.query();

            final var end = System.currentTimeMillis();

            if (logger.isDebugEnabled())
            {
                final var samples = result.sampleResolutionX * result.sampleResolutionY;
                logger.debug("Elevation query for {} samples in {} ms (unresolved={})", samples, end - start, samples - result.numSamples);
            }
        } catch (NoninvertibleTransformException e)
        {
            result.valid = false;
        } catch (BufferUnderflowException bue)
        {
            logger.error("Underflow error reading tile, damaged for area ll={} ur={}", result.lowerLeft, result.upperRight);

            Arrays.fill(result.elevationData, Float.NaN);
        }
    }

    /**
     * @param state The heat map state
     * @param result The params, including the query result that will be used to determine the colors
     */
    protected void updateRGBA(@Nonnull HeatMapState state, @Nonnull HeatMapParams result)
    {
        validateLUT(state, result);

        final int count = result.sampleResolutionX * result.sampleResolutionY;

        int dataIndex = 0;

        for (int i = 0; i < count; i++)
        {
            if (Double.isNaN(result.elevationData[i])) continue;

            final float value = (result.elevationData[i] - result.minElev) / (result.maxElev - result.minElev) * (float) (result.hsvLut.length - 1);
            final int rgba = result.hsvLut[(int) MathUtils.clamp(value, 0, result.hsvLut.length - 1)];
            final var color = new Color(rgba, true);

            result.rgbaData[dataIndex++] = (byte) color.getRed();
            result.rgbaData[dataIndex++] = (byte) color.getGreen();
            result.rgbaData[dataIndex++] = (byte) color.getBlue();
            result.rgbaData[dataIndex++] = (byte) color.getAlpha();
        }

        result.valid = true;
    }

    /**
     * @param state The heat map state
     * @param result The heat map params and query result data
     */
    private void validateLUT(@Nonnull HeatMapState state, @Nonnull HeatMapParams result)
    {
        if (result.hsvLut == null || result.lutAlpha != state.alpha || result.lutSaturation != state.saturation || result.lutValue != state.value)
        {
            if (result.hsvLut == null) result.hsvLut = new int[256];

            result.lutAlpha = state.alpha;
            result.lutSaturation = state.saturation;
            result.lutValue = state.value;

            final var alpha = (int) (255f * this.alpha);

            for (int i = 0; i < result.hsvLut.length; i++)
            {
                final var hue = (1.0f - (float) i / (float) (result.hsvLut.length - 1)) * 255f;
                final var color = Color.getHSBColor(hue, state.saturation, state.value);
                result.hsvLut[i] = new Color(color.getRed(), color.getGreen(), color.getGreen(), alpha).getRGB();
            }
        }
    }

    /**
     * Maintains the corner coordinates for the heatmap texture. This class is a double buffer. The {@link #get(int)} 
     * method provides read-only access to the front buffer; the {@link #set(int, GeoPoint)} method provides write-only 
     * access to the back buffer.  The method {@link #swap} should be invoked to swap the front and back buffers.
     */
    private static class CornerCoords
    {
        private static final int UPPER_LEFT = 0;
        private static final int UPPER_RIGHT = 1;
        private static final int LOWER_RIGHT = 2;
        private static final int LOWER_LEFT = 3;

        private final GeoPoint[] coordinates;
        private int idx;

        CornerCoords()
        {
            coordinates = new GeoPoint[8];

            for (int i = 0; i < coordinates.length; i++)
            {
                coordinates[i] = GeoPoint.createMutable();
            }
        }

        /**
         * @param coord The coordinate, one of {@link #UPPER_LEFT}, {@link #UPPER_RIGHT}, {@link #LOWER_LEFT}, 
         * {@link #LOWER_RIGHT}
         * @return The front buffer coordinate {@link GeoPoint} value
         */
        GeoPoint get(int coord)
        {
            return coordinates[(idx * 4) + coord];
        }

        /**
         * 
         * @param coord The coordinate, one of {@link #UPPER_LEFT}, {@link #UPPER_RIGHT}, {@link #LOWER_LEFT}, 
         * {@link #LOWER_RIGHT}
         * @param point The {@link GeoPoint} value to set in the back buffer at the specified coordinate
         */
        void set(int coord, @Nonnull GeoPoint point)
        {
            coordinates[((idx ^ 1) * 4) + coord].set(point.getLatitude(), point.getLongitude());
        }

        /**
         * Swaps the front and back buffers.
         */
        void swap()
        {
            idx ^= 1;
        }
    }

    /**
     * Holds the heatmap texture data.
     */
    private static class GLHeatMapTexture implements GLMapRenderable
    {
        private final CornerCoords cornerCoords = new CornerCoords();;

        private boolean dirty;
        private int height;
        private int width;
        private GLTexture texture;
        private ByteBuffer image;
        private GLTileMesh mesh;

        void update(@Nonnull HeatMapParams params)
        {
            // Make sure the data buffer is ready to store the new data
            if (image == null || image.capacity() < (4 * params.sampleResolutionX * params.sampleResolutionY))
            {
                image = Unsafe.allocateDirect(4 * params.sampleResolutionX * params.sampleResolutionY);
                image.order(ByteOrder.nativeOrder());
            }

            // Put the RGBA data into the buffer
            image.put(params.rgbaData, 0, 4 * params.sampleResolutionX * params.sampleResolutionY);

            // Reset the position/limit on the buffer
            image.clear();

            // Update the width/height of the data
            width = params.sampleResolutionX;
            height = params.sampleResolutionY;

            // Set the corner coordinates for the latest data update. Remember that when we 'set', we are assigning the
            // values in the back buffer -- these coordinates won't be visible in 'draw' until we have invoked 'swap'
            cornerCoords.set(CornerCoords.UPPER_LEFT, params.upperLeft);
            cornerCoords.set(CornerCoords.UPPER_RIGHT, params.upperRight);
            cornerCoords.set(CornerCoords.LOWER_RIGHT, params.lowerRight);
            cornerCoords.set(CornerCoords.LOWER_LEFT, params.lowerLeft);

            // The data has been updated, mark us dirty
            dirty = true;
        }

        @Override
        public void draw(GLMapView view)
        {
            // Move heatmap/viewshed to proper unwrap
            if (view.currentPass.crossesIDL)
            {
                final int hemisphere = view.currentPass.drawLng < 0 ? GeoCalculations.HEMISPHERE_WEST : GeoCalculations.HEMISPHERE_EAST;
                double unwrap = 0;

                for (int i = 0; i < 4; i++)
                {
                    final GeoPoint point = cornerCoords.get(i);
                    final double pointLongitude = point.getLongitude();
                    final int pointHemisphere = GeoCalculations.getHemisphere(point);

                    if (pointHemisphere == GeoCalculations.HEMISPHERE_WEST && hemisphere == GeoCalculations.HEMISPHERE_EAST
                            && pointLongitude < view.currentPass.eastBound)
                    {
                        unwrap = 360;
                        break;
                    } else if (pointHemisphere == GeoCalculations.HEMISPHERE_EAST && hemisphere == GeoCalculations.HEMISPHERE_WEST
                            && pointLongitude > view.currentPass.westBound)
                    {
                        unwrap = -360;
                        break;
                    }
                }

                if (unwrap != 0)
                {
                    for (int i = 0; i < 4; i++)
                    {
                        final GeoPoint point = cornerCoords.get(i);
                        point.set(point.getLatitude(), point.getLongitude() + unwrap);
                    }

                    cornerCoords.swap();
                    dirty = true;
                }
            }

            // If we are marked dirty, the data has been updated and we need to reload the texture
            if (dirty)
            {
                // Mke sure our texture is valid
                if (texture == null || texture.getTexWidth() < width || texture.getTexHeight() < height)
                {
                    if (texture != null) texture.release();

                    texture = new GLTexture(width, height, GLES20FixedPipeline.GL_RGBA, GLES20FixedPipeline.GL_UNSIGNED_BYTE);
                }

                // Load the data
                texture.load(image, 0, 0, width, height);

                // Swap the coordinates to match the data that we just loaded 
                cornerCoords.swap();

                // Update the mesh to reflect the new coordinates
                if (mesh == null)
                {
                    try
                    {
                        mesh = new GLTileMesh(0, 0, width, height,
                                0.5f / (float) texture.getTexWidth(),
                                0.5f / (float) texture.getTexHeight(),
                                ((float) width - 0.5f) / (float) texture.getTexWidth(),
                                ((float) height - 0.5f) / (float) texture.getTexHeight(),
                                new DefaultDatasetProjection2(4326, width, height,
                                        cornerCoords.get(CornerCoords.UPPER_LEFT),
                                        cornerCoords.get(CornerCoords.UPPER_RIGHT),
                                        cornerCoords.get(CornerCoords.LOWER_RIGHT),
                                        cornerCoords.get(CornerCoords.LOWER_LEFT)));
                    } catch (IllegalArgumentException iae)
                    {
                        logger.debug("Error when drawing heatmap", iae);
                        return;
                    }
                } else
                {
                    try
                    {
                        mesh.resetMesh(0, 0, width, height,
                                0.5f / (float) texture.getTexWidth(),
                                0.5f / (float) texture.getTexHeight(),
                                ((float) width - 0.5f) / (float) texture.getTexWidth(),
                                ((float) height - 0.5f) / (float) texture.getTexHeight(),
                                new DefaultDatasetProjection2(4326, width, height,
                                        cornerCoords.get(CornerCoords.UPPER_LEFT),
                                        cornerCoords.get(CornerCoords.UPPER_RIGHT),
                                        cornerCoords.get(CornerCoords.LOWER_RIGHT),
                                        cornerCoords.get(CornerCoords.LOWER_LEFT)));
                    } catch (IllegalArgumentException iae)
                    {
                        logger.debug("Error when drawing heatmap", iae);
                        return;
                    }
                }

                dirty = false;
            } else if (texture == null)
            {
                // Not dirty, no texture - no data has been loaded yet so just return
                return;
            }

            // Draw the texture
            mesh.drawMesh(view, texture.getTexId(), 1f, 1f, 1f, 1f);
        }

        @Override
        public void release()
        {
            if (texture != null)
            {
                texture.release();
                texture = null;
            }

            if (mesh != null)
            {
                mesh.release();
                mesh = null;
            }

            image = null;
            dirty = false;
        }
    }

    /**
     * Creates an x,y to {@link GeoPoint} grid based on the heatmap corners and resolution, queries the
     * {@link ElevationManager} for elevation data and populates the provided {@link HeatMapParams result} with
     * elevation data.
     */
    private static class GridCache
    {
        private final HeatMapParams result;
        private final Matrix img2geo;
        private final PointD img;
        private final PointD geo;

        GridCache(@Nonnull HeatMapParams result) throws NoninvertibleTransformException
        {
            this.result = result;

            Arrays.fill(result.elevationData, Float.NaN);

            final Matrix geo2img = Matrix.mapQuads(
                    result.upperLeft.getLongitude(), result.upperLeft.getLatitude(),
                    result.upperRight.getLongitude(), result.upperRight.getLatitude(),
                    result.lowerRight.getLongitude(), result.lowerRight.getLatitude(),
                    result.lowerLeft.getLongitude(), result.lowerLeft.getLatitude(),
                    0, 0,
                    result.sampleResolutionX, 0,
                    result.sampleResolutionX, result.sampleResolutionY,
                    0, result.sampleResolutionY);

            if (geo2img == null) throw new NoninvertibleTransformException("null source geo2img");

            // May throw NoninvertibleTransformException
            img2geo = geo2img.createInverse();

            img = new PointD(0.0d, 0.0d);
            geo = new PointD(0.0d, 0.0d);
        }

        public void query()
        {
            Collection<ElevationSource> sources = new LinkedList<>();
            ElevationSourceManager.getSources(sources);

            // Perform an initial pass against user registered elevation sources clamp so the value is greater than 0.
            final int maxSamples = 150; // todo: make configurable for developers?

            final int ntx = (result.sampleResolutionX - 1) / maxSamples + 1;
            final int nty = (result.sampleResolutionY - 1) / maxSamples + 1;

            final var filter = new ElevationManager.QueryParameters();
            filter.elevationModel = result.elevationModel;

            if (ntx == 1 && nty == 1)
            {
                queryElevationManager(filter);
            } else
            {
                result.numSamples = 0;

                final var hints = new ElevationData.Hints();
                hints.preferSpeed = true;
                hints.interpolate = true;
                hints.resolution = DatasetDescriptor.computeGSD(
                        result.sampleResolutionX, result.sampleResolutionY, result.upperLeft, result.upperRight,
                        result.lowerRight, result.lowerLeft);

                final var samples = new double[maxSamples * maxSamples];
                final List<GeoPoint> points = new ArrayList<>(maxSamples * maxSamples);

                for (int ty = 0; ty < nty; ty++)
                {
                    for (int tx = 0; tx < ntx; tx++)
                    {
                        final int rows = Math.min(maxSamples, result.sampleResolutionY - (ty * maxSamples));
                        final int columns = Math.min(maxSamples, result.sampleResolutionX - (tx * maxSamples));

                        // Build out the points
                        points.clear();

                        for (int y = 0; y < rows; y++)
                        {
                            for (int x = 0; x < columns; x++)
                            {
                                img.x = x + (tx * maxSamples);
                                img.y = y + (ty * maxSamples);
                                img2geo.transform(img, geo);
                                points.add(new GeoPoint(geo.y, geo.x));
                            }
                        }

                        ElevationManager.getElevation(points.iterator(), samples, filter, hints);

                        for (int i = 0; i < (rows * columns); i++)
                        {
                            if (Double.isNaN(samples[i])) continue;

                            final int resultX = (i % columns) + (tx * maxSamples);
                            final int resultY = (i / columns) + (ty * maxSamples);
                            final int resultIdx = (resultY * result.sampleResolutionX) + resultX;
                            final GeoPoint geo = points.get(i);

                            final double altMSL = EGM96.getMSL(geo.getLatitude(), geo.getLongitude(), samples[i]);

                            if (!GeoPoint.isAltitudeValid(altMSL))
                            {
                                result.elevationData[resultIdx] = Float.NaN;
                            } else
                            {
                                result.elevationData[resultIdx] = (float) altMSL;

                                if (result.numSamples == 0)
                                {
                                    result.minElev = result.elevationData[resultIdx];
                                    result.maxElev = result.elevationData[resultIdx];
                                } else
                                {
                                    if (result.elevationData[resultIdx] < result.minElev)
                                    {
                                        result.minElev = result.elevationData[resultIdx];
                                    } else if (result.elevationData[resultIdx] > result.maxElev)
                                    {
                                        result.maxElev = result.elevationData[resultIdx];
                                    }
                                }

                                result.numSamples++;
                            }
                        }
                    }
                }
            }
        }

        protected void queryElevationManager(@Nonnull ElevationManager.QueryParameters filter)
        {
            final int numSamples = result.sampleResolutionX * result.sampleResolutionY;
            final Collection<GeoPoint> points = new LinkedList<>();

            for (int y = 0; y < result.sampleResolutionY; y++)
            {
                for (int x = 0; x < result.sampleResolutionX; x++)
                {
                    img.x = x;
                    img.y = y;
                    img2geo.transform(img, geo);
                    points.add(new GeoPoint(geo.y, geo.x));
                }
            }

            final var hints = new ElevationData.Hints();
            hints.preferSpeed = true;
            hints.interpolate = true;
            hints.resolution = DatasetDescriptor.computeGSD(
                    result.sampleResolutionX, result.sampleResolutionY,
                    result.upperLeft, result.upperRight,
                    result.lowerRight, result.lowerLeft);
            filter.elevationModel = result.elevationModel;

            final var elevationSamples = new double[numSamples];
            ElevationManager.getElevation(points.iterator(), elevationSamples, filter, hints);

            result.numSamples = 0;

            for (int i = 0; i < numSamples; i++)
            {
                if (Double.isNaN(elevationSamples[i])) continue;

                img.x = i % result.sampleResolutionX;
                img.y = i / result.sampleResolutionX;
                img2geo.transform(img, geo);

                final double altMSL = EGM96.getMSL(geo.y, geo.x, elevationSamples[i]);

                if (!GeoPoint.isAltitudeValid(altMSL))
                {
                    result.elevationData[i] = Float.NaN;
                } else
                {
                    result.elevationData[i] = (float) altMSL;

                    if (result.numSamples == 0)
                    {
                        result.minElev = result.elevationData[i];
                        result.maxElev = result.elevationData[i];
                    } else
                    {
                        if (result.elevationData[i] < result.minElev)
                        {
                            result.minElev = result.elevationData[i];
                        } else if (result.elevationData[i] > result.maxElev)
                        {
                            result.maxElev = result.elevationData[i];
                        }
                    }

                    result.numSamples++;
                }
            }
        }
    }

    /**
     * Maintains the heatmap state.
     */
    protected class HeatMapState extends ViewState
    {
        private float alpha;
        private float saturation;
        private float value;
        protected int elevationModel;

        @Override
        public void set(GLMapView view)
        {
            super.set(view);

            alpha = GLHeatMap.this.alpha;
            saturation = GLHeatMap.this.saturation;
            value = GLHeatMap.this.value;
            elevationModel = GLHeatMap.this.elevationModel;
        }

        @Override
        public void copy(ViewState view)
        {
            super.copy(view);

            final var state = (HeatMapState) view;

            alpha = state.alpha;
            saturation = state.saturation;
            value = state.value;
            elevationModel = state.elevationModel;
        }
    }

    /**
     * Holds heatmap configuration data and query results.
     */
    protected static class HeatMapParams
    {
        final GeoPoint upperLeft = GeoPoint.createMutable();
        final GeoPoint upperRight = GeoPoint.createMutable();
        final GeoPoint lowerRight = GeoPoint.createMutable();
        final GeoPoint lowerLeft = GeoPoint.createMutable();

        int drawVersion;
        float[] elevationData;
        @SuppressWarnings("deprecation")
        int elevationModel = ElevationData.MODEL_TERRAIN;
        int[] hsvLut;
        float lutAlpha;
        float lutSaturation;
        float lutValue;
        float maxElev;
        float minElev;
        boolean needsRefresh;
        int numSamples;
        boolean quick;
        byte[] rgbaData;
        int sampleResolutionX;
        int sampleResolutionY;
        boolean valid;
    }
}
