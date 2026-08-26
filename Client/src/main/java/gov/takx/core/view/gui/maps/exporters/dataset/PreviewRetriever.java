package gov.takx.core.view.gui.maps.exporters.dataset;

import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.raster.osm.OSMUtils;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.platform.NameFixingRunnable;
import gov.takx.core.view.gui.maps.exporters.dataset.MapDataWrapper.PreviewState;
import jakarta.annotation.Nonnull;

/**
 * @since 1.43
 */
public class PreviewRetriever extends NameFixingRunnable
{
    private final MapDataWrapper wrapper;
    private final ILocation previewLocation;
    private final int levelNumber;
    private final int width;
    private final int height;
    private final Runnable refreshRunnable;

    /**
     * @param wrapper The wrapper containing the map info
     * @param previewLocation The preview location
     * @param levelNumber The resolution level
     * @param width The desired preview image width
     * @param height The desired preview image height
     * @param previewChangedRunnable Runnable that will be called when the preview changes
     */
    public PreviewRetriever(@Nonnull MapDataWrapper wrapper, ILocation previewLocation, int levelNumber, int width,
                            int height, @Nonnull Runnable previewChangedRunnable)
    {
        super("Layer Preview Retriever: " + wrapper.getKey());

        if (levelNumber < 0) throw new IllegalArgumentException("'levelNumber' must be >= 0");

        this.wrapper = wrapper;
        this.previewLocation = previewLocation;
        this.levelNumber = levelNumber;
        this.width = width;
        this.height = height;
        this.refreshRunnable = previewChangedRunnable;
    }

    @Override
    protected void doRun()
    {
        wrapper.setPreviewState(PreviewState.Retrieving);
        wrapper.setPreview(null);
        refreshRunnable.run(); // Update previews in UI

        double resolution = OSMUtils.mapnikTileResolution(levelNumber);
        Envelope envelope = PreviewUtilities.computePreviewEnvelope(previewLocation, resolution, width, height);
        PreviewUtilities.PreviewResult previewResult;
        try (MapDataWrapper.TileClientWrapper tileClientWrapper = wrapper.getTileClient())
        {
            previewResult = tileClientWrapper != null
                    ? PreviewUtilities.createImageryPreview(tileClientWrapper.tileClient(), width, height, levelNumber, envelope)
                    : new PreviewUtilities.PreviewResult(null, PreviewUtilities.Status.NOT_AVAILABLE, 0, null);
        }

        switch (previewResult.status())
        {
            case ERROR ->
            {
                wrapper.setPreviewState(PreviewState.Error);
                wrapper.setPreview(null);
            }
            case NOT_AVAILABLE ->
            {
                wrapper.setPreviewState(PreviewState.NotAvailable);
                wrapper.setPreview(null);
            }
            case SUCCESS ->
            {
                wrapper.setPreview(previewResult.image());
                wrapper.setPreviewState(PreviewState.Available);
                wrapper.setTileDataSize(previewResult.dataSize());
            }
        }

        refreshRunnable.run();
    }
}
