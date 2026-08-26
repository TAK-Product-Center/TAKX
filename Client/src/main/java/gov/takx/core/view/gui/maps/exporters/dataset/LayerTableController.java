package gov.takx.core.view.gui.maps.exporters.dataset;

import gov.nasa.worldwind.geom.LatLon;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.platform.utils.WWUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

/**
 * Manages the layers in a table, updating the estimated size for each layer whenever the selected boundaries change.
 *
 * @since 1.43 (renamed in 5.7)
 */
@PseudoBean
public class LayerTableController implements PropertyChangeListener
{
    private static final int PREVIEW_WIDTH = 128;
    private static final int PREVIEW_HEIGHT = 80;

    private final LayerTableModel model;
    private final Map<String, Future<?>> sizeEstimateTasks = new ConcurrentHashMap<>();
    private final Runnable previewChangedRunnable;

    /**
     * Map to keep track of preview retrieval tasks.
     */
    private final Map<String, Future<?>> previewTasks = new ConcurrentHashMap<>();
    private LatLon previewLocation;

    public LayerTableController(@Nonnull LayerTableModel model, @Nonnull Runnable previewChangedRunnable)
    {
        this.model = model;
        this.previewChangedRunnable = previewChangedRunnable;
    }

    public Collection<MapDataWrapper> getSelectedRows()
    {
        return model.getSelectedRows();
    }

    public Collection<MapDataWrapper> getRows()
    {
        return model.getRows();
    }

    @InvokeOnEDT
    public void addCachedWrappers(List<MapDataWrapper> wrappers)
    {
        wrappers.forEach(this::addCacheWrapper);
    }

    @InvokeOnEDT
    public void clear()
    {
        model.clear();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        // The layer preview must be updated when the acceptable range has been updated.
        if (MapDataWrapper.MAX_ACCEPTABLE_LEVEL_PROPERTY.equals(evt.getPropertyName()))
        {
            MapDataWrapper wrapper = (MapDataWrapper) evt.getSource();
            updatePreview(wrapper, previewLocation);
        }
    }

    private void addCacheWrapper(MapDataWrapper wrapper)
    {
        wrapper.addPropertyChangeListener(this);
        model.addRow(wrapper);
    }

    /**
     * Calculate the estimated missing data sizes for all the imagery layers or elevation models.
     *
     * @param boundary The layer or model boundary
     */
    public void updateEstimatedSizes(Boundary boundary)
    {
        // If there's any tasks that were previously submitted and that haven't finished, cancel them.
        sizeEstimateTasks.forEach((key, future) -> future.cancel(true));

        sizeEstimateTasks.clear();

        for (MapDataWrapper wrapper : model.getRows())
        {
            wrapper.setTotalTileCount(null);
            wrapper.setTotalAcceptableTileCount(null);

            sizeEstimateTasks.put(
                    wrapper.getKey(),
                    ForkJoinPool.commonPool().submit(
                            new MapDataQueries.UpdateEstimatedSizeByBoundary(wrapper, boundary)));
        }
    }

    /**
     * Update the preview images for all layers.
     *
     * @param location Location to preview.
     */
    public void updatePreviews(LatLon location)
    {
        previewLocation = location;
        model.getRows().forEach(wrapper -> updatePreview(wrapper, location));
    }

    /**
     * Update the preview for one layer of the map set.
     *
     * @param wrapper  Layer to update.
     * @param location Location for which to generate a preview. Has no effect if location is {@code null}.
     */
    private void updatePreview(@Nonnull MapDataWrapper wrapper, @Nullable LatLon location)
    {
        if (location == null || wrapper.getElevationSource() != null) return;

        final var key = wrapper.getKey();

        final var task = previewTasks.remove(key);
        if (task != null) task.cancel(true);

        // Determine the layer to preview. Use either the max resolution layer chosen by the user, or the maximum
        // available layer if the user has not made a selection.
        final int maxLevel = Objects.requireNonNullElse(wrapper.getMaxAcceptableLevel(), wrapper.getMaxAvailableResolutionLevel());

        final var retriever = new PreviewRetriever(wrapper, WWUtils.latLonToRaptorLocation(location), maxLevel,
                PREVIEW_WIDTH, PREVIEW_HEIGHT, previewChangedRunnable);
        previewTasks.put(key, ForkJoinPool.commonPool().submit(retriever));
    }
}
