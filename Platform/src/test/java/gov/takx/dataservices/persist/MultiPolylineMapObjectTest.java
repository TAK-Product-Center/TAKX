package gov.takx.dataservices.persist;

import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.IMultiPolylineMapObject;
import gov.takx.api.mapobject.IShapePathMapObject;
import gov.takx.test.AIntegrationTest;
import gov.takx.test.mock.mapobject.TestMapObjectPropertyChangeListener;
import gov.takx.test.utils.MapObjectTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.beans.PropertyChangeEvent;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIndexOutOfBoundsException;

/**
 * Tests the functionality of {@link MultiPolylineMapObject}. Persistence verification is covered in
 * MapObjectPersistTest.
 *
 * @since 3.0
 */
public class MultiPolylineMapObjectTest extends AIntegrationTest
{
    private MapObjectTestUtils moTestUtils;
    private IMapObjectManager mapObjectManager;

    @Override
    protected void postProjectOpen()
    {
        mapObjectManager = getBean(IMapObjectManager.class);
        moTestUtils = new MapObjectTestUtils(mapObjectManager);
    }

    @AfterEach
    public void cleanupAfterMethod()
    {
        deleteAllMapObjects();
    }

    @Test
    public void test_get_polylines_when_empty()
    {
        assertThat(moTestUtils.createMultiPolylineMapObject(null, null, null).getPolylines()).isEmpty();
    }

    @Test
    public void test_get_polyline_with_bad_index()
    {
        assertThatIndexOutOfBoundsException()
                .isThrownBy(() -> moTestUtils.createMultiPolylineMapObject(null, null, null).getPolyline(0));
    }

    @Test
    public void test_add_polyline()
    {
        TestMapObjectPropertyChangeListener listener = new TestMapObjectPropertyChangeListener();
        final IMultiPolylineMapObject multiPolylineMapObject = moTestUtils.createMultiPolylineMapObject(null, null, listener);

        listener.clearEvents(); // Clear events after setup

        final IShapePathMapObject polylineMO = moTestUtils.createPathMapObject(null, null, null);
        multiPolylineMapObject.addPolyline(polylineMO);

        assertThat(multiPolylineMapObject.getPolylines()).hasSize(1).contains(polylineMO);
        assertThat(multiPolylineMapObject.getPolyline(0)).isEqualTo(polylineMO);
        final List<PropertyChangeEvent> events = listener.getEventsForEventType(IMultiPolylineMapObject.POLYLINE_ADDED);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getNewValue()).isEqualTo(polylineMO);
    }

    @Test
    public void test_remove_polyline()
    {
        TestMapObjectPropertyChangeListener listener = new TestMapObjectPropertyChangeListener();
        final IMultiPolylineMapObject multiPolylineMapObject = moTestUtils.createMultiPolylineMapObject(null, null, listener);
        final IShapePathMapObject polylineMO1 = moTestUtils.createPathMapObject(null, null, null);
        multiPolylineMapObject.addPolyline(polylineMO1);
        final IShapePathMapObject polylineMO2 = moTestUtils.createPathMapObject(null, null, null);
        multiPolylineMapObject.addPolyline(polylineMO2);

        listener.clearEvents(); // Clear events after setup

        multiPolylineMapObject.removePolyline(polylineMO1);

        assertThat(multiPolylineMapObject.getPolylines()).containsOnly(polylineMO2);
        final List<PropertyChangeEvent> events = listener.getEventsForEventType(IMultiPolylineMapObject.POLYLINE_REMOVED);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getNewValue()).isEqualTo(polylineMO1);
    }

    @Test
    public void test_delete_polylines()
    {
        final IMultiPolylineMapObject multiPolylineMapObject = moTestUtils.createMultiPolylineMapObject(null, null, null);
        final IShapePathMapObject polylineMO1 = moTestUtils.createPathMapObject(null, null, null);
        multiPolylineMapObject.addPolyline(polylineMO1);
        final IShapePathMapObject polylineMO2 = moTestUtils.createPathMapObject(null, null, null);
        multiPolylineMapObject.addPolyline(polylineMO2);

        final MapObjectTestUtils.MapObjectDeletionListener deletionListener =
                new MapObjectTestUtils.MapObjectDeletionListener(multiPolylineMapObject);

        // Delete one polyline; multi-polyline should remove it but not delete itself
        mapObjectManager.deleteMapObject(polylineMO1);

        assertThat(multiPolylineMapObject.getPolylines()).doesNotContain(polylineMO1);
        assertThat(deletionListener.getEvent()).isNull();

        deletionListener.reset();

        // Deleting the last polyline should delete the overall multi-polyline
        mapObjectManager.deleteMapObject(polylineMO2);
        assertThat(deletionListener.getEvent()).isNotNull();
    }

    @Test
    public void test_height()
    {
        TestMapObjectPropertyChangeListener listener = new TestMapObjectPropertyChangeListener();
        final IMultiPolylineMapObject multiPolylineMapObject = moTestUtils.createMultiPolylineMapObject(null, null, listener);

        listener.clearEvents(); // Clear events after setup

        assertThat(multiPolylineMapObject.hasHeight()).isFalse();
        multiPolylineMapObject.setHeightMeters(5.5);
        final List<PropertyChangeEvent> events = listener.getEventsForEventType(IMultiPolylineMapObject.HEIGHT);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getNewValue()).isEqualTo(5.5);
        assertThat(multiPolylineMapObject.hasHeight()).isTrue();
        assertThat(multiPolylineMapObject.getHeightMeters()).isEqualTo(5.5);
    }

    @Test
    public void test_mo_delete_includes_polylines()
    {
        final IMultiPolylineMapObject multiPolylineMapObject = moTestUtils.createMultiPolylineMapObject(null, null, null);
        final IShapePathMapObject polylineMO1 = moTestUtils.createPathMapObject(null, null, null);
        multiPolylineMapObject.addPolyline(polylineMO1);
        final IShapePathMapObject polylineMO2 = moTestUtils.createPathMapObject(null, null, null);
        multiPolylineMapObject.addPolyline(polylineMO2);

        final MapObjectTestUtils.MapObjectDeletionListener poly1DeletionListener =
                new MapObjectTestUtils.MapObjectDeletionListener(polylineMO1);
        final MapObjectTestUtils.MapObjectDeletionListener poly2DeletionListener =
                new MapObjectTestUtils.MapObjectDeletionListener(polylineMO2);

        mapObjectManager.deleteMapObject(multiPolylineMapObject);

        // Deleting the multi-polyline should also delete the child polyline MOs
        assertThat(poly1DeletionListener.getEvent()).isNotNull();
        assertThat(poly2DeletionListener.getEvent()).isNotNull();
    }
}
