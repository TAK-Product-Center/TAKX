package gov.takx.api.mapobject;

import gov.takx.api.shapes.ShapeType;
import gov.takx.dataservices.persist.AnnotationMapObject;
import gov.takx.dataservices.persist.FlatAnnotationMapObject;
import gov.takx.dataservices.persist.RouteMapObject;
import gov.takx.dataservices.persist.ShapeArrowMapObject;
import gov.takx.dataservices.persist.ShapeBullseyeMapObject;
import gov.takx.dataservices.persist.ShapeCircleMapObject;
import gov.takx.dataservices.persist.ShapeEllipseMapObject;
import gov.takx.dataservices.persist.ShapePathMapObject;
import gov.takx.dataservices.persist.ShapePolygonMapObject;
import gov.takx.dataservices.persist.ShapeRangeFanMapObject;
import gov.takx.dataservices.persist.ShapeRectangleMapObject;
import gov.takx.dataservices.persist.ShapeSquareMapObject;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * This enum is used to define the "map tools" for drawing surface shapes on the globe.
 * Each tool contains the shape Map Object concrete class to create for the shape and a "shape type"
 * constant, used as part of the MO key and as a unique identifier for the tool.
 * @since 3.4 (renamed from MapToolTypes)
 */
public enum MapToolType
{
    DRAWING_ARROW(ShapeType.ARROW, ShapeArrowMapObject.class),
    DRAWING_POLYGON(ShapeType.POLYGON, ShapePolygonMapObject.class),
    DRAWING_FREEHAND_POLYGON(ShapeType.FREEHAND_POLYGON, ShapePolygonMapObject.class),
    DRAWING_CIRCLE(ShapeType.CIRCLE, ShapeCircleMapObject.class),
    DRAWING_ELLIPSE(ShapeType.ELLIPSE, ShapeEllipseMapObject.class),
    DRAWING_SQUARE(ShapeType.SQUARE, ShapeSquareMapObject.class),
    DRAWING_PATH(ShapeType.POLYLINE, ShapePathMapObject.class),
    DRAWING_FREEHAND_PATH(ShapeType.FREEHAND_POLYLINE, ShapePathMapObject.class),
    DRAWING_QUAD(ShapeType.RECTANGLE, ShapeRectangleMapObject.class),
    DRAWING_ROUTE(ShapeType.ROUTE, RouteMapObject.class),
    DRAWING_BULLSEYE(ShapeType.BULLSEYE, ShapeBullseyeMapObject.class),
    DRAWING_RANGE_FAN(ShapeType.RANGE_FAN, ShapeRangeFanMapObject.class),

    DRAWING_ANNOTATION(ShapeType.NONE, AnnotationMapObject.class),
    DRAWING_FLAT_ANNOTATION(ShapeType.NONE, FlatAnnotationMapObject.class),

    NONE(ShapeType.NONE, null);

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ShapeType shapeType;
    private final Class<? extends IMapObject> mapObjectClass;

    private static final Map<String, MapToolType> SHAPE_TYPE_STRING_2_TOOL_TYPE = new HashMap<>();
    private static final Map<ShapeType, MapToolType> SHAPE_TYPE_2_TOOL_TYPE = new EnumMap<>(ShapeType.class);

    private static final MapToolType[] VALUES = values();

    static
    {
        // The shapeType2ToolType map is used when restoring MOs from persistence, and the FREEHAND
        // types are persisted as their non-FREEHAND type.  So, exclude the FREEHAND entries.
        for (MapToolType mtt : VALUES)
        {
            if (!mtt.name().contains("FREEHAND")) SHAPE_TYPE_STRING_2_TOOL_TYPE.put(mtt.getShapeTypeName(), mtt);
            SHAPE_TYPE_2_TOOL_TYPE.put(mtt.getShapeType(), mtt);
        }

    }

    /**
     * @param shapeType Used for display name and shape type
     * @param mapObjectClass The MapObject subclass used to represent this tool
     */
    MapToolType(ShapeType shapeType, Class<? extends IMapObject> mapObjectClass)
    {
        this.shapeType = shapeType;
        this.mapObjectClass = mapObjectClass;
    }

    /**
     * @return The display name for this tool type
     */
    @Nonnull
    public String getDisplayName()
    {
        return shapeType.getShapeType();
    }

    /**
     * @return The shape type name (for World Wind) associated with this tool type
     */
    @Nonnull
    public String getShapeTypeName()
    {
        return shapeType.getShapeType();
    }

    /**
     * @return The ShapeType associated with this map tool
     * @since 3.3
     */
    @Nonnull
    public ShapeType getShapeType()
    {
        return shapeType;
    }

    /**
     * @return The MapObject subclass used to represent this tool, {@code null} for {@link MapToolType#NONE}
     * @since 2.28
     */
    @Nullable
    public Class<? extends IMapObject> getMapObjectClass()
    {
        return mapObjectClass;
    }

    /**
     * Get the Enum object given the shape type
     *
     * @param shapeType Name to lookup
     * @return Enum object with that shape type or {@code null} if not found
     */
    @Nullable
    public static MapToolType valueOfShapeType(String shapeType)
    {
        final MapToolType mapToolType = SHAPE_TYPE_STRING_2_TOOL_TYPE.get(shapeType);
        if (mapToolType == null) logger.error("MapToolType not found for shapeType '{}'", shapeType);

        return mapToolType;
    }

    /**
     * Get the MapToolType equivalent for the given shape type.
     *
     * @param shapeType Shape type to lookup
     * @return Enum object with that shape type or {@code null} if not found
     * @since 3.5
     */
    @Nullable
    public static MapToolType valueOfShapeType(ShapeType shapeType)
    {
        final MapToolType mapToolType = SHAPE_TYPE_2_TOOL_TYPE.get(shapeType);
        if (mapToolType == null) logger.error("MapToolType not found for shapeType '{}'", shapeType.name());

        return mapToolType;
    }

    /**
     * Determine if a given type is one of the map tool types.
     *
     * @param type Type to test
     * @return true if the type is one of the map tools
     */
    public static boolean isMapToolType(String type)
    {
        return SHAPE_TYPE_STRING_2_TOOL_TYPE.containsKey(type);
    }

    @Override
    public String toString()
    {
        return getDisplayName();
    }
}
