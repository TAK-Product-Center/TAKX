package gov.takx.api.preferences;

import gov.takx.api.annotation.UserPreferenceType;
import gov.takx.api.event.PlatformEvent;

/**
 * Event fired when the value of a preference entry changes.  This is fired to listeners registered with
 * {@link IPreferenceManager#addPreferenceEntryChangeListener(IPreferenceEntryChangeListener)} or
 * {@link IPreferenceEntry#addPreferenceEntryChangeListener(IPreferenceEntryChangeListener)}.
 * <p>
 * These events are observable using standard CDI {@link jakarta.enterprise.event.Observes} (as it is a platform event
 * and is thus re-fired into all plugin containers).  Observations may be limited to a specific preference entry by
 * using the {@link UserPreferenceType} qualifier on the {@code @Observes} annotation.
 *
 * @see UserPreferenceType
 * @since 2.35
 */
public class PreferenceEntryChange implements PlatformEvent, ITypedPreferenceValue
{
    private final IPreferenceManager source;
    private final IPreferenceEntry preferenceEntry;

    /**
     * Construct a change event.
     *
     * @param source          Source preference manager on which the preference was changed
     * @param preferenceEntry The preference entry that changed
     */
    public PreferenceEntryChange(IPreferenceManager source, IPreferenceEntry preferenceEntry)
    {
        this.source = source;
        this.preferenceEntry = preferenceEntry;
    }

    @Override
    public IPreferenceEntry getPreferenceEntry()
    {
        return preferenceEntry;
    }

    @Override
    public IPreferenceManager getPreferenceManager()
    {
        return preferenceEntry.getPreferenceManager();
    }

    @Override
    public String toString()
    {
        return "PreferenceEntryChange{" +
                "source=" + source.getClass().getSimpleName() +
                ", entry=" + preferenceEntry +
                ", value='" + getStringValue() + "'" +
                '}';
    }
}
