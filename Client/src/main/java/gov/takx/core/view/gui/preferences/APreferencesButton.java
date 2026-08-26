package gov.takx.core.view.gui.preferences;

import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.preferences.IPreferenceEntryChangeListener;
import gov.takx.api.preferences.PreferenceEntryChange;
import gov.takx.api.ui.APopupButton;
import gov.takx.platform.preferences.UserPreferenceEntry;

import javax.swing.JMenuItem;

import java.util.List;

/**
 * Abstract class that creates a button out of all of a given preference keys values.
 *
 */
public abstract class APreferencesButton extends APopupButton implements IPreferenceEntryChangeListener
{
    protected final UserPreferenceEntry preferenceEntry;
    protected final List<String> possibleValues;

    /**
     * Constructor.
     *
     * @param toolTip         tool tip text to display for the main button that contains the pop-up menu.
     * @param preferenceEntry preferences entry
     */
    public APreferencesButton(String toolTip, UserPreferenceEntry preferenceEntry)
    {
        super(toolTip);

        this.preferenceEntry = preferenceEntry;

        // Get the possible values that this preferences key can have.
        possibleValues = PreferenceButtonUtils.getPossibleKeyValues(preferenceEntry);

        //As long as we have some values, create menu items out of all of them.
        for (String value : possibleValues)
        {
            JMenuItem menuItem = new JMenuItem(getMenuLabelForValue(value));
            menuItem.setName(value); // Note: this is where we get the preference value from.
            menuItem.addActionListener(e -> updatePreferencesValue(((JMenuItem) e.getSource()).getName()));

            addMenuItem(menuItem); // Add the new menu item.
        }

        preferenceEntry.addPreferenceEntryChangeListener(this);

        setSelectedMenuItem(preferenceEntry.getStringValue());
    }

    /**
     * Get the label to use in the menu entry for a given preference value.
     *
     * @param value preference value
     * @return Default to value
     */
    protected String getMenuLabelForValue(String value)
    {
        return value;
    }

    /**
     * Update the preference button to reflect the new preference.
     *
     * @param change change event
     */
    @InvokeOnEDT
    @Override
    public void preferenceEntryChanged(PreferenceEntryChange change)
    {
        setSelectedMenuItem(change.getStringValue());
    }

    /**
     * Update user preferences using the currently selected item.
     */
    protected final void updatePreferencesWithSelection()
    {
        updatePreferencesValue(getSelectedMenuItem().getName());
    }

    /**
     * Update the user preferences with the given value.
     *
     * @param newValue New preference value to store
     * @since 2.25
     */
    private void updatePreferencesValue(String newValue)
    {
        preferenceEntry.putAndSave(newValue);
    }

    /**
     * Dispose of our resources (unregister as a listener).
     *
     * @since 2.35
     */
    public void dispose()
    {
        preferenceEntry.removePreferenceEntryChangeListener(this);
    }
}
