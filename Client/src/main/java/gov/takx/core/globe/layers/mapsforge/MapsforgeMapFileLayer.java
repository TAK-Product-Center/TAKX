package gov.takx.core.globe.layers.mapsforge;

import com.jogamp.opengl.util.texture.TextureData;
import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.cache.BasicMemoryCache;
import gov.nasa.worldwind.cache.MemoryCache;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.layers.mercator.MercatorSector;
import gov.nasa.worldwind.layers.mercator.MercatorTextureTile;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.LevelSet;
import gov.nasa.worldwind.util.OGLUtil;
import gov.nasa.worldwind.util.WWIO;
import gov.takx.api.layer.IRaptorLayer;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.awt.graphics.AwtGraphicFactory;
import org.mapsforge.map.layer.cache.InMemoryTileCache;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.renderer.DatabaseRenderer;
import org.mapsforge.map.layer.renderer.RendererJob;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.reader.header.MapFileInfo;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderThemeMenuCallback;
import org.mapsforge.map.rendertheme.rule.RenderThemeFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * A tiled image layer that renders tiles generated from a Mapsforge .map file.
 *
 * @since 1.35
 */
public class MapsforgeMapFileLayer extends AbstractMapsforgeTiledImageLayer
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final DisplayModel DISPLAY_MODEL = new DisplayModel();
    private static final GraphicFactory GRAPHIC_FACTORY = AwtGraphicFactory.INSTANCE;
    private static final float TEXT_SCALE = 1.0f;

    // Maps from level 0 in WW to the initial level in the map file
    private static final int LEVEL_SHIFT = 4;

    private final Object fileLock = new Object();

    private final MapDataStoreInfo mapDataStoreInfo;
    private final DatabaseRenderer databaseRenderer;
    private final RenderThemeFuture renderThemeFuture;

    /**
     * Construct a map file layer on the given file.  Package protected, should only be called from
     * {@link MapsForgeLayerFactory#makeGlobeLayer(IRaptorLayer, java.lang.String)}.
     *
     * @param mapFile Map file to ingest
     */
    static MapsforgeMapFileLayer makeLayer(File mapFile)
    {
        final MapDataStoreInfo info = new MapDataStoreInfo(mapFile);

        return new MapsforgeMapFileLayer(info, makeParameters(info));
    }

    /**
     * Create our construction parameters from the data in the map file database.
     *
     * @param mapDataStoreInfo Database info from map file
     * @return construction parameters
     */
    private static AVList makeParameters(MapDataStoreInfo mapDataStoreInfo)
    {
        final MapFileInfo mapFileInfo = mapDataStoreInfo.mapDataStore.getMapFileInfo();
        final String mapFileName = mapDataStoreInfo.mapFile.getName();

        final AVList params = new AVListImpl();

        params.setValue(AVKey.TILE_WIDTH, mapFileInfo.tilePixelSize);
        params.setValue(AVKey.TILE_HEIGHT, mapFileInfo.tilePixelSize);
        params.setValue(AVKey.DATA_CACHE_NAME, "Earth/MapsforgeMap/" + mapFileName);
        params.setValue(AVKey.SERVICE, "http://bogus-service/");
        params.setValue(AVKey.DATASET_NAME, "map");
        params.setValue(AVKey.FORMAT_SUFFIX, ".png");
        params.setValue(AVKey.NUM_LEVELS, 23 - LEVEL_SHIFT); // 22 is the max level
        params.setValue(AVKey.NUM_EMPTY_LEVELS, 0);
        final int nTiles = 1 << LEVEL_SHIFT;
        params.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, LatLon.fromDegrees(180.0 / nTiles, 360.0 / nTiles));

        // Determine the bounding box as a MercatorSector
        final BoundingBox bbox = mapFileInfo.boundingBox;

        final Sector boundingSector = new Sector(
                Angle.fromDegrees(bbox.minLatitude),
                Angle.fromDegrees(bbox.maxLatitude),
                Angle.fromDegrees(bbox.minLongitude),
                Angle.fromDegrees(bbox.maxLongitude));

        final MercatorSector mercatorSector = MercatorSector.fromSector(boundingSector);

        if (logger.isDebugEnabled())
        {
            logger.debug("Bounding    box: {}", bbox);
            logger.debug("Bounding sector: {}", boundingSector);
            logger.debug("Bounding     MS: {}", mercatorSector);
        }

        params.setValue(AVKey.SECTOR, mercatorSector);

        return params;
    }

    /**
     * @return Our restorable state is just the name of our map file in VFS
     */
    @Override
    public String getRestorableState()
    {
        return mapDataStoreInfo.mapFile.getName();
    }

    /**
     * Internal constructor.
     *
     * @param mapDataStoreInfo Map file database info
     * @param params           Construction parameters (derived from map file)
     */
    private MapsforgeMapFileLayer(MapDataStoreInfo mapDataStoreInfo, AVList params)
    {
        super(new LevelSet(params));

        // Make a copy of our construction parameters so other areas can access data like our bounding Sector

        this.setValue(AVKey.CONSTRUCTION_PARAMETERS, params.copy());

        // Set up our memory cache
        if (!WorldWind.getMemoryCacheSet().containsCache(MercatorTextureTile.class.getName()))
        {
            long size = Configuration.getLongValue(AVKey.TEXTURE_IMAGE_CACHE_SIZE, 3000000L);
            MemoryCache cache = new BasicMemoryCache((long) (0.85 * size), size);
            cache.setName("Texture Tiles");
            WorldWind.getMemoryCacheSet().addCache(MercatorTextureTile.class.getName(), cache);
        }

        setUseTransparentTextures(true);
        setSplitScale(1.4);  // This helps make the images crisper

        this.mapDataStoreInfo = mapDataStoreInfo;

        TileCache tileCache = new InMemoryTileCache(16); // in-memory cache alone is sufficient, WW already performs tile caching on the rendered tiles
        databaseRenderer = new DatabaseRenderer(mapDataStoreInfo.mapDataStore, GRAPHIC_FACTORY, tileCache, null, true, false, null);
        renderThemeFuture = new RenderThemeFuture(GRAPHIC_FACTORY, new MapFileLayerXmlRenderTheme(), DISPLAY_MODEL);

        new Thread(renderThemeFuture).start();
    }

    @Override
    public void dispose()
    {
        super.dispose();
        renderThemeFuture.decrementRefCount();
    }

    @Override
    protected void forceTextureLoad(MercatorTextureTile tile)
    {
        final URL textureURL = this.getDataFileStore().findFile(tile.getPath(), true);

        if (textureURL != null)
        {
            this.loadTexture(tile, textureURL);
        }
    }

    @Override
    protected void requestTexture(DrawContext dc, MercatorTextureTile tile)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("requestTexture: {}/{}/{}  path={}",
                    tile.getLevel().getLevelNumber(), tile.getColumn(), tile.getRow(), tile.getPath());
        }

        final Vec4 centroid = tile.getCentroidPoint(dc.getGlobe());
        final Vec4 referencePoint = getReferencePoint();

        if (referencePoint != null)
        {
            tile.setPriority(centroid.distanceTo3(referencePoint));
        }

        RequestTask task = new RequestTask(tile);
        this.getRequestQ().add(task);
    }

    private boolean loadTexture(MercatorTextureTile tile, java.net.URL textureURL)
    {
        TextureData textureData;

        synchronized (this.fileLock)
        {
            textureData = readTexture(textureURL, this.isUseMipMaps());
        }

        if (textureData == null)
        {
            return false;
        }

        tile.setTextureData(textureData);
        if (tile.getLevelNumber() != 0 || !this.isRetainLevelZeroTiles())
        {
            this.addTileToCache(tile);
        }

        return true;
    }

    private static TextureData readTexture(java.net.URL url, boolean useMipMaps)
    {
        try
        {
            return OGLUtil.newTextureData(Configuration.getMaxCompatibleGLProfile(), url, useMipMaps);
        } catch (Exception e)
        {
            logger.error("Exception attempting to read texture file {}", url, e);
            return null;
        }
    }

    private void addTileToCache(MercatorTextureTile tile)
    {
        WorldWind.getMemoryCache(MercatorTextureTile.class.getName()).add(tile.getTileKey(), tile);
    }

    /**
     * Generate the actual image content for the requested tile.  This uses the mapsforge renderer to generate
     * the image.
     *
     * @param tile The WW tile to process
     * @return buffer of bytes for the image
     * @throws IOException If conversion to PNG fails
     */
    @Nullable
    private ByteBuffer createImage(@Nonnull MercatorTextureTile tile) throws IOException
    {
        // Construct a Mapsforge Tile from the requested Mercator tile (convert from WW tile indices to mapsforge)
        //
        // 1. W level 0 equates to LEVEL_SHIFT in mapsforge
        // 2. Y axis tile indices run bottom to top in WW, but top to bottom in mapsforge

        int level = tile.getLevelNumber() + LEVEL_SHIFT;
        int x = tile.getColumn();
        int y = (1 << level) - 1 - tile.getRow();
        if (y < 0) return null;

        final ByteArrayOutputStream os = new ByteArrayOutputStream(256 * 256);

        // The database renderer is not thread safe, so synchronize here
        synchronized (databaseRenderer)
        {
            Tile mfTile = new Tile(x, y, (byte) level, DISPLAY_MODEL.getTileSize());
            if (logger.isDebugEnabled()) logger.debug("Generate tile: {}", mfTile);

            final Bitmap bitmap = databaseRenderer.executeJob(new RendererJob(mfTile, mapDataStoreInfo.mapDataStore, renderThemeFuture, DISPLAY_MODEL, TEXT_SCALE, true, false));
            bitmap.compress(os); // Convert to PNG
        }

        final ByteBuffer buffer = ByteBuffer.allocate(os.size());
        buffer.put(os.toByteArray());
        return buffer;
    }

    /**
     * Simple composite to hold the map file and the map database handling the data in the file.
     */
    private static class MapDataStoreInfo
    {
        final File mapFile;
        final MapFile mapDataStore;

        MapDataStoreInfo(File mapFile)
        {
            this.mapFile = mapFile;
            mapDataStore = new MapFile(mapFile);
        }
    }

    /**
     * Instances of this class are used to request a tile from the data store.  The texture data will either be
     * loaded from the store or a new download task will be added to the queue to generate the needed tile data.
     */
    private class RequestTask extends TileTask
    {
        private RequestTask(MercatorTextureTile tile)
        {
            super(tile);
        }

        public void run()
        {
            final MercatorTextureTile tile = getTile();

            // TODO: check to ensure load is still needed

            final java.net.URL textureURL = WorldWind.getDataFileStore().findFile(tile.getPath(), false);
            if (textureURL != null)
            {
                if (loadTexture(tile, textureURL))
                {
                    getLevels().unmarkResourceAbsent(tile);
                    firePropertyChange(AVKey.LAYER, null, this);
                    return;
                } else
                {
                    // Assume that something's wrong with the file and delete it.
                    WorldWind.getDataFileStore().removeFile(textureURL);
                    getLevels().markResourceAbsent(tile);
                    logger.info("Deleted corrupted data file {}", textureURL);
                }
            }

            downloadTexture(tile);
        }

        /**
         * Initiate a "download" of the given tile.
         *
         * @param tile Tile to download
         */
        private void downloadTexture(final MercatorTextureTile tile)
        {
            DownloadTask task = new DownloadTask(tile);
            getRequestQ().add(task);
        }
    }

    /**
     * Instances of this class are used to hold the "download" task for a given tile.  In our case, we don't actually
     * download anything, we generate the tile image from the map database.
     */
    private class DownloadTask extends TileTask
    {
        private DownloadTask(MercatorTextureTile tile)
        {
            super(tile);
        }

        public void run()
        {
            final MercatorTextureTile tile = getTile();
            try
            {
                ByteBuffer buffer = createImage(tile);
                if (buffer == null)
                {
                    getLevels().markResourceAbsent(tile);
                    return;
                }
                final File outFile = WorldWind.getDataFileStore().newFile(tile.getPath());
                if (outFile != null)
                {
                    saveBuffer(buffer, outFile);
                }
            } catch (Exception e)
            {
                logger.error("Exception attempting to create Tile Image", e);
                getLevels().markResourceAbsent(tile);
            }
        }

        /**
         * Save the given byte buffer tot he specified file.  This places the file into the WW data cache.
         *
         * @param buffer  Buffer to write out
         * @param outFile Where to write it
         * @throws IOException If the write fails
         */
        private void saveBuffer(ByteBuffer buffer, File outFile) throws IOException
        {
            synchronized (fileLock) // sychronized with read of file in RequestTask.run()
            {
                WWIO.saveBuffer(buffer, outFile);
            }
        }
    }

    /**
     * Base class for tasks that are based upon a tile (using the tile for equality).
     */
    private abstract static class TileTask implements Runnable, Comparable<TileTask>
    {
        private final MercatorTextureTile tile;

        private TileTask(MercatorTextureTile tile)
        {
            this.tile = tile;
        }

        public final MercatorTextureTile getTile()
        {
            return this.tile;
        }

        public void run()
        {
        }

        /**
         * @param that the task to compare
         * @return -1 if {@code this} less than {@code that}, 1 if greater than, 0 if equal
         */
        public int compareTo(@Nonnull TileTask that)
        {
            final double thisPriority = this.tile.getPriority();
            final double thatPriority = that.tile.getPriority();

            return Double.compare(thisPriority, thatPriority);
        }

        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (o == null || getClass() != o.getClass())
            {
                return false;
            }

            final TileTask that = (TileTask) o;

            // Don't include layer in comparison so that requests are shared among layers
            return Objects.equals(tile, that.tile);
        }

        public int hashCode()
        {
            return (tile != null ? tile.hashCode() : 0);
        }

        public String toString()
        {
            return this.tile.getPath();
        }
    }

    /**
     * @since 3.4 (renamed from RaptorXmlRenderTheme)
     */
    private static final class MapFileLayerXmlRenderTheme implements XmlRenderTheme
    {
        @Override
        public XmlRenderThemeMenuCallback getMenuCallback()
        {
            return InternalRenderTheme.OSMARENDER.getMenuCallback();
        }

        @Override
        public String getRelativePathPrefix()
        {
            return InternalRenderTheme.OSMARENDER.getRelativePathPrefix();
        }

        @Override
        public InputStream getRenderThemeAsStream()
        {
            return getClass().getResourceAsStream("/osmarender/RX_osmarender.xml");
        }

        @Override
        public void setMenuCallback(XmlRenderThemeMenuCallback menuCallback)
        {
            InternalRenderTheme.OSMARENDER.setMenuCallback(menuCallback);
        }
    }
}
