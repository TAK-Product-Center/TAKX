package gov.takx.core.geofence.monitor;

import android.database.sqlite.SQLiteException;
import com.atakmap.spatial.SpatialCalculator;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.maps.PointMapItem;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.blacksmith.Blacksmith;
import gov.tak.blacksmith.BlacksmithExtension;
import gov.tak.blacksmith.mock.Mocked;
import gov.tak.blacksmith.mock.strategy.MockStrategy;
import gov.tak.blacksmith.mock.strategy.MockitoStrategy;
import gov.takx.api.annotation.SuppressLogging;
import gov.takx.api.coordinatesystem.ILocation;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(BlacksmithExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GeoFenceSpatialCalculatorTest
{
    static
    {
        MockStrategy mockStrategy = new MockitoStrategy().cached();
        BlacksmithExtension.setBlacksmithFactory(classes -> new Blacksmith(classes).withMockStrategy(mockStrategy));
    }

    private static final String POINT_UID = "UID1";
    private static final GeoPoint LOCATION1 = new GeoPoint(1, 1);
    private static final GeoPoint LOCATION2 = new GeoPoint(2, 2);
    private static final GeoPoint LOCATION3 = new GeoPoint(3, 3);

    private static final String SHAPE_NAME = "polygon";
    private static final String SHAPE_UID = "polygonUID";
    private static final List<ILocation> LOCATIONS1 = List.of(LOCATION1, LOCATION2, LOCATION3);
    private static final List<ILocation> LOCATIONS2 = List.of(LOCATION3, LOCATION2, LOCATION1);

    private final SpatialCalculator spatialCalculatorReal = new SpatialCalculator.Builder().inMemory().build();
    private final SpatialCalculator spatialCalculator = Mockito.spy(spatialCalculatorReal);

    private GeoFenceSpatialCalculator geoFenceSpatialCalculator;

    @BeforeEach
    public void setup()
    {
        spatialCalculatorReal.clear();
        reset(spatialCalculator);
        geoFenceSpatialCalculator = new GeoFenceSpatialCalculator(() -> spatialCalculator);
    }

    @AfterAll
    public void afterAll()
    {
        geoFenceSpatialCalculator.stopService(null);
        spatialCalculator.dispose();
        spatialCalculatorReal.dispose();
    }

    @Test
    void whenNewPoint_addOrUpdate_addsToCalculator(@Mocked(allowCaching = false) PointMapItem point)
    {
        when(point.getUid()).thenReturn(POINT_UID);
        when(point.getLocation()).thenReturn(LOCATION1);

        assertThat(geoFenceSpatialCalculator.isPointTracked(POINT_UID)).isFalse();

        geoFenceSpatialCalculator.addOrUpdatePoint(point);
        verify(spatialCalculator).createPoint(any(IGeoPoint.class));
        assertThat(geoFenceSpatialCalculator.isPointTracked(POINT_UID)).isTrue();
    }

    @Test
    void whenExistingPoint_addOrUpdate_updatesCalculator(@Mocked(allowCaching = false) PointMapItem point)
    {
        when(point.getUid()).thenReturn(POINT_UID);
        when(point.getLocation()).thenReturn(LOCATION1);

        geoFenceSpatialCalculator.addOrUpdatePoint(point);
        assertThat(geoFenceSpatialCalculator.isPointTracked(POINT_UID)).isTrue();

        when(point.getLocation()).thenReturn(LOCATION2);
        geoFenceSpatialCalculator.addOrUpdatePoint(point);

        verify(spatialCalculator).updatePoint(anyLong(), any(IGeoPoint.class));
        assertThat(geoFenceSpatialCalculator.isPointTracked(POINT_UID)).isTrue();
    }

    @Test
    @SuppressLogging(GeoFenceSpatialCalculator.class)
    void whenNewPoint_onCalculatorException_pointIsNotTracked(@Mocked(allowCaching = false) PointMapItem point)
    {
        when(point.getUid()).thenReturn(POINT_UID);
        when(point.getLocation()).thenReturn(LOCATION1);

        assertThat(geoFenceSpatialCalculator.isPointTracked(POINT_UID)).isFalse();

        Mockito.doThrow(new SQLiteException("test")).when(spatialCalculator).createPoint(any(IGeoPoint.class));
        geoFenceSpatialCalculator.addOrUpdatePoint(point);

        assertThat(geoFenceSpatialCalculator.isPointTracked(POINT_UID)).isFalse();
    }

    @Test
    @SuppressLogging(GeoFenceSpatialCalculator.class)
    void whenExistingPoint_onCalculatorException_pointIsNotTracked(@Mocked(allowCaching = false) PointMapItem point)
    {
        when(point.getUid()).thenReturn(POINT_UID);
        when(point.getLocation()).thenReturn(LOCATION1);

        geoFenceSpatialCalculator.addOrUpdatePoint(point);
        assertThat(geoFenceSpatialCalculator.isPointTracked(POINT_UID)).isTrue();

        when(point.getLocation()).thenReturn(LOCATION2);

        Mockito.doThrow(new SQLiteException("test")).when(spatialCalculator).updatePoint(anyLong(), any(IGeoPoint.class));
        geoFenceSpatialCalculator.addOrUpdatePoint(point);

        assertThat(geoFenceSpatialCalculator.isPointTracked(POINT_UID)).isFalse();
    }

    @Test
    void removePoint_deletesGeometry(@Mocked(allowCaching = false) PointMapItem point)
    {
        when(point.getUid()).thenReturn(POINT_UID);
        when(point.getLocation()).thenReturn(LOCATION1);

        geoFenceSpatialCalculator.addOrUpdatePoint(point);
        assertThat(geoFenceSpatialCalculator.isPointTracked(POINT_UID)).isTrue();

        geoFenceSpatialCalculator.removePoint(point);
        verify(spatialCalculator).deleteGeometry(anyLong());
        assertThat(geoFenceSpatialCalculator.isPointTracked(POINT_UID)).isFalse();
    }

    @Test
    void whenNewBoundary_addOrUpdate_addsToCalculator()
    {
        assertThat(geoFenceSpatialCalculator.isGeoFenceTracked(SHAPE_UID)).isFalse();

        geoFenceSpatialCalculator.addOrUpdateGeoFenceBoundary(SHAPE_UID, SHAPE_NAME, LOCATIONS1);

        assertThat(geoFenceSpatialCalculator.isGeoFenceTracked(SHAPE_UID)).isTrue();
        verify(spatialCalculator).createPolygon(any(IGeoPoint[].class));
    }

    @Test
    void whenExistingBoundary_addOrUpdate_updatesCalculator()
    {
        geoFenceSpatialCalculator.addOrUpdateGeoFenceBoundary(SHAPE_UID, SHAPE_NAME, LOCATIONS1);
        verify(spatialCalculator).createPolygon(any(IGeoPoint[].class));
        assertThat(geoFenceSpatialCalculator.isGeoFenceTracked(SHAPE_UID)).isTrue();

        geoFenceSpatialCalculator.addOrUpdateGeoFenceBoundary(SHAPE_UID, SHAPE_NAME, LOCATIONS2);
        verify(spatialCalculator).updatePolygon(anyLong(), any(IGeoPoint[].class));
        assertThat(geoFenceSpatialCalculator.isGeoFenceTracked(SHAPE_UID)).isTrue();
    }

    @Test
    @SuppressLogging(GeoFenceSpatialCalculator.class)
    void whenNewBoundary_onCalculatorException_boundaryIsNotTracked(@Mocked(allowCaching = false) PointMapItem point)
    {
        assertThat(geoFenceSpatialCalculator.isGeoFenceTracked(SHAPE_UID)).isFalse();

        Mockito.doThrow(new SQLiteException("test")).when(spatialCalculator).createPolygon(any(IGeoPoint[].class));
        geoFenceSpatialCalculator.addOrUpdateGeoFenceBoundary(SHAPE_UID, SHAPE_NAME, LOCATIONS1);

        assertThat(geoFenceSpatialCalculator.isGeoFenceTracked(SHAPE_UID)).isFalse();
    }

    @Test
    @SuppressLogging(GeoFenceSpatialCalculator.class)
    void whenExistingBoundary_onCalculatorException_boundaryIsNotTracked(@Mocked(allowCaching = false) PointMapItem point)
    {
        geoFenceSpatialCalculator.addOrUpdateGeoFenceBoundary(SHAPE_UID, SHAPE_NAME, LOCATIONS1);
        assertThat(geoFenceSpatialCalculator.isGeoFenceTracked(SHAPE_UID)).isTrue();

        Mockito.doThrow(new SQLiteException("test")).when(spatialCalculator).updatePolygon(anyLong(), any(IGeoPoint[].class));
        geoFenceSpatialCalculator.addOrUpdateGeoFenceBoundary(SHAPE_UID, SHAPE_NAME, LOCATIONS2);

        assertThat(geoFenceSpatialCalculator.isGeoFenceTracked(SHAPE_UID)).isFalse();
    }

    @Test
    void removeBoundary_deletesGeometry()
    {
        geoFenceSpatialCalculator.addOrUpdateGeoFenceBoundary(SHAPE_UID, SHAPE_NAME, LOCATIONS1);
        assertThat(geoFenceSpatialCalculator.isGeoFenceTracked(SHAPE_UID)).isTrue();

        geoFenceSpatialCalculator.removeGeoFenceBoundary(SHAPE_UID);
        verify(spatialCalculator).deleteGeometry(anyLong());
        assertThat(geoFenceSpatialCalculator.isGeoFenceTracked(SHAPE_UID)).isFalse();
    }
}
