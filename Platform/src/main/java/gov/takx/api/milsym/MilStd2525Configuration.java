package gov.takx.api.milsym;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import gov.tak.api.symbology.ISymbolTable;
import gov.tak.api.symbology.ISymbolTable.Symbol;
import gov.tak.api.symbology.ISymbologyProvider;
import gov.tak.api.symbology.ShapeType;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IShapeMapObject;
import gov.takx.api.shapes.IMutableShape;
import gov.takx.api.shapes.IShape;
import gov.takx.commons.utils.StringUtils;
import gov.takx.dataservices.shapes.Circle;
import gov.takx.dataservices.shapes.Ellipse;
import gov.takx.dataservices.shapes.Polygon;
import gov.takx.dataservices.shapes.Polyline;
import gov.takx.dataservices.shapes.Rectangle;
import gov.takx.dataservices.shapes.Square;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Defines the attributes to configure a MIL2525 multipoint graphic, and an associated builder to create it.
 * This can then be used to create a shape on the map via {@link gov.takx.api.shapes.IShapeService#createMil2525Graphic(MilStd2525Configuration)}.
 * <p/>
 * Example usage:
 * <pre>{@code
 *         final String CIRCLE_CODE = "G*F*ACVC--****X"; // This code requires a circle shape to define it
 *         final var circle = new Circle("my circle", someLocation, radius);
 *         final var configuration = MilStd2525Configuration.builder(CIRCLE_CODE)
 *                 .shape(circle) // Since no name is set, it will be taken from the circle's name
 *                 .withModifier("T", "T value")
 *                 .withModifier("W", "051630ZJAN2024")
 *                 .withModifier("W1", "081630ZJAN2024")
 *                 .initializer(smo -> {
 *                     smo.setDescription("some description");
 *                     smo.setMetaBoolean("someKey", true);
 *                 })
 *                 .build();
 *
 *         final Circle circleSymbol = shapeService.createMil2525Graphic(configuration);
 * }</pre>
 *
 * @since 5.1
 */
@AutoValue
public abstract class MilStd2525Configuration
{
    private static final Consumer<IShapeMapObject<?>> NOOP_INITIALIZER = mo -> {
    };

    /**
     * Defines a modifier to add to a configuration.  Some modifiers require specific text formats:
     * <ul>
     *     <li>{@code X} - must be a comma separated list of 2 numbers, e.g. {@code "100,400"}</li>
     *     <li>{@code W} and {@code W1} - must be a date in the format {@code "DDHHMM[SS]ZMONYY[YY]"}</li>
     *     <li>all others are arbitrary text strings</li>
     * </ul>
     *
     * @param id The modifier id
     * @param value The modifier value
     */
    public record ModifierValue(String id, String value)
    {
    }

    /**
     * @return The MIL2525 symbol code (SIDC)
     */
    @Nonnull
    public abstract String symbolCode();

    /**
     * @return The name to assign to the Map Object created from this configuration, may be null
     */
    @Nullable
    public abstract String name();

    /**
     * @return The shape that defines the geometry for the symbol graphic
     */
    @Nonnull
    public abstract IMutableShape shape();

    /**
     * @return Collection of modifiers to apply to the symbol graphic
     * @see ModifierValue
     */
    public abstract ImmutableSet<ModifierValue> modifiers();

    /**
     * @return Initializer to apply to any Map Object created from this configuration.
     * @see Builder#initializer(Consumer)
     */
    @Nonnull
    public abstract Consumer<IShapeMapObject<?>> initializer();

    /**
     * @return a new builder, initialized with the given {@code symbolCode}
     */
    public static Builder builder(@Nonnull String symbolCode)
    {
        return new AutoValue_MilStd2525Configuration.Builder().symbolCode(symbolCode).initializer(NOOP_INITIALIZER);
    }

    /**
     * @return a new builder, specifying the symbol, from which the symbol code will be extracted
     * @see Symbol#getCode()
     */
    public static Builder builder(@Nonnull Symbol symbol)
    {
        return builder(symbol.getCode());
    }

    @PseudoBean
    @AutoValue.Builder
    public static abstract class Builder
    {
        private static final Map<Class<? extends IShape>, ShapeType> SHAPE_TO_TYPE = Map.of(
                Polyline.class, ShapeType.LineString,
                Polygon.class, ShapeType.Polygon,
                Square.class, ShapeType.Rectangle,
                Rectangle.class, ShapeType.Rectangle,
                Circle.class, ShapeType.Circle,
                Ellipse.class, ShapeType.Ellipse);

        @Inject
        private IMilStd2525ServiceProvider milStd2525ServiceProvider;

        /**
         * @param symbolCode The symbol code defining the tactical graphic to be drawn
         * @return this builder
         */
        @Nonnull
        public abstract Builder symbolCode(@Nonnull String symbolCode);

        /**
         * @param symbol The symbol, from which the symbol code is extracted
         * @return this builder
         * @see Symbol#getCode()
         * @see #symbolCode(String)
         */
        @Nonnull
        public Builder symbol(@Nonnull Symbol symbol)
        {
            symbolCode(symbol.getCode());
            return this;
        }

        /**
         * Set the name for the Map Object created from this configuration.  If this method is not called, a default name
         * like "Circle 1" will be generated based on the class name of the shape type specified in {@link #shape(IMutableShape)}.
         *
         * @param name Name to use
         * @return this builder
         * @see IShape#getType()
         */
        @Nonnull
        public abstract Builder name(@Nonnull String name);

        /**
         * Specify the shape that defines the geometry to use for rendering the tactical graphic.  If this method is not
         * called, an {@link IllegalStateException} will be thrown when {@link #build()} is called.
         * <p/>
         * If no name has been specified for this configuration (via {@link #name(String)}), and the given shape has
         * a non-null name, then that name will be used for this configuration.
         * <p/>
         * <b>NOTE: </b> This shape will be associated with the map object created via
         * {@link gov.takx.api.shapes.IShapeService#createMil2525Graphic(MilStd2525Configuration)}, so any future changes
         * made to this shape will affect the rendering of the created shape map object.
         *
         * @param shape An appropriate shape with the required geometry (center, points, etc.) to render the graphic
         * @return this builder
         */
        @Nonnull
        public abstract Builder shape(@Nonnull IMutableShape shape);

        /**
         * Add a modifier to this configuration.  Modifiers are used to augment the drawn graphic with additional
         * information relating the shape/area designated by the graphic.  Some modifiers require specific value formats,
         * see {@link ModifierValue} for details.
         *
         * @param modifierId ID of modifier to add
         * @param value modifier value
         * @return this builder
         * @see ModifierValue
         */
        public Builder withModifier(@Nonnull String modifierId, @Nonnull String value)
        {
            return withModifier(new ModifierValue(modifierId, value));
        }

        /**
         * Add a modifier to this configuration.  Modifiers are used to augment the drawn graphic with additional
         * information relating the shape/area designated by the graphic.
         *
         * @param modifier Modifier to add
         * @return this builder
         */
        public Builder withModifier(@Nonnull ModifierValue modifier)
        {
            modifiersBuilder().add(modifier);
            return this;
        }

        /**
         * Specify arbitrary customizations or logic to be performed if/when a MapObject is created via the shape service.
         * The initializer will be passed the newly constructed Map Object as an argument.  For example,
         * <pre>{@code
         *     final MilStd2525Configuration.builder(someCode)
         *         .initializer(shapeMapObject -> {
         *             shapeMapObject.setDescription("some description");
         *             shapeMapObject.setMetaString(someKey, someValue);
         *         })
         *         .build();
         * }</pre>
         *
         * @param initializer Initializer to apply to any Map Object created by the shape service
         * @return this builder
         * @see gov.takx.api.shapes.IShapeService#createMil2525Graphic(MilStd2525Configuration)
         */
        @Nonnull
        public abstract Builder initializer(@Nonnull Consumer<IShapeMapObject<?>> initializer);

        /**
         * Builds a new configuration instance.  If any of the following conditions are found an {@link IllegalStateException}
         * is thrown:
         * <ul>
         *     <li>If the symbol code is not valid</li>
         *     <li>If the symbol given doesn't support ANY shape types</li>
         *     <li>If the shape given does not map to a known {@link ShapeType}</li>
         *     <li>If the shape given is not a supported shape type for the symbol</li>
         *     <li>If a modifier is added and the modifier id is not known</li>
         *     <li>If a modifier is added and the modifier id is not supported by the symbol</li>
         * </ul>
         * @return New instance of {@link MilStd2525Configuration}
         * @see ISymbologyProvider#getSymbolTable()
         * @see ISymbolTable#getSymbol(String)
         * @see ISymbolTable.Entry#getContentMask()
         * @see ISymbologyProvider#getModifiers(String)
         * @throws IllegalStateException If any of the conditions listed above are found
         */
        @Nonnull
        public MilStd2525Configuration build() throws IllegalStateException
        {
            IMilStd2525Service milStd2525Service = milStd2525ServiceProvider.getPreferred2525Service();

            // Validate the symbol code
            final String code = symbolCode();
            final var symbolWrapper = milStd2525Service.getSymbol(code);
            if (symbolWrapper == null) throw new IllegalStateException("Unknown symbol code: " + code);
            final Symbol symbol = symbolWrapper.getSymbol();

            // The symbol must support a shape
            final var shapeTypes = symbol.getContentMask();
            if (shapeTypes.isEmpty()) throw new IllegalStateException("Symbol " + code + " does not support any shape types; it cannot be rendered");

            // Must have a shape type that is supported by the selected symbol
            final var shape = shape();
            final var shapeClass = shape.getClass();
            final var shapeType = SHAPE_TO_TYPE.get(shapeClass);
            final String shapeTypeName = shape.getType();

            if (shapeType == null) throw new IllegalStateException("Unsupported shape type: " + shapeTypeName);

            // The symbol must support the given shape
            if (!shapeTypes.contains(shapeType)) throw new IllegalStateException("Symbol '" + code + "' does not support a shape of " + shapeTypeName);

            // Any modifiers specified must be declared as supported on the symbol
            for (ModifierValue modifier : modifiers())
            {
                final String id = modifier.id;

                if (!milStd2525Service.isValidModifier(id)) throw new IllegalStateException("Unknown modifier id: '" + id + "'");

                if (!milStd2525Service.isValidModifierForSymbol(code, id))
                {
                    throw new IllegalStateException("Symbol '" + code + "' does not support a modifier with id '" + id + "'");
                }
            }

            // If our name is null, and the shape has a name, then use that as our name
            // Else, apply our name to our shape
            final String shapeName = shape.getName();
            final String ourName = name();

            if (ourName == null)
            {
                if (!StringUtils.isEmpty(shapeName, true)) name(shapeName);
            } else
            {
                shape.setName(ourName);
            }

            return autoBuild();
        }

        /**
         * @return builder for adding modifiers to the configuration
         */
        @Nonnull
        abstract ImmutableSet.Builder<ModifierValue> modifiersBuilder();

        @Nonnull
        abstract String symbolCode();

        @Nullable
        abstract String name();

        @Nonnull
        abstract IMutableShape shape();

        @Nonnull
        abstract ImmutableSet<ModifierValue> modifiers();

        @Nonnull
        abstract MilStd2525Configuration autoBuild();
    }
}
