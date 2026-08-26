package gov.takx.api.globe;

import com.google.auto.value.AutoValue;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.shapes.ShapeType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * Defines a set of configuration properties for controlling the operation of the {@link IMultiSelector MultiSelector}.
 *
 * @see #customHandlingBuilder(ShapeType)
 * @see #builder(ShapeType)
 * @since 5.7
 */
@AutoValue
public abstract class MultiSelectorConfiguration
{
    /**
     * @return {@code true} if the multi-selector should append to the existing selection (if there is one),
     * {@code false} the new selection should replace the old.
     */
    public abstract boolean append();

    /**
     * @return {@code true} if the panel allowing the user to refine their selection should NOT be shown
     */
    public abstract boolean customSelectionHandling();

    /**
     * @return The type of shape to be drawn.
     */
    @Nonnull
    public abstract ShapeType shapeType();

    /**
     * @return Consumes the map objects that were selected by the multi-selector.
     */
    @Nullable
    public abstract Consumer<Collection<IMapObject>> cotConsumer();

    /**
     * Construct a new builder, specifying the shape type to draw.
     *
     * @param shapeType Type of shape to draw.  Legal values are {@link ShapeType#RECTANGLE}, {@link ShapeType#POLYGON},
     *                  and {@link ShapeType#FREEHAND_POLYGON}.
     * @return A new builder
     */
    @Nonnull
    public static Builder builder(@Nonnull ShapeType shapeType)
    {
        return builderWithDefaults().shapeType(shapeType);
    }

    /**
     * Construct a new builder with the given shape type, and disabling the typical user interaction after the
     * selection is complete.  It is up to the caller to perform whatever processing is desired on the objects
     * selected by the user, either using the {@link java.util.concurrent.CompletableFuture} returned from
     * {@link IMultiSelector#arm(MultiSelectorConfiguration)} or by specifying a {@link Builder#cotConsumer(Consumer)}.
     *
     * @param shapeType Type of shape to draw.  Legal values are {@link ShapeType#RECTANGLE}, {@link ShapeType#POLYGON},
     *                  and {@link ShapeType#FREEHAND_POLYGON}.
     * @return A new builder
     * @see Builder#customSelectionHandling(boolean)
     */
    @Nonnull
    public static Builder customHandlingBuilder(@Nonnull ShapeType shapeType)
    {
        return builderWithDefaults().shapeType(shapeType).customSelectionHandling(true);
    }

    /**
     * @return New builder with default properties configured
     */
    @Nonnull
    private static Builder builderWithDefaults()
    {
        return new AutoValue_MultiSelectorConfiguration.Builder().append(false).customSelectionHandling(false);
    }

    @AutoValue.Builder
    public abstract static class Builder
    {
        /**
         * @return New instance of {@link MultiSelectorConfiguration} built from this configuration
         */
        @Nonnull
        public final MultiSelectorConfiguration build()
        {
            final var shapeType = shapeType();
            if (shapeType != ShapeType.RECTANGLE && shapeType != ShapeType.POLYGON && shapeType != ShapeType.FREEHAND_POLYGON)
            {
                throw new IllegalArgumentException("'shapeType' must be either RECTANGLE, POLYGON, or FREEHAND_POLYGON; got " + shapeType.name());
            }
            return autoBuild(); // Call the actual generated build
        }

        /**
         * @param shapeType Type of shape to draw.  Legal values are {@link ShapeType#RECTANGLE}, {@link ShapeType#POLYGON},
         *                  and {@link ShapeType#FREEHAND_POLYGON}.
         * @return this builder
         */
        @Nonnull
        public abstract Builder shapeType(@Nonnull ShapeType shapeType);

        /**
         * @param cotConsumer Consumer invoked when the user completes the selection operation.  It is passed the collection
         *                    of map objects that were selected by the user.  This consumer will NOT be called if the
         *                    user cancels the drawing operation.  The list of map objects can be empty.
         * @return this builder
         */
        @Nonnull
        public abstract Builder cotConsumer(@Nullable Consumer<Collection<IMapObject>> cotConsumer);

        /**
         * @param append {@code true} if the multi-selector operation should append to the existing selection,
         *               {@code false} to replace the previous selection.
         * @return this builder
         */
        @Nonnull
        public abstract Builder append(boolean append);

        /**
         * @param customSelectionHandling Pass {@code true} if the panel allowing the user to refine their selection and
         *                                choose the operation to perform on the selected objects should NOT be shown,
         *                                meaning that the invoker of the selection process is responsible for the
         *                                processing of the selected objects.
         * @return this builder
         */
        public abstract Builder customSelectionHandling(boolean customSelectionHandling);

        protected abstract ShapeType shapeType();

        protected abstract MultiSelectorConfiguration autoBuild();
    }
}
