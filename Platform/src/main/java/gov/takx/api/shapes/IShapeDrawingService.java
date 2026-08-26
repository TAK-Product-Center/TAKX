package gov.takx.api.shapes;

import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.globe.IGlobeDelegate;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Service to invoke user interaction to draw  various shape types (e.g., circle, square, etc.). All shape types
 * as defined in {@link ShapeType} are supported except {@link ShapeType#RANGE_FAN} and {@link ShapeType#ROUTE}.
 * To draw routes, use the {@link gov.takx.api.routes.IRoutesManager}. There is currently no direct mechanism
 * to request the user draw a Range Fan.
 * <p>
 * When a draw operation is invoked, the selected globe will be setup to allow the user to interactively draw the shape.
 * When the shape drawing is complete (or canceled), the invoker will be notified of the resulting shape (or cancellation).
 *
 * @see ShapeType
 * @since 3.5
 */
@NoPluginImplementations
@InjectableService
public interface IShapeDrawingService
{
    /**
     * Request the user to draw the given shape type on the given globe.
     *
     * @param globe The globe on which to perform the drawing operation
     * @param shapeType The type of shape to draw (controls the specific shape editor to be used)
     * @return Future that will return the shape when the user completes the drawing operation
     * @throws IllegalStateException if a drawing operation is already in progress
     */
    @Nonnull
    default CompletableFuture<IMutableShape> requestDraw(@Nonnull IGlobeDelegate globe, @Nonnull ShapeType shapeType) throws IllegalStateException
    {
        return requestDraw(ShapeDrawingConfiguration.builder(globe, shapeType).build());
    }

    /**
     * Request the user to draw the given shape type on the given globe.
     *
     * @param globe        The globe on which to perform the drawing operation
     * @param shapeType    The type of shape to draw (controls the specific shape editor to be used)
     * @param instructions Optional instructions to be displayed at the top of the globe
     * @return Future that will return the shape when the user completes the drawing operation
     * @throws IllegalStateException if a drawing operation is already in progress
     * @since 5.2
     */
    @Nonnull
    default CompletableFuture<IMutableShape> requestDraw(@Nonnull IGlobeDelegate globe, @Nonnull ShapeType shapeType, @Nullable String instructions) throws IllegalStateException
    {
        return requestDraw(ShapeDrawingConfiguration.builder(globe, shapeType).instructions(instructions).build());
    }

    /**
     * Request the user to draw a shape according to the given configuration.
     *
     * @param configuration Configuration controlling how shapes are to be drawn
     * @return Future that will return the shape when the user completes the drawing operation
     * @throws IllegalStateException if a drawing operation is already in progress
     * @since 5.2
     */
    @Nonnull
    CompletableFuture<IMutableShape> requestDraw(@Nonnull ShapeDrawingConfiguration configuration) throws IllegalStateException;

    /**
     * Cancel any active drawing operation.  If there is no draw operation active, this request is ignored.
     */
    void cancelDraw();

    /**
     * Remove a previously drawn shape from the globe.
     *
     * @param shape The shape to remove, which must have been returned previously by {@link #requestDraw}
     * @throws IllegalArgumentException If the shape was not created by this service or has already been removed
     */
    void removeDrawnShape(@Nonnull IShape shape) throws IllegalArgumentException;
}
