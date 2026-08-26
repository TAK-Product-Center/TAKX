package gov.takx.api.propertymanager;

import gov.tak.api.annotation.DeprecatedApi;

import java.beans.PropertyChangeEvent;

/**
 * A system property change event that provides an enum that can be easily switched, facilitating clean casting of old
 * and new values.
 *
 * @since 1.6
 * @deprecated Use {@link gov.takx.platform.preferences.PreferenceEntry} and {@link gov.takx.platform.preferences.UserPreferenceEntry}
 * monitors instead.
 */
@Deprecated(since = "5.5", forRemoval = true)
@DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
public class SystemPropertyChangeEvent extends PropertyChangeEvent
{
    private SystemPropertyType systemPropertyType;

    /**
     * Constructs a new SystemPropertyChangeEvent.
     *
     * @param source             The bean that fired the event.
     * @param propertyName       The programmatic name of the property
     *                           that was changed.
     * @param oldValue           The old value of the property.
     * @param newValue           The new value of the property.
     * @param systemPropertyType The system property type.
     */
    public SystemPropertyChangeEvent(Object source, String propertyName, Object oldValue, Object newValue, SystemPropertyType systemPropertyType)
    {
        super(source, propertyName, oldValue, newValue);
        this.systemPropertyType = systemPropertyType;
    }

    /**
     * Returns the system property type for this system property change event.
     * See{@link SystemPropertyType}
     *
     * @return the system property type
     */
    public SystemPropertyType getPropertyType()
    {
        return systemPropertyType;
    }

    @Override
    public String toString()
    {
        return "SystemPropertyChangeEvent{" +
                "systemPropertyType=" + systemPropertyType +
                ", " + super.toString() +
                '}';
    }
}
