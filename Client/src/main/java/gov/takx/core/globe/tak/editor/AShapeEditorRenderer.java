package gov.takx.core.globe.tak.editor;

import jakarta.annotation.Nonnull;

import java.util.Objects;

/**
 * Base class for building an {@link IShapeEditorRenderer} implementation.
 *
 * @since 5.4
 */
public abstract class AShapeEditorRenderer implements IShapeEditorRenderer
{
    private IShapeEditor shapeEditor;

    @Override
    @Nonnull
    public IShapeEditor getShapeEditor()
    {
        return Objects.requireNonNull(shapeEditor);
    }

    @Override
    public void setShapeEditor(@Nonnull IShapeEditor shapeEditor)
    {
        this.shapeEditor = shapeEditor;
    }
}
