package gov.takx.dataservices.shapes;

import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.shapes.IRangeFan;
import gov.takx.api.shapes.ShapeProperties;
import gov.takx.platform.utils.ShapeUtils;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static gov.tak.api.maps.coords.GeoPoint.ZERO_POINT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Unit tests for {@link RangeFan}.
 *
 * @since 3.4
 */
public class RangeFanTest
{
    @Test
    public void newDefaultRangeFan_doesNotUseDiscreteDistances()
    {
        final RangeFan rangeFan = new RangeFan();

        assertThat(rangeFan.getRangeDistances()).isNull();
        assertThat(rangeFan.getRangeLabels()).isNull();
    }

    @Test
    public void contains_worksProperly()
    {
        final RangeFan rangeFan = new RangeFan("rf1", ZERO_POINT, 10, 1000, 100);
        assertContains(rangeFan, 1000);

        final RangeFan rangeFan2 = new RangeFan("rf2", new GeoPoint(45, 45), List.of(100d, 200d, 500d), null);
        assertContains(rangeFan2, 500);
    }

    /**
     * Assert that the center point and {@code diameter / 2} are contained, and {@code diameter + 1} is not.
     */
    private void assertContains(RangeFan rangeFan, double diameter)
    {
        final ILocation center = rangeFan.getLocation();
        assertThat(rangeFan.contains(center)).isTrue();
        assertThat(rangeFan.contains(ShapeUtils.greatCircleEndPosition(center, diameter / 2, 0))).isTrue();
        assertThat(rangeFan.contains(ShapeUtils.greatCircleEndPosition(center, diameter + 1, 0))).isFalse();
    }

    @SuppressWarnings({"ResultOfObjectAllocationIgnored", "ConstantConditions"})
    @Test
    public void rangeFanConstructors_throw_whenGivenInvalidValues()
    {
        assertThatNullPointerException().isThrownBy(() -> new RangeFan(null, ZERO_POINT, 10, 100, 10));
        assertThatNullPointerException().isThrownBy(() -> new RangeFan("name", null, 10, 100, 10));
        assertThatNullPointerException().isThrownBy(() -> new RangeFan("name", ZERO_POINT, null, null));

        assertThatIllegalArgumentException().isThrownBy(() -> new RangeFan("rangeFan", ZERO_POINT, 100, 10, 10))
                .withMessageContaining("maxDistance must be > minDistance");

        assertThatIllegalArgumentException().isThrownBy(() -> new RangeFan("rangeFan", ZERO_POINT, List.of(), null))
                .withMessageContaining("'rangeDistances' must not be empty");

        assertThatIllegalArgumentException().isThrownBy(() -> new RangeFan("rangeFan", ZERO_POINT, List.of(-10d), null))
                .withMessageContaining("range distances must be > 0");

        assertThatIllegalArgumentException().isThrownBy(() -> new RangeFan("rangeFan", ZERO_POINT, List.of(100d, 10d), null))
                .withMessageContaining("range distances must be strictly increasing");
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void setters_throw_whenGivenInvalidValues()
    {
        final RangeFan rangeFan = new RangeFan("name", ZERO_POINT, 10, 100, 10);

        assertThatIllegalArgumentException().isThrownBy(() -> rangeFan.setMaxDistance(-10))
                .withMessageContaining("'maxDistance' must be > 0");

        assertThatIllegalArgumentException().isThrownBy(() -> rangeFan.setMinDistance(-10))
                .withMessageContaining("'minDistance' must be > 0");

        assertThatIllegalArgumentException().isThrownBy(() -> rangeFan.setInterval(-10))
                .withMessageContaining("interval must be > 0");

        assertThatIllegalArgumentException().isThrownBy(() -> rangeFan.setSampleDistance(0))
                .withMessageContaining("'sampleDistance' must be > 0");

        assertThatIllegalArgumentException().isThrownBy(() -> rangeFan.setSweep(0))
                .withMessageContaining("'sweep' must be >= 1.0 and <= 360.0");

        assertThatIllegalArgumentException().isThrownBy(() -> rangeFan.setSweep(361))
                .withMessageContaining("'sweep' must be >= 1.0 and <= 360.0");

        assertThatNullPointerException().isThrownBy(() -> rangeFan.setRangeColors(null));

        assertThatIllegalArgumentException().isThrownBy(() -> rangeFan.setRangeColors(List.of()))
                .withMessageContaining("'rangeColors' must not be empty");

        assertThatIllegalArgumentException().isThrownBy(() -> rangeFan.setRangeColors(Arrays.asList(Color.red, null)))
                .withMessageContaining("'rangeColors' entries must not be null");

        assertThatNullPointerException().isThrownBy(() -> rangeFan.setRangeDistances(null));

        assertThatIllegalArgumentException().isThrownBy(() -> rangeFan.setRangeDistances(List.of()))
                .withMessageContaining("'rangeDistances' must not be empty");

        assertThatIllegalArgumentException().isThrownBy(() -> rangeFan.setRangeDistances(List.of(10d, 0d)))
                .withMessageContaining("range distances must be > 0");

        assertThatIllegalArgumentException().isThrownBy(() -> rangeFan.setRangeDistances(List.of(100d, 10d)))
                .withMessageContaining("range distances must be strictly increasing");

        assertThatIllegalArgumentException().isThrownBy(() -> rangeFan.setRangeDistances(List.of(100d, 10d), List.of("l1")))
                .withMessageContaining("'rangeDistances' and 'rangeLabels' must have the same number of elements:");
    }

    @Test
    public void setters_fireCorrectEvents_whenValuesAreChanged()
    {
        assertCorrectEvents(rf -> rf.setShowEdges(false), rf -> rf.setShowEdges(true), ShapeProperties.SHOW_EDGES);
        assertCorrectEvents(rf -> rf.setShowOrientation(false), rf -> rf.setShowOrientation(true), ShapeProperties.SHOW_ORIENTATION);
        assertCorrectEvents(rf -> rf.setShowRangeValues(false), rf -> rf.setShowRangeValues(true), ShapeProperties.SHOW_RANGE_VALUES);
        assertCorrectEvents(rf -> rf.setInterval(10), rf -> rf.setInterval(100), ShapeProperties.INTERVAL);
        assertCorrectEvents(rf -> rf.setMinDistance(10), rf -> rf.setMinDistance(100), ShapeProperties.MIN_DISTANCE);
        assertCorrectEvents(rf -> rf.setMaxDistance(10), rf -> rf.setMaxDistance(100), ShapeProperties.MAX_DISTANCE);
        assertCorrectEvents(rf -> rf.setSweep(10), rf -> rf.setSweep(100), ShapeProperties.SWEEP);
        assertCorrectEvents(rf -> rf.setSampleDistance(10), rf -> rf.setSampleDistance(100), ShapeProperties.SAMPLE_DISTANCE);
        assertCorrectEvents(rf -> rf.setRangeType(IRangeFan.GREAT_CIRCLE), rf -> rf.setRangeType(IRangeFan.FOLLOW_TERRAIN), ShapeProperties.RANGE_TYPE);

        assertCorrectEvents(RangeFan::clearRangeDistances, rf -> rf.setRangeDistances(List.of(100d)), ShapeProperties.RANGE_DISTANCES);
        assertCorrectEvents(RangeFan::clearRangeDistances, rf -> rf.setRangeDistances(List.of(100d), List.of("r1")),
                ShapeProperties.RANGE_DISTANCES, ShapeProperties.RANGE_LABELS);

        assertCorrectEvents(rf -> rf.setRangeDistances(List.of(100d)), RangeFan::clearRangeDistances, ShapeProperties.RANGE_DISTANCES);
        assertCorrectEvents(rf -> rf.setRangeDistances(List.of(100d), List.of("r1")), RangeFan::clearRangeDistances,
                ShapeProperties.RANGE_DISTANCES, ShapeProperties.RANGE_LABELS);

        assertCorrectEvents(rf -> rf.setRangeColors(List.of(Color.red, Color.blue)), rf -> rf.setRangeColors(List.of(Color.blue, Color.red)),
                ShapeProperties.RANGE_COLORS);
    }

    /**
     * Assert that a given set of property change events occur when a RangeFan is mutated.
     *
     * @param initializer Consumer to prepare initial state
     * @param mutator Consumer to mutate the RangeFan
     * @param expectedEvents List of expected event names
     */
    private void assertCorrectEvents(Consumer<RangeFan> initializer, Consumer<RangeFan> mutator, String... expectedEvents)
    {
        final RangeFan rangeFan = new RangeFan();
        final Set<String> events = new HashSet<>();

        initializer.accept(rangeFan);
        rangeFan.addPropertyChangeListener(pce -> events.add(pce.getPropertyName()));
        mutator.accept(rangeFan);

        assertThat(events).containsExactlyInAnyOrder(expectedEvents);
    }

    @Test
    public void setters_fireNoEvents_whenPropertiesAreSetToSameValue()
    {
        assertNoEvents(rf -> rf.setShowEdges(false));
        assertNoEvents(rf -> rf.setShowOrientation(false));
        assertNoEvents(rf -> rf.setShowRangeValues(false));
        assertNoEvents(rf -> rf.setInterval(10));
        assertNoEvents(rf -> rf.setMinDistance(10));
        assertNoEvents(rf -> rf.setMaxDistance(10));
        assertNoEvents(rf -> rf.setSweep(10));
        assertNoEvents(rf -> rf.setSampleDistance(10));
        assertNoEvents(rf -> rf.setRangeType(IRangeFan.GREAT_CIRCLE));

        assertNoEvents(RangeFan::clearRangeDistances);
        assertNoEvents(rf -> rf.setRangeDistances(List.of(100d)));
        assertNoEvents(rf -> rf.setRangeDistances(List.of(100d), List.of("r1")));

        assertNoEvents(rf -> rf.setRangeColors(List.of(Color.red, Color.blue)));
    }

    /**
     * Assert that applying the same mutation twice produces no property change events.
     */
    private void assertNoEvents(Consumer<RangeFan> mutator)
    {
        assertCorrectEvents(mutator, mutator);
    }
}
