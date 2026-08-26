package gov.takx.api.shapes;

import gov.takx.dataservices.shapes.Arrow;
import gov.takx.dataservices.shapes.Bullseye;
import gov.takx.dataservices.shapes.Circle;
import gov.takx.dataservices.shapes.Ellipse;
import gov.takx.dataservices.shapes.Polygon;
import gov.takx.dataservices.shapes.Polyline;
import gov.takx.dataservices.shapes.RangeFan;
import gov.takx.dataservices.shapes.Rectangle;
import gov.takx.dataservices.shapes.Route;
import gov.takx.dataservices.shapes.Square;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.lang.reflect.Constructor;

/**
 * All supported shape types.  The {@link #getShapeType() type name} will be used as the "type" for Map Objects
 * created from these shapes.
 */
public enum ShapeType
{
    ARROW(Arrow.class, "arrow.png"),
    /** @since 5.5 **/
    BULLSEYE(Bullseye.class, "bullseye.png"),
    CIRCLE(Circle.class, "circle.png"),
    ELLIPSE(Ellipse.class, "ellipse.png"),
    POLYGON(Polygon.class, "polygon.png"),
    /** @since 3.5 */
    FREEHAND_POLYGON(Polygon.class, "polygon_free_draw.png"),
    POLYLINE(Polyline.class, "polyline.png"),
    /** @since 3.5 */
    FREEHAND_POLYLINE(Polyline.class, "polyline_free_draw.png"),
    RANGE_FAN(RangeFan.class, "range_fan.png"),
    RECTANGLE(Rectangle.class, "rectangle.png"),
    ROUTE(Route.class, "route.png"),
    SQUARE(Square.class, "square.png"),

    NONE(null, null);

    private static final ShapeType[] VALUES = values();

    private final String shapeType;
    private final String iconName;
    private final Class<? extends IMutableShape> shapeModelClass;
    private final Constructor<? extends IMutableShape> modelConstructor;

    /**
     * Constructor.
     *
     * @param shapeModelClass The class defining the shape model
     * @param iconName The name of the icon within {@link gov.takx.platform.ui.ResourceDirectory#PLATFORM_ICONS} used to
     *                 define the corresponding map object type
     */
    ShapeType(Class<? extends IMutableShape> shapeModelClass, String iconName)
    {
        this.shapeModelClass = shapeModelClass;
        shapeType = shapeModelClass == null ? null : shapeModelClass.getSimpleName();
        this.iconName = iconName;

        try
        {
            modelConstructor = shapeModelClass == null ? null : shapeModelClass.getDeclaredConstructor();
        } catch (NoSuchMethodException e)
        {
            throw new RuntimeException("Failed to initialize ShapeType: " + name(), e);
        }
    }

    /**
     * @return The name of the icon within {@link gov.takx.platform.ui.ResourceDirectory#PLATFORM_ICONS} used to define
     * the corresponding map object type.
     * @since 3.3
     */
    @Nullable // only null for NONE
    public String getIconName()
    {
        return iconName;
    }

    /**
     * @return The shape type name
     */
    @Nullable // only null for NONE
    public String getShapeType()
    {
        return shapeType;
    }

    /**
     * @return The model class for this shape type
     * @since 3.3
     */
    @Nullable // only null for NONE
    public Class<? extends IMutableShape> getShapeModelClass()
    {
        return shapeModelClass;
    }

    /**
     * @return The ShapeType for the given shape type name
     * @since 3.3
     */
    @Nonnull
    public static ShapeType valueForType(@Nonnull String shapeType)
    {
        for (ShapeType type : VALUES)
        {
            if (shapeType.equals(type.getShapeType())) return type;
        }
        throw new IllegalArgumentException("No ShapeType for '" + shapeType + "'");
    }

    /**
     * @return a newly created empty model instance
     * @param <T> Inferred model type
     * @since 3.3
     */
    @SuppressWarnings("unchecked")
    public <T extends IMutableShape> T createEmptyModel()
    {
        try
        {
            return (T) modelConstructor.newInstance();
        } catch (ReflectiveOperationException e)
        {
            throw new RuntimeException("Failed to create model instance for " + shapeModelClass, e);
        }
    }

    @Override
    public String toString()
    {
        return getShapeType();
    }
}
