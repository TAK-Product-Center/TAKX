package gov.takx.api.shapes;

import gov.takx.api.mapobject.IShapeMapObject;

/**
 * Interface to an IShapeFilter. IShapeFilter's are used to obtain lists of shapes that match certain criteria.
 * See {@link IShapeFilterBuilder} for more information on how to create an IShapeFilter instance. Valid
 * fields to filter by and their corresponding value types are listed below.
 *
 * @since 1.4
 */
public interface IShapeFilter
{
    /**
     * Filter on the {@link IShapeMapObject#isPublished() published} value. Value type is Boolean.
     */
    String PUBLISHED_FLAG = ShapeProperties.PUBLISHED;

    /**
     * Filter on the {@link IShapeMapObject#isEditable() editable} value. Value type is Boolean.
     */
    String EDITABLE_FLAG = ShapeProperties.EDITABLE;

    /**
     * Filter on the {@link IShapeMapObject#isVisible() visible} value. Value type is Boolean.
     */
    String VISIBLE_FLAG = ShapeProperties.VISIBILITY;

    /**
     * Filter on the {@link IShapeMapObject#getName() name} value. Value type is String.
     */
    String NAME = ShapeProperties.NAME;

    /**
     * Filter on the {@link IShapeMapObject#getType() shape type} value. Value type is {@link ShapeType}.
     * <p>
     * Must be one of {@link #ARROW}, {@link #BULLSEYE}, {@link #CIRCLE}, {@link #ELLIPSE}, {@link #POLYGON},
     * {@link #POLYLINE}, {@link #RECTANGLE}, or {@link #SQUARE}.
     */
    String SHAPE_TYPE = "shape type";

    // Valid Shape Types
    ShapeType ARROW = ShapeType.ARROW;
    /** @since 5.5 **/
    ShapeType BULLSEYE = ShapeType.BULLSEYE;
    ShapeType CIRCLE = ShapeType.CIRCLE;
    ShapeType ELLIPSE = ShapeType.ELLIPSE;
    ShapeType POLYGON = ShapeType.POLYGON;
    ShapeType POLYLINE = ShapeType.POLYLINE;
    ShapeType RECTANGLE = ShapeType.RECTANGLE;
    ShapeType SQUARE = ShapeType.SQUARE;

    /**
     * Determine if the given shape matches the filter configuration.
     *
     * @param shape Shape to test
     * @return {@code true} if the shape matches, {@code false} otherwise
     * @since 2.29
     */
    boolean matches(IShape shape);
}
