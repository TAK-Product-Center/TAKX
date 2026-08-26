package gov.takx.api.shapes;

import jakarta.annotation.Nonnull;

/**
 * Contract for an IShapeFilterBuilder. These are used to define and construct new IShapeFilter instances,
 * which can be passed in when getting lists of shapes, in order to get shapes that are pertinent to the matter at hand.
 * IShapeFilterBuilder objects can be created through an instance of the IShapeService
 * see: {@link IShapeService#createShapeFilterBuilder()}
 *
 * @since 1.4
 * @see IShapeFilter
 * @see IShapeService#getShapes(IShapeFilter)
 * @see IShapeService#getMutableShapes(IShapeFilter)
 */
public interface IShapeFilterBuilder
{
    /**
     * Add a filter condition.
     *
     * @param field The field type to filter on. Valid field strings can be found in {@link IShapeFilter}.
     * @param value The value that corresponds to the field. If the value type is not appropriate for the field an
     *        {@link InvalidShapeFilterException} is thrown. Passing in {@code null} for the value will throw an
     *        IllegalArgumentException. It's important to note that each shape filter builder only contains one of each
     *        field, and each one of those unique fields can only have one corresponding value. Because of this,
     *        you are unable to perform an action such as defining one shape filter builder with multiple shape
     *        types, or multiple names to filter by - it will simply over-write the previous value. For example, calling:
     *        <pre>
     *        {@code
     *        defineFilter(IShapeFilter.SHAPE_TYPE,IShapeFilter.RECTANGLE);
     *        defineFilter(IShapeFilter.SHAPE_TYPE,IShapeFilter.SQUARE);
     *        }
     *        </pre>
     *        would cause the IShapeFilter to filter by squares only, and not by rectangles, nor by both
     *        rectangles and squares.
     * @return {@code this} for chaining
     * @throws InvalidShapeFilterException if the value is not appropriate for the field
     */
    IShapeFilterBuilder defineFilter(@Nonnull String field, @Nonnull Object value) throws InvalidShapeFilterException;

    /**
     * @return a new instance of an IShapeFilter built from the conditions added using {@link #defineFilter(String, Object)}
     */
    IShapeFilter build();
}
