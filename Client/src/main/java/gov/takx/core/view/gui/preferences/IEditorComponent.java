package gov.takx.core.view.gui.preferences;

import gov.takx.api.preferences.IPreferenceEntry;
import jakarta.annotation.Nonnull;

import javax.swing.JComponent;

/**
 * Contract for a preference value editor.
 *
 * @since 5.4
 */
public interface IEditorComponent
{
    /**
     * Install the current value of the given preference {@code entry} into the editor.
     */
    void updateFromPreference(@Nonnull IPreferenceEntry entry);

    /**
     * Store the current value of the editor into the given preference {@code entry} and save the preferences.
     */
    void applyToPreference(@Nonnull IPreferenceEntry entry);

    /**
     * @return The value to display to the user for the current value of the given preference {@code entry}
     */
    default String getDisplayValue(@Nonnull IPreferenceEntry entry)
    {
        return entry.getStringValue();
    }

    /**
     * @return The UI component comprising the editor, by default the component implements this interface directly
     */
    default JComponent getComponent()
    {
        return (JComponent) this;
    }
}
