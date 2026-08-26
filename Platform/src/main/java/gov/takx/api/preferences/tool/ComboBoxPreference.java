package gov.takx.api.preferences.tool;

import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines an integer {@link gov.takx.api.preferences.IPreferenceEntry} that will be controlled by a JComboBox. The
 * preference entry's integer value will correspond to the index of selected item.
 * 
 * @since 5.0
 */
public class ComboBoxPreference extends AToolPreference
{
    @Nonnull
    private final List<String> items;

    /**
     * @param id The preference id. This id will be combined with the tool uid to form a unique preference entry id.
     *           See {@link IToolPreference#getId()}.
     * @param title The title that will be displayed in the tool preference list
     * @param description The description that will be displayed in the tool preference list
     * @param defaultValue The initial value to select if no value has been stored. The value must match one of the 
     *                     values in the {@code items} list
     * @param items The items to display in the combo box
     */
    public ComboBoxPreference(@Nonnull String id, @Nonnull String title, @Nonnull String description,
                              String defaultValue, @Nonnull List<String> items)
    {
        super(id, title, description, defaultValue);
        this.items = new ArrayList<>(items);
    }

    /**
     * @return The list of items to be displayed by the combo box
     */
    @Nonnull
    public List<String> getItems()
    {
        return new ArrayList<>(items);
    }
}