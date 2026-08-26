package gov.takx.api.preferences;

import gov.takx.api.annotation.Internal;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.annotation.PreferenceDescriptor;
import gov.takx.platform.preferences.PreferenceMonitor;
import gov.takx.platform.preferences.PreferencesNode;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * Contract for Preference Entries, holding a key and a default value.  This interface supports retrieving values
 * (with type-specific getters), registering change listeners, and constructing {@link IPreferenceMonitor} instances
 * for the basic types.
 * <p>
 * Methods to set preference values are strictly for internal use.
 * <p>
 * Generally, a bunch of {@code default} methods is a code smell, except when dealing with API evolution.
 * However, in this case we are trying to add uniform behavior to a set of Enum types.  Since enums can not extend from
 * any other class, the use of default methods in this interface is the mechanism to avoid tons of duplicated code.
 *
 * @since 2.35
 */
@NoPluginImplementations
public interface IPreferenceEntry extends ITypedPreferenceValue
{
    /**
     * @return Default value for preference
     */
    String getDefaultValue();

    /**
     * @return The preference key
     */
    String getKey();

    /**
     * @return The preference entry name (resolves to {@link Enum#name()}
     */
    String name();

    /**
     * @return A human-readable title for the preference (may be empty for preferences not included in the UI).
     * @since 3.2
     */
    default String getTitle()
    {
        final PreferenceDescriptor preferenceDescriptor = getPreferenceDescriptorAnnotation();
        return preferenceDescriptor == null ? "" : preferenceDescriptor.title();
    }

    /**
     * @return A human-readable description for the preference (may be empty for preferences not included in the UI).
     * @since 3.2
     */
    default String getDescription()
    {
        final PreferenceDescriptor preferenceDescriptor = getPreferenceDescriptorAnnotation();
        return preferenceDescriptor == null ? "" : preferenceDescriptor.description();
    }

    /**
     * @return The preference descriptor annotation for this preference entry; may be {@code null} if this preference
     * entry is not annotated.
     * @since 3.2
     */
    @Internal
    @Nullable
    default PreferenceDescriptor getPreferenceDescriptorAnnotation()
    {
        try
        {
            // This returns null if the annotation isn't found, which is exactly what we want to happen
            return getClass().getField(name()).getAnnotation(PreferenceDescriptor.class);
        } catch (NoSuchFieldException ignore)
        {
        }

        return null;
    }

    /**
     * Add a change listener, notified on changes this preference entry.
     *
     * @param listener Preference entry change listener to add
     */
    default void addPreferenceEntryChangeListener(IPreferenceEntryChangeListener listener)
    {
        getChangeListeners().add(listener);
    }

    /**
     * Remove a change listener.
     *
     * @param listener Preference entry change listener to remove
     */
    default void removePreferenceEntryChangeListener(IPreferenceEntryChangeListener listener)
    {
        getChangeListeners().remove(listener);
    }

    /**
     * Factory function to produce a PreferenceMonitor for String values. The monitor is immediately started upon construction.
     *
     * @param changeHandler Handler to invoke when preference value changes
     * @return new PreferenceMonitor
     */
    default IPreferenceMonitor<String> newStringMonitor(Consumer<String> changeHandler)
    {
        return newStringMonitor(changeHandler, true);
    }

    /**
     * Factory function to produce a PreferenceMonitor for String values.
     *
     * @param changeHandler Handler to invoke when preference value changes
     * @param start Pass {@code true} to have the monitor start on construction
     * @return new PreferenceMonitor
     * @since 3.5
     */
    default IPreferenceMonitor<String> newStringMonitor(Consumer<String> changeHandler, boolean start)
    {
        return new PreferenceMonitor<>(getPreferenceEntry(), changeHandler, ITypedPreferenceValue::getStringValue, start);
    }

    /**
     * Factory function to produce a PreferenceMonitor for Boolean values. The monitor is immediately started upon construction.
     *
     * @param changeHandler Handler to invoke when preference value changes
     * @return new PreferenceMonitor
     */
    default IPreferenceMonitor<Boolean> newBooleanMonitor(Consumer<Boolean> changeHandler)
    {
        return newBooleanMonitor(changeHandler, true);
    }

    /**
     * Factory function to produce a PreferenceMonitor for Boolean values.
     *
     * @param changeHandler Handler to invoke when preference value changes
     * @param start Pass {@code true} to have the monitor start on construction
     * @return new PreferenceMonitor
     * @since 3.5
     */
    default IPreferenceMonitor<Boolean> newBooleanMonitor(Consumer<Boolean> changeHandler, boolean start)
    {
        return new PreferenceMonitor<>(getPreferenceEntry(), changeHandler, ITypedPreferenceValue::getBooleanValue, start);
    }

    /**
     * Factory function to produce a PreferenceMonitor for Integer values. The monitor is immediately started upon construction.
     *
     * @param changeHandler Handler to invoke when preference value changes
     * @return new PreferenceMonitor
     */
    default IPreferenceMonitor<Integer> newIntegerMonitor(Consumer<Integer> changeHandler)
    {
        return newIntegerMonitor(changeHandler, true);
    }

    /**
     * Factory function to produce a PreferenceMonitor for Integer values.
     *
     * @param changeHandler Handler to invoke when preference value changes
     * @param start Pass {@code true} to have the monitor start on construction
     * @return new PreferenceMonitor
     * @since 3.5
     */
    default IPreferenceMonitor<Integer> newIntegerMonitor(Consumer<Integer> changeHandler, boolean start)
    {
        return new PreferenceMonitor<>(getPreferenceEntry(), changeHandler, ITypedPreferenceValue::getIntegerValue, start);
    }

    /**
     * Factory function to produce a PreferenceMonitor for Long values. The monitor is immediately started upon construction.
     *
     * @param changeHandler Handler to invoke when preference value changes
     * @return new PreferenceMonitor
     */
    default IPreferenceMonitor<Long> newLongMonitor(Consumer<Long> changeHandler)
    {
        return newLongMonitor(changeHandler, true);
    }

    /**
     * Factory function to produce a PreferenceMonitor for Long values.
     *
     * @param changeHandler Handler to invoke when preference value changes
     * @param start Pass {@code true} to have the monitor start on construction
     * @return new PreferenceMonitor
     * @since 3.5
     */
    default IPreferenceMonitor<Long> newLongMonitor(Consumer<Long> changeHandler, boolean start)
    {
        return new PreferenceMonitor<>(getPreferenceEntry(), changeHandler, ITypedPreferenceValue::getLongValue, start);
    }

    /**
     * Factory function to produce a PreferenceMonitor for Float values. The monitor is immediately started upon construction.
     *
     * @param changeHandler Handler to invoke when preference value changes
     * @return new PreferenceMonitor
     */
    default IPreferenceMonitor<Float> newFloatMonitor(Consumer<Float> changeHandler)
    {
        return newFloatMonitor(changeHandler, true);
    }

    /**
     * Factory function to produce a PreferenceMonitor for Float values.
     *
     * @param changeHandler Handler to invoke when preference value changes
     * @param start Pass {@code true} to have the monitor start on construction
     * @return new PreferenceMonitor
     * @since 3.5
     */
    default IPreferenceMonitor<Float> newFloatMonitor(Consumer<Float> changeHandler, boolean start)
    {
        return new PreferenceMonitor<>(getPreferenceEntry(), changeHandler, ITypedPreferenceValue::getFloatValue, start);
    }

    /**
     * Factory function to produce a PreferenceMonitor for Double values. The monitor is immediately started upon construction.
     *
     * @param changeHandler Handler to invoke when preference value changes
     * @return new PreferenceMonitor
     */
    default IPreferenceMonitor<Double> newDoubleMonitor(Consumer<Double> changeHandler)
    {
        return newDoubleMonitor(changeHandler, true);
    }

    /**
     * Factory function to produce a PreferenceMonitor for Double values.
     *
     * @param changeHandler Handler to invoke when preference value changes
     * @param start Pass {@code true} to have the monitor start on construction
     * @return new PreferenceMonitor
     * @since 3.5
     */
    default IPreferenceMonitor<Double> newDoubleMonitor(Consumer<Double> changeHandler, boolean start)
    {
        return new PreferenceMonitor<>(getPreferenceEntry(), changeHandler, ITypedPreferenceValue::getDoubleValue, start);
    }

    /**
     * Factory function to produce a PreferenceMonitor for a specified class value. The monitor is immediately started upon construction.
     *
     * @param type type of preference value
     * @param changeHandler Handler to invoke when preference value changes
     * @return new PreferenceMonitor
     */
    default <T> IPreferenceMonitor<T> newMonitor(Class<T> type, Consumer<T> changeHandler)
    {
        return newMonitor(type, changeHandler, true);
    }

    /**
     * Factory function to produce a PreferenceMonitor for a specified class value.
     *
     * @param type type of preference value
     * @param changeHandler Handler to invoke when preference value changes
     * @param start Pass {@code true} to have the monitor start on construction
     * @return new PreferenceMonitor
     * @since 3.5
     */
    default <T> IPreferenceMonitor<T> newMonitor(Class<T> type, Consumer<T> changeHandler, boolean start)
    {
        return new PreferenceMonitor<>(getPreferenceEntry(), changeHandler, e -> e.getValueAs(type), start);
    }

    /**
     * Fire a preference entry change event on this preference entry to all listeners.
     * @since 5.4
     */
    @Internal
    default void firePreferenceEntryChangeEvent()
    {
        final var change = new PreferenceEntryChange(getPreferenceManager(), getPreferenceEntry());
        getChangeListeners().forEach(l -> l.preferenceEntryChanged(change));
    }

    /**
     * Fire a preference entry change event to all listeners.
     *
     * @param change The change event to fire
     */
    @Internal
    default void firePreferenceEntryChangeEvent(PreferenceEntryChange change)
    {
        getChangeListeners().forEach(l -> l.preferenceEntryChanged(change));
    }

    /**
     * @return The Preferences node associated with this entry
     */
    @Internal
    default PreferencesNode node()
    {
        return getPreferenceManager().node(this);
    }

    /**
     * Put a String value into the preferences.
     *
     * @param value Value to set
     */
    @Internal
    default void put(String value)
    {
        getPreferenceManager().put(this, value);
    }

    /**
     * Put the given value and then save the preferences.
     *
     * @param value Value to set
     */
    @Internal
    default void putAndSave(String value)
    {
        final IPreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.put(this, value);
        preferenceManager.savePreferences();
    }

    /**
     * Put a Number value into the preferences.
     *
     * @param value Value to set
     */
    @Internal
    default void put(Number value)
    {
        getPreferenceManager().put(this, value);
    }

    /**
     * Put the given value and then save the preferences.
     *
     * @param value Value to set
     */
    @Internal
    default void putAndSave(Number value)
    {
        final IPreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.put(this, value);
        preferenceManager.savePreferences();
    }

    /**
     * Put a boolean value into the preferences.
     *
     * @param value Value to set
     */
    @Internal
    default void put(boolean value)
    {
        getPreferenceManager().put(this, value);
    }

    /**
     * Put the given value and then save the preferences.
     *
     * @param value Value to set
     */
    @Internal
    default void putAndSave(boolean value)
    {
        final IPreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.put(this, value);
        preferenceManager.savePreferences();
    }

    @Override
    default IPreferenceEntry getPreferenceEntry()
    {
        return this;
    }

    /**
     * @return The change listener list for this preference entry
     */
    @Internal
    List<IPreferenceEntryChangeListener> getChangeListeners();
}
