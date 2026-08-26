package gov.takx.core.globe.tak.editor;

import jakarta.annotation.Nonnull;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Base class for building an {@link IShapeEditor} implementation. Provides lifecycle management of an {@link IShapeEditorSource}, an {@link IControlPointController}, and an {@link IShapeEditorSource} object.
 *
 * @since 5.4
 */
public abstract class AShapeEditor implements IShapeEditor
{
    private final Set<ShapeEditorEventListener> listeners = new LinkedHashSet<>();
    private final IShapeEditorSource<?> shapeEditorSource;
    private final IControlPointController controlPointController;
    private final IShapeEditorRenderer shapeEditorRenderer;
    private boolean enabled;
    private boolean armed;

    /**
     * Create a shape editor with the provided implementations.
     *
     * @param shapeEditorSource the shape editor source implementation
     * @param controlPointController the control point controller implementation
     * @param shapeEditorRenderer the shape editor renderer implementation
     */
    protected AShapeEditor(@Nonnull IShapeEditorSource<?> shapeEditorSource, @Nonnull IControlPointController controlPointController, @Nonnull IShapeEditorRenderer shapeEditorRenderer)
    {
        this.shapeEditorSource = shapeEditorSource;
        this.controlPointController = controlPointController;
        this.shapeEditorRenderer = shapeEditorRenderer;
        controlPointController.setShapeEditor(this);
        shapeEditorRenderer.setShapeEditor(this);
        shapeEditorSource.setShapeEditor(this);
    }

    @Override
    public boolean isEnabled()
    {
        return enabled;
    }

    @Override
    public boolean setEnabled(boolean enabled)
    {
        boolean changed = this.enabled != enabled;
        this.enabled = enabled;
        if (changed)
        {
            shapeEditorSource.setEnabled(enabled);
            controlPointController.setEnabled(enabled);
            shapeEditorRenderer.setEnabled(enabled);
        }
        return changed;
    }

    @Override
    public boolean isArmed()
    {
        return armed;
    }

    @Override
    public boolean setArmed(boolean armed)
    {
        boolean changed = this.armed != armed;
        this.armed = armed;
        if (changed)
        {
            shapeEditorSource.setArmed(armed);
            controlPointController.setArmed(armed);
            shapeEditorRenderer.setArmed(armed);
            if (armed)
            {
                onArm();
            } else
            {
                onDisarm();
            }
        }
        return changed;
    }

    @Override
    public void dispose()
    {
        if (armed) setArmed(false);
        if (enabled) setEnabled(false);
    }

    @Override
    public void addShapeEditorEventListener(ShapeEditorEventListener listener)
    {
        listeners.add(listener);
    }

    @Override
    public void removeShapeEditorEventListener(ShapeEditorEventListener listener)
    {
        listeners.remove(listener);
    }

    /**
     * Send an {@link ShapeEditorEventListener#onArm()} event to listeners.
     */
    protected void onArm()
    {
        listeners.forEach(ShapeEditorEventListener::onArm);
    }

    /**
     * Send an {@link ShapeEditorEventListener#onDisarm()} event to listeners.
     */
    protected void onDisarm()
    {
        listeners.forEach(ShapeEditorEventListener::onDisarm);
    }

    /**
     * Send an {@link ShapeEditorEventListener#onApplyChangesToSource()} event to listeners.
     */
    protected void onApplyChangesToSource()
    {
        listeners.forEach(ShapeEditorEventListener::onApplyChangesToSource);
    }

    @Override
    public IShapeEditorSource<?> getShapeEditorSource()
    {
        return shapeEditorSource;
    }

    @Override
    public IControlPointController getControlPointController()
    {
        return controlPointController;
    }

    @Override
    public IShapeEditorRenderer getShapeEditorRenderer()
    {
        return shapeEditorRenderer;
    }
}
