package gov.tak.api.comms.takserver.mission.data;

/**
 * @param uid The UID of the mission/feed pairing.
 * @param dataFeedUid The UID of the feed itself.
 * @param name Name of the feed.
 * @param filterType Filters the feed to include events of the given CoT type.
 * @param filterBbox Filters the feed to include events within a bounding box in
 * the format {@code <max lat>, <min lon>, <min lat>, <max lon>} <i>({@code top, left, bottom, right})</i>.
 * @param filterCallsign Filters the feed to include events from a {@link ClientEndpoint#callsign() client's callsign}.
 *
 * @since 3.5
 */
public record MissionFeed(String uid,
        String dataFeedUid,
        String name,
        String filterType,
        String filterBbox,
        String filterCallsign) implements Comparable<MissionFeed>
{
    @Override
    public int compareTo(MissionFeed o)
    {
        int cmp = name.compareTo(o.name);
        if (cmp == 0) cmp = uid.compareTo(o.uid);
        if (cmp == 0) cmp = dataFeedUid.compareTo(o.dataFeedUid);
        return cmp;
    }
}