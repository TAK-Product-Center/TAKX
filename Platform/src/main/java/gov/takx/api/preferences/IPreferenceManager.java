package gov.takx.api.preferences;

import gov.takx.api.annotation.Internal;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.preferences.PreferencesNode;

import java.net.URL;

/**
 * Contract for preference managers. This includes the program properties, user preferences, and project preferences.
 *
 * @since 2.0.0
 */
@SuppressWarnings("unused")
@NoPluginImplementations
public interface IPreferenceManager
{
    /**
     * Add a global change listener, notified on changes to ANY preference entry.
     *
     * @param listener Preference entry change listener to add
     * @since 2.35
     */
    void addPreferenceEntryChangeListener(IPreferenceEntryChangeListener listener);

    /**
     * Remove a global change listener.
     *
     * @param listener Preference entry change listener to remove
     * @since 2.35
     */
    void removePreferenceEntryChangeListener(IPreferenceEntryChangeListener listener);

    /**
     * Get the preferences node associated with a given preferences entry.
     *
     * @param entry preferences entry to use for the key
     * @return Preferences node
     */
    PreferencesNode node(IPreferenceEntry entry);

    /**
     * Test whether a given node exists.
     *
     * @param entry Node to test
     * @return {@code true} if the node exists
     */
    boolean nodeExists(IPreferenceEntry entry);

    /**
     * Save the program preferences out in XML format. If this is not the default instance of TAKX and a preferences
     * file specific to this instance is already present, then save to that file. Otherwise, we save to the default
     * preferences file.
     *
     * @return {@code true} if the preferences were saved, {@code false} if an error occurred
     */
    @Internal
    boolean savePreferences();

    /**
     * Get a URL for the given preference entry. An empty preference value will result in a {@code null} return value.
     *
     * @param entry Entry to retrieve
     * @return URL created from value, using {@link IPreferenceEntry#getDefaultValue()} if not present
     * @see URL#URL(String)
     * @since 2.1.2
     */
    URL getURLValue(IPreferenceEntry entry);

    /**
     * Get an Integer value for the given preference entry.
     *
     * @param entry Entry to retrieve
     * @return value, using {@link IPreferenceEntry#getDefaultValue()} if not present
     */
    Integer getIntegerValue(IPreferenceEntry entry);

    /**
     * Get a Long value for the given preference entry.
     *
     * @param entry Entry to retrieve
     * @return value, using {@link IPreferenceEntry#getDefaultValue()} if not present
     */
    Long getLongValue(IPreferenceEntry entry);

    /**
     * Get a Boolean value for the given preference entry.
     *
     * @param entry Entry to retrieve
     * @return value, using {@link IPreferenceEntry#getDefaultValue()} if not present
     */
    Boolean getBooleanValue(IPreferenceEntry entry);

    /**
     * Get a Float value for the given preference entry.
     *
     * @param entry Entry to retrieve
     * @return value, using {@link IPreferenceEntry#getDefaultValue()} if not present
     */
    Float getFloatValue(IPreferenceEntry entry);

    /**
     * Get a Double value for the given preference entry.
     *
     * @param entry Entry to retrieve
     * @return value, using {@link IPreferenceEntry#getDefaultValue()} if not present
     */
    Double getDoubleValue(IPreferenceEntry entry);

    /**
     * Get the preference value given the IPreferenceEntry defining it.
     *
     * @param entry IPreferenceEntry to lookup
     * @return the preference value or the defaultValue if not found
     */
    String getStringValue(IPreferenceEntry entry);

    /**
     * Get the preference value converted to the given class type.  The class type must be one which is known
     * to the preference manager, an Enum type (which will be converted using {@link Enum#valueOf(Class, String)},
     * or a class which has a constructor accepting a String as its only argument.
     *
     * @param entry IPreferenceEntry to lookup
     * @param type  Type to which to convert the string value of the preference
     * @param <T>   Inferred type
     * @return the preference value or the defaultValue if not found
     */
    <T> T getValueAs(IPreferenceEntry entry, Class<T> type);

    /**
     * Get the preference node converted to the given class type.  The class type must be one which is known
     * to the preference manager or a class which has a constructor accepting a {@link PreferencesNode}
     * node as its only argument.
     * <p>
     * If the given node doesn't exist, then the converter/constructor will be given a {@code null} value as its argument.
     *
     * @param type Type to which to convert the Preferences node
     * @param <T>  Inferred type
     * @return the converted value or the {@code null} if the node does not exist
     */
    <T> T getNodeAs(IPreferenceEntry entry, Class<T> type);

    /**
     * Get the preference node converted to the given class type.  The class type must be one which is known
     * to the preference manager or a class which has a constructor accepting a {@link PreferencesNode}
     * node as its only argument.
     * <p>
     * If the given node doesn't exist, then the converter/constructor will be given a {@code null} value as its
     * argument unless {@code create} is {@code true} in which case an empty node will be created and passed as the
     * argument.
     *
     * @param type   Type to which to convert the Preferences node
     * @param create Pass {@code true} to create the node if it doesn't already exist
     * @param <T>    Inferred type
     * @return the converted value or the {@code null} if the node does not exist
     */
    <T> T getNodeAs(IPreferenceEntry entry, Class<T> type, boolean create);

    /**
     * Strictly here for API backward compatibility.
     */
    default String getPreferenceValue(PreferenceEntry preferenceEntry)
    {
        return getStringValue(preferenceEntry);
    }

    /**
     * Put a String value into the preferences.
     *
     * @param entry IPreferenceEntry to update
     * @param value Value to set
     */
    @Internal
    void put(IPreferenceEntry entry, String value);

    /**
     * Put a Number value into the preferences.
     *
     * @param entry IPreferenceEntry to update
     * @param value Value to set
     */
    @Internal
    void put(IPreferenceEntry entry, Number value);

    /**
     * Put a boolean value into the preferences.
     *
     * @param entry IPreferenceEntry to update
     * @param value Value to set
     */
    @Internal
    void put(IPreferenceEntry entry, boolean value);
}
