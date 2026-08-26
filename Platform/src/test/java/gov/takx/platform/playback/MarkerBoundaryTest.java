package gov.takx.platform.playback;

import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.playback.IPlaybackLane;
import gov.takx.platform.playback.ui.ATimeCursor;
import gov.takx.platform.playback.ui.TimeCursorLine;
import gov.takx.platform.playback.ui.TimeCursorMarkerDiamond;
import gov.takx.test.mock.playback.TestPlaybackLane;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 1.48
 */
public class MarkerBoundaryTest
{
    @Test
    public void testMarkerBoundary()
    {
        runMarkerBoundaryTests(new MarkerBoundary());
    }

    @Test
    public void testDefaultMarker()
    {
        final DefaultMarker marker = new DefaultMarker("foo", Color.RED);

        runMarkerBoundaryTests(marker); // Ensure that our default marker behaves properly as a boundary

        assertThat(marker.getLabel()).isEqualTo("foo");
        assertThat(marker.getMarkerColor()).isEqualTo(Color.RED);
        assertThat(marker.isVisible()).isTrue();

        final ATimeCursor rulerCursor = marker.getRulerCursor();
        assertThat(rulerCursor).isNotNull();
        assertThat(rulerCursor.getX()).isEqualTo(0);
        assertThat(rulerCursor.getXLocation()).isEqualTo(-1);

        // Verify that a change in the marker position updates the ruler cursor as well
        marker.updateCursorPosition(25);
        final int xLocation = rulerCursor.getXLocation();
        assertThat(xLocation).isEqualTo(25);
        final Rectangle bounds = rulerCursor.getBounds();
        assertThat(xLocation).isBetween(bounds.x, bounds.x + bounds.width);

        final IPlaybackLane lane1 = new TestPlaybackLane();
        final IPlaybackLane lane2 = new TestPlaybackLane();

        assertThat(marker.isTargetLane(lane1)).isFalse();
        assertThat(marker.isTargetLane(lane2)).isFalse();

        marker.addTargetLane(lane1);
        assertThat(marker.isTargetLane(lane1)).isTrue();
        assertThat(marker.isTargetLane(lane2)).isFalse();
        assertThat(marker.getLaneCursor(lane1)).isInstanceOf(TimeCursorMarkerDiamond.class);
        assertThat(marker.getLaneCursor(lane2)).isInstanceOf(TimeCursorLine.class);
        assertThat(marker.getTargetLanes()).isEqualTo(List.of(lane1));

        marker.addTargetLane(lane2);
        assertThat(marker.isTargetLane(lane1)).isTrue();
        assertThat(marker.isTargetLane(lane2)).isTrue();
        assertThat(marker.getLaneCursor(lane1)).isInstanceOf(TimeCursorMarkerDiamond.class);
        assertThat(marker.getLaneCursor(lane2)).isInstanceOf(TimeCursorMarkerDiamond.class);
        assertThat(marker.getTargetLanes()).isEqualTo(List.of(lane1, lane2));

        marker.removeTargetLane(lane1);
        assertThat(marker.isTargetLane(lane1)).isFalse();
        assertThat(marker.isTargetLane(lane2)).isTrue();
        assertThat(marker.getLaneCursor(lane1)).isInstanceOf(TimeCursorLine.class);
        assertThat(marker.getLaneCursor(lane2)).isInstanceOf(TimeCursorMarkerDiamond.class);
        assertThat(marker.getTargetLanes()).isEqualTo(List.of(lane2));
    }

    /**
     * Common tests on a MarkerBoundary.
     *
     * @param boundary Boundary to test
     */
    private void runMarkerBoundaryTests(MarkerBoundary boundary)
    {
        assertThat(boundary.hasChronologicalBoundary()).isFalse();
        assertThat(boundary.hasGeographicBoundary()).isFalse();
        assertThat(boundary.hasMoment()).isFalse();
        assertThat(boundary.hasPoint()).isFalse();
        assertThat(boundary.getStartTime()).isNull();
        assertThat(boundary.getEndTime()).isNull();
        assertThat(boundary.getMoment()).isNull();
        assertThat(boundary.getPoint()).isNull();
        assertThat(boundary.getGeographicBoundary()).isNull();

        boundary.setChronologicalBoundary(10, 200);
        assertThat(boundary.getStartTime()).isEqualTo(10);
        assertThat(boundary.getEndTime()).isEqualTo(200);
        assertThat(boundary.hasChronologicalBoundary()).isTrue();
        assertThat(boundary.hasGeographicBoundary()).isFalse();
        assertThat(boundary.hasMoment()).isFalse();
        assertThat(boundary.hasPoint()).isFalse();

        boundary.clearChronologicalBoundary();
        assertThat(boundary.hasChronologicalBoundary()).isFalse();
        assertThat(boundary.hasGeographicBoundary()).isFalse();
        assertThat(boundary.hasMoment()).isFalse();
        assertThat(boundary.hasPoint()).isFalse();

        boundary.setMoment(10);
        assertThat(boundary.getMoment()).isEqualTo(10);
        assertThat(boundary.hasChronologicalBoundary()).isFalse();
        assertThat(boundary.hasGeographicBoundary()).isFalse();
        assertThat(boundary.hasMoment()).isTrue();
        assertThat(boundary.hasPoint()).isFalse();

        boundary.clearMoment();
        assertThat(boundary.hasChronologicalBoundary()).isFalse();
        assertThat(boundary.hasGeographicBoundary()).isFalse();
        assertThat(boundary.hasMoment()).isFalse();
        assertThat(boundary.hasPoint()).isFalse();

        final ILocation[] geography = {new RaptorLocation(1, 1), new RaptorLocation(2, 2)};
        boundary.setGeographicBoundary(geography);
        assertThat(boundary.getGeographicBoundary()).isEqualTo(geography);
        assertThat(boundary.hasChronologicalBoundary()).isFalse();
        assertThat(boundary.hasGeographicBoundary()).isTrue();
        assertThat(boundary.hasMoment()).isFalse();
        assertThat(boundary.hasPoint()).isFalse();

        boundary.setGeographicBoundary(null);
        assertThat(boundary.hasChronologicalBoundary()).isFalse();
        assertThat(boundary.hasGeographicBoundary()).isFalse();
        assertThat(boundary.hasMoment()).isFalse();
        assertThat(boundary.hasPoint()).isFalse();

        final ILocation point = new RaptorLocation(1, 1);
        boundary.setPoint(point);
        assertThat(boundary.getPoint()).isEqualTo(point);
        assertThat(boundary.hasChronologicalBoundary()).isFalse();
        assertThat(boundary.hasGeographicBoundary()).isFalse();
        assertThat(boundary.hasMoment()).isFalse();
        assertThat(boundary.hasPoint()).isTrue();

        boundary.setPoint(null);
        assertThat(boundary.hasChronologicalBoundary()).isFalse();
        assertThat(boundary.hasGeographicBoundary()).isFalse();
        assertThat(boundary.hasMoment()).isFalse();
        assertThat(boundary.hasPoint()).isFalse();
    }
}
