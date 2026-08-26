package gov.takx.dataservices.shapes;

import gov.takx.api.shapes.IShapeFilter;
import gov.takx.api.shapes.IShapeFilterBuilder;
import gov.takx.api.shapes.InvalidShapeFilterException;
import gov.takx.api.shapes.ShapeType;
import jakarta.annotation.Nonnull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static gov.takx.api.shapes.IShapeFilter.SHAPE_TYPE;

/**
 * Concrete class to an {@link IShapeFilterBuilder}
 */
public class ShapeFilterBuilder implements IShapeFilterBuilder
{
    // Use a map to ensure no field is included twice in the filter entries
    private final Map<String, ShapeFilter.FilterEntry> filterEntries = new HashMap<>();

    private static final Map<String, Class<?>> FIELD_VALUE_TYPE = Map.of(
            IShapeFilter.EDITABLE_FLAG, Boolean.class,
            IShapeFilter.PUBLISHED_FLAG, Boolean.class,
            IShapeFilter.VISIBLE_FLAG, Boolean.class,
            IShapeFilter.NAME, String.class,
            SHAPE_TYPE, ShapeType.class);

    private static final Set<ShapeType> VALID_SHAPE_TYPES = Set.of(
            IShapeFilter.ARROW, IShapeFilter.BULLSEYE, IShapeFilter.CIRCLE, IShapeFilter.ELLIPSE, IShapeFilter.POLYGON,
            IShapeFilter.POLYLINE, IShapeFilter.RECTANGLE, IShapeFilter.SQUARE);

    private static final String INVALID_TYPE_MSG = "'%s' value must be of type %s; got %s";
    private static final String INVALID_SHAPE_TYPE_MSG = SHAPE_TYPE + " must be one of " + VALID_SHAPE_TYPES + "; got '%s'";

    @Override
    public IShapeFilterBuilder defineFilter(@Nonnull String field, @Nonnull Object value)
            throws InvalidShapeFilterException
    {
        if (!FIELD_VALUE_TYPE.containsKey(field)) throw new InvalidShapeFilterException("Unsupported field '" + field + "'");

        // Ensure the value is of the correct type for the given field.
        final Class<?> valueType = FIELD_VALUE_TYPE.get(field);

        if (!valueType.isInstance(value)) throw new InvalidShapeFilterException(
                String.format(INVALID_TYPE_MSG, field, valueType, value.getClass()));

        if (SHAPE_TYPE.equals(field) && !VALID_SHAPE_TYPES.contains((ShapeType) value))
        {
            throw new InvalidShapeFilterException(String.format(INVALID_SHAPE_TYPE_MSG, value));
        }

        filterEntries.put(field, new ShapeFilter.FilterEntry(field, value));

        return this;
    }

    @Override
    public IShapeFilter build()
    {
        return new ShapeFilter(filterEntries.values());
    }
}
