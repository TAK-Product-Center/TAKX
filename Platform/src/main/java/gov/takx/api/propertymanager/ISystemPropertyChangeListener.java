package gov.takx.api.propertymanager;

/**
 * This is a single method interface with a callback that is handed a SystemPropertyChangeEvent. The SystemProperChangeEvent
 * provides an enum that can easily be switched, facilitating clean casting of the old and new values. Implementing this
 * interface allows you to register as a listener with a system property manager {@link ISystemPropertyManager}
 * in order to be notified when a system property change event occurs. The supported system properties that will prompt
 * notifications when changed are:
 * {@link SystemPropertyType#CoordinateSystem}
 * {@link SystemPropertyType#GlobalAudioThreshold}
 * {@link SystemPropertyType#UnitsOfMeasure}
 * {@link SystemPropertyType#WMSNetworkStatus}
 *
 * @since 1.6
 */
public interface ISystemPropertyChangeListener
{

    /**
     * A callback for when a user changes one of the system properties during a project session. This method provides
     * a SystemPropertyChangeEvent that can easily be switched, facilitating the clean casting of old and new values.
     *
     * @param event the system property change event that occurred.
     */
    void onSystemPropertyChange(SystemPropertyChangeEvent event);
}
