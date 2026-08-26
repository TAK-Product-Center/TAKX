package gov.takx.api.preferences;

import gov.takx.platform.preferences.PreferencesNode;

import java.net.URL;

/**
 * Contract for obtaining typed values from a preference entry.
 * <p>
 * Implementors must provide implementations of {@link #getPreferenceEntry()} and {@link #getPreferenceManager()}.
 *
 * @since 2.35
 */
public interface ITypedPreferenceValue
{
    /**
     * Get the String value for this preference entry.
     *
     * @return value, using {@link IPreferenceEntry#getDefaultValue()} if not present
     */
    default String getStringValue()
    {
        return getPreferenceManager().getStringValue(getPreferenceEntry());
    }

    /**
     * Get a Boolean value for this preference entry.
     *
     * @return value, using {@link IPreferenceEntry#getDefaultValue()} if not present
     */
    default Boolean getBooleanValue()
    {
        return getPreferenceManager().getBooleanValue(getPreferenceEntry());
    }

    /**
     * Get an Integer value for this preference entry.
     *
     * @return value, using {@link IPreferenceEntry#getDefaultValue()} if not present
     */
    default Integer getIntegerValue()
    {
        return getPreferenceManager().getIntegerValue(getPreferenceEntry());
    }

    /**
     * Get a Long value for this preference entry.
     *
     * @return value, using {@link IPreferenceEntry#getDefaultValue()} if not present
     */
    default Long getLongValue()
    {
        return getPreferenceManager().getLongValue(getPreferenceEntry());
    }

    /**
     * Get a Float value for this preference entry.
     *
     * @return value, using {@link IPreferenceEntry#getDefaultValue()} if not present
     */
    default Float getFloatValue()
    {
        return getPreferenceManager().getFloatValue(getPreferenceEntry());
    }

    /**
     * Get a Double value for this preference entry.
     *
     * @return value, using {@link IPreferenceEntry#getDefaultValue()} if not present
     */
    default Double getDoubleValue()
    {
        return getPreferenceManager().getDoubleValue(getPreferenceEntry());
    }

    /**
     * Get a URL for this preference entry.  An empty preference value will result in a {@code null} return value.
     *
     * @return URL created from value, using {@link IPreferenceEntry#getDefaultValue()} if not present
     * @see URL#URL(String)
     */
    default URL getURLValue()
    {
        return getPreferenceManager().getURLValue(getPreferenceEntry());
    }

    /**
     * Get the preference value converted to the given class type.  The class type must be one which is known
     * to the preference manager, an Enum type (which will be converted using {@link Enum#valueOf(Class, String)},
     * or a class which has a constructor accepting a String as its only argument.
     *
     * @param type Type to which to convert the string value of the preference
     * @param <T>  Inferred type
     * @return the preference value or the defaultValue if not found
     */
    default <T> T getValueAs(Class<T> type)
    {
        return getPreferenceManager().getValueAs(getPreferenceEntry(), type);
    }

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
    default <T> T getNodeAs(Class<T> type)
    {
        return getPreferenceManager().getNodeAs(getPreferenceEntry(), type);
    }

    /**
     * Get the preference node converted to the given class type.  The class type must be one which is known
     * to the preference manager or a class which has a constructor accepting a {@link PreferencesNode}
     * node as its only argument.
     * <p>
     * If the given node doesn't exist, then the converter/constructor will be given a {@code null} value as its
     * argument unless {@code create} is {@code true} in which case an empty node will be created and passed to the
     * constructor.
     *
     * @param type   Type to which to convert the Preferences node
     * @param create Pass {@code true} to create the node if it doesn't already exist
     * @param <T>    Inferred type
     * @return the converted value or the {@code null} if the node does not exist
     */
    default <T> T getNodeAs(Class<T> type, boolean create)
    {
        return getPreferenceManager().getNodeAs(getPreferenceEntry(), type, create);
    }

    /**
     * @return The preference entry from which to extract values
     */
    IPreferenceEntry getPreferenceEntry();

    /**
     * @return The preference manager responsible for handling this preference entry
     */
    IPreferenceManager getPreferenceManager();
}
