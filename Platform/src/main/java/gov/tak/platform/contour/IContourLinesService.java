package gov.tak.platform.contour;

import gov.tak.api.annotation.IncubatingApi;
import gov.tak.api.contour.ContourLayerParameters;
import gov.tak.api.contour.ContourLineFactory;
import gov.tak.api.contour.ContourProgressListener;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.shapes.IRectangle;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service to manage contour line creation.
 *
 * @since 5.8
 */
@IncubatingApi(since = "5.8")
@InjectableService
@NoPluginImplementations
public interface IContourLinesService
{
    /**
     * @return List of active contour groups.
     */
    @Nonnull
    List<ContourLineGroup> getContourGroups();

    /**
     * @param group Contour group to remove.
     */
    void removeContourGroup(@Nonnull ContourLineGroup group);

    /**
     * @param generationParams Contour generation parameters.
     * @param layerParams Contour layer style parameters.
     * @param cancellation Optional cancellation handle.
     * @param contourGenerationListener Optional progress listener for the contour generation stage.
     * @param contourLayerListener Optional progress listener for the contour layer creation stage.
     * @return Future of the created contour group. Yielded value can be {@code null} if the user cancels drawing.
     */
    @Nonnull
    CompletableFuture<ContourLineGroup> initiateContourDrawing(@Nonnull ContourLineFactory.Generation generationParams,
                                                               @Nonnull ContourLayerParameters layerParams,
                                                               @Nullable AtomicBoolean cancellation,
                                                               @Nullable ContourProgressListener contourGenerationListener,
                                                               @Nullable ContourProgressListener contourLayerListener);

    /**
     * @param generationParams Contour generation parameters.
     * @param layerParams Contour layer style parameters.
     * @param cancellation Optional cancellation handle.
     * @param contourGenerationListener Optional progress listener for the contour generation stage.
     * @param contourLayerListener Optional progress listener for the contour layer creation stage.
     * @param area Area to create a contour for.
     * @return Future of the created contour group. Yielded value can be {@code null} if the user cancels drawing.
     */
    @Nonnull
    CompletableFuture<ContourLineGroup> buildFromArea(@Nonnull ContourLineFactory.Generation generationParams,
                                                      @Nonnull ContourLayerParameters layerParams,
                                                      @Nullable AtomicBoolean cancellation,
                                                      @Nullable ContourProgressListener contourGenerationListener,
                                                      @Nullable ContourProgressListener contourLayerListener,
                                                      @Nonnull IRectangle area);

    /**
     * @param listener Listener to add.
     */
    void addContourGroupListener(@Nonnull ContourGroupListener listener);

    /**
     * @param listener Listener to remove.
     */
    void removeContourGroupListener(@Nonnull ContourGroupListener listener);
}
