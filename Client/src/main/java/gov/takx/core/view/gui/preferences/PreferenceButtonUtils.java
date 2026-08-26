package gov.takx.core.view.gui.preferences;

import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.commons.media.AudioAlert;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.utils.units.AltitudeUnits;
import gov.takx.platform.utils.units.RangeUnits;
import gov.takx.platform.utils.units.SpeedUnits;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 */
public class PreferenceButtonUtils
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private PreferenceButtonUtils()
    {
    }

    /**
     * Get all possible key values for a given preference string key. In general, this will just return a list
     * of strings created from .toString() on each of each value returned from an Enum.values() array. However,
     * this is not the case for the units of measurement since we do not have an enum associated with it.
     *
     * @param preferenceEntry preference entry
     * @return List of enum string values
     */
    public static List<String> getPossibleKeyValues(@Nonnull UserPreferenceEntry preferenceEntry)
    {
        return switch (preferenceEntry)
        {
            case AUDIO_ALERT -> getEnumValues(AudioAlert.values());
            case COORDINATE_SYSTEM -> getEnumValues(CoordinateSystem.values());
            case DISPLAY_ALTITUDE_UNITS -> getEnumValues(AltitudeUnits.values());
            case DISPLAY_RANGE_UNITS -> getEnumValues(RangeUnits.values());
            case DISPLAY_SPEED_UNITS -> getEnumValues(SpeedUnits.values());
            default ->
            {
                logger.warn("Unexpected preference key: {}", preferenceEntry.getKey());
                yield Collections.emptyList();
            }
        };
    }

    /**
     * Create a string array from an enum.
     *
     * @param values enum values.
     * @return new string array containing all the enums as strings
     */
    private static List<String> getEnumValues(@Nonnull Enum<?>[] values)
    {
        return Arrays.stream(values)
                .map(Object::toString)
                .collect(Collectors.toList());
    }
}
