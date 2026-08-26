package gov.takx.api.propertymanager;

import gov.tak.api.annotation.DeprecatedApi;

/**
 * SystemPropertyType enum used within {@link ISystemPropertyManager} which provides
 * read-only access to system properties within the project session.
 *
 * @since 1.6
 * @see gov.takx.platform.utils.units.AltitudeUnits
 * @deprecated Use {@link gov.takx.platform.preferences.PreferenceEntry} and {@link gov.takx.platform.preferences.UserPreferenceEntry}
 * directly instead
 */
@Deprecated(since = "5.5", forRemoval = true)
@DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
public enum SystemPropertyType
{
    CoordinateSystem,
    GlobalAudioThreshold,
    UnitsOfMeasure,
    WMSNetworkStatus
}
