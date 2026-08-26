package gov.takx.core.view.gui.preferences;

import gov.takx.api.preferences.IPreferenceEntry;
import gov.takx.api.preferences.tool.CheckBoxPreference;
import gov.takx.api.preferences.tool.ComboBoxPreference;
import gov.takx.api.preferences.tool.CustomControlPreference;
import gov.takx.api.preferences.tool.HiddenPreference;
import gov.takx.api.preferences.tool.SliderPreference;
import gov.takx.api.preferences.tool.TextPreference;
import gov.takx.api.preferences.tool.ToggleButtonPreference;
import gov.takx.core.preferences.ToolPreferenceManager.ToolDefinition;
import gov.takx.core.view.gui.preferences.tools.ToolPreferenceEntry;
import jakarta.annotation.Nonnull;

import javax.swing.JComponent;

/**
 * Preferences page for a single tool, providing editors for the tool's defined preference entries.
 *
 * @since 5.8
 */
class ToolPreferencesPage extends APreferencesPage
{
    private final ToolDefinition toolDefinition;

    public ToolPreferencesPage(ToolDefinition toolDefinition)
    {
        super(toolDefinition.title());
        this.toolDefinition = toolDefinition;

        toolDefinition.preferenceEntries().forEach(this::addToolPreferenceEditor);
    }

    public ToolDefinition getToolDefinition()
    {
        return toolDefinition;
    }

    /**
     * Add the appropriate editor tile for the given tool preference entry.
     */
    private void addToolPreferenceEditor(@Nonnull ToolPreferenceEntry preferenceEntry)
    {
        final var toolPreference = preferenceEntry.getToolPreference();

        //noinspection StatementWithEmptyBody
        if (toolPreference instanceof HiddenPreference)
        {
            // Hidden preferences are not shown on the page
        } else if (toolPreference instanceof CheckBoxPreference)
        {
            addCheckboxEditor(preferenceEntry);
        } else if (toolPreference instanceof ToggleButtonPreference toggleButtonPreference)
        {
            addToggleButtonEditor(preferenceEntry, toggleButtonPreference.getText(), toggleButtonPreference.getIcon());
        } else if (toolPreference instanceof ComboBoxPreference comboBoxPreference)
        {
            addEditor(preferenceEntry, PreferencesComponentFactory.makeComboBox(comboBoxPreference.getItems()));
        } else if (toolPreference instanceof SliderPreference sliderPreference)
        {
            addEditor(preferenceEntry, PreferencesComponentFactory.makeSliderEditor(sliderPreference.getMin(), sliderPreference.getMax()));
        } else if (toolPreference instanceof TextPreference)
        {
            addEditor(preferenceEntry, PreferencesComponentFactory.makeTextEditor());
        } else if (toolPreference instanceof CustomControlPreference customControlPreference)
        {
            addEditor(preferenceEntry, new CustomEditorComponent(preferenceEntry, customControlPreference));
        } else
        {
            throw new IllegalArgumentException("Unknown tool preference class: " + toolPreference.getClass().getName());
        }
    }

    /**
     * Adapter to make a {@link CustomControlPreference} behave like an {@link IEditorComponent}.
     */
    private static class CustomEditorComponent implements IEditorComponent
    {
        private final CustomControlPreference preference;

        CustomEditorComponent(@Nonnull IPreferenceEntry entry, @Nonnull CustomControlPreference preference)
        {
            this.preference = preference;
            updateFromPreference(entry); // Initialize the custom control from the current preference value
        }

        @Override
        public void updateFromPreference(@Nonnull IPreferenceEntry entry)
        {
            preference.getValueRestorer().accept(entry.getStringValue());
        }

        @Override
        public void applyToPreference(@Nonnull IPreferenceEntry entry)
        {
            entry.putAndSave(preference.getValueSupplier().get());
        }

        @Override
        public String getDisplayValue(@Nonnull IPreferenceEntry entry)
        {
            return preference.getValueSupplier().get();
        }

        @Override
        public JComponent getComponent()
        {
            return preference.getControl();
        }
    }
}
