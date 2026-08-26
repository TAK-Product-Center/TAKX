package gov.takx.platform.utils;

import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// TODO KMB: Combine into one ShapeUtilsTest
/**
 * Test ellipse intersection.
 *
 */
public class EllipseIntersectsTest
{
    @Test
    public void standardEllipseIntersects()
    {
        Ellipse ellipse = getStandardEllipse();

        List<Line> lines = getLinesIntersectingStandardEllipse();
        for (Line line : lines)
        {
            checkLine(ellipse, line.locationA, line.locationB, true);
            checkLine(ellipse, line.locationB, line.locationA, true);
        }

        lines = getLinesNotIntersectingStandardEllipse();

        for (Line line : lines)
        {
            checkLine(ellipse, line.locationA, line.locationB, false);
        }
    }

    private Ellipse getStandardEllipse()
    {
        return new Ellipse(new RaptorLocation(18.4031, 2.6811), 94.1, 167.7, 143.3830);
    }

    private List<Line> getLinesIntersectingStandardEllipse()
    {
        List<Line> lines = new ArrayList<>();

        lines.add(new Line(new RaptorLocation(18.40361, 2.68086), new RaptorLocation(18, 2)));
        lines.add(new Line(new RaptorLocation(18.40386, 2.67904), new RaptorLocation(18.40356, 2.68137)));

        return lines;
    }

    private List<Line> getLinesNotIntersectingStandardEllipse()
    {
        List<Line> lines = new ArrayList<>();

        lines.add(new Line(new RaptorLocation(0, 0), new RaptorLocation(-25, -25)));

        return lines;
    }

    private void checkLine(Ellipse ellipse, ILocation locationA, ILocation locationB, boolean inside)
    {
        assertThat(ShapeUtils.ellipseIntersectsLine(ellipse.ellipseCenter, ellipse.minorAxisMeters, ellipse.majorAxisMeters, ellipse.angle, locationA, locationB)).withFailMessage(locationA.getLatitude() + "/" + locationA.getLongitude() + ", " + locationB.getLatitude() + "/" + locationB.getLongitude()).isEqualTo(inside);
    }

    private record Ellipse(ILocation ellipseCenter, double minorAxisMeters, double majorAxisMeters, double angle)
    {
    }

    private record Line(ILocation locationA, ILocation locationB)
    {
    }
}
