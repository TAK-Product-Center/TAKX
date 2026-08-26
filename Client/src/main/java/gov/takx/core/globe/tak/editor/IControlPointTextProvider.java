package gov.takx.core.globe.tak.editor;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * An interface to report back text for a control point. Implemented by {@link IShapeEditorSource} objects.
 *
 * @since 5.4
 */
public interface IControlPointTextProvider
{
    /**
     * Get the text to be displayed for the provided control point.
     * @param controlPoint the control point
     * @return the text for the control point, or {@code null} if no text should be displayed
     */
    @Nullable
    String getControlPointText(@Nonnull IControlPoint controlPoint);
}
