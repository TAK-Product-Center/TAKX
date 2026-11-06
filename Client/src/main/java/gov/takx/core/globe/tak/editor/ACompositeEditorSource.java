package gov.takx.core.globe.tak.editor;

import gov.takx.api.coordinatesystem.ILocation;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Base class for building an {@link IShapeEditorSource} implementation that defers implementation of methods to a list of {@link IControlPointProvider}.
 *
 * @param <T> the type of source object being edited
 * @since 5.8.1 (backported from 5.9)
 */
public abstract class ACompositeEditorSource<T> extends AShapeEditorSource<T> implements IControlPointTextProvider
{
    private final List<IControlPointProvider> controlPointProviders;

    /**
     * @param source the source object being edited
     * @param controlPointProviders the list of control point providers to defer to
     */
    protected ACompositeEditorSource(@Nonnull T source, @Nonnull List<IControlPointProvider> controlPointProviders)
    {
        super(source);
        this.controlPointProviders = controlPointProviders;
    }

    @Override
    public void setShapeEditor(@Nonnull IShapeEditor shapeEditor)
    {
        controlPointProviders.forEach(it -> it.setShapeEditor(shapeEditor));
        super.setShapeEditor(shapeEditor);
    }

    @Override
    @Nonnull
    public List<ControlPointDefinition> createControlPointDefinitions()
    {
        return controlPointProviders.stream().flatMap(it -> it.createControlPointDefinitions().stream()).toList();
    }

    @Override
    public void onControlPointDragged(@Nonnull String controlPointType, @Nonnull ILocation location)
    {
        controlPointProviders.forEach(controlPointProvider -> controlPointProvider.onControlPointDragged(controlPointType, location, () -> onSourceUpdated(getSource())));
    }

    @Override
    public void onControlPointAdded(@Nonnull ILocation location, @Nonnull Map<String, Object> metadata)
    {
        controlPointProviders.forEach(it -> it.onControlPointAdded(location, metadata, () -> onSourceUpdated(getSource())));
        super.onControlPointAdded(location, metadata);
    }

    @Override
    public void onControlPointRemoved(@Nonnull IControlPoint controlPoint)
    {
        controlPointProviders.forEach(it -> it.onControlPointRemoved(controlPoint, () -> onSourceUpdated(getSource())));
        super.onControlPointRemoved(controlPoint);
    }

    @Override
    public String getControlPointText(@Nonnull IControlPoint controlPoint)
    {
        return controlPointProviders.stream()
                .map(it -> it.getControlPointText(controlPoint))
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n")).stripTrailing();
    }
}
