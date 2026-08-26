package gov.takx.api.coordinatesystem;

import gov.takx.api.annotation.SuppressLogging;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import static gov.takx.api.coordinatesystem.CoordinateSystem.getDMDegreesParts;
import static gov.takx.api.coordinatesystem.CoordinateSystem.getDMSDegreesParts;
import static gov.takx.api.coordinatesystem.CoordinateSystem.getMGRSParts;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.within;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CoordinateSystemTest
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private List<CoordinateSystem> coordinateSystems;
    private List<String> coordinateSystemStrings;
    private static final double ACCEPTABLE_TOLERANCE_OFFSET = .0001;
    private static final double ACCEPTABLE_TOLERANCE_OFFSET_MINUTES = ACCEPTABLE_TOLERANCE_OFFSET * 10;
    private static final double ACCEPTABLE_TOLERANCE_OFFSET_SECONDS = ACCEPTABLE_TOLERANCE_OFFSET * 100;

    @BeforeAll
    public void start()
    {
        coordinateSystems = new ArrayList<>();
        coordinateSystems.add(CoordinateSystem.DECIMAL_DEGREES);
        coordinateSystems.add(CoordinateSystem.DM_DEGREES);
        coordinateSystems.add(CoordinateSystem.DMS_DEGREES);
        coordinateSystems.add(CoordinateSystem.MGRS);

        coordinateSystemStrings = new ArrayList<>();
        for (CoordinateSystem cs : coordinateSystems)
        {
            coordinateSystemStrings.add(cs.getName());
        }
    }

    @Test
    public void getCoordinateSystemTest()
    {
        logger.info("Get Coordinate System Test");
        for (String css : coordinateSystemStrings)
        {
            try
            {
                CoordinateSystem cs = CoordinateSystem.getCoordinateSystem(css);
                assertThat(cs).isNotNull();
            } catch (UnknownCoordinateSystemException e)
            {
                fail(e.getCause().getMessage());
            }
        }

        CoordinateSystem cs;
        for (String css : coordinateSystemStrings)
        {
            cs = CoordinateSystem.getCoordinateSystem(css, CoordinateSystem.DECIMAL_DEGREES);
            assertThat(css).isEqualTo(cs.getName());
            assertThat(cs).isNotNull();
        }
    }

    @Test
    public void locationStringTest()
    {
        logger.info("Location String Test");
        ILocation location = new RaptorLocation(50.083333D, 14.416667D);
        String badLocationString = "A3$2 9Svk# jW3x#Vg";

        for (CoordinateSystem cs : coordinateSystems)
        {
            String locationString = CoordinateSystem.getLocationString(location, cs, false);
            assertThat(locationString).isNotNull();
            assertThat(locationString).isNotEmpty();

            ILocation reverse = CoordinateSystem.computeLatLonFromString(locationString);
            assertThat(reverse).withFailMessage("Failed to parse: " + locationString + " in CS " + cs).isNotNull();

            assertThat(location.toString()).isEqualTo(reverse.toString());
        }

        assertThat(CoordinateSystem.computeLatLonFromString(badLocationString)).isNull();
    }

    @Test
    public void getStringInDecimalDegrees()
    {
        double lat = 50.08333333;
        String latString = CoordinateSystem.getLatitudeStringInDecimalDegrees(lat);
        assertThat(latString).isNotNull();

        double latReverse = Double.parseDouble(latString);
        assertThat(lat).isCloseTo(latReverse, within(0.001D));

        double lon = 14.4166777777D;
        String lonString = CoordinateSystem.getLongitudeStringInDecimalDegrees(lon);
        assertThat(lonString).isNotNull();

        double lonReverse = Double.parseDouble(lonString);
        assertThat(lon).isCloseTo(lonReverse, within(0.001D));
    }

    @Test
    public void getElevationStringTest()
    {
        double elevation = 101.987654321;
        String elevString = CoordinateSystem.getElevationString(elevation);
        assertThat(elevString).isNotNull();

        double reverse = Double.parseDouble(elevString);
        assertThat(elevation).isCloseTo(reverse, within(0.01D));
    }

    @Test
    public void exampleStringsTest()
    {
        String exampleString;
        for (CoordinateSystem cs : coordinateSystems)
        {
            exampleString = CoordinateSystem.getExampleString(cs);
            assertThat(exampleString).isNotNull();

            // And test that it can be parsed back out
            final ILocation location = CoordinateSystem.computeLatLonFromString(exampleString);
            assertThat(location).withFailMessage("Failed to parse: " + exampleString).isNotNull();
        }
    }

    @Test
    public void miscParsingTest()
    {
        // decimal degrees
        computeLatLonFromStringHelper("38.5\u00B0 -96.5\u00B0 Elev 353.35 m", 38.5, -96.5, 353.35);
        computeLatLonFromStringHelper("38.5* -96.5* Elev 353.35 m", 38.5, -96.5, 353.35);
        computeLatLonFromStringHelper("38.5d -96.5d Elev 353.35 m", 38.5, -96.5, 353.35);
        computeLatLonFromStringHelper("38.5D -96.5D Elev 353.35 m", 38.5, -96.5, 353.35);
        computeLatLonFromStringHelper("38.5D -96.5d Elev 353.35 m", 38.5, -96.5, 353.35);
        computeLatLonFromStringHelper("38.5* -96.5\u00B0 Elev 353.35 m", 38.5, -96.5, 353.35);
        computeLatLonFromStringHelper("38.5\u00B0N 96.5\u00B0W Elev 353.35 m", 38.5, -96.5, 353.35);
        computeLatLonFromStringHelper("38.5\u00B0N, 96.5\u00B0W Elev 353.35 m", 38.5, -96.5, 353.35);
        computeLatLonFromStringHelper("38.5\u00B0, -96.5\u00B0 Elev 353.35 m", 38.5, -96.5, 353.35);
        computeLatLonFromStringHelper("38.5, -96.5 Elev 353.35 m", 38.5, -96.5, 353.35);
        computeLatLonFromStringHelper("38.5, -96.5E Elev 353.35 m", 38.5, -96.5, 353.35);
        computeLatLonFromStringHelper("38.5N, -96.5 Elev 353.35 m", 38.5, -96.5, 353.35);
        computeLatLonFromStringHelper("-38.5S, -96.5 Elev 353.35 m", 38.5, -96.5, 353.35);
        computeLatLonFromStringHelper("-38.5S, -96.5E Elev 353.35 m", 38.5, -96.5, 353.35);
        computeLatLonFromStringHelper("-38.5S, -96.5E Elev 2,353.35 m", 38.5, -96.5, 2_353.35);
        computeLatLonFromStringHelper("Lat 38.31242\u00B0 Lon -96.10767\u00B0 Elev 353.35 m", 38.31242, -96.10767, 353.35);
        computeLatLonFromStringHelper("Lat 38.31242\u00B0 Lon -96.10767\u00B0 Elev 2,353.35 m", 38.31242, -96.10767, 2_353.35);

        // degrees minutes decimal seconds
        computeLatLonFromStringHelper("38d 30' 0\" N, 96d 30' 0\" W Elev 353.35 m", 38.5, -96.5, 353.35);
        computeLatLonFromStringHelper("38* 30' 0\" N, 96D 30' 0\" W Elev 353.35 m", 38.5, -96.5, 353.35);
        computeLatLonFromStringHelper("38d 30' 0\" N, -96d 30' 0\" E Elev 353.35 m", 38.5, -96.5, 353.35);
        computeLatLonFromStringHelper("-38d 30' 0\" S, 96d 30' 0\" W Elev 353.35 m", 38.5, -96.5, 353.35);
        computeLatLonFromStringHelper("-38d 30' 0\" S, -96d 30' 0\" E Elev 353.35 m", 38.5, -96.5, 353.35);
        computeLatLonFromStringHelper("38\u00B0 30' 0\" N, 96\u00B0 30' 0\" W Elev 353.35 m", 38.5, -96.5, 353.35);
        computeLatLonFromStringHelper("38d 30' 0\" N 96d 30' 0\" W Elev 353.35 m", 38.5, -96.5, 353.35);
        computeLatLonFromStringHelper("38d 30' 0\" N 96d 30' 0\" W Elev 353.35 m", 38.5, -96.5, 353.35);
        computeLatLonFromStringHelper("38d 30' 0\" -96d 30' 0\" Elev 353.35 m", 38.5, -96.5, 353.35);
        computeLatLonFromStringHelper("38d 30' 0.5\" -96d 30' 0.5\" Elev 353.35 m", 38.50014, -96.50014, 353.35);
        computeLatLonFromStringHelper("38d 30' 0.5\" -96d 30' 0.5\" Elev 2,353.35 m", 38.50014, -96.50014, 2_353.35);

        //degrees decimal minutes
        computeLatLonFromStringHelper("38d 30.5' -96d 30.5' Elev 353.35 m", 38.50833, -96.50833, 353.35);

        // mgrs
        computeLatLonFromStringHelper("09QUB 59649 94501 Elev 115m", 19.84152, -130.34029, 115);

        var mgrsParts = getMGRSParts(19.84152, -130.34029);
        assertThat(mgrsParts).isEqualTo(new String[]{"09Q", "UB", "59649", "94501"});

        var dmParts = getDMDegreesParts(38.50833);
        assertThat(dmParts[0]).isEqualTo(38D);
        assertThat(dmParts[1]).isCloseTo(30.5D, Offset.offset(ACCEPTABLE_TOLERANCE_OFFSET_MINUTES));
        dmParts = getDMDegreesParts(-96.50833D);
        assertThat(dmParts[0]).isEqualTo(-96D);
        assertThat(dmParts[1]).isCloseTo(30.5D, Offset.offset(ACCEPTABLE_TOLERANCE_OFFSET_MINUTES));

        var dmsParts = getDMSDegreesParts(38.50014);
        assertThat(dmsParts[0]).isEqualTo(38D);
        assertThat(dmsParts[1]).isEqualTo(30L);
        assertThat(dmsParts[2]).isCloseTo(0.5D, Offset.offset(ACCEPTABLE_TOLERANCE_OFFSET_SECONDS));
        dmsParts = getDMSDegreesParts(-96.50014);
        assertThat(dmsParts[0]).isEqualTo(-96D);
        assertThat(dmsParts[1]).isEqualTo(30L);
        assertThat(dmsParts[2]).isCloseTo(0.5D, Offset.offset(ACCEPTABLE_TOLERANCE_OFFSET_SECONDS));

        var location = CoordinateSystem.computeLatLonFromDMS(38, 30, 0.5, -96, 30, 0.5);
        assertThat(location.getLatitude()).isCloseTo(38.50014, Offset.offset(ACCEPTABLE_TOLERANCE_OFFSET));
        assertThat(location.getLongitude()).isCloseTo(-96.50014, Offset.offset(ACCEPTABLE_TOLERANCE_OFFSET));

        location = CoordinateSystem.computeLatLonFromDM(38, 30.5, -96, 30.5);
        assertThat(location.getLatitude()).isCloseTo(38.50833, Offset.offset(ACCEPTABLE_TOLERANCE_OFFSET));
        assertThat(location.getLongitude()).isCloseTo(-96.50833, Offset.offset(ACCEPTABLE_TOLERANCE_OFFSET));

        // testing invalid strings to ensure parsing returns null
        assertThat(CoordinateSystem.computeLatLonFromString("Lon -96.10767\u00B0 Lat 38.31242\u00B0  Elev 353.35 m")).isNull(); // assure lat before lon
        assertThat(CoordinateSystem.computeLatLonFromString("96.5\u00B0W 38.5\u00B0N Elev 353.35 m")).isNull(); // assure lat before lon
        assertThat(CoordinateSystem.computeLatLonFromString("37.2")).isNull(); // test to make sure the single number doesn't get parsed into a set of lat/lon

    }

    private void computeLatLonFromStringHelper(String coordString, double lat, double lon, double elev)
    {
        ILocation location = CoordinateSystem.computeLatLonFromString(coordString);
        assertThat(location).isNotNull();
        assertThat(location.getLongitude()).isCloseTo(lon, Offset.offset(ACCEPTABLE_TOLERANCE_OFFSET));
        assertThat(location.getLatitude()).isCloseTo(lat, Offset.offset(ACCEPTABLE_TOLERANCE_OFFSET));
        assertThat(location.getElevation()).isCloseTo(elev, Offset.offset(ACCEPTABLE_TOLERANCE_OFFSET));
    }

    @Test
    @SuppressLogging(CoordinateSystem.class)
    public void getAngleStringTest()
    {
        double angle = 90D;
        for (CoordinateSystem cs : coordinateSystems)
        {
            String angleString = CoordinateSystem.getAngleString(angle, cs);
            assertThat(angleString).isNotNull();
            if (cs == CoordinateSystem.MGRS)
            {
                assertThat(angleString).isEqualTo(CoordinateSystem.NO_LOCATION);
            }
        }
    }

    @Test
    public void miscCoordinateSystemTest()
    {
        for (CoordinateSystem cs : coordinateSystems)
        {
            assertThat(CoordinateSystem.getCoordinateSystemFormattedTextField(cs)).isNotNull();
        }
        assertThat(coordinateSystems).hasSize(CoordinateSystem.sortedValues().length);
    }

    @Test
    public void getLocationTypeFromString()
    {
        String testMGRS = CoordinateSystem.getMGRSString(1, 1);
        String testDMS = CoordinateSystem.getHMSDegreesString(1, 1);
        String testDec = "1.0 1.0";

        assertThat(CoordinateSystem.getLocationTypeFromString(testMGRS)).isEqualTo(CoordinateSystem.MGRS);
        assertThat(CoordinateSystem.getLocationTypeFromString(testDMS)).isEqualTo(CoordinateSystem.DMS_DEGREES);
        assertThat(CoordinateSystem.getLocationTypeFromString(testDec)).isEqualTo(CoordinateSystem.DECIMAL_DEGREES);

        // check to make sure elevation doesn't mess up method
        testMGRS = testMGRS + " Elev 254m";
        testDMS = testDMS + " Elev 254m";
        testDec = testDec + " Elev 254m";

        assertThat(CoordinateSystem.getLocationTypeFromString(testMGRS)).isEqualTo(CoordinateSystem.MGRS);
        assertThat(CoordinateSystem.getLocationTypeFromString(testDMS)).isEqualTo(CoordinateSystem.DMS_DEGREES);
        assertThat(CoordinateSystem.getLocationTypeFromString(testDec)).isEqualTo(CoordinateSystem.DECIMAL_DEGREES);

        testMGRS = CoordinateSystem.getMGRSString(1, 1) + "invalid";
        testDMS = CoordinateSystem.getHMSDegreesString(1, 1) + "invalid";
        testDec = "invalid 1.0 1.0";
        String testBlank = "";

        assertThat(CoordinateSystem.getLocationTypeFromString(testMGRS)).isNull();
        assertThat(CoordinateSystem.getLocationTypeFromString(testDMS)).isNull();
        assertThat(CoordinateSystem.getLocationTypeFromString(testDec)).isNull();
        assertThat(CoordinateSystem.getLocationTypeFromString(testBlank)).isNull();
    }
}
