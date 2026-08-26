package gov.takx.core.view.gui.maptools;

import gov.takx.api.mapobject.MapToolType;
import gov.takx.api.shapes.IRectangle;
import gov.takx.api.shapes.IShape;
import gov.takx.api.shapes.ISquare;

import java.util.Set;

/**
 * @since 1.42
 */
public class ShapePropertyUtils
{
    private static final Set<MapToolType> TOOL_TYPES_NOT_ORIENTABLE = Set.of(
            MapToolType.DRAWING_ARROW, MapToolType.DRAWING_PATH, MapToolType.DRAWING_FREEHAND_PATH,
            MapToolType.DRAWING_POLYGON, MapToolType.DRAWING_FREEHAND_POLYGON, MapToolType.DRAWING_ROUTE);

    private static final Set<MapToolType> TOOL_TYPES_NO_INTERIOR = Set.of(
            MapToolType.DRAWING_ARROW, MapToolType.DRAWING_PATH, MapToolType.DRAWING_FREEHAND_PATH, MapToolType.DRAWING_ROUTE);

    private static final Set<MapToolType> TOOL_TYPES_SUPPORTS_VERTICAL_HEIGHT = Set.of(
            MapToolType.DRAWING_CIRCLE, MapToolType.DRAWING_ELLIPSE, MapToolType.DRAWING_PATH, MapToolType.DRAWING_FREEHAND_PATH,
            MapToolType.DRAWING_POLYGON, MapToolType.DRAWING_FREEHAND_POLYGON, MapToolType.DRAWING_QUAD, MapToolType.DRAWING_SQUARE);

    private static final Set<MapToolType> TOOL_TYPES_USE_RUBBER_BAND = Set.of(
            MapToolType.DRAWING_ARROW, MapToolType.DRAWING_CIRCLE, MapToolType.DRAWING_ELLIPSE, MapToolType.DRAWING_QUAD,
            MapToolType.DRAWING_SQUARE, MapToolType.DRAWING_FREEHAND_PATH, MapToolType.DRAWING_FREEHAND_POLYGON);

    private static final Set<MapToolType> TOOL_TYPES_USE_FREEHAND = Set.of(MapToolType.DRAWING_FREEHAND_PATH,
            MapToolType.DRAWING_FREEHAND_POLYGON);

    /**
     * Determine if a shape has an interior.  This is all shapes except polylines/arrows.
     *
     * @return {@code true} if the shape has an interior
     */
    public static boolean shapeHasInterior(MapToolType toolType)
    {
        return !TOOL_TYPES_NO_INTERIOR.contains(toolType);
    }

    /**
     * @return {@code true} if the shape is a Square
     */
    public static boolean supportsOrientation(MapToolType toolType)
    {
        return !TOOL_TYPES_NOT_ORIENTABLE.contains(toolType);
    }

    /**
     * @return {@code true} if the tool type supports a vertical height
     */
    public static boolean supportsVerticalHeight(MapToolType toolType)
    {
        return TOOL_TYPES_SUPPORTS_VERTICAL_HEIGHT.contains(toolType);
    }

    /**
     * @return {@code true} if the tool type supports rings
     * @since 5.1
     */
    public static boolean supportsRings(MapToolType toolType)
    {
        return toolType == MapToolType.DRAWING_CIRCLE || toolType == MapToolType.DRAWING_ELLIPSE;
    }

    /**
     * @return {@code true} if the tool type is created using a "rubber band" style drawing
     */
    public static boolean usesRubberBandDrawing(MapToolType toolType)
    {
        return TOOL_TYPES_USE_RUBBER_BAND.contains(toolType);
    }

    /**
     * @return {@code true} if the tool type is created using a "freehand" style drawing
     */
    public static boolean usesFreehandDrawing(MapToolType toolType)
    {
        return TOOL_TYPES_USE_FREEHAND.contains(toolType);
    }

    /**
     * @return {@code true} if the shape is a Square
     */
    public static boolean isSquareShape(IShape shape)
    {
        return shape instanceof ISquare;
    }

    /**
     * @return {@code true} if the shape is a Rectangle, but not a Square (which is a subtype of rectangle)
     */
    public static boolean isRectangleShape(IShape shape)
    {
        return shape instanceof IRectangle && !(shape instanceof ISquare);
    }
}
