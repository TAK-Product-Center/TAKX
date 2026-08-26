package gov.takx.platform.utils.units;

import com.atakmap.coremap.conversions.Angle;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.test.AContainerTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link BearingUnits}.
 * 
 * @since 5.5
 */
public class BearingUnitsTest extends AContainerTest
{
    private static final IGeoPoint GEO_POINT = new GeoPoint(45, 45);
    private static final double TEST_VALUE = 3.19;

    @Test
    public void testDegree()
    {
        validateResults(BearingUnits.DEGREE, Angle.DEGREE, "°", "Degrees", "3.2 °T");
    }

    @Test
    public void testMil()
    {
        validateResults(BearingUnits.MIL, Angle.MIL, "mils", "Angular mil (NATO)", "56.7 milsT");
    }

    @Test
    public void testRadians()
    {
        validateResults(BearingUnits.RADIAN, Angle.RADIAN, "rad", "Radians", "0.1 radT");
    }

    private void validateResults(BearingUnits unit, Angle angle, String abbreviation, String displayName, String format)
    {
        assertThat(BearingUnits.from(angle)).isEqualTo(unit);
        assertThat(unit.getAbbreviation()).isEqualTo(abbreviation);
        assertThat(unit.getDisplayName()).isEqualTo(displayName);
        assertThat(unit.format(GEO_POINT, 0, TEST_VALUE, 1)).isEqualTo(format);
        assertThat(unit.toString()).isEqualTo(displayName);
    }

    /**
     * @since 5.6
     */
    @Test
    public void testDirection()
    {
        assertThat(BearingUnits.Direction.fromDegrees(11)).isEqualTo(BearingUnits.Direction.N);
        assertThat(BearingUnits.Direction.fromDegrees(350)).isEqualTo(BearingUnits.Direction.N);
        assertThat(BearingUnits.Direction.fromDegrees(90)).isEqualTo(BearingUnits.Direction.E);
        assertThat(BearingUnits.Direction.fromDegrees(78.75)).isEqualTo(BearingUnits.Direction.E);
        assertThat(BearingUnits.Direction.fromDegrees(112.25)).isEqualTo(BearingUnits.Direction.ESE);
        assertThat(BearingUnits.Direction.fromDegrees8(113.25)).isEqualTo(BearingUnits.Direction.SE);
    }
}
