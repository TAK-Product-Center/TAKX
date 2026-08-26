package gov.takx.core.globe.utils;

import com.koloboke.collect.LongCursor;
import com.koloboke.collect.map.LongObjCursor;
import com.koloboke.collect.map.LongObjMap;
import com.koloboke.collect.map.hash.HashLongObjMaps;
import com.koloboke.collect.set.LongSet;
import com.koloboke.function.LongObjPredicate;
import gov.takx.core.mapdata.TrackPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.TimeUnit;

/**
 * Class to manage summary counts in the {@link SummaryQuadTree} grouped by styling context.
 *
 * @since 1.13
 */
public final class SummaryCounter
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // For preventing spewing continuous error messages
    private static volatile long lastNullReportTime = 0;
    private static final long TEN_SECONDS = TimeUnit.SECONDS.toMillis(10);

    private final LongObjMap<Counter> counts = HashLongObjMaps.newMutableMap();

    /**
     * Default constructor.
     */
    public SummaryCounter()
    {
    }

    /**
     * Copy constructor.
     *
     * @param source Source to copy
     */
    public SummaryCounter(SummaryCounter source)
    {
        // Need to make a deep copy of the source
        source.forEachEntry((mapKey, counter) -> {
            counts.put(mapKey, new Counter(counter));
            return true;
        });
    }

    /**
     * Copy the counts from the given source summary counter into this one, replacing any existing counts.
     *
     * @param source Source from which to copy counts
     */
    public void copyCounts(SummaryCounter source)
    {
        counts.clear();

        // Need to make a deep copy of the source
        source.forEachEntry((mapKey, counter) -> {
            counts.put(mapKey, new Counter(counter));
            return true;
        });
    }

    /**
     * @return The number of counter entries (unique groups represented)
     */
    public final int getSize()
    {
        return counts.size();
    }

    /**
     * Ensure that an SC key exists in the counter set, make its count 0 if it is not present.
     *
     * @param key The key of interest
     */
    public synchronized void ensureKeyExists(StylingContextKey key)
    {
        // We are getting a null key in our counts from somewhere (I think from deleted/suppressed MO mishandling)
        // This leads to NPEs in several places, so prevent it from occurring byt not allowing null as a key.
        if (key == null)
        {
            reportNullKey();
            return;
        }

        if (!counts.containsKey(key.mapKey))
        {
            counts.put(key.mapKey, new Counter());
        }
    }

    /**
     * Increment the counter for the given styling context key by 1.  Initialize it to 1 if there was no previous count
     * for that key.  Also keep track of the latest relevant time for any track point in this cell.
     *
     * @param scKey Context key for which to increment the counter
     * @param point the TrackPoint for which we are bumping the count
     */
    public synchronized void bump(StylingContextKey scKey, TrackPoint point)
    {
        // We are getting a null key in our counts from somewhere (I think from deleted/suppressed MO mishandling)
        // This leads to NPEs in several places, so prevent it from occurring byt not allowing null as a key.
        if (scKey == null)
        {
            reportNullKey();
            return;
        }

        final Counter counter = getOrCreateCounter(scKey.mapKey);
        final long time = point.time;

        counter.count += 1;
        if (time > counter.latestTime) counter.latestTime = time;
    }

    /**
     * Determine if any of the counts recorded are affected by the TSD assignments given.
     *
     * @param assignments The TSDAssignment set to process
     * @return {@code true} if we have a count entry for any MO referenced in the assignment set, {@code false} otherwise
     */
    public boolean hasAffectedCounts(TSDAssignmentSet assignments)
    {
        // Don't convert this to a lambda within a call to counts.forEachKey since that would require the use
        // of a local variable (like an AtomicBoolean) to hold the "return" value, and that seems to make the compiler
        // generate a lambda object to hold the capture, thus leading to additional memory allocations.

        for (LongCursor cursor = counts.keySet().cursor(); cursor.moveNext();)
        {
            final int moId = StylingContextKey.getMapObjectIdFromMapKey(cursor.elem());
            if (assignments.containsMO(moId))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Apply a visitor to each entry in the counts map.
     *
     * @param visitor Visitor to apply to each map entry
     */
    public final synchronized void forEachEntry(LongObjPredicate<Counter> visitor)
    {
        counts.forEachWhile(visitor);
    }

    /**
     * Get all the styling context keys referenced in this counter.  A new array is allocated on each call.
     *
     * @return context keys
     */
    public final long[] getScKeys()
    {
        return counts.keySet().toLongArray();
    }

    /**
     * Test to see if there is a count for any of the given styling context keys in this counter set.
     *
     * @param scKeys keys to test
     * @return {@code true} if we have a count for any of the given keys
     */
    public final boolean hasCountForAnyKey(LongSet scKeys)
    {
        for (LongCursor cursor = scKeys.cursor(); cursor.moveNext();)
        {
            if (counts.containsKey(cursor.elem())) return true;
        }
        return false;
    }

    /**
     * Determine if there are any counts for the given MapObject id, regardless of the styling context.
     *
     * @param moId Id of MapObject of interest
     * @return {@code true} if we have any count entries for that MO
     */
    public final synchronized boolean hasCountForMapObject(int moId)
    {
        for (LongCursor cursor = counts.keySet().cursor(); cursor.moveNext();)
        {
            if (StylingContextKey.getMapObjectIdFromMapKey(cursor.elem()) == moId) return true;
        }

        return false;
    }

    /**
     * @return The total of all counts in this counter
     */
    public final synchronized int getTotalCount()
    {
        int totalCount = 0;
        for (Counter counter : counts.values())
        {
            totalCount += counter == null ? 0 : counter.count;
        }

        return totalCount;
    }

    /**
     * Get the count associated with the given styling context map key.  If no count has been recorded, return 0.
     *
     * @param mapKey Map Key of styling context
     * @return Count recorded, or 0 if none
     */
    public final int getCount(long mapKey)
    {
        Counter counter = counts.get(mapKey);
        return counter == null ? 0 : counter.count;
    }

    /**
     * Get an existing counter for the given map key, or create a new one if one doesn't exist.
     * <p/>
     * Note: This must be called from a method that is already synchronized.
     *
     * @param mapKey Map key from styling context key
     * @return Existing or newly created counter entry
     */
    private Counter getOrCreateCounter(long mapKey)
    {
        return counts.computeIfAbsent(mapKey, k -> new Counter());
    }

    /**
     * Report a {@code null} context key being used on/in this counter.
     */
    private void reportNullKey()
    {
        // Only report this every 10 seconds.  We're still tracking down all the places that it can occur
        // and if we just report it on every hit we quickly flood the log and bog down the globe.

        long now = System.currentTimeMillis();

        if (now > (lastNullReportTime + TEN_SECONDS))
        {
            logger.error("NULL key used in counter", new RuntimeException("NULL Key"));
            //noinspection AssignmentToStaticFieldFromInstanceMethod
            lastNullReportTime = now;
        }
    }

    /**
     * Adjust the count with the given Map Key by the given adjustment (which may be positive or negative).
     *
     * @param mapKey     The SC map key of the count to update
     * @param adjustment The adjustment value
     */
    public final synchronized void adjustCount(long mapKey, int adjustment)
    {
        Counter counter = getOrCreateCounter(mapKey);
        counter.count += adjustment;
    }

    /**
     * Update this counter according to the given delta counter.  Each count in our counts that has an entry in
     * the delta counter will have its count updated by the delta count (which may be positive or negative).
     * Any counts in the delta counter not present in our counts will be copied into our counts.
     *
     * @param deltaCounter The delta counter to apply
     */
    @SuppressWarnings("LogStatementGuardedByLogCondition")
    public final synchronized void adjustCounts(SummaryCounter deltaCounter)
    {
        final boolean debugEnabled = logger.isDebugEnabled();
        final int oldSize = counts.size();
        boolean changed = false;

        // Loop through all our counts and add the deltas in
        for (LongObjCursor<Counter> countsCursor = counts.cursor(); countsCursor.moveNext();)
        {
            final long mapKey = countsCursor.key();
            final Counter counter = countsCursor.value();
            final int delta = deltaCounter.getCount(mapKey);

            if (delta == 0) continue; // Skip update if no actual change

            final int ourUpdatedCount = counter.count += delta; // Add in the delta

            changed = true;

            if (ourUpdatedCount == 0)
            {
                if (debugEnabled) logger.debug("remove count {}", countAndKey(null, mapKey));
                countsCursor.remove();
            } else
            {
                if (debugEnabled) logger.debug("Updated count = {}", countAndKey(counter, mapKey));
            }
        }

        // Now loop through the delta counter and add counts for entries we don't have
        for (LongObjCursor<Counter> deltaCursor = deltaCounter.counts.cursor(); deltaCursor.moveNext();)
        {
            final long mapKey = deltaCursor.key();
            final Counter counter = deltaCursor.value();

            if (counter.count <= 0) continue; // Only copy in positive values (since we're applying a delta)
            if (counts.containsKey(mapKey)) continue; // Only add "new" entries

            counts.put(mapKey, new Counter(counter)); // Add the new entry
            changed = true;

            if (debugEnabled) logger.debug("Add count {}", countAndKey(counter, mapKey));
        }

        if (debugEnabled && changed)
        {
            logger.debug("counts changed old={} new={} {}", oldSize, counts.size(), countsToString());
        }
    }

    /**
     * Convert this counter into a delta counter.  A Delta counter is the difference between the given counter's
     * counts and this counter's counts.  The delta can then be efficiently applied up the tree to adjust parent
     * cell counts.
     * <p/>
     * Delta counters are intended to be transient and this method should <em>never</em> be called on a counter
     * actually in use within a summary tree.  Note that this method doesn't need to be synchronized since
     * it is never supposed to be applied to a live counter, right?!
     *
     * @param newCounter Counter containing "new" counts, calculate deltas from that
     */
    @SuppressWarnings("LogStatementGuardedByLogCondition")
    public final void convertToDelta(SummaryCounter newCounter)
    {
        final boolean debugEnabled = logger.isDebugEnabled();
        final int oldSize = counts.size();

        // Loop through the new counter and add a 0 count for entries we don't have
        newCounter.counts.keySet().forEach((long mapKey) -> counts.computeIfAbsent(mapKey, k -> new Counter()));

        // Loop through all our counts to calculate the deltas
        // Calculate the latestTime so when deltas that add new entries are applied, they have a correct latestTime
        for (LongObjCursor<Counter> countsCursor = counts.cursor(); countsCursor.moveNext();)
        {
            final long mapKey = countsCursor.key();
            final Counter oldCounter = countsCursor.value();
            final Counter counter = newCounter.counts.get(mapKey);

            if (counter != null)
            {
                oldCounter.count = counter.count - oldCounter.count;
                oldCounter.latestTime = Math.max(counter.latestTime, oldCounter.latestTime);
            } else
            {
                oldCounter.count = -oldCounter.count; // New count is absent (meaning 0), negate old count
            }

            // Remove 0 deltas
            if (oldCounter.count == 0)
            {
                if (debugEnabled) logger.debug("remove count {}", countAndKey(null, mapKey));
                countsCursor.remove();
            } else
            {
                if (debugEnabled) logger.debug("Delta count = {}", countAndKey(oldCounter, mapKey));
            }
        }

        if (debugEnabled) logger.debug("Delta old={} new={} {}", oldSize, counts.size(), countsToString());
    }

    /**
     * @return Debug string combination of counter value (if non-null) and String value of mapKey
     * @since 2.38
     */
    private String countAndKey(Counter counter, long mapKey)
    {
        return (counter == null ? "" : counter + " ") + "for " + StylingContextKey.fromMapKey(mapKey);
    }

    /**
     * @return Readable string form of counts map, for debugging purposes
     */
    private String countsToString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append('{');

        counts.forEach((long mapKey, Counter counter) -> sb.append(StylingContextKey.fromMapKey(mapKey)).append('=').append(counter));

        sb.append('}');
        return sb.toString();
    }

    @Override
    public final synchronized String toString()
    {
        return counts.toString();
    }

    /**
     * Counter entry to keep track of the count and latest relevant time for a given context key.
     */
    public static class Counter
    {
        public int count = 0;
        public long latestTime = 0;

        public Counter()
        {
        }

        /**
         * Copy constructor.
         *
         * @param source Source counter entry to copy
         */
        public Counter(Counter source)
        {
            count = source.count;
            latestTime = source.latestTime;
        }

        @Override
        public String toString()
        {
            return String.valueOf(count);
        }
    }
}
