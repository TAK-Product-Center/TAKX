package gov.takx.platform.query;

import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.query.IQueryConstraints;

import java.util.Calendar;
import java.util.Set;
import java.util.function.Predicate;

/**
 * This class provides a message matcher to determine if a given {@link IRaptorDataMessage RDM}
 * matches a given {@link QueryConstraints query constraint}.
 * <p>
 * Instances of this matcher are intended to be reused on arbitrary RDMs, so the constraints are compiled up front
 * to minimize overhead on subsequent {@link #matches(IRaptorDataMessage)} operations.
 *
 * @since 1.49
 */
public class MessageMatcher
{
    private final QueryConstraints qc;
    private final MessageFieldMatcher fieldMatcher;
    private final Predicate<IRaptorDataMessage> allPredicates;

    public MessageMatcher(IQueryConstraints qc)
    {
        // Yes, this is a little gross, but I don't want to expose the internals of the query constraints via
        // the API, and this class needs access to a lot of the internals to perform the constraint tests.
        // So, we'll live with a cast for now.
        this.qc = (QueryConstraints) qc;

        allPredicates = compile();
        fieldMatcher = new MessageFieldMatcher(qc);
    }

    /**
     * @return The query constraints used to construct this matcher
     */
    public IQueryConstraints getQueryConstraints()
    {
        return qc;
    }

    /**
     * Determine if the RDM matches all the conditions in the query constraints.
     *
     * @param rdm Message to test
     * @return {@code true} if all constraints are satisfied
     */
    public boolean matches(IRaptorDataMessage rdm)
    {
        return allPredicates.test(rdm) && fieldMatcher.matches(rdm);
    }

    /**
     * Compile the constraints into a set of predicates that validate the constraints.  This will optimize repeated
     * use of this matcher against a series of RDMs.
     *
     * @return composite predicate that performs all constraint tests
     */
    private Predicate<IRaptorDataMessage> compile()
    {
        // Compose all the predicates into a single composite that properly short circuits as soon as a predicate fails

        Predicate<IRaptorDataMessage> predicate = rdm -> true; // start true

        // Note that the list of MOs can change after we are compiled, and this test must work against the
        // those future changes.

        if (!qc.mapObjects.isEmpty())
        {
            predicate = predicate.and(rdm -> qc.moIds.get(rdm.getMapObject().getId()));
        }

        if (!qc.messageIdSet.isEmpty())
        {
            predicate = predicate.and(rdm -> qc.messageIdSet.contains(rdm.getId()));
        }

        if (qc.hasDeviceDateRange)
        {
            predicate = predicate.and(rdm -> {
                final long time = rdm.getTime();
                return rdm.hasTime() && time >= qc.deviceMinDate && time <= qc.deviceMaxDate;
            });
        }

        if (qc.hasReceiveDateRange)
        {
            predicate = predicate.and(rdm -> {
                final long time = rdm.getReceiveTime();
                return time >= qc.receiveMinDate && time <= qc.receiveMaxDate;
            });
        }

        if (qc.weekDays != null)
        {
            predicate = predicate.and(rdm -> {
                final Calendar c = Calendar.getInstance();
                c.setTimeInMillis(rdm.getRelevantTime());
                int dayOfWeek = c.get(Calendar.DAY_OF_WEEK) - 1; // We use 0 - 6

                return qc.weekDays.contains(dayOfWeek);
            });
        }

        if (qc.hasTimeBucketRange)
        {
            predicate = predicate.and(this::testTimeBucket);
        }

        if (qc.hasLocationRange)
        {
            predicate = predicate.and(this::testLocationRange);
        }

        final Set<Integer> messageStructureIds = qc.getMessageStructureIds();

        if (!messageStructureIds.isEmpty())
        {
            if (messageStructureIds.size() == 1)
            {
                final int structureId = messageStructureIds.iterator().next();
                predicate = predicate.and(rdm -> structureId == rdm.getStructure().getId());
            } else
            {
                predicate = predicate.and(rdm -> messageStructureIds.contains(rdm.getStructure().getId()));
            }
        }

        return predicate;
    }

    /**
     * Test the given RDM against the time bucket constraints.
     *
     * @param rdm RDM to test
     * @return {@code true} if time bucket constraints are satisfied
     */
    private boolean testTimeBucket(IRaptorDataMessage rdm)
    {
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(rdm.getRelevantTime());

        long relTime = timeToMillisOfDay(rdm.getRelevantTime());

        final long minMillis = timeToMillisOfDay(qc.minBucketTime.getTime());
        final long maxMillis = timeToMillisOfDay(qc.maxBucketTime.getTime());

        cal.setTime(qc.minBucketTime);
        int minHour = cal.get(Calendar.HOUR_OF_DAY);
        cal.setTime(qc.maxBucketTime);
        int maxHour = cal.get(Calendar.HOUR_OF_DAY);

        if (minHour > maxHour)
        {
            // Since the times are being given to us in local time we need to adjust for the case that the
            // requested time value spans the midnight boundary. If the user is in AZ (GMT -7) and requests to
            // see tracks from 0600 to 0900 then this will be translated by Java/TAKX/Postgres to be 1100 to
            // 0200 which will result in zero rows.
            return relTime <= maxMillis || relTime >= minMillis;
        } else
        {
            return relTime >= minMillis && relTime <= maxMillis;
        }
    }

    /**
     * Test the given RDM against the location range constraints.
     *
     * @param rdm RDM to test
     * @return {@code true} if location range constraints are satisfied
     */
    private boolean testLocationRange(IRaptorDataMessage rdm)
    {
        if (!rdm.hasLocation()) return false; // Must have a location to match

        final double latitude = rdm.getLatitude();
        final double longitude = rdm.getLongitude();

        final double minLat = qc.minLocation.getLatitude();
        final double minLon = qc.minLocation.getLongitude();
        final double maxLat = qc.maxLocation.getLatitude();
        final double maxLon = qc.maxLocation.getLongitude();

        if (qc.strictCoordinates)
        {
            // Strict interpretation of the min/max longitude.  This may require the generation of a select
            // that includes two distinct longitude regions.  Consider the case of a min lon of 170 and a
            // max lon of -170.  This region spans the date line, and it requires that we generate two
            // sets of where clauses, one for 170 to 180 and one for -170 to -180.

            if (minLon > maxLon)
            {
                if (longitude > maxLon && longitude < minLon) return false;
            } else
            {
                // No special handling, just use what they said (strict interpretation)
                if (longitude < minLon || longitude > maxLon) return false;
            }
        } else
        {
            // Normalize longitude
            if (longitude < Math.min(minLon, maxLon) || longitude > Math.max(minLon, maxLon)) return false;
        }

        // Now test latitude (which we always normalize)
        if (latitude < Math.min(minLat, maxLat) || latitude > Math.max(minLat, maxLat)) return false;

        // Now elevation, if included
        if (qc.minLocation.hasElevation() && qc.maxLocation.hasElevation())
        {
            // Intentionally avoid a "rdm.hasElevation" test so that messages with no
            // elevation will be included if the minElevation specified is 0

            final double minElev = qc.minLocation.getElevation();
            final double maxElev = qc.maxLocation.getElevation();
            final double elevation = rdm.getElevation();

            return !(elevation < Math.min(minElev, maxElev)) && !(elevation > Math.max(minElev, maxElev));
        }

        return true;
    }

    /**
     * Convert a given time to the number of milliseconds into the day.
     *
     * @param t Time to convert
     * @return Number of milliseconds into the day
     */
    private long timeToMillisOfDay(long t)
    {
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(t);

        long millis = cal.get(Calendar.HOUR);
        millis = millis * 60 + cal.get(Calendar.MINUTE);
        millis = millis * 60 + cal.get(Calendar.SECOND);
        millis = millis * 1000 + cal.get(Calendar.MILLISECOND);

        return millis;
    }
}
