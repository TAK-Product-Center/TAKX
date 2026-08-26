package gov.takx.core.dataportal;

import org.junit.jupiter.api.Test;

import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test functionality of the PrioritizedEntry type.
 *
 * @since 3.0
 */
public class PrioritizedEntryTest
{
    /**
     * Validate the PrioritizedEntry class properly implements comparison and equals.
     */
    @Test
    @SuppressWarnings({"SelfAssertion", "EqualsWithItself"})
    public void testComparisons()
    {
        final PrioritizedEntry<String> ps1 = new PrioritizedEntry<>("1", 1);
        final PrioritizedEntry<String> ps2 = new PrioritizedEntry<>("2", 2);
        final PrioritizedEntry<String> ps3 = new PrioritizedEntry<>("3", 3);
        final PrioritizedEntry<String> ps31 = new PrioritizedEntry<>("1", 3);
        final PrioritizedEntry<String> ps3dup = new PrioritizedEntry<>("3", 3);

        assertThat(ps1).isGreaterThan(ps2);
        assertThat(ps2).isGreaterThan(ps3);
        assertThat(ps3).isLessThan(ps1);
        assertThat(ps31).isLessThan(ps1);

        assertThat(ps3).isEqualTo(ps3);
        assertThat(ps3dup).isEqualTo(ps3);

        assertThat(ps31).isNotEqualTo(ps3);
        assertThat(ps31).isNotEqualTo(ps3dup);
    }

    /**
     * Validate the PrioritizedEntry class properly orders within sorted sets.
     */
    @Test
    public void testInCollections()
    {
        final NavigableSet<PrioritizedEntry<String>> set = new ConcurrentSkipListSet<>();

        set.add(new PrioritizedEntry<>("3", 3));
        set.add(new PrioritizedEntry<>("2", 2));
        set.add(new PrioritizedEntry<>("1", 1));
        set.add(new PrioritizedEntry<>("1.1", 1));
        set.add(new PrioritizedEntry<>("3.1", 3));
        set.add(new PrioritizedEntry<>("3.1", 3));

        checkPrioritizedOrder(set, 5);

        final NavigableSet<PrioritizedEntry<String>> set2 = new TreeSet<>(set);

        // Add one existing and one new entry
        set2.add(new PrioritizedEntry<>("1", 1));
        set2.add(new PrioritizedEntry<>("4", 4));

        checkPrioritizedOrder(set2, 6);
    }

    /**
     * Verify that the given ordered set has the expected size and is in priority order.
     *
     * @param set          Set to test
     * @param expectedSize Expected size
     */
    private void checkPrioritizedOrder(NavigableSet<PrioritizedEntry<String>> set, int expectedSize)
    {
        assertThat(set.size()).isEqualTo(expectedSize);

        // Ensure that entries are in priority order, highest first
        int priority = Integer.MAX_VALUE;
        for (PrioritizedEntry<String> entry : set)
        {
            assertThat(entry.priority).isLessThanOrEqualTo(priority);
            priority = entry.priority;
        }
    }
}
