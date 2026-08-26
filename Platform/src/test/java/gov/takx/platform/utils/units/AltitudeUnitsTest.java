package gov.takx.platform.utils.units;

import com.atakmap.coremap.conversions.ConversionFactors;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link AltitudeUnits}.
 * 
 * @since 5.5
 */
public class AltitudeUnitsTest
{
    private static final double TEST_VALUE = 3.19;

    @Test
    public void testMeters()
    {
        validateResults(AltitudeUnits.METERS, 1.0, "m", "Meters (m)", "3.2 m");
    }

    @Test
    public void testFeet()
    {
        validateResults(AltitudeUnits.FEET, ConversionFactors.METERS_TO_FEET, "ft", "Feet (ft)", "10.5 ft");
    }

    private void validateResults(AltitudeUnits unit, double multiplier, String abbreviation, String displayName, String format)
    {
        assertThat(unit.getMultiplier()).isEqualTo(multiplier);
        assertThat(unit.getAbbreviation()).isEqualTo(abbreviation);
        assertThat(unit.getDisplayName()).isEqualTo(displayName);
        assertThat(unit.format(TEST_VALUE, 1)).isEqualTo(format);
        assertThat(unit.toString()).isEqualTo(displayName);
    }
}
