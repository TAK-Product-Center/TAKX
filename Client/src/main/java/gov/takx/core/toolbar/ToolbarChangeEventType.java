package gov.takx.core.toolbar;

import jakarta.annotation.Nonnull;

/**
 * The types of change events which occur for a toolbar configuration.
 *
 * @since 2.0 (Renamed in 3.4)
 */
public enum ToolbarChangeEventType
{
    TAB_ADDED(ToolbarComponentType.CONFIGURATION),
    TAB_REMOVED(ToolbarComponentType.CONFIGURATION),
    TAB_TEXT_CHANGED(ToolbarComponentType.TAB),
    TAB_VALIDITY_CHANGED(ToolbarComponentType.TAB),
    TAB_VISIBILITY_CHANGED(ToolbarComponentType.TAB),
    COMPONENT_ADDED(ToolbarComponentType.TAB),
    COMPONENT_REMOVED(ToolbarComponentType.TAB),
    BUTTON_IMAGE_CHANGED(ToolbarComponentType.BUTTON),
    BUTTON_LISTENER_ADDED(ToolbarComponentType.BUTTON),
    BUTTON_LISTENER_REMOVED(ToolbarComponentType.BUTTON),
    BUTTON_STATE_CHANGED(ToolbarComponentType.BUTTON),
    BUTTON_TEXT_CHANGED(ToolbarComponentType.BUTTON),
    BUTTON_TOOLTIP_CHANGED(ToolbarComponentType.BUTTON),
    BUTTON_VALIDITY_CHANGED(ToolbarComponentType.BUTTON);

    private final ToolbarComponentType sourceType;

    ToolbarChangeEventType(@Nonnull ToolbarComponentType sourceType)
    {
        this.sourceType = sourceType;
    }

    @Nonnull
    public ToolbarComponentType getSourceType()
    {
        return sourceType;
    }
}
