package gov.takx.platform.utils.units;

import com.atakmap.coremap.conversions.ConversionFactors;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link SpeedUnits}.
 * 
 * @since 5.5
 */
public class SpeedUnitsTest
{
    private static final double TEST_VALUE = 3.19;

    @Test
    public void testMetersPerSecond()
    {
        validateResults(SpeedUnits.MPS, 1.0, "mps", "Meters per Second (mps)",
                "3.2 mps");
    }

    @Test
    public void testKilometersPerHour()
    {
        validateResults(SpeedUnits.KMPH, ConversionFactors.METERS_PER_S_TO_KILOMETERS_PER_H, "kmph",
                "Kilometers per Hour (kmph)", "11.5 kmph");
    }

    @Test
    public void testKnots()
    {
        validateResults(SpeedUnits.KTS, ConversionFactors.METERS_PER_S_TO_KNOTS, "kts", "KGS (kts)",
                "6.2 kts");
    }

    @Test
    public void testMilesPerHour()
    {
        validateResults(SpeedUnits.MPH, ConversionFactors.METERS_PER_S_TO_MILES_PER_H, "mph",
                "Miles per Hour (mph)", "7.1 mph");
    }

    private void validateResults(SpeedUnits unit, double multiplier, String abbreviation, String displayName, String format)
    {
        assertThat(unit.getMultiplier()).isEqualTo(multiplier);
        assertThat(unit.getAbbreviation()).isEqualTo(abbreviation);
        assertThat(unit.getDisplayName()).isEqualTo(displayName);
        assertThat(unit.format(TEST_VALUE, 1)).isEqualTo(format);
        assertThat(unit.toString()).isEqualTo(displayName);
    }
}
