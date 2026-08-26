package gov.takx.platform.utils;

import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// TODO KMB: Combine into one ShapeUtilsTest
/**
 * Test polygon containment.
 *
 */
public class EllipseContainmentTest
{
    @Test
    public void standardEllipseContainment()
    {
        Ellipse ellipse = getStandardEllipse();

        List<ILocation> locations = getLocationsInsideStandardEllipse();
        for (ILocation location : locations)
        {
            checkLocation(ellipse, location, true);
        }

        locations = getLocationsOutsideStandardEllipse();
        for (ILocation location : locations)
        {
            checkLocation(ellipse, location, false);
        }
    }

    @Test
    public void equatorAndEllipseCrossingContainment()
    {
        Ellipse ellipse = getEquatorAndIDLCrossingEllipse();

        List<ILocation> locations = getLocationsInsideEquatorIDLEllipse();
        for (ILocation location : locations)
        {
            checkLocation(ellipse, location, true);
        }

        locations = getLocationsOutsideEquatorIDLEllipse();
        for (ILocation location : locations)
        {
            checkLocation(ellipse, location, false);
        }
    }

    private Ellipse getStandardEllipse()
    {
        return new Ellipse(new RaptorLocation(18.4031, 2.6811), 94.1, 167.7, 143.3830);
    }

    private List<ILocation> getLocationsInsideStandardEllipse()
    {
        List<ILocation> locations = new ArrayList<>();

        locations.add(new RaptorLocation(18.40361, 2.68086));
        locations.add(new RaptorLocation(18.40299, 2.68077));
        locations.add(new RaptorLocation(18.40288, 2.68140));
        locations.add(new RaptorLocation(18.40268, 2.68140));
        locations.add(new RaptorLocation(18.40248, 2.68129));
        locations.add(new RaptorLocation(18.40374, 2.68078));

        return locations;
    }

    private List<ILocation> getLocationsOutsideStandardEllipse()
    {
        List<ILocation> locations = new ArrayList<>();

        locations.add(new RaptorLocation(18.40395, 2.68097));
        locations.add(new RaptorLocation(18.40271, 2.68066));
        locations.add(new RaptorLocation(18.40228, 2.68170));
        locations.add(new RaptorLocation(18.40228, 2.68123));
        locations.add(new RaptorLocation(18.40341, 2.68156));
        locations.add(new RaptorLocation(18.40376, 2.68078));

        return locations;
    }

    private Ellipse getEquatorAndIDLCrossingEllipse()
    {
        return new Ellipse(new RaptorLocation(0, 0), 1648.7, 2445.3, 40.3989);
    }

    private List<ILocation> getLocationsInsideEquatorIDLEllipse()
    {
        List<ILocation> locations = new ArrayList<>();

        locations.add(new RaptorLocation(0, 0));
        locations.add(new RaptorLocation(0.00852, 0.00244));
        locations.add(new RaptorLocation(-0.00500, -0.00841));
        locations.add(new RaptorLocation(-0.00312, 0.00640));
        locations.add(new RaptorLocation(0.00556, -0.00470));

        return locations;
    }

    private List<ILocation> getLocationsOutsideEquatorIDLEllipse()
    {
        List<ILocation> locations = new ArrayList<>();

        locations.add(new RaptorLocation(0.00969, 0.00137));
        locations.add(new RaptorLocation(-0.00171, -0.00931));
        locations.add(new RaptorLocation(-0.00990, -0.00102));
        locations.add(new RaptorLocation(0.00625, 0.00903));

        return locations;
    }

    private void checkLocation(Ellipse ellipse, ILocation location, boolean inside)
    {
        assertThat(ShapeUtils.ellipseContainsPoint(ellipse.ellipseCenter, ellipse.minorAxisMeters, ellipse.majorAxisMeters, ellipse.angle, location)).withFailMessage(location.getLatitude() + "/" + location.getLongitude()).isEqualTo(inside);
    }

    private record Ellipse(ILocation ellipseCenter, double minorAxisMeters, double majorAxisMeters, double angle)
    {
    }
}
