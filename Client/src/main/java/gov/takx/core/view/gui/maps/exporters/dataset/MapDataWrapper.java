package gov.takx.core.view.gui.maps.exporters.dataset;

import com.atakmap.map.elevation.ElevationSource;
import com.atakmap.map.layer.control.Controls;
import com.atakmap.map.layer.raster.DatasetDescriptor;
import com.atakmap.map.layer.raster.DatasetRasterLayer2;
import com.atakmap.map.layer.raster.RasterDataStore;
import com.atakmap.map.layer.raster.tilematrix.TileClient;
import com.atakmap.map.layer.raster.tilematrix.TileClientFactory;
import com.atakmap.map.layer.raster.tilematrix.TileClientSpi;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.core.globe.layers.GlobeLayer;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Wrapper around an underlying image layer or elevation model.
 *
 * @since 1.23
 */
public class MapDataWrapper
{
    public static final String SELECTED_PROPERTY = "Selection";
    public static final String MIN_ACCEPTABLE_LEVEL_PROPERTY = "MinAcceptableLevel";
    public static final String MAX_ACCEPTABLE_LEVEL_PROPERTY = "MaxAcceptableLevel";
    public static final String TILE_DATA_SIZE = "TileDataSize";
    public static final String TOTAL_TILE_COUNT = "TotalTileCount";
    public static final String TOTAL_ACCEPTABLE_TILE_COUNT = "TotalAcceptableTileCount";

    /**
     * Key used to retrieve an imagery layer's array of {@link DatasetDescriptor}
     * @since 5.0
     */
    public static final String DATASET_DESCRIPTORS = "DatasetDescriptors";
    /**
     * Key used to retrieve an elevation layer's {@link com.atakmap.map.elevation.ElevationSource}
     * @since 5.6
     */
    public static final String ELEVATION_SOURCE_KEY = "ElevationSource";

    // A very rough tile data size estimate. The resulting size of the output is dependent on the TileClient and is difficult to predict depending on factors such as compression rate and database structure.
    private static final long TILE_DATA_SIZE_ESTIMATE = 256 * 256 * 4;

    public enum PreviewState
    {
        Available,
        NotAvailable,
        Retrieving,
        Error
    }

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    /**
     * The unique key for the cache.
     */
    private final String key;

    /**
     * The display name of the wrapped data object.
     */
    private final String name;

    /**
     * The elevation source wrapped by this object.
     * @since 5.6
     */
    private final ElevationSource elevationSource;

    /**
     * Indicates if this cache has been selected by the user.
     */
    private boolean selected;

    private final DatasetDescriptor[] datasetDescriptors;

    /**
     * The total estimated tile count. The value is typically updated from another thread as the tile count calculation may take a
     * while.
     */
    private Long totalTileCount = null;

    /**
     * The estimated tile count for the acceptable sub range.
     */
    private Long totalAcceptableTileCount = null;

    /**
     * The approx individual tile data size for this data source.
     */
    private long tileDataSize = TILE_DATA_SIZE_ESTIMATE;

    /**
     * The minimum available resolution level for the specified sector.
     */
    private int minAvailableResolutionLevel;

    /**
     * The max available resolution level for the specified sector.
     */
    private int maxAvailableResolutionLevel;

    private Integer minAcceptableLevel;
    private Integer maxAcceptableLevel;

    private Double minAcceptableResolutionLevel;
    private Double maxAcceptableResolutionLevel;
    private Boundary boundary;

    private PreviewState previewState = PreviewState.NotAvailable;
    private BufferedImage preview;

    private boolean sizeManaged;

    /**
     * Construct a MapDataWrapper for the given elevation source. The source name will be used to construct the wrapper key.
     *
     * @param elevationSource Elevation source to process
     * @return MapDataWrapper
     * @since 5.6
     */
    static MapDataWrapper forElevationSource(ElevationSource elevationSource)
    {
        final String displayName = elevationSource.getName();
        return new MapDataWrapper(ELEVATION_SOURCE_KEY + displayName, displayName, null, elevationSource);
    }

    /**
     * Construct a MapDataWrapper for the given {@link GlobeLayer}. The raptor layer key will be used as the wrapper key.
     *
     * @param globeLayer globe layer to process
     * @return MapDataWrapper
     * @since 5.6
     */
    static MapDataWrapper forImageGlobeLayer(GlobeLayer globeLayer)
    {
        List<DatasetDescriptor> datasetDescriptors = new ArrayList<>();
        if (globeLayer.getTakLayer() instanceof DatasetRasterLayer2 datasetRasterLayer)
        {
            RasterDataStore.DatasetQueryParameters params = new RasterDataStore.DatasetQueryParameters();
            params.order = List.of(RasterDataStore.DatasetQueryParameters.GSD.INSTANCE);
            try (var cursor = datasetRasterLayer.getDataStore().queryDatasets(params))
            {
                while (cursor.moveToNext())
                {
                    datasetDescriptors.add(cursor.get());
                }
            }
        }
        IRaptorLayer raptorLayer = globeLayer.getRaptorLayer();
        return new MapDataWrapper(raptorLayer.getKey(), raptorLayer.getDisplayName(), datasetDescriptors.toArray(DatasetDescriptor[]::new), null);
    }

    /**
     * Constructor.
     *
     * @param key                The retrievable key
     * @param name               The display name for the retrievable
     * @param datasetDescriptors The dataset descriptors
     * @param elevationSource    The elevation source
     */
    private MapDataWrapper(String key, String name, @Nullable DatasetDescriptor[] datasetDescriptors, @Nullable ElevationSource elevationSource)
    {
        this.elevationSource = elevationSource;
        this.key = key;
        this.name = name;
        this.datasetDescriptors = datasetDescriptors;
    }

    /**
     * Attach a listener to receive property change events for this object.
     *
     * @param listener New listener.
     */
    public void addPropertyChangeListener(@Nonnull PropertyChangeListener listener)
    {
        pcs.addPropertyChangeListener(listener);
    }

    /**
     * Remove  property change listener.
     *
     * @param listener Listener to remove.
     */
    public void removePropertyChangeListener(@Nonnull PropertyChangeListener listener)
    {
        pcs.removePropertyChangeListener(listener);
    }

    /**
     * @return actual name of the underlying elevation model or imagery layer.
     */
    public String getCacheName()
    {
        return name;
    }

    /**
     * Set the underlying elevation model or imagery layer as flagged for download.
     *
     * @param selected {@code true} to flag this cache to download data from, {@code false} to not download
     *                 the data from this cache
     */
    public void setSelected(boolean selected)
    {
        boolean oldValue = this.selected;
        this.selected = selected;
        pcs.firePropertyChange(SELECTED_PROPERTY, oldValue, selected);
    }

    /**
     * @return unique key for the underlying elevation model or imagery layer
     */
    public String getKey()
    {
        return key;
    }

    /**
     * @return {@code true} if this cache wrapper has been selected for download {@code false} otherwise
     */
    public boolean isSelected()
    {
        return selected;
    }

    /**
     * Return the cache name.
     *
     * @return cache name
     */
    @Override
    public String toString()
    {
        return name;
    }

    /**
     * Indicates the approximate tile data size.
     *
     * @return the approximate tile data size.
     * @since 5.7
     */
    public long getTileDataSize()
    {
        return tileDataSize;
    }

    /**
     * Specify the approximate tile data size.
     *
     * @param tileDataSize The approximate tile data size.
     * @since 5.7
     */
    public void setTileDataSize(long tileDataSize)
    {
        long oldValue = this.tileDataSize;
        this.tileDataSize = tileDataSize;
        pcs.firePropertyChange(TILE_DATA_SIZE, oldValue, tileDataSize);
    }

    /**
     * Indicates the approximate total available tile count for the entire data set.
     *
     * @return the approximate tile count, or {@code null} if unknown
     * @since 5.7
     */
    @Nullable
    public Long getTotalTileCount()
    {
        return totalTileCount;
    }

    /**
     * Specify the approximate total available tile count for the entire data set.
     *
     * @param totalTileCount the approximate tile count, or {@code null} if unknown
     * @since 5.7
     */
    public void setTotalTileCount(@Nullable Long totalTileCount)
    {
        Long oldValue = this.totalTileCount;
        this.totalTileCount = totalTileCount;
        pcs.firePropertyChange(TOTAL_TILE_COUNT, oldValue, totalTileCount);
    }

    /**
     * Indicates the approximate total available tile count for the acceptable sub range.
     *
     * @return the approximate tile count for the acceptable sub range, or {@code null} if not set
     * @since 5.7
     */
    public @Nullable Long getTotalAcceptableTileCount()
    {
        return totalAcceptableTileCount;
    }

    /**
     * Specify the approximate total available tile count for the acceptable sub range.
     *
     * @param totalAcceptableTileCount the approximate tile count for the acceptable sub range, or {@code null} if not set
     * @since 5.7
     */
    public void setTotalAcceptableTileCount(@Nullable Long totalAcceptableTileCount)
    {
        Long oldValue = this.totalAcceptableTileCount;
        this.totalAcceptableTileCount = totalAcceptableTileCount;
        pcs.firePropertyChange(TOTAL_ACCEPTABLE_TILE_COUNT, oldValue, totalAcceptableTileCount);
    }

    public int getMinAvailableResolutionLevel()
    {
        return minAvailableResolutionLevel;
    }

    public int getMaxAvailableResolutionLevel()
    {
        return maxAvailableResolutionLevel;
    }

    /**
     * Get the elevation source for this wrapper, or {@code null} if there is none.
     * @return the elevation source, or {@code null}
     * @since 5.6
     */
    @Nullable
    public ElevationSource getElevationSource()
    {
        return elevationSource;
    }

    /**
     * Gets a {@link TileClientWrapper} if the MapDataWrapper's underlying source has a {@link TileClient} associated with it.
     * @return the {@link TileClientWrapper} or {@code null} if the source does not have one
     * @since 5.7
     */
    @Nullable
    TileClientWrapper getTileClient()
    {
        if (datasetDescriptors != null)
        {
            for (DatasetDescriptor datasetDescriptor : datasetDescriptors)
            {
                TileClient tileClient = TileClientFactory.create(datasetDescriptor.getUri(), null, TileClientSpi.Options.defaults);
                if (tileClient != null) return new TileClientWrapper(tileClient, true);
            }
        } else if (elevationSource != null)
        {
            if (elevationSource instanceof Controls controls)
            {
                TileClient tileClient = controls.getControl(TileClient.class);
                if (tileClient != null) return new TileClientWrapper(tileClient, false);
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        return Objects.equals(elevationSource, ((MapDataWrapper) o).elevationSource) && Arrays.equals(datasetDescriptors, ((MapDataWrapper) o).datasetDescriptors);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(elevationSource, Arrays.hashCode(datasetDescriptors));
    }

    public void setMinAvailableResolutionLevel(int minLevel)
    {
        minAvailableResolutionLevel = minLevel;
    }

    public void setMaxAvailableResolutionLevel(int maxLevel)
    {
        maxAvailableResolutionLevel = maxLevel;
    }

    public Integer getMaxAcceptableLevel()
    {
        return maxAcceptableLevel;
    }

    public void setMaxAcceptableLevel(Integer maxAcceptableLevel)
    {
        Integer oldValue = this.maxAcceptableLevel;
        this.maxAcceptableLevel = maxAcceptableLevel;
        pcs.firePropertyChange(MAX_ACCEPTABLE_LEVEL_PROPERTY, oldValue, maxAcceptableLevel);
    }

    public Integer getMinAcceptableLevel()
    {
        return minAcceptableLevel;
    }

    public void setMinAcceptableLevel(Integer minAcceptableLevel)
    {
        Integer oldValue = this.minAcceptableLevel;
        this.minAcceptableLevel = minAcceptableLevel;
        pcs.firePropertyChange(MIN_ACCEPTABLE_LEVEL_PROPERTY, oldValue, minAcceptableLevel);
    }

    public void setMinAcceptableResolutionLevel(double minAcceptableResolutionLevel)
    {
        this.minAcceptableResolutionLevel = minAcceptableResolutionLevel;
    }

    public void setMaxAcceptableResolutionLevel(double maxAcceptableResolutionLevel)
    {
        this.maxAcceptableResolutionLevel = maxAcceptableResolutionLevel;
    }

    public Double getMaxAcceptableResolutionLevel()
    {
        return maxAcceptableResolutionLevel;
    }

    public Double getMinAcceptableResolutionLevel()
    {
        return minAcceptableResolutionLevel;
    }

    /**
     * @return
     * @since 1.43
     */
    public boolean isSizeManaged()
    {
        return sizeManaged;
    }

    /**
     * @param sizeManaged
     * @since 1.43
     */
    public void setSizeManaged(boolean sizeManaged)
    {
        this.sizeManaged = sizeManaged;
    }

    /**
     * Get the boundary for the map data.
     *
     * @return the boundary or null if not set
     * @since 5.0
     */
    @Nullable
    public Boundary getBoundary()
    {
        return boundary;
    }

    /**
     * Set the boundary for the map data.
     *
     * @param boundary the boundary or null to unset
     * @since 5.0
     */
    public void setBoundary(@Nullable Boundary boundary)
    {
        this.boundary = boundary;
    }

    /**
     * Get the dataset descriptors.
     * @return the dataset descriptors
     * @since 5.0
     */
    @Nullable
    public DatasetDescriptor[] getDatasetDescriptors()
    {
        return datasetDescriptors;
    }

    /**
     * Returns a preview image of this layer. The layer preview may not always be available. Call {@link
     * #getPreviewState()} to determine if a preview is available.
     *
     * @return A preview image. Returns {@code null} if the preview is not available.
     * @since 1.43
     */
    public BufferedImage getPreview()
    {
        return preview;
    }

    /**
     * Specifies a preview image for this layer.
     *
     * @param preview New preview image. May be {@code null}.
     * @since 1.43
     */
    public void setPreview(BufferedImage preview)
    {
        this.preview = preview;
    }

    /**
     * Specify the status of the preview image for this layer.
     *
     * @param state Status of the preview.
     * @since 1.43
     */
    public void setPreviewState(PreviewState state)
    {
        previewState = state;
    }

    /**
     * Indicates the status of the layer preview.
     *
     * @return The status of the preview.
     * @since 1.43
     */
    public PreviewState getPreviewState()
    {
        return previewState;
    }

    /**
     * A {@link AutoCloseable} wrapper for a {@link TileClient} that can optionally call {@link TileClient#dispose()} if the {@link TileClient} is owned by the caller.
     *
     * @param tileClient the tile client
     * @param closeable {@code true} if the TileClient should be disposed, {@code false} if otherwise
     * @since 5.7
     */
    record TileClientWrapper(@Nonnull TileClient tileClient, boolean closeable) implements AutoCloseable
    {
        @Override
        public void close()
        {
            if (closeable) tileClient.dispose();
        }
    }
}
