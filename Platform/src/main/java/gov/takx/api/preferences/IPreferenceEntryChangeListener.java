package gov.takx.api.preferences;

/**
 * Contract for listeners of preference entry change events.
 *
 * @see PreferenceEntryChange
 * @since 2.35
 */
public interface IPreferenceEntryChangeListener
{
    /**
     * Called when the value of a preference entry changes.
     *
     * @param change Event describing the change
     */
    void preferenceEntryChanged(PreferenceEntryChange change);
}
