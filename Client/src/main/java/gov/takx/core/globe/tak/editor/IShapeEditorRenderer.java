package gov.takx.core.globe.tak.editor;

import jakarta.annotation.Nonnull;

/**
 * An interface to render the graphics for a shape editor.
 *
 * @since 5.4
 */
public interface IShapeEditorRenderer
{
    /**
     * Get the shape editor for this renderer.
     * @return the shape editor
     * @throws NullPointerException if this is called before {@link #setShapeEditor(IShapeEditor)} is called
     */
    @Nonnull
    IShapeEditor getShapeEditor();

    /**
     * Set the shape editor for this renderer.
     * @param shapeEditor the shape editor
     */
    void setShapeEditor(@Nonnull IShapeEditor shapeEditor);

    /**
     * Set whether this editor is enabled. If enabled, the editor will be visible and ready for input.
     * @param enabled {@code true} if enabled, {@code false} if not
     */
    void setEnabled(boolean enabled);

    /**
     * Set whether this editor is armed. If armed, the editor will be armed to create a new object.
     * @param armed {@code true} if armed, {@code false} if not
     */
    void setArmed(boolean armed);
}
