package gov.takx.api.preferences;

import gov.takx.api.annotation.Experimental;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.preferences.tool.IToolPreference;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.ImageIcon;

import java.util.List;

/**
 * Contract for a manager that allows plugins to add custom preferences to the "Tools" preferences panel.
 * <p>
 * When a tool preference is created, a special form of {@link IPreferenceEntry} is created to manage it within
 * the overall preferences framework, including persistence of values.  However, unlike other preferences, like
 * {@link gov.takx.platform.preferences.PreferenceEntry} or {@link gov.takx.platform.preferences.UserPreferenceEntry},
 * tool preference entries are transient by nature, and the normal binding rules for CDI cannot be used.
 * <p>
 * To support CDI use with tool preferences, you can't directly inject the current value of a tool preference before it
 * has been created.  You can, however, inject a {@link java.util.function.Supplier} of the value you want, and then
 * later use the {@link java.util.function.Supplier#get()} method to obtain the current value.  To do this, you use the
 * {@link gov.takx.api.annotation.ToolPreference} annotation to specify the tool UID and preference ID for the desired
 * preference. For example:
 * <pre>{@code
 *     @Inject
 *     @ToolPreference(toolUid = TOOL_UID, preferenceId = BOOLEAN_PREF_ID)
 *     private Supplier<Boolean> booleanPreferenceValueSupplier;
 * }</pre>
 * If you want to inject a reference to the tool preference entry, which allows for modifying the value, versus
 * retrieving it from this manager, you can inject a supplier for it.  For example:
 * <pre>{@code
 *     @Inject
 *     @ToolPreference(toolUid = TOOL_UID, preferenceId = BOOLEAN_PREF_ID)
 *     private Supplier<IPreferenceEntry> booleanPreferenceSupplier;
 * }</pre>
 * Similarly, observing preference value changes on tool preferences makes use of a {@link gov.takx.api.annotation.ToolPreferenceType}
 * annotation to specify the preference to observe. For example:
 * <pre>{@code
 *     private void onPreferenceChange(@Observes @ToolPreferenceType(toolUid = TOOL_UID, preferenceId = BOOLEAN_PREF_ID) PreferenceEntryChange change) {
 *         final var entry = change.getPreferenceEntry();
 *         final boolean newValue = entry.getBooleanValue();
 *         ...
 *     }
 * }</pre>
 * A plugin may request the display of its preferences page in the settings panel using the {@link #showToolSettings(String)}
 * method.
 *
 * @since 5.0
 */
@NoPluginImplementations
@Experimental
public interface IToolPreferenceManager extends IPreferenceManager
{
    /**
     * @param toolUid The tool UID. This id <em>must</em> be unique to the tool. All the tool's preferences will be 
     *                added to a preference node with this name. The {@link IToolPreference}s returned by
     *                {@link IPreferenceEntryChangeListener}s keys will have the prefix "[toolUid]/" 
     * @param toolName The name that will be displayed in the tool list and the tool preferences panel
     * @param description The description that will be displayed in the tool list
     * @param icon The tool icon that will be displayed in the tool list and the tool preferences panel
     * @param entries The tool preference entries to add to the tool preferences panel
     */
    void addTool(@Nonnull String toolUid, @Nonnull String toolName, @Nonnull String description, @Nonnull ImageIcon icon,
                 @Nonnull List<IToolPreference> entries);

    /**
     * @param toolUid The tool UID. This id <em>must</em> be unique to the tool. All the tool's preferences will be
     *                added to a preference node with this name. The {@link IToolPreference}s returned by
     *                {@link IPreferenceEntryChangeListener}s keys will have the prefix "[toolUid]/"
     * @param toolName The name that will be displayed in the tool list and the tool preferences panel
     * @param description The description that will be displayed in the tool list
     * @param icon The tool icon that will be displayed in the tool list and the tool preferences panel
     * @param entries The tool preference entries to add to the tool preferences panel
     * @param listener Optional change listener
     * @throws IllegalArgumentException thrown if a tool with the specified UID already exists
     */
    void addTool(@Nonnull String toolUid, @Nonnull String toolName, @Nonnull String description, @Nonnull ImageIcon icon,
                 @Nonnull List<IToolPreference> entries, @Nullable IPreferenceEntryChangeListener listener)
            throws IllegalArgumentException;

    /**
     * Removes the tool from the manager and the UI
     *
     * @param toolUid The tool's UID
     */
    void removeTool(@Nonnull String toolUid);

    /**
     * Deletes the tool preferences from persistence. The tool will also be removed from the manager and the UI if it is
     * currently loaded.
     *
     * @param toolUid The tool's UID
     */
    void deleteTool(@Nonnull String toolUid);

    /**
     * @param toolUid The tool's UID
     * @return The tool's {@link IPreferenceEntry}s or {@code null} if there is no matching tool
     */
    @Nullable
    List<IPreferenceEntry> getToolPreferenceEntries(@Nonnull String toolUid);

    /**
     * @param toolUid The tool's UID
     * @param preferenceId The preference id
     * @return The tool's {@link IPreferenceEntry} or {@code null} if there is no matching tool or preference
     */
    @Nullable
    IPreferenceEntry getToolPreferenceEntry(@Nonnull String toolUid, @Nonnull String preferenceId);

    /**
     * Open the Settings tab (if not already open), and show the preferences page for the given tool.
     *
     * @param toolUid The tool's UID
     * @since 5.8
     */
    @SuppressWarnings("unused") // Only called from plugins
    void showToolSettings(@Nonnull String toolUid);
}