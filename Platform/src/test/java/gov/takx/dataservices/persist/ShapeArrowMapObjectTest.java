package gov.takx.dataservices.persist;

import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.IMarkerMapObject;
import gov.takx.api.mapobject.IShapeArrowMapObject;
import gov.takx.api.mapobject.IShapePathMapObject;
import gov.takx.api.shapes.ShapeProperties;
import gov.takx.platform.utils.ShapeUtils;
import gov.takx.test.AIntegrationTest;
import gov.takx.test.mock.mapobject.TestMapObjectPropertyChangeListener;
import gov.takx.test.utils.MapObjectTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static gov.tak.test.utils.CotTestConstants.EPSILON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Tests the functionality of {@link ShapeArrowMapObject}.
 *
 * @since 3.0
 */
public class ShapeArrowMapObjectTest extends AIntegrationTest
{
    private MapObjectTestUtils moTestUtils;

    // Some correlated default values (rough values were chosen using WinTAK, then refined for results in WW)
    private static final GeoPoint DEFAULT_STARTING_POINT = new GeoPoint(37.000000, -114.000000);
    private static final GeoPoint DEFAULT_ENDING_POINT = new GeoPoint(37.0081908, -114.0122768);
    private static final double DEFAULT_RANGE = ShapeUtils.distanceTo(DEFAULT_STARTING_POINT, DEFAULT_ENDING_POINT);
    private static final double DEFAULT_BEARING = ShapeUtils.bearingTo(DEFAULT_STARTING_POINT, DEFAULT_ENDING_POINT);

    // Values for default ending point and a new starting point
    private static final GeoPoint NEW_STARTING_POINT = new GeoPoint(37.0021355, -114.0083486);
    private static final double NEW_START_RANGE = ShapeUtils.distanceTo(NEW_STARTING_POINT, DEFAULT_ENDING_POINT);
    private static final double NEW_START_BEARING = ShapeUtils.bearingTo(NEW_STARTING_POINT, DEFAULT_ENDING_POINT);

    // Values for default starting point and a new ending point
    private static final GeoPoint NEW_ENDING_POINT = new GeoPoint(36.9935411, -113.9992143);
    private static final double NEW_END_RANGE = ShapeUtils.distanceTo(DEFAULT_STARTING_POINT, NEW_ENDING_POINT);
    private static final double NEW_END_BEARING = ShapeUtils.bearingTo(DEFAULT_STARTING_POINT, NEW_ENDING_POINT);
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
    public void test_bearing_change_updates_end_point()
    {
        TestMapObjectPropertyChangeListener listener = new TestMapObjectPropertyChangeListener();
        final IShapeArrowMapObject arrowMapObject = createArrowMapObject(listener, DEFAULT_STARTING_POINT, new GeoPoint(0, 0));
        arrowMapObject.setRange(DEFAULT_RANGE);
        listener.clearEvents(); // Clear events after setup

        arrowMapObject.setBearing(DEFAULT_BEARING);
        assertThat(listener.getEventsForEventType(ShapeProperties.BEARING)).hasSize(1);
        assertThat(arrowMapObject.getBearing()).isCloseTo(DEFAULT_BEARING, EPSILON);

        // Setting the bearing should result in the locations being updated
        assertThat(listener.getEventsForEventType(ShapeProperties.LOCATIONS)).hasSize(1);
        final List<ILocation> vertices = arrowMapObject.getVertices();
        assertThat(vertices).hasSize(2);
        assertThat(vertices.get(0)).isEqualTo(DEFAULT_STARTING_POINT);
        assertThat(vertices.get(1).getLatitude()).isCloseTo(DEFAULT_ENDING_POINT.getLatitude(), EPSILON);
        assertThat(vertices.get(1).getLongitude()).isCloseTo(DEFAULT_ENDING_POINT.getLongitude(), EPSILON);
    }

    @Test
    public void test_bearing_normalization()
    {
        final IShapeArrowMapObject arrowMapObject = createArrowMapObject(null, DEFAULT_STARTING_POINT, new GeoPoint(0, 0));

        arrowMapObject.setBearing(-1);
        assertThat(arrowMapObject.getBearing()).isCloseTo(359.0, EPSILON);

        arrowMapObject.setBearing(-361);
        assertThat(arrowMapObject.getBearing()).isCloseTo(359.0, EPSILON);

        arrowMapObject.setBearing(360);
        assertThat(arrowMapObject.getBearing()).isCloseTo(0.0, EPSILON);

        arrowMapObject.setBearing(361);
        assertThat(arrowMapObject.getBearing()).isCloseTo(1.0, EPSILON);

        arrowMapObject.setBearing(1);
        assertThat(arrowMapObject.getBearing()).isCloseTo(1.0, EPSILON);
    }

    @Test
    public void test_range_too_small()
    {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> moTestUtils.createArrowMapObject(null, null, null).setRange(0.0));
    }

    @Test
    public void test_range_change_updates_end_point()
    {
        TestMapObjectPropertyChangeListener listener = new TestMapObjectPropertyChangeListener();
        final IShapeArrowMapObject arrowMapObject = createArrowMapObject(listener, DEFAULT_STARTING_POINT, new GeoPoint(0, 0));
        arrowMapObject.setBearing(DEFAULT_BEARING);

        listener.clearEvents(); // Clear events after setup

        arrowMapObject.setRange(DEFAULT_RANGE);
        assertThat(arrowMapObject.getRange()).isCloseTo(DEFAULT_RANGE, EPSILON);

        // Setting the range should result in the locations being updated
        assertThat(listener.getEventsForEventType(ShapeProperties.LOCATIONS)).hasSize(1);
        final List<ILocation> vertices = arrowMapObject.getVertices();
        assertThat(vertices).hasSize(2);
        assertThat(vertices.get(0)).isEqualTo(DEFAULT_STARTING_POINT);
        assertThat(vertices.get(1).getLatitude()).isCloseTo(DEFAULT_ENDING_POINT.getLatitude(), EPSILON);
        assertThat(vertices.get(1).getLongitude()).isCloseTo(DEFAULT_ENDING_POINT.getLongitude(), EPSILON);
    }

    @Test
    public void test_null_vertices()
    {
        assertThatNullPointerException()
                .isThrownBy(() -> moTestUtils.createArrowMapObject(null, null, null).setVertices(null));
    }

    @Test
    public void test_too_few_vertices()
    {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> moTestUtils.createArrowMapObject(null, null, null)
                        .setVertices(Collections.singletonList(DEFAULT_STARTING_POINT)));
    }

    @Test
    public void test_too_many_vertices()
    {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> moTestUtils.createArrowMapObject(null, null, null)
                        .setVertices(Arrays.asList(new GeoPoint(1, 1), new GeoPoint(2, 2), new GeoPoint(3, 3))));
    }

    @Test
    public void test_set_vertices()
    {
        TestMapObjectPropertyChangeListener listener = new TestMapObjectPropertyChangeListener();
        final IShapeArrowMapObject arrowMapObject = moTestUtils.createArrowMapObject(null, null, listener);

        listener.clearEvents(); // Clear events after setup

        arrowMapObject.setVertices(List.of(DEFAULT_STARTING_POINT, DEFAULT_ENDING_POINT));

        // A change event should be fired and the new values should be set
        assertThat(listener.getEventsForEventType(ShapeProperties.LOCATIONS)).hasSize(1);
        final List<ILocation> vertices = arrowMapObject.getVertices();
        assertThat(vertices).hasSize(2);
        assertThat(vertices.get(0)).isEqualTo(DEFAULT_STARTING_POINT);
        assertThat(vertices.get(1)).isEqualTo(DEFAULT_ENDING_POINT);

        // Updating the locations should also update the range and bearing
        assertThat(arrowMapObject.getRange()).isCloseTo(DEFAULT_RANGE, EPSILON);
        assertThat(arrowMapObject.getBearing()).isCloseTo(DEFAULT_BEARING, EPSILON);
    }

    @Test
    public void test_start_anchor_null_when_anchored()
    {
        final IShapeArrowMapObject arrowMapObject = createArrowMapObject(null, DEFAULT_STARTING_POINT, DEFAULT_ENDING_POINT);
        arrowMapObject.setStartAnchor(moTestUtils.createMarkerMapObject(null, DEFAULT_STARTING_POINT, null));
        arrowMapObject.setRangeAnchor(moTestUtils.createMarkerMapObject(null, DEFAULT_ENDING_POINT, null));
        arrowMapObject.setAnchored(true);

        // Arrow relies on the anchors, so they can't be removed
        assertThatIllegalArgumentException()
                .isThrownBy(() -> arrowMapObject.setStartAnchor(null));
    }

    @Test
    public void test_start_anchor_invalid_location()
    {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> moTestUtils.createArrowMapObject(null, null, null)
                        .setStartAnchor(moTestUtils.createMarkerMapObject(null, null, null)));
    }

    @Test
    public void test_set_and_move_start_anchor()
    {
        TestMapObjectPropertyChangeListener listener = new TestMapObjectPropertyChangeListener();
        final IShapeArrowMapObject arrowMapObject = moTestUtils.createArrowMapObject(null, null, listener);
        arrowMapObject.setRange(DEFAULT_RANGE);
        arrowMapObject.setBearing(DEFAULT_BEARING);

        listener.clearEvents(); // Clear events after setup

        final IMarkerMapObject startAnchor = moTestUtils.createMarkerMapObject(null, DEFAULT_STARTING_POINT, null);
        arrowMapObject.setStartAnchor(startAnchor);

        // Two events should fire: one for the anchor and one for the updated locations
        List<PropertyChangeEvent> startAnchorEvents = listener.getEventsForEventType(ShapeProperties.START_ANCHOR);
        assertThat(startAnchorEvents).hasSize(1);
        assertThat(startAnchorEvents.get(0).getNewValue()).isEqualTo(startAnchor.getKey());
        assertThat(listener.getEventsForEventType(ShapeProperties.LOCATIONS)).hasSize(1);

        assertThat(arrowMapObject.getStartAnchor()).isEqualTo(startAnchor);
        List<ILocation> vertices = arrowMapObject.getVertices();
        assertThat(vertices).hasSize(2);
        assertThat(vertices.get(0).getLatitude()).isCloseTo(DEFAULT_STARTING_POINT.getLatitude(), EPSILON);
        assertThat(vertices.get(0).getLongitude()).isCloseTo(DEFAULT_STARTING_POINT.getLongitude(), EPSILON);
        assertThat(vertices.get(1).getLatitude()).isCloseTo(DEFAULT_ENDING_POINT.getLatitude(), EPSILON);
        assertThat(vertices.get(1).getLongitude()).isCloseTo(DEFAULT_ENDING_POINT.getLongitude(), EPSILON);

        assertThat(arrowMapObject.getRange()).isCloseTo(DEFAULT_RANGE, EPSILON);
        assertThat(arrowMapObject.getBearing()).isCloseTo(DEFAULT_BEARING, EPSILON);

        listener.clearEvents();

        // Now move the start anchor to a new location and ensure all the appropriate values are updated
        startAnchor.setLocation(NEW_STARTING_POINT, false);

        assertThat(listener.getEventsForEventType(ShapeProperties.LOCATIONS)).hasSize(1);

        vertices = arrowMapObject.getVertices();
        assertThat(vertices).hasSize(2);
        assertThat(vertices.get(0).getLatitude()).isCloseTo(NEW_STARTING_POINT.getLatitude(), EPSILON);
        assertThat(vertices.get(0).getLongitude()).isCloseTo(NEW_STARTING_POINT.getLongitude(), EPSILON);
        assertThat(vertices.get(1).getLatitude()).isCloseTo(DEFAULT_ENDING_POINT.getLatitude(), EPSILON);
        assertThat(vertices.get(1).getLongitude()).isCloseTo(DEFAULT_ENDING_POINT.getLongitude(), EPSILON);
        assertThat(arrowMapObject.getRange()).isCloseTo(NEW_START_RANGE, EPSILON);
        assertThat(arrowMapObject.getBearing()).isCloseTo(NEW_START_BEARING, EPSILON);

        listener.clearEvents();

        // Now remove the start anchor and nothing else should change
        arrowMapObject.setStartAnchor(null);

        assertThat(listener.getReceivedEventTypes()).containsOnly(ShapeProperties.START_ANCHOR);
        startAnchorEvents = listener.getEventsForEventType(ShapeProperties.START_ANCHOR);
        assertThat(startAnchorEvents).hasSize(1);
        assertThat(startAnchorEvents.get(0).getNewValue()).isEqualTo(null);

        vertices = arrowMapObject.getVertices();
        assertThat(vertices).hasSize(2);
        assertThat(vertices.get(0).getLatitude()).isCloseTo(NEW_STARTING_POINT.getLatitude(), EPSILON);
        assertThat(vertices.get(0).getLongitude()).isCloseTo(NEW_STARTING_POINT.getLongitude(), EPSILON);
        assertThat(vertices.get(1).getLatitude()).isCloseTo(DEFAULT_ENDING_POINT.getLatitude(), EPSILON);
        assertThat(vertices.get(1).getLongitude()).isCloseTo(DEFAULT_ENDING_POINT.getLongitude(), EPSILON);
        assertThat(arrowMapObject.getRange()).isCloseTo(NEW_START_RANGE, EPSILON);
        assertThat(arrowMapObject.getBearing()).isCloseTo(NEW_START_BEARING, EPSILON);
    }

    @Test
    public void test_range_anchor_null_when_anchored()
    {
        final IShapeArrowMapObject arrowMapObject = createArrowMapObject(null, DEFAULT_STARTING_POINT, DEFAULT_ENDING_POINT);
        arrowMapObject.setStartAnchor(moTestUtils.createMarkerMapObject(null, DEFAULT_STARTING_POINT, null));
        arrowMapObject.setRangeAnchor(moTestUtils.createMarkerMapObject(null, DEFAULT_ENDING_POINT, null));
        arrowMapObject.setAnchored(true);

        // Arrow relies on the anchors, so they can't be removed
        assertThatIllegalArgumentException()
                .isThrownBy(() -> arrowMapObject.setRangeAnchor(null));
    }

    @Test
    public void test_range_anchor_invalid_location()
    {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> moTestUtils.createArrowMapObject(null, null, null)
                        .setRangeAnchor(moTestUtils.createMarkerMapObject(null, null, null)));
    }

    @Test
    public void test_set_range_anchor()
    {
        TestMapObjectPropertyChangeListener listener = new TestMapObjectPropertyChangeListener();
        final IShapeArrowMapObject arrowMapObject = moTestUtils.createArrowMapObject(null, null, listener);
        arrowMapObject.setStartingPoint(DEFAULT_STARTING_POINT);

        listener.clearEvents(); // Clear events after setup

        final IMarkerMapObject rangeAnchor = moTestUtils.createMarkerMapObject(null, DEFAULT_ENDING_POINT, null);
        arrowMapObject.setRangeAnchor(rangeAnchor);

        // Two events should fire: one for the anchor and one for the updated locations
        List<PropertyChangeEvent> rangeAnchorEvents = listener.getEventsForEventType(ShapeProperties.RANGE_ANCHOR);
        assertThat(rangeAnchorEvents).hasSize(1);
        assertThat(rangeAnchorEvents.get(0).getNewValue()).isEqualTo(rangeAnchor.getKey());
        assertThat(listener.getEventsForEventType(ShapeProperties.LOCATIONS)).hasSize(1);

        assertThat(arrowMapObject.getRangeAnchor()).isEqualTo(rangeAnchor);
        List<ILocation> vertices = arrowMapObject.getVertices();
        assertThat(vertices).hasSize(2);
        assertThat(vertices.get(0).getLatitude()).isCloseTo(DEFAULT_STARTING_POINT.getLatitude(), EPSILON);
        assertThat(vertices.get(0).getLongitude()).isCloseTo(DEFAULT_STARTING_POINT.getLongitude(), EPSILON);
        assertThat(vertices.get(1).getLatitude()).isCloseTo(DEFAULT_ENDING_POINT.getLatitude(), EPSILON);
        assertThat(vertices.get(1).getLongitude()).isCloseTo(DEFAULT_ENDING_POINT.getLongitude(), EPSILON);

        // The range and bearing values should also be updated
        assertThat(arrowMapObject.getRange()).isCloseTo(DEFAULT_RANGE, EPSILON);
        assertThat(arrowMapObject.getBearing()).isCloseTo(DEFAULT_BEARING, EPSILON);

        listener.clearEvents();

        // Now move the range anchor to a new location and ensure all the appropriate values are updated
        rangeAnchor.setLocation(NEW_ENDING_POINT, false);

        assertThat(listener.getEventsForEventType(ShapeProperties.LOCATIONS)).hasSize(1);

        vertices = arrowMapObject.getVertices();
        assertThat(vertices).hasSize(2);
        assertThat(vertices.get(0).getLatitude()).isCloseTo(DEFAULT_STARTING_POINT.getLatitude(), EPSILON);
        assertThat(vertices.get(0).getLongitude()).isCloseTo(DEFAULT_STARTING_POINT.getLongitude(), EPSILON);
        assertThat(vertices.get(1).getLatitude()).isCloseTo(NEW_ENDING_POINT.getLatitude(), EPSILON);
        assertThat(vertices.get(1).getLongitude()).isCloseTo(NEW_ENDING_POINT.getLongitude(), EPSILON);
        assertThat(arrowMapObject.getRange()).isCloseTo(NEW_END_RANGE, EPSILON);
        assertThat(arrowMapObject.getBearing()).isCloseTo(NEW_END_BEARING, EPSILON);

        listener.clearEvents();

        // Now remove the range anchor and nothing else should change
        arrowMapObject.setRangeAnchor(null);

        assertThat(listener.getReceivedEventTypes()).containsOnly(ShapeProperties.RANGE_ANCHOR);
        rangeAnchorEvents = listener.getEventsForEventType(ShapeProperties.RANGE_ANCHOR);
        assertThat(rangeAnchorEvents).hasSize(1);
        assertThat(rangeAnchorEvents.get(0).getNewValue()).isEqualTo(null);

        vertices = arrowMapObject.getVertices();
        assertThat(vertices).hasSize(2);
        assertThat(vertices.get(0).getLatitude()).isCloseTo(DEFAULT_STARTING_POINT.getLatitude(), EPSILON);
        assertThat(vertices.get(0).getLongitude()).isCloseTo(DEFAULT_STARTING_POINT.getLongitude(), EPSILON);
        assertThat(vertices.get(1).getLatitude()).isCloseTo(NEW_ENDING_POINT.getLatitude(), EPSILON);
        assertThat(vertices.get(1).getLongitude()).isCloseTo(NEW_ENDING_POINT.getLongitude(), EPSILON);
        assertThat(arrowMapObject.getRange()).isCloseTo(NEW_END_RANGE, EPSILON);
        assertThat(arrowMapObject.getBearing()).isCloseTo(NEW_END_BEARING, EPSILON);
    }

    @Test
    public void test_set_anchored_true_when_missing_anchor()
    {
        final IShapeArrowMapObject arrowMapObject = createArrowMapObject(null, DEFAULT_STARTING_POINT, DEFAULT_ENDING_POINT);
        arrowMapObject.setStartAnchor(moTestUtils.createMarkerMapObject(null, DEFAULT_STARTING_POINT, null));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> arrowMapObject.setAnchored(true));
    }

    @Test
    public void test_set_anchored_true_and_delete_start_anchor()
    {
        TestMapObjectPropertyChangeListener listener = new TestMapObjectPropertyChangeListener();
        final IShapeArrowMapObject arrowMapObject = createArrowMapObject(listener, DEFAULT_STARTING_POINT, DEFAULT_ENDING_POINT);
        final IMarkerMapObject startAnchor = moTestUtils.createMarkerMapObject(null, DEFAULT_STARTING_POINT, null);
        arrowMapObject.setStartAnchor(startAnchor);
        arrowMapObject.setRangeAnchor(moTestUtils.createMarkerMapObject(null, DEFAULT_ENDING_POINT, null));

        listener.clearEvents(); // Clear events after setup

        arrowMapObject.setAnchored(true);
        final List<PropertyChangeEvent> events = listener.getEventsForEventType(ShapeProperties.ANCHORED);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getNewValue()).isEqualTo(true);

        // Now delete the start anchor
        final MapObjectTestUtils.MapObjectDeletionListener deletionListener =
                new MapObjectTestUtils.MapObjectDeletionListener(arrowMapObject);
        mapObjectManager.deleteMapObject(startAnchor);
        assertThat(deletionListener.getEvent()).isNotNull();
    }

    @Test
    public void test_set_anchored_true_and_delete_range_anchor()
    {
        TestMapObjectPropertyChangeListener listener = new TestMapObjectPropertyChangeListener();
        final IShapeArrowMapObject arrowMapObject = createArrowMapObject(listener, DEFAULT_STARTING_POINT, DEFAULT_ENDING_POINT);
        arrowMapObject.setStartAnchor(moTestUtils.createMarkerMapObject(null, DEFAULT_STARTING_POINT, null));
        final IMarkerMapObject rangeAnchor = moTestUtils.createMarkerMapObject(null, DEFAULT_ENDING_POINT, null);
        arrowMapObject.setRangeAnchor(rangeAnchor);

        listener.clearEvents(); // Clear events after setup

        arrowMapObject.setAnchored(true);
        final List<PropertyChangeEvent> events = listener.getEventsForEventType(ShapeProperties.ANCHORED);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getNewValue()).isEqualTo(true);

        // Now delete the range anchor
        final MapObjectTestUtils.MapObjectDeletionListener deletionListener =
                new MapObjectTestUtils.MapObjectDeletionListener(arrowMapObject);
        mapObjectManager.deleteMapObject(rangeAnchor);
        assertThat(deletionListener.getEvent()).isNotNull();
    }

    @Test
    public void test_set_anchored_false_and_delete_anchors()
    {
        TestMapObjectPropertyChangeListener listener = new TestMapObjectPropertyChangeListener();
        final IShapeArrowMapObject arrowMapObject = createArrowMapObject(listener, DEFAULT_STARTING_POINT, DEFAULT_ENDING_POINT);
        final IMarkerMapObject startAnchor = moTestUtils.createMarkerMapObject(null, DEFAULT_STARTING_POINT, null);
        arrowMapObject.setStartAnchor(startAnchor);
        final IMarkerMapObject rangeAnchor = moTestUtils.createMarkerMapObject(null, DEFAULT_ENDING_POINT, null);
        arrowMapObject.setRangeAnchor(rangeAnchor);
        arrowMapObject.setAnchored(false);

        listener.clearEvents(); // Clear events after setup

        // Delete the start anchor and make sure this MO is not deleted
        final MapObjectTestUtils.MapObjectDeletionListener deletionListener =
                new MapObjectTestUtils.MapObjectDeletionListener(arrowMapObject);
        mapObjectManager.deleteMapObject(startAnchor);
        assertThat(deletionListener.getEvent()).isNull();

        // Delete the range anchor and make sure this MO is not deleted
        mapObjectManager.deleteMapObject(rangeAnchor);
        assertThat(deletionListener.getEvent()).isNull();
    }

    /**
     * Create an arrow map object with the provide listener and vertices.
     *
     * @param listener      The listener to add (or {@code null} to skip adding a listener)
     * @param startingPoint The starting point for the arrow
     * @param endingPoint   The ending point for the arrow
     * @return The arrow map object
     */
    private IShapeArrowMapObject createArrowMapObject(PropertyChangeListener listener, ILocation startingPoint, ILocation endingPoint)
    {
        return moTestUtils.createArrowMapObject(
                mo -> ((IShapePathMapObject<?>) mo).setVertices(List.of(startingPoint, endingPoint)), null, listener);
    }
}
