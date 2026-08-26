package gov.takx.dataservices.shapes;

import gov.takx.api.shapes.IShape;
import gov.takx.api.shapes.IShapeFilter;
import gov.takx.api.shapes.ShapeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Collection;

/**
 * Implementation of IShapeFilter.
 *
 * @param filterEntries List of filter entries to match against given shapes
 */
public record ShapeFilter(Collection<FilterEntry> filterEntries) implements IShapeFilter
{

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Individual filter entry, holding the field name and filter value.
     * @since 3.3
     */
    record FilterEntry(String field, Object value)
    {
    }

    @Override
    public boolean matches(IShape shape)
    {
        return filterEntries.stream().allMatch(filterEntry -> matches(shape, filterEntry));
    }

    /**
     * Test if the given shape passes a filter entry.
     *
     * @param shape       The shape to test
     * @param filterEntry Filter entry to test against
     * @return {@code true} if the shape passes the filter entry, {@code false} otherwise
     */
    private boolean matches(IShape shape, FilterEntry filterEntry)
    {
        final String field = filterEntry.field;
        final Object value = filterEntry.value;

        return switch (field)
        {
            case IShapeFilter.SHAPE_TYPE -> shape.getType().equals(((ShapeType) value).getShapeType());
            case IShapeFilter.NAME -> shape.getName().equals(value);
            case IShapeFilter.EDITABLE_FLAG -> shape.isEditable() == (boolean) value;
            case IShapeFilter.PUBLISHED_FLAG -> shape.isPublished() == (boolean) value;
            case IShapeFilter.VISIBLE_FLAG -> shape.isVisible() == (boolean) value;
            default ->
            {
                logger.error("Ignoring unsupported ShapeFilter field: {}", field);
                yield false;
            }
        };
    }
}
