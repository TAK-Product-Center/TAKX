package gov.takx.core.globe.tak.editor;

import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.shapes.IShape.AltitudeMode;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.beans.PropertyChangeListener;
import java.util.Collection;

/**
 * A representation of a control point for shape editing.
 *
 * @since 5.4
 */
public interface IControlPoint
{
    String DRAGGABLE = "DRAGGABLE";
    /** @since 5.6 */
    String DRAGGING = "DRAGGING";
    String LOCATION = "LOCATION";
    String SELECTED = "SELECTED";
    String CONTROL_POINT_TYPE = "CONTROL_POINT_TYPE";
    String POINT_SIZE = "POINT_SIZE";
    String COLOR = "COLOR";
    String HIGHLIGHT_COLOR = "HIGHLIGHT_COLOR";
    /** @since 5.6 */
    String ALTITUDE_MODE = "ALTITUDE_MODE";

    int DEFAULT_POINT_SIZE = 48;
    int DEFAULT_COLOR = 0xFFFFFFFF;
    int DEFAULT_HIGHLIGHT_COLOR = 0xFF00FF00;

    String CONTROL_POINT_CENTER = "CONTROL_POINT_CENTER";
    String CONTROL_POINT_NORTH = "CONTROL_POINT_NORTH";
    String CONTROL_POINT_EAST = "CONTROL_POINT_EAST";
    String CONTROL_POINT_SOUTH = "CONTROL_POINT_SOUTH";
    String CONTROL_POINT_WEST = "CONTROL_POINT_WEST";
    String CONTROL_POINT_NORTHWEST = "CONTROL_POINT_NORTHWEST";
    String CONTROL_POINT_NORTHEAST = "CONTROL_POINT_NORTHEAST";
    String CONTROL_POINT_SOUTHWEST = "CONTROL_POINT_SOUTHWEST";
    String CONTROL_POINT_SOUTHEAST = "CONTROL_POINT_SOUTHEAST";
    String CONTROL_POINT_ORIENTATION = "CONTROL_POINT_ORIENTATION";
    /** @since 5.9 */
    String CONTROL_POINT_2525 = "CONTROL_POINT_2525";
    /** @since 5.9 */
    String CONTROL_POINT_VERTEX_PREFIX = "CONTROL_POINT_VERTEX_";

    String MODIFIER_CTRL = "MODIFIER_CTRL";

    default void set(@Nonnull IControlPoint controlPoint)
    {
        setLocation(controlPoint.getLocation());
        setSelected(controlPoint.isSelected());
        setDraggable(controlPoint.isDraggable());
        setControlPointType(controlPoint.getControlPointType());
        setPointSize(controlPoint.getPointSize());
        setColor(controlPoint.getColor());
        setHighlightColor(controlPoint.getHighlightColor());
        setAltitudeMode(controlPoint.getAltitudeMode());
    }

    /**
     * Get the location of the control point.
     * @return the location of the control point
     */
    @Nonnull
    ILocation getLocation();

    /**
     * Set the location of the control point.
     * @param location the location of the control point
     */
    void setLocation(@Nonnull ILocation location);

    /**
     * Get whether the control point is selected.
     * @return {@code true} if selected, {@code false} if not
     */
    boolean isSelected();

    /**
     * Set whether the control point is selected.
     * @param selected {@code true} if selected, {@code false} if not
     */
    void setSelected(boolean selected);

    /**
     * Get whether the control point is draggable.
     * @return {@code true} if draggable, {@code false} if not
     */
    boolean isDraggable();

    /**
     * Set whether the control point is draggable.
     * @param draggable {@code true} if draggable, {@code false} if not
     */
    void setDraggable(boolean draggable);

    /**
     * Get whether the control point is currently being dragged.
     * @return {@code true} if dragging, {@code false} if not
     * @since 5.6
     */
    boolean isDragging();

    /**
     * Set whether the control point is currently being dragged.
     * @param dragging {@code true} if dragging, {@code false} if not
     * @since 5.6
     */
    void setDragging(boolean dragging);

    /**
     * Get the control point type.
     * @return the control point type
     */
    @Nonnull
    String getControlPointType();

    /**
     * Set the control point type.
     * @param controlPointType the control point type
     */
    void setControlPointType(@Nonnull String controlPointType);

    /**
     * Get the point size of the control point graphic.
     * @return the point size
     */
    int getPointSize();

    /**
     * Set the point size of the control point graphic.
     * @param pointSize the point size
     */
    void setPointSize(int pointSize);

    /**
     * Get the color of the control point graphic.
     * @return the control point color
     */
    int getColor();

    /**
     * Set the color of the control point graphic.
     * @param color the control point color
     */
    void setColor(int color);

    /**
     * Get the highlight color of the control point graphic.
     * @return the control point highlight color
     */
    int getHighlightColor();

    /**
     * Set the highlight color of the control point graphic.
     * @param highlightColor the control point highlight color
     */
    void setHighlightColor(int highlightColor);

    /**
     * Get the altitude mode for the shape vertices.
     *
     * @return the altitude mode for this shape
     * @since 5.6
     */
    @Nonnull
    AltitudeMode getAltitudeMode();

    /**
     * Set the altitude mode for the shape vertices.
     *
     * @param altitudeMode the altitude mode for this shape
     * @since 5.6
     */
    void setAltitudeMode(@Nonnull AltitudeMode altitudeMode);

    void addPropertyChangeListener(@Nonnull PropertyChangeListener listener);

    void removePropertyChangeListener(@Nonnull PropertyChangeListener listener);

    /**
     * Get the first control point with the provided control point type.
     * @param controlPoints the control points to search
     * @param controlPointType the control point to find
     * @return the control point or {@code null}
     */
    @Nullable
    static IControlPoint getControlPointByType(@Nonnull Collection<IControlPoint> controlPoints, @Nonnull String controlPointType)
    {
        return controlPoints.stream()
                .filter(cp -> controlPointType.equals(cp.getControlPointType()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get the opposite control point type for the provided control point type.
     * @param controlPointType the control point type
     * @return the opposite control point type, or {@code null} if there is none
     */
    @Nullable
    static String getOppositeControlPointType(@Nonnull String controlPointType)
    {
        return switch (controlPointType)
        {
            case IControlPoint.CONTROL_POINT_NORTH -> IControlPoint.CONTROL_POINT_SOUTH;
            case IControlPoint.CONTROL_POINT_SOUTH -> IControlPoint.CONTROL_POINT_NORTH;
            case IControlPoint.CONTROL_POINT_WEST -> IControlPoint.CONTROL_POINT_EAST;
            case IControlPoint.CONTROL_POINT_EAST -> IControlPoint.CONTROL_POINT_WEST;
            case IControlPoint.CONTROL_POINT_NORTHWEST -> IControlPoint.CONTROL_POINT_SOUTHEAST;
            case IControlPoint.CONTROL_POINT_NORTHEAST -> IControlPoint.CONTROL_POINT_SOUTHWEST;
            case IControlPoint.CONTROL_POINT_SOUTHWEST -> IControlPoint.CONTROL_POINT_NORTHEAST;
            case IControlPoint.CONTROL_POINT_SOUTHEAST -> IControlPoint.CONTROL_POINT_NORTHWEST;
            default -> null;
        };
    }

    /**
     * Given a {@code controlPointType}, return the vertex point index that it represents, or {@code null} if it doesn't represent a vertex control point.
     * @param controlPointType the control point type
     * @return the vertex index of the control point, or {@code null} if it is not a vertex control point
     * @since 5.8.1 (backported from 5.9)
     */
    static Integer getVertexIndex(@Nonnull String controlPointType)
    {
        if (!controlPointType.startsWith(IControlPoint.CONTROL_POINT_VERTEX_PREFIX)) return null;
        String intValue = controlPointType.substring(IControlPoint.CONTROL_POINT_VERTEX_PREFIX.length());
        try
        {
            return Integer.parseInt(intValue);
        } catch (NumberFormatException e)
        {
            return null;
        }
    }

    /**
     * Given a control point index, return the control point type that it represents.
     * @param index the control point index
     * @return the control point type
     * @since 5.8.1 (backported from 5.9)
     */
    static String toVertexControlPointType(int index)
    {
        return CONTROL_POINT_VERTEX_PREFIX + index;
    }
}
