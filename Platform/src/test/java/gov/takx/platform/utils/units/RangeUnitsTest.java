package gov.takx.platform.utils.units;

import com.atakmap.coremap.conversions.ConversionFactors;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link RangeUnits}.
 * 
 * @since 5.5
 */
public class RangeUnitsTest
{
    @Test
    public void testMeters()
    {
        validateResults(RangeUnits.METERS, 3.19, 1.0, "m", "Meters/Kilometers (m/km)",
                "3.190 m", "meters", "kilometers", "m²", "km²",
                ConversionFactors.METERS_TO_KM, "3.190 m²", RangeUnits.METERS.getLargeAreaThreshold() + 1,
                "0.100 km²");
    }

    @Test
    public void testFeet()
    {
        validateResults(RangeUnits.FEET, 3.19, ConversionFactors.METERS_TO_FEET, "ft",
                "Feet/Miles (ft/mi)", "10.466 ft", "feet", "miles",
                "ft²", "mi²", ConversionFactors.METERS_TO_MILES, "34.337 ft²",
                RangeUnits.FEET.getLargeAreaThreshold() + 1, "0.036 mi²");
    }

    @Test
    public void testNauticalMiles()
    {
        validateResults(RangeUnits.NAUTICAL_MILES, 50000, ConversionFactors.METERS_TO_NM, "nm",
                "Nautical Miles (nm)", "26.998 nm", "nautical miles", "nautical miles",
                "nm²", "nm²", ConversionFactors.METERS_TO_NM, "0.015 nm²",
                50000, "0.015 nm²");
    }

    private void validateResults(RangeUnits unit, double testValue, double multiplier, String abbreviation,
                                 String displayName, String format, String unitName, String largeUnitName,
                                 String areaAbbreviation, String largeAreaAbbreviation, double largeMultiplier,
                                 String formatArea, double largeAreaValue, String formatLargeArea)
    {
        assertThat(unit.getMultiplier()).isEqualTo(multiplier);
        assertThat(unit.getAbbreviation()).isEqualTo(abbreviation);
        assertThat(unit.getDisplayName()).isEqualTo(displayName);
        assertThat(unit.format(testValue, 3)).isEqualTo(format);
        assertThat(unit.toString()).isEqualTo(displayName);

        assertThat(unit.getMultiplier(unit.getLargeThreshold())).isEqualTo(largeMultiplier);
        assertThat(unit.getUnitName()).isEqualTo(unitName);
        assertThat(unit.getUnitName(unit.getLargeThreshold() + 1)).isEqualTo(largeUnitName);
        assertThat(unit.getAreaAbbreviation()).isEqualTo(areaAbbreviation);
        assertThat(unit.getAreaAbbreviation(unit.getLargeAreaThreshold() + 1)).isEqualTo(largeAreaAbbreviation);
        assertThat(unit.getAreaMultiplier()).isEqualTo(multiplier * multiplier);
        assertThat(unit.getAreaMultiplier(unit.getLargeAreaThreshold())).isEqualTo(largeMultiplier * largeMultiplier);
        assertThat(unit.formatArea(testValue, 3)).isEqualTo(formatArea);
        assertThat(unit.formatArea(largeAreaValue, 3)).isEqualTo(formatLargeArea);
    }
}
