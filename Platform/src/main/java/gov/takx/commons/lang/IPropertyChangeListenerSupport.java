package gov.takx.commons.lang;

import gov.tak.api.annotation.DeprecatedApi;
import jakarta.annotation.Nonnull;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;

/**
 * Interface for consistently exposing add/remove property change listener methods. Public APIs should expose this interface
 * while most concrete implementations should use {@link IPropertyChangeDelegateSupport} (See that class for explanation).
 *
 * @since 5.6
 */
public interface IPropertyChangeListenerSupport
{
    /**
     * Calls {@link #addPropertyChangeListener(String, PropertyChangeListener)} for all properties
     * in the given list.
     *
     * @param properties the list of property names
     * @param listener the listener
     */
    default void addPropertyChangeListener(@Nonnull List<String> properties, @Nonnull PropertyChangeListener listener)
    {
        properties.forEach(property -> addPropertyChangeListener(property, listener));
    }

    /**
     * Calls {@link #addPropertyChangeListener(String, PropertyChangeListener)} for all properties in
     * the vararg parameter.
     *  
     * @param listener the listener
     * @param properties the array of property names
     */
    default void addPropertyChangeListener(@Nonnull PropertyChangeListener listener, @Nonnull String... properties)
    {
        for (String property : properties)
        {
            addPropertyChangeListener(property, listener);
        }
    }

    /**
     * @see PropertyChangeSupport#addPropertyChangeListener(PropertyChangeListener)
     */
    void addPropertyChangeListener(@Nonnull PropertyChangeListener listener);

    /**
     * @see PropertyChangeSupport#addPropertyChangeListener(String, PropertyChangeListener)
     */
    void addPropertyChangeListener(@Nonnull String property, @Nonnull PropertyChangeListener listener);

    /**
     * Calls {@link #removePropertyChangeListener(String, PropertyChangeListener)} for all properties
     * in the given list.
     *
     * @param properties the list of property names
     * @param listener the listener
     * @since 5.7
     */
    default void removePropertyChangeListener(@Nonnull List<String> properties, @Nonnull PropertyChangeListener listener)
    {
        properties.forEach(property -> removePropertyChangeListener(property, listener));
    }

    /**
     * @see PropertyChangeSupport#removePropertyChangeListener(PropertyChangeListener)
     */
    void removePropertyChangeListener(@Nonnull PropertyChangeListener listener);

    /**
     * @see PropertyChangeSupport#addPropertyChangeListener(String, PropertyChangeListener)
     */
    void removePropertyChangeListener(@Nonnull String property, @Nonnull PropertyChangeListener listener);

    /**
     * @deprecated use {@link #addPropertyChangeListener(String, PropertyChangeListener)}
     * @since 5.7
     */
    @Deprecated(since = "5.7", forRemoval = true)
    @DeprecatedApi(since = "5.7", removeAt = "5.10")
    default void addPropertyChangeListener(@Nonnull PropertyChangeListener listener, @Nonnull String property)
    {
        addPropertyChangeListener(property, listener);
    }

    /**
     * @deprecated use {@link #removePropertyChangeListener(String, PropertyChangeListener)}
     * @since 5.7
     */
    @Deprecated(since = "5.7", forRemoval = true)
    @DeprecatedApi(since = "5.7", removeAt = "5.10")
    default void removePropertyChangeListener(@Nonnull PropertyChangeListener listener, @Nonnull String property)
    {
        removePropertyChangeListener(property, listener);
    }

    /**
     * @deprecated use {@link #addPropertyChangeListener(List, PropertyChangeListener)}
     * @since 5.7
     */
    @Deprecated(since = "5.7", forRemoval = true)
    @DeprecatedApi(since = "5.7", removeAt = "5.10")
    default void addPropertyChangeListener(@Nonnull PropertyChangeListener listener, @Nonnull List<String> properties)
    {
        addPropertyChangeListener(properties, listener);
    }

    /**
     * @deprecated use {@link #removePropertyChangeListener(List, PropertyChangeListener)}
     * @since 5.7
     */
    @Deprecated(since = "5.7", forRemoval = true)
    @DeprecatedApi(since = "5.7", removeAt = "5.10")
    default void removePropertyChangeListener(@Nonnull PropertyChangeListener listener, @Nonnull List<String> properties)
    {
        removePropertyChangeListener(properties, listener);
    }
}
