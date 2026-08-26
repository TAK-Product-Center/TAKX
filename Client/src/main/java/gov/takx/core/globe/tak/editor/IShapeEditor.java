package gov.takx.core.globe.tak.editor;

/**
 * An interface to manage a shape editor and the {@link IShapeEditorSource}, {@link IControlPointController}, and {@link IShapeEditorRenderer} objects that implement the shape editing behavior.
 *
 * @since 5.4
 */
public interface IShapeEditor
{
    /**
     * Get whether this editor is enabled. If enabled, the editor will be visible and ready for input.
     * @return {@code true} if enabled, {@code false} if not
     */
    boolean isEnabled();

    /**
     * Set whether this editor is enabled. If enabled, the editor will be visible and ready for input.
     * @param enabled {@code true} if enabled, {@code false} if not
     */
    boolean setEnabled(boolean enabled);

    /**
     * Get whether this editor is armed. If armed, the editor will be armed to create a new object.
     * @return {@code true} if armed, {@code false} if not
     */
    boolean isArmed();

    /**
     * Set whether this editor is armed. If armed, the editor will be armed to create a new object.
     * @param armed {@code true} if armed, {@code false} if not
     */
    boolean setArmed(boolean armed);

    /**
     * Get the {@link IShapeEditorSource} for this editor.
     * @return the shape editor source object
     */
    IShapeEditorSource<?> getShapeEditorSource();

    /**
     * Get the {@link IControlPointController} for this editor.
     * @return the control point controller object
     */
    IControlPointController getControlPointController();

    /**
     * Get the {@link IShapeEditorRenderer} for this editor.
     * @return the shape editor renderer object
     */
    IShapeEditorRenderer getShapeEditorRenderer();

    /**
     * Apply all edits in the editor to the source object.
     * @return {@code true} if successful, {@code false} if not
     */
    boolean applyChangesToSource();

    void addShapeEditorEventListener(ShapeEditorEventListener listener);

    void removeShapeEditorEventListener(ShapeEditorEventListener listener);

    /**
     * Dispose all resources used by the editor. The editor is no longer usable after disposing.
     */
    void dispose();

    /**
     * Listener for {@link IShapeEditor} events.
     */
    interface ShapeEditorEventListener
    {
        /**
         * Event indicating the shape editor is armed.
         */
        void onArm();

        /**
         * Event indicating the shape editor is disarmed.
         */
        void onDisarm();

        /**
         * Event indicating the shape editor has applied changes to the source.
         */
        void onApplyChangesToSource();
    }
}
