package gov.takx.api.shapes;

import com.google.auto.value.AutoBuilder;
import gov.takx.api.globe.IGlobeDelegate;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Defines a set of configuration properties for controlling the operation of the {@link IShapeDrawingService shape draqwing service}.
 *
 * @param shapeType The type of shape to be drawn
 * @param globe The globe one which to perform the draw operation
 * @param instructions Instructions to be displayed at the top of the globe when the craw operation is started, pass {@code null} for none
 * @param escapeAction Action to perform if/when the user presses the ESC key while drawing
 *
 * @since 5.2
 */
public record ShapeDrawingConfiguration(@Nonnull ShapeType shapeType, @Nonnull IGlobeDelegate globe, @Nullable String instructions, @Nonnull EscapeAction escapeAction)
{
    /**
     * Action to perform if/when the user hits the ESC key while a draw operation is active.
     */
    public enum EscapeAction
    {
        /**
         * Complete the drawing operation if enough control points have been specified.
         */
        complete,

        /**
         * Discard the drawn shape and return an exception to the caller.
         */
        discard
    }

    /**
     * Construct a builder using the given globe a shape type.
     *
     * @param globe Globe on which to perform the draw operation
     * @param shapeType Type of shape to be drawn
     * @return a new builder
     */
    @Nonnull
    public static Builder builder(@Nonnull IGlobeDelegate globe, @Nonnull ShapeType shapeType)
    {
        return builderWithDefaults().globe(globe).shapeType(shapeType);
    }

    /**
     * @return New builder with default properties configured
     */
    @Nonnull
    private static Builder builderWithDefaults()
    {
        return new AutoBuilder_ShapeDrawingConfiguration_Builder().escapeAction(EscapeAction.discard);
    }

    @AutoBuilder
    public abstract static class Builder
    {
        /**
         * @param shapeType The type of shape to be drawn
         * @return this builder
         */
        @Nonnull
        public abstract Builder shapeType(@Nonnull ShapeType shapeType);

        /**
         * @param globe The globe one which to perform the draw operation
         * @return this builder
         */
        @Nonnull
        public abstract Builder globe(@Nonnull IGlobeDelegate globe);

        /**
         * @param instructions Instructions to be displayed at the top of the globe when the craw operation is started
         * @return this builder
         */
        @Nonnull
        public abstract Builder instructions(@Nullable String instructions);

        /**
         * @param escapeAction Action to perform if/when the user presses the ESC key
         * @return this builder
         */
        @Nonnull
        public abstract Builder escapeAction(@Nonnull EscapeAction escapeAction);

        @Nonnull
        public abstract ShapeDrawingConfiguration build();
    }
}
