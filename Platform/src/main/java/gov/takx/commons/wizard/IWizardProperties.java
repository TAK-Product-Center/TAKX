package gov.takx.commons.wizard;

import jakarta.annotation.Nonnull;

import java.beans.PropertyChangeListener;

/**
 * A collection of properties used by a wizard. Each property has a string name. A wizard keeps the same property map
 * through its whole lifecycle, so different wizard panels can use properties to exchange information.
 *
 */
public interface IWizardProperties
{
    /**
     * Get a property by name.
     *
     * @param propertyName Property name.
     * @return The value of the property, or {@code null} if no such property has been set.
     */
    Object getProperty(String propertyName);

    /**
     * Get a string property by name.
     *
     * @param propertyName Property name.
     * @return The value of the property, or {@code null} if no such property has been set.
     */
    String getStringProperty(String propertyName);

    /**
     * Get a boolean property by name.
     *
     * @param propertyName Property name.
     * @return The value of the property, or {@code null} if no such property has been set.
     */
    Boolean getBooleanProperty(String propertyName);

    /**
     * Get an integer property by name.
     *
     * @param propertyName Property name.
     * @return The value of the property, or {@code null} if no such property has been set.
     */
    Integer getIntegerProperty(String propertyName);

    /**
     * Set a property.
     *
     * @param propertyName Name of the property to set.
     * @param newValue     The new value for the property. This will replace any existing value.
     */
    void setProperty(String propertyName, Object newValue);

    /**
     * Add a property listener to this property map. The listener will be notified when properties change.
     *
     * @param listener New listener.
     */
    void addPropertyChangeListener(@Nonnull PropertyChangeListener listener);

    /**
     * Remove a property change listener from the property map.
     *
     * @param listener Listener to remove.
     */
    void removePropertyChangeListener(@Nonnull PropertyChangeListener listener);
}
