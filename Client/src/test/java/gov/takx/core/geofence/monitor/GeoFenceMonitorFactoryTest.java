package gov.takx.core.geofence.monitor;

import gov.tak.blacksmith.Blacksmith;
import gov.tak.blacksmith.BlacksmithExtension;
import gov.tak.blacksmith.mock.Mocked;
import gov.tak.blacksmith.mock.strategy.MockStrategy;
import gov.tak.blacksmith.mock.strategy.MockitoStrategy;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.geofence.GeoFence;
import gov.takx.api.mapobject.IShapeMapObject;
import gov.takx.core.geofence.AlertManager;
import gov.takx.dataservices.persist.ShapeCircleMapObject;
import gov.takx.dataservices.persist.ShapeEllipseMapObject;
import gov.takx.dataservices.persist.ShapePolygonMapObject;
import gov.takx.dataservices.persist.ShapeRectangleMapObject;
import gov.takx.dataservices.persist.ShapeSquareMapObject;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @since 5.3
 */
@ExtendWith(BlacksmithExtension.class)
public class GeoFenceMonitorFactoryTest
{
    static
    {
        MockStrategy mockStrategy = new MockitoStrategy().cached();
        BlacksmithExtension.setBlacksmithFactory(classes -> new Blacksmith(classes).withMockStrategy(mockStrategy));
    }

    private static final ILocation LOCATION = new RaptorLocation(1, 1);
    private static final String UID = "shape-uid";
    private static final GeoFence GEOFENCE = GeoFence.builder().shapeUid(UID).build();

    @Mocked
    private GeoFenceSpatialCalculator calculator;
    @Mocked
    private AlertManager alertManager;

    @Mocked
    private ShapeCircleMapObject circle;
    @Mocked
    private ShapeEllipseMapObject ellipse;
    @Mocked
    private ShapeRectangleMapObject rectangle;
    @Mocked
    private ShapeSquareMapObject square;
    @Mocked
    private ShapePolygonMapObject polygon;

    private GeoFenceMonitorFactory factory;

    @BeforeEach
    public void setup()
    {
        Mockito.reset(calculator, circle, ellipse, rectangle, square, polygon);

        when(calculator.calculateCenter(any())).thenReturn(LOCATION);
        when(calculator.calculateFurthestDistanceFromCenter(any(), Mockito.eq(LOCATION))).thenReturn(1000d);

        for (var shape : new IShapeMapObject[]{circle, ellipse, rectangle, square, polygon}) when(shape.getUid()).thenReturn(UID);

        factory = new GeoFenceMonitorFactory(calculator, alertManager);
    }

    @Test
    void createWithCircle_callsCorrectMonitorConstructor()
    {
        try (MockedConstruction<CircleGeoFenceMonitor> mockMonitor = Mockito.mockConstruction(CircleGeoFenceMonitor.class))
        {
            factory.create(circle, GEOFENCE);

            Assertions.assertThat(mockMonitor.constructed()).hasSize(1);
        }
    }

    @Test
    void createWithEllipse_callsCorrectMonitorConstructor()
    {
        try (MockedConstruction<EllipseGeoFenceMonitor> mockMonitor = Mockito.mockConstruction(EllipseGeoFenceMonitor.class))
        {
            factory.create(ellipse, GEOFENCE);

            Assertions.assertThat(mockMonitor.constructed()).hasSize(1);
        }
    }

    @Test
    void createWithRectangle_callsCorrectMonitorConstructor()
    {
        try (MockedConstruction<RectangleGeoFenceMonitor> mockMonitor = Mockito.mockConstruction(RectangleGeoFenceMonitor.class))
        {
            factory.create(rectangle, GEOFENCE);

            Assertions.assertThat(mockMonitor.constructed()).hasSize(1);
        }
    }

    @Test
    void createWithSquare_callsCorrectMonitorConstructor()
    {
        try (MockedConstruction<RectangleGeoFenceMonitor> mockMonitor = Mockito.mockConstruction(RectangleGeoFenceMonitor.class))
        {
            factory.create(square, GEOFENCE);

            Assertions.assertThat(mockMonitor.constructed()).hasSize(1);
        }
    }

    @Test
    void createWithPolygon_callsCorrectMonitorConstructor()
    {
        try (MockedConstruction<PolygonGeoFenceMonitor> mockMonitor = Mockito.mockConstruction(PolygonGeoFenceMonitor.class))
        {
            factory.create(polygon, GEOFENCE);

            Assertions.assertThat(mockMonitor.constructed()).hasSize(1);
        }
    }
}
