package gov.takx.core.preferences.tool;

import gov.takx.core.preferences.ToolPreferenceManager;
import jakarta.annotation.Nonnull;

/**
 * Event fired when a Tool Preference is removed.
 *
 * @param toolDefinition Definition of tool preference that was removed
 * @see gov.takx.api.preferences.IToolPreferenceManager#removeTool(String)
 * @since 5.7
 */
public record ToolPreferenceRemoved(@Nonnull ToolPreferenceManager.ToolDefinition toolDefinition)
{
}
