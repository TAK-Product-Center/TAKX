package gov.takx.core.globe.tak.editor;

import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.shapes.IShape.AltitudeMode;
import gov.takx.core.globe.tak.editor.IShapeEditorSource.ControlPointDefinition;
import jakarta.annotation.Nonnull;

import java.util.Objects;

/**
 * Base class for building an {@link IControlPointProvider} implementation.
 *
 * @param <T> the type of source object being edited
 * @since 5.8.1 (backported from 5.9)
 */
public abstract class AControlPointProvider<T> implements IControlPointProvider
{
    private final T source;
    private IShapeEditor shapeEditor;

    /**
     * @param source the source object being edited
     */
    protected AControlPointProvider(@Nonnull T source)
    {
        this.source = source;
    }

    @Override
    public void setShapeEditor(@Nonnull IShapeEditor shapeEditor)
    {
        this.shapeEditor = shapeEditor;
    }

    /**
     * Get the source object.
     * @return the source object
     */
    @Nonnull
    public T getSource()
    {
        return source;
    }

    /**
     * Get the shape editor for this renderer.
     * @return the shape editor
     * @throws NullPointerException if this is called before {@link #setShapeEditor(IShapeEditor)} is called
     */
    @Nonnull
    public IShapeEditor getShapeEditor()
    {
        return Objects.requireNonNull(shapeEditor);
    }

    /**
     * Create a {@link ControlPointDefinition} that is properly IDL wrapped.
     * @param point the control point location with altitude
     * @param controlPointType the control point type
     * @param editable {@code true} if the control point is editable, {@code false} if not
     * @param altitudeMode the control point altitude mode
     * @return the control point definition
     * @since 5.8.1 (backported from 5.9)
     */
    static ControlPointDefinition createControlPointDefinition(@Nonnull IGeoPoint point, @Nonnull String controlPointType, boolean editable, @Nonnull AltitudeMode altitudeMode)
    {
        return new ControlPointDefinition(new GeoPoint(point.getLatitude(), wrapIdl(point.getLongitude()), point.getAltitude()), controlPointType, editable, altitudeMode);
    }

    /**
     * Create a {@link ControlPointDefinition} that is properly IDL wrapped.
     * @param point the control point location
     * @param altitude the control point altitude
     * @param controlPointType the control point type
     * @param editable {@code true} if the control point is editable, {@code false} if not
     * @param altitudeMode the control point altitude mode
     * @return the control point definition
     * @since 5.8.1 (backported from 5.9)
     */
    static ControlPointDefinition createControlPointDefinition(@Nonnull IGeoPoint point, double altitude, @Nonnull String controlPointType, boolean editable, @Nonnull AltitudeMode altitudeMode)
    {
        return new ControlPointDefinition(new GeoPoint(point.getLatitude(), wrapIdl(point.getLongitude()), altitude), controlPointType, editable, altitudeMode);
    }

    /**
     * Wrap a longitude value that has potentially crossed the IDL. Resulting value will be -180 <= lon <= 180.
     * @param longitude the unwrapped longitude value
     * @return the wrapped longitude value if it crossed the IDL, or the original longitude value
     * @since 5.8.1 (backported from 5.9) (moved from AShapeEditorSource)
     */
    static double wrapIdl(double longitude)
    {
        longitude = longitude < -180d ? longitude + 360 : longitude;
        longitude = longitude > 180d ? longitude - 360 : longitude;
        return longitude;
    }
}
