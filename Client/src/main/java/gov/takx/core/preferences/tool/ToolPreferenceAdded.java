package gov.takx.core.preferences.tool;

import gov.takx.api.preferences.IPreferenceEntryChangeListener;
import gov.takx.core.preferences.ToolPreferenceManager;
import jakarta.annotation.Nonnull;

import javax.swing.ImageIcon;

import java.util.List;

/**
 * Event fired when a Tool Preference is added.
 *
 * @param toolDefinition Definition of tool preference that was added
 *
 * @see gov.takx.api.preferences.IToolPreferenceManager#addTool(String, String, String, ImageIcon, List)
 * @see gov.takx.api.preferences.IToolPreferenceManager#addTool(String, String, String, ImageIcon, List, IPreferenceEntryChangeListener)
 * @since 5.7
 */
public record ToolPreferenceAdded(@Nonnull ToolPreferenceManager.ToolDefinition toolDefinition)
{
}
