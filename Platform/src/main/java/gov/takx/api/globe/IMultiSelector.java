package gov.takx.api.globe;

import gov.tak.api.annotation.IncubatingApi;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.shapes.ShapeType;
import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Contract for service that manages the MultiSelect tool.
 *
 * @since 3.5
 */
@InjectableService
@NoPluginImplementations
@IncubatingApi(since = "3.5")
public interface IMultiSelector
{
    /**
     * Arms the MultiSelector editor to draw using the given {@link MultiSelectorConfiguration#shapeType()} and enables
     * the selection process.
     *
     * @param configuration Configuration to control how selection will be handled.
     * @return Future that will complete when the user has finished the selection process, either normally, or by
     * canceling.  If the user cancels, the future will complete exceptionally.
     *
     * @since 5.7
     */
    @Nonnull
    CompletableFuture<Collection<IMapObject>> arm(@Nonnull MultiSelectorConfiguration configuration);

    /**
     * Arms the MultiSelector editor to draw using the given {@link ShapeType} and enables the selection process.
     *
     * @param shapeType The shape that the editor should use (currently Rectangle, Polygon, or Freehand Polygon)
     * @return Future that will complete when the user has finished the selection process, either normally, or by
     * canceling.  If the user cancels, the future will complete exceptionally.
     * @throws IllegalArgumentException If the given {@code shapeType} is not one of the allowed types
     * @since 5.7
     */
    @Nonnull
    default CompletableFuture<Collection<IMapObject>> arm(@Nonnull ShapeType shapeType) throws IllegalArgumentException
    {
        return arm(MultiSelectorConfiguration.builder(shapeType).build());
    }

    /**
     * Arms the MultiSelector editor to draw using the given {@link ShapeType} and enables the selection process.
     * Using this method will suppress the display of the selection panel, which normally allows the user to refine
     * their selection and select what to do with the selected objects.  The given {@code cotConsumer} is fully
     * responsible for processing the selected objects.  Note that the consumer will <b>NOT</b> be called if the user
     * cancels the selection operation.
     *
     * @param shapeType The shape that the editor should use (currently Rectangle, Polygon, or Freehand Polygon)
     * @param cotConsumer Consumer that will be passed the collection of selected objects
     * @return Future that will complete when the user has finished the selection process, either normally, or by
     * canceling.  If the user cancels, the future will complete exceptionally.
     * @throws IllegalArgumentException If the given {@code shapeType} is not one of the allowed types
     * @since 5.7 (signature change)
     */
    @Nonnull
    default CompletableFuture<Collection<IMapObject>> arm(@Nonnull ShapeType shapeType, @Nonnull Consumer<Collection<IMapObject>> cotConsumer) throws IllegalArgumentException
    {
        return arm(MultiSelectorConfiguration.customHandlingBuilder(shapeType).cotConsumer(cotConsumer).build());
    }

    /**
     * Return the list of objects that were contained within the user drawn shape.  This is unaffected by any
     * selections made by the user in the individual selection panel.
     *
     * @return A List of map objects which may be empty but not {@code null}.
     */
    @Nonnull
    List<IMapObject> getSurroundedObjects();

    /**
     * Gathers and returns a list of objects contained in the user drawn region that have been further selected
     * by the user in the selection panel.
     *
     * @return A List of map objects which may be empty but not {@code null}.
     */
    @Nonnull
    List<IMapObject> getSelectedObjects();

    /**
     * @return if the selector editor is currently armed
     */
    boolean isArmed();

    /**
     * Disarm (if armed) and remove the last drawn shape from the globe (if any).
     */
    void disarmAndClear();
}
