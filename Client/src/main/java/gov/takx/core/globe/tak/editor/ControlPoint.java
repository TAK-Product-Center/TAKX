package gov.takx.core.globe.tak.editor;

import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.shapes.IShape.AltitudeMode;
import jakarta.annotation.Nonnull;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * An implementation of {@link IControlPoint}.
 *
 * @since 5.4
 */
public class ControlPoint implements IControlPoint
{
    private ILocation location;
    private boolean selected;
    private boolean draggable;
    private boolean dragging;
    private String controlPointType;
    private int pointSize;
    private int color;
    private int highlightColor;
    private AltitudeMode altitudeMode;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    /**
     * @param def a {@link IShapeEditorSource.ControlPointDefinition control point definition} that describes the properties of the control point
     */
    public ControlPoint(@Nonnull IShapeEditorSource.ControlPointDefinition def)
    {
        this(def.location(), def.controlPointType(), def.draggable(), def.pointSize(), def.color(), def.highlightColor(), def.altitudeMode());
    }

    /**
     * @param location the location of the control point
     * @param controlPointType the control point type
     */
    public ControlPoint(@Nonnull ILocation location, String controlPointType, AltitudeMode altitudeMode)
    {
        this(location, controlPointType, true, DEFAULT_POINT_SIZE, DEFAULT_COLOR, DEFAULT_HIGHLIGHT_COLOR, altitudeMode);
    }

    /**
     * @param location the location of the control point
     * @param controlPointType the control point type
     * @param draggable {@code true} if draggable, {@code false} if not
     * @param pointSize the point size
     * @param color the control point color
     * @param highlightColor the control point highlight color
     */
    public ControlPoint(@Nonnull ILocation location, @Nonnull String controlPointType, boolean draggable, int pointSize, int color, int highlightColor, AltitudeMode altitudeMode)
    {
        this.location = location;
        this.controlPointType = controlPointType;
        this.draggable = draggable;
        this.pointSize = pointSize;
        this.color = color;
        this.highlightColor = highlightColor;
        this.altitudeMode = altitudeMode;
    }

    @Override
    @Nonnull
    public ILocation getLocation()
    {
        return location;
    }

    @Override
    public void setLocation(@Nonnull ILocation location)
    {
        ILocation oldValue = this.location;
        this.location = location;
        pcs.firePropertyChange(LOCATION, oldValue, this.location);
    }

    @Override
    public boolean isSelected()
    {
        return selected;
    }

    @Override
    public void setSelected(boolean selected)
    {
        boolean oldValue = this.selected;
        this.selected = selected;
        pcs.firePropertyChange(SELECTED, oldValue, this.selected);
    }

    @Override
    public boolean isDraggable()
    {
        return draggable;
    }

    @Override
    public void setDraggable(boolean draggable)
    {
        boolean oldValue = this.draggable;
        this.draggable = draggable;
        pcs.firePropertyChange(DRAGGABLE, oldValue, this.draggable);
    }

    @Override
    public boolean isDragging()
    {
        return dragging;
    }

    @Override
    public void setDragging(boolean dragging)
    {
        boolean oldValue = this.dragging;
        this.dragging = dragging;
        pcs.firePropertyChange(DRAGGING, oldValue, this.dragging);
    }

    @Override
    @Nonnull
    public String getControlPointType()
    {
        return controlPointType;
    }

    @Override
    public void setControlPointType(@Nonnull String controlPointType)
    {
        String oldValue = this.controlPointType;
        this.controlPointType = controlPointType;
        pcs.firePropertyChange(CONTROL_POINT_TYPE, oldValue, this.controlPointType);
    }

    @Override
    public int getPointSize()
    {
        return pointSize;
    }

    @Override
    public void setPointSize(int pointSize)
    {
        int oldValue = this.pointSize;
        this.pointSize = pointSize;
        pcs.firePropertyChange(POINT_SIZE, oldValue, this.pointSize);
    }

    @Override
    public int getColor()
    {
        return color;
    }

    @Override
    public void setColor(int color)
    {
        int oldValue = this.color;
        this.color = color;
        pcs.firePropertyChange(COLOR, oldValue, this.color);
    }

    @Override
    public int getHighlightColor()
    {
        return highlightColor;
    }

    @Override
    public void setHighlightColor(int highlightColor)
    {
        int oldValue = this.highlightColor;
        this.highlightColor = highlightColor;
        pcs.firePropertyChange(HIGHLIGHT_COLOR, oldValue, this.highlightColor);
    }

    @Override
    @Nonnull
    public AltitudeMode getAltitudeMode()
    {
        return altitudeMode;
    }

    @Override
    public void setAltitudeMode(@Nonnull AltitudeMode altitudeMode)
    {
        AltitudeMode oldValue = this.altitudeMode;
        this.altitudeMode = altitudeMode;
        pcs.firePropertyChange(ALTITUDE_MODE, oldValue, this.altitudeMode);
    }

    @Override
    public void addPropertyChangeListener(@Nonnull PropertyChangeListener listener)
    {
        pcs.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(@Nonnull PropertyChangeListener listener)
    {
        pcs.removePropertyChangeListener(listener);
    }

    @Override
    public String toString()
    {
        return "ControlPoint{" +
                "controlPointType='" + controlPointType + '\'' +
                ", draggable=" + draggable +
                ", selected=" + selected +
                ", location=" + location +
                '}';
    }
}
