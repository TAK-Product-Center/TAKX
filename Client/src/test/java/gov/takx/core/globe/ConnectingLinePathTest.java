package gov.takx.core.globe;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.platform.utils.WWUtils;
import gov.takx.test.AIntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

public class ConnectingLinePathTest extends AIntegrationTest
{
    @Test
    public void getCenterPoint_baseCase()
    {
        ConnectingLinePath path = new ConnectingLinePath();
        path.setPositions(List.of(Position.ZERO, new Position(Angle.POS90, Angle.POS90, 0)));
        final ILocation centerPoint = path.getCenterPoint();

        assertThat(centerPoint.getLatitude()).isEqualTo(60, offset(2E-6));
        assertThat(centerPoint.getLongitude()).isEqualTo(0, offset(1E-14));
    }

    @Test
    public void getCenterPoint_handlesZeroLength()
    {
        ConnectingLinePath path = new ConnectingLinePath();
        path.setPositions(List.of(Position.ZERO, Position.ZERO));
        final ILocation centerPoint = path.getCenterPoint();

        assertThat(centerPoint).isEqualTo(WWUtils.latLonToRaptorLocation(Position.ZERO));
    }
}
