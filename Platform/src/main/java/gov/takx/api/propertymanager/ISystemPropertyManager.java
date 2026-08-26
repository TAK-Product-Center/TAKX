package gov.takx.api.propertymanager;

import gov.tak.api.annotation.DeprecatedApi;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.globe.ICameraEye;
import gov.takx.commons.media.AudioAlert;
import gov.takx.platform.utils.ILengthMeasurer;
import gov.takx.platform.utils.UnitsFormat;
import gov.takx.platform.utils.units.AltitudeUnits;
import gov.takx.platform.utils.units.RangeUnits;
import gov.takx.platform.utils.units.SpeedUnits;
import jakarta.annotation.Nonnull;

/**
 * Interface to a system property manager. The system property manager is intended to provide a single point of
 * reference for referencing current system property values, in addition to providing an event notification registration
 * system for proactive notification to a plugin that a system property has been modified by the user, see
 * {@link ISystemPropertyChangeListener} for more information. The current
 * system properties that are available for notification can be found in
 * {@link SystemPropertyType}. Currently, plug-ins have only read-access to the scoped
 * system properties.
 * <p>
 * <em>Note: The change notification callbacks takes place on the EDT, so avoid performing long running
 * operations directly.</em>
 *
 * @since 1.6
 * @deprecated Use {@link gov.takx.platform.preferences.PreferenceEntry} and {@link gov.takx.platform.preferences.UserPreferenceEntry} directly instead
 */
@Deprecated(since = "5.5", forRemoval = true)
@DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
@SuppressWarnings("UnusedDeclaration")
@InjectableService
@NoPluginImplementations
public interface ISystemPropertyManager
{
    /**
     * Whether the current project is in WMS online mode.
     *
     * @return true if the project is in WMS online mode, false if the project is in WMS offline mode
     */
    boolean isWMSNetworkOnline();

    /**
     * Returns the coordinate system for the project.
     *
     * @return the coordinate system for the project
     * @see CoordinateSystem for more information on coordate systems
     */
    CoordinateSystem getCoordinateSystem();

    /**
     * Returns the current audio alert level.
     *
     * @return the audio alert level for the project
     * @see AudioAlert for more information on audio alert levels
     */
    AudioAlert getAudioAlertLevel();

    /**
     * Returns the units of measure that the project is using to calculate size and distance in the display.
     * More generally, you will probably want to use the current {@link UnitsFormat Unit formatter},
     * see {@link #getUnitsFormat()}.
     *
     * @return the units of measure being used
     * @see ILengthMeasurer for more information on units of measure
     * @see #getUnitsFormat()
     * @deprecated Use {@link AltitudeUnits#name()}, {@link RangeUnits#name()} and {@link SpeedUnits#name()} instead
     */
    String getUnitsOfMeasure();

    /**
     * Get the current UnitsFormat (as determined by the Units of Measure) instance.  This instance can be
     * used to format lengths, areas, etc.
     *
     * @return The current units formatter
     * @see #getUnitsOfMeasure()
     * @since 1.43
     * @deprecated Use {@link AltitudeUnits}, {@link RangeUnits} and {@link SpeedUnits} instead
     */
    UnitsFormat getUnitsFormat();

    /**
     * Allows the ability to register a system property change listener with the system property manager. Whenever
     * the user changes one of the properties during a project session, a property change event is generated and all
     * listeners contained within the system property manager are notified. {@code null} values are not allowed and an
     * illegal argument exception will be thrown if one is passed as the parameter.
     *
     * @param listener the listener to be added to the system property manager
     * @throws IllegalArgumentException {@code null} values are not allowed.
     * @see ISystemPropertyChangeListener for more information on the listeners passed in
     * @see SystemPropertyType for the properties pertaining to the listeners
     */
    void addSystemPropertyChangeListener(@Nonnull ISystemPropertyChangeListener listener);

    /**
     * Allows the ability to unregister a system property change listener with the system property manager.
     *
     * @param listener the listener to be added to the system property manager
     * @return true if the listener was successfully removed from the system property manager, false if it was not
     * @throws IllegalArgumentException {@code null} values are not allowed
     * @see ISystemPropertyChangeListener for more information on the listeners passed in
     * @see SystemPropertyType for the properties pertaining to the listeners
     */
    boolean removeSystemPropertyChangeListener(@Nonnull ISystemPropertyChangeListener listener);

    /**
     * Provides access to the current camera eye.
     *
     * @return Camera eye as presently focused on the globe
     * @since 1.11
     */
    ICameraEye getCurrentCameraEye();
}
