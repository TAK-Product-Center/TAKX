package gov.takx.core.view.gui.maps.exporters.dataset;

import android.graphics.Bitmap;
import com.atakmap.map.contentservices.CacheRequest;
import com.atakmap.map.contentservices.CacheRequestListener;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.feature.geometry.GeometryFactory;
import com.atakmap.map.layer.raster.osm.OSMUtils;
import com.atakmap.map.layer.raster.tilematrix.TileClient;
import com.atakmap.map.layer.raster.tilematrix.TileContainer;
import com.atakmap.map.layer.raster.tilematrix.TileScraper;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.ImageUtil;
import gov.tak.api.commons.graphics.BitmapFactory;
import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.api.engine.map.coords.GeoPoint;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.engine.map.coords.IProjection;
import gov.tak.api.engine.math.PointD;
import gov.tak.platform.engine.map.coords.ProjectionFactory;
import gov.tak.platform.marshal.MarshalManager;
import gov.takx.api.coordinatesystem.ILocation;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to create a preview image for an imagery layer.
 *
 * @since 5.6
 */
public class PreviewUtilities
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public enum Status
    {
        SUCCESS,
        NOT_AVAILABLE,
        ERROR
    }

    /**
     * Create an imagery layer preview.
     *
     * @param tileClient the tile client for the imagery layer
     * @param width the width of the result image in pixels
     * @param height the height of the result image in pixels
     * @param level the OSM level to retrieve the imagery from
     * @param envelope the area to preview
     * @return the result of the operation
     * @since 5.7
     */
    public static PreviewResult createImageryPreview(TileClient tileClient, int width, int height, int level, Envelope envelope)
    {
        IProjection projection = ProjectionFactory.getProjection(tileClient.getSRID());
        if (projection == null) return new PreviewResult(null, Status.NOT_AVAILABLE, 0, null);
        try
        {
            CacheRequest request = new CacheRequest();
            double resolution = OSMUtils.mapnikTileResolution(level);
            request.maxResolution = resolution;
            request.minResolution = resolution;
            request.region = GeometryFactory.fromEnvelope(envelope);
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
            MyTileContainer sink = new MyTileContainer(tileClient, projection, envelope, image);
            MyCacheRequestListener requestListener = new MyCacheRequestListener(tileClient.getName());
            TileScraper tileScraper = new TileScraper(tileClient, sink, request, requestListener);
            tileScraper.run();
            if (requestListener.error)
            {
                return new PreviewResult(null, Status.ERROR, 0, requestListener.throwable);
            } else
            {
                if (sink.imageWritten)
                {
                    return new PreviewResult(image, Status.SUCCESS, (int) sink.dataSizes.stream().mapToInt(Integer::valueOf).average().orElse(0), null);
                } else
                {
                    return new PreviewResult(null, Status.NOT_AVAILABLE, 0, null);
                }
            }
        } catch (Exception e)
        {
            logger.warn("Exception creating preview for layer {}", tileClient.getName(), e);
            return new PreviewResult(null, Status.ERROR, 0, e);
        }
    }

    /**
     * The result of an imagery preview request.
     *
     * @param image the resulting image, or {@code null} if not successful
     * @param status the result status
     * @param dataSize the average tile data size that compose the preview image
     * @param error an error encountered during the operation, or {@code null}
     */
    public record PreviewResult(@Nullable BufferedImage image, Status status, int dataSize, @Nullable Throwable error)
    {
    }

    /**
     * Create an {@link Envelope} representative of an area for preview.
     *
     * @param previewLocation the preview location
     * @param resolution the resolution of the imagery in meters per pixel
     * @param width the width of the preview area in pixels
     * @param height the height of the preview area in pixels
     * @return the representative envelope
     */
    public static Envelope computePreviewEnvelope(ILocation previewLocation, double resolution, int width, int height)
    {
        Envelope.Builder builder = new Envelope.Builder();
        IGeoPoint north = GeoCalculations.pointAtDistance(previewLocation, 0, resolution * height);
        IGeoPoint south = GeoCalculations.pointAtDistance(previewLocation, 180, resolution * height);
        IGeoPoint east = GeoCalculations.pointAtDistance(previewLocation, 270, resolution * width);
        IGeoPoint west = GeoCalculations.pointAtDistance(previewLocation, 90, resolution * width);
        builder.add(north.getLongitude(), north.getLatitude());
        builder.add(south.getLongitude(), south.getLatitude());
        builder.add(east.getLongitude(), east.getLatitude());
        builder.add(west.getLongitude(), west.getLatitude());
        return builder.build();
    }

    private static class MyTileContainer implements TileContainer
    {
        private final TileClient tileClient;
        private final IProjection projection;
        private final Envelope envelope;
        private final BufferedImage image;
        private final double aspectRatio;
        private boolean imageWritten;
        private final List<Integer> dataSizes = new ArrayList<>();

        public MyTileContainer(TileClient tileClient, IProjection projection, Envelope envelope, BufferedImage image)
        {
            this.tileClient = tileClient;
            this.projection = projection;
            this.envelope = envelope;
            this.image = image;
            this.aspectRatio = (double) image.getWidth() / image.getHeight();
        }

        @Override
        public boolean isReadOnly()
        {
            return false;
        }

        @Override
        public synchronized void setTile(int level, int x, int y, byte[] data, long expiration)
        {
            Envelope tileBounds = Util.getTileBounds(tileClient, level, x, y);
            GeoPoint lowerLeft = GeoPoint.createMutable();
            GeoPoint upperRight = GeoPoint.createMutable();
            if (!projection.inverse(new PointD(tileBounds.minX, tileBounds.minY, 0), lowerLeft)) return;
            if (!projection.inverse(new PointD(tileBounds.maxX, tileBounds.maxY, 0), upperRight)) return;
            try
            {
                gov.tak.api.commons.graphics.Bitmap bitmap = BitmapFactory.decodeStream(new ByteArrayInputStream(data));
                if (bitmap == null)
                {
                    logger.warn("Exception creating preview for layer {} tile x,y,z {},{},{}", tileClient.getName(), x, y, level);
                    return;
                }
                BufferedImage tileImage = MarshalManager.marshal(bitmap, gov.tak.api.commons.graphics.Bitmap.class, BufferedImage.class);
                if (tileImage == null)
                {
                    logger.warn("Exception creating preview for layer {} tile x,y,z {},{},{}", tileClient.getName(), x, y, level);
                    return;
                }
                Sector imageSector = Sector.fromDegrees(lowerLeft.getLatitude(), upperRight.getLatitude(), lowerLeft.getLongitude(), upperRight.getLongitude());
                Sector previewSector = Sector.fromDegrees(envelope.minY, envelope.maxY, envelope.minX, envelope.maxX);
                ImageUtil.mergeImage(previewSector, imageSector, aspectRatio, tileImage, this.image);
                dataSizes.add(data.length);
                imageWritten = true;
            } catch (Exception e)
            {
                logger.warn("Exception creating preview for layer {} tile x,y,z {},{},{}", tileClient.getName(), x, y, level, e);
            }
        }

        @Override
        public void setTile(int level, int x, int y, Bitmap data, long expiration)
        {
        }

        @Override
        public boolean hasTileExpirationMetadata()
        {
            return false;
        }

        @Override
        public long getTileExpiration(int level, int x, int y)
        {
            return 0;
        }

        @Override
        public String getName()
        {
            return tileClient.getName();
        }

        @Override
        public int getSRID()
        {
            return tileClient.getSRID();
        }

        @Override
        public ZoomLevel[] getZoomLevel()
        {
            return tileClient.getZoomLevel();
        }

        @Override
        public double getOriginX()
        {
            return tileClient.getOriginX();
        }

        @Override
        public double getOriginY()
        {
            return tileClient.getOriginY();
        }

        @Override
        public Bitmap getTile(int zoom, int x, int y, Throwable[] error)
        {
            return tileClient.getTile(zoom, x, y, error);
        }

        @Override
        public byte[] getTileData(int zoom, int x, int y, Throwable[] error)
        {
            return tileClient.getTileData(zoom, x, y, error);
        }

        @Override
        public Envelope getBounds()
        {
            return tileClient.getBounds();
        }

        @Override
        public void dispose()
        {
        }
    }

    private static class MyCacheRequestListener implements CacheRequestListener
    {
        private final String name;
        private boolean error;
        private Throwable throwable;

        public MyCacheRequestListener(String name)
        {
            this.name = name;
        }

        @Override
        public void onRequestStarted()
        {
        }

        @Override
        public void onRequestComplete()
        {
        }

        @Override
        public void onRequestProgress(int taskNum, int numTasks, int taskProgress, int maxTaskProgress, int totalProgress, int maxTotalProgress)
        {
        }

        @Override
        public boolean onRequestError(@Nullable Throwable t, @Nullable String message, boolean fatal)
        {
            String errorMessage = message != null ? message : "";
            logger.warn("Exception creating preview for layer {}: cause: {}", name, errorMessage, t);
            error = true;
            throwable = t;
            return false;
        }

        @Override
        public void onRequestCanceled()
        {
            error = true;
        }
    }
}
