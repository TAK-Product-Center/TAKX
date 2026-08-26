package gov.takx.core.database.provider.hypersonic;

import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.persist.IPersistService;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.dataservices.shapes.Ellipse;
import gov.takx.test.AContainerTest;
import gov.takx.test.PersistTestSetup;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Test the custom SQL functions installed when HSQLDB is used as the DB provider.
 *
 * @since 5.5
 */
public class CustomFunctionSqlFunctionsTest extends AContainerTest
{
    private PersistTestSetup test;
    private IPersistService persistService;

    @BeforeAll
    public void setup()
    {
        test = new PersistTestSetup();
        test.init();
        persistService = getBean(IPersistServiceProvider.class).getPersistService();
    }

    @AfterAll
    public void teardown()
    {
        test.shutdown();
    }

    @Test
    public void testCircle()
    {
        final double distance = GeoCalculations.distance(new GeoPoint(0, 0), new GeoPoint(1, 1));

        var result = persistService.queryUniqueSQL("call point_in_circle(1, 1, 0, 0, " + (distance + 10) + ")");
        Assertions.assertThat(result).isEqualTo(true);

        result = persistService.queryUniqueSQL("call point_in_circle(2, 2, 0, 0, " + (distance + 10) + ")");
        Assertions.assertThat(result).isEqualTo(false);
    }

    @Test
    public void testRectangle()
    {
        var result = persistService.queryUniqueSQL("call point_in_rectangle(1, 1, 0, 0, 1.1, 1.1)");
        Assertions.assertThat(result).isEqualTo(true);

        result = persistService.queryUniqueSQL("call point_in_rectangle(2, 2, 0, 0, 1.1, 1.1)");
        Assertions.assertThat(result).isEqualTo(false);
    }

    @Test
    public void testEllipse()
    {
        final Ellipse ellipse = new Ellipse("", new GeoPoint(0d, 0d), 0d, 200_000, 100_000);

        final var lats = new StringBuilder();
        final var lons = new StringBuilder();
        ellipse.getVertices().forEach(loc -> {
            if (!lats.isEmpty()) lats.append(",");
            lats.append(loc.getLatitude());
            if (!lons.isEmpty()) lons.append(",");
            lons.append(loc.getLongitude());
        });

        var result = persistService.queryUniqueSQL("call point_in_polygon(.8, .8, ARRAY[" + lats + "], ARRAY[" + lons + "])");
        Assertions.assertThat(result).isEqualTo(true);

        result = persistService.queryUniqueSQL("call point_in_polygon(2, 2, ARRAY[" + lats + "], ARRAY[" + lons + "])");
        Assertions.assertThat(result).isEqualTo(false);
    }

    @Test
    public void testPolygon()
    {
        var result = persistService.queryUniqueSQL("call point_in_polygon(.9, .9, ARRAY[0, 0, 1, 1], ARRAY[0, 1, 1, 0])");
        Assertions.assertThat(result).isEqualTo(true);

        result = persistService.queryUniqueSQL("call point_in_polygon(1.1, 1.1, ARRAY[0, 0, 1, 1], ARRAY[0, 1, 1, 0])");
        Assertions.assertThat(result).isEqualTo(false);
    }
}