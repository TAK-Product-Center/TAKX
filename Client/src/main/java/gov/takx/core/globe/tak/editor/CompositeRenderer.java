package gov.takx.core.globe.tak.editor;

import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * An implementation of {@link IShapeEditorRenderer} that composites multiple renderers.
 *
 * @since 5.4
 */
public class CompositeRenderer extends AShapeEditorRenderer
{
    private final List<IShapeEditorRenderer> renderers;

    /**
     * @param renderers the renderers to composite together
     */
    public CompositeRenderer(IShapeEditorRenderer... renderers)
    {
        this.renderers = List.of(renderers);
    }

    @Override
    @Nonnull
    public IShapeEditor getShapeEditor()
    {
        return renderers.get(0).getShapeEditor();
    }

    @Override
    public void setShapeEditor(@Nonnull IShapeEditor shapeEditor)
    {
        renderers.forEach(r -> r.setShapeEditor(shapeEditor));
    }

    @Override
    public void setEnabled(boolean enabled)
    {
        renderers.forEach(r -> r.setEnabled(enabled));
    }

    @Override
    public void setArmed(boolean armed)
    {
        renderers.forEach(r -> r.setArmed(armed));
    }
}
