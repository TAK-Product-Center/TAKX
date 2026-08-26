package gov.takx.api.messages;

import java.util.Comparator;

/**
 * Comparator to compare {@link IRaptorDataMessage}s by time. The messages are sorted by the "relevant" time in
 * ascending order (oldest messages first).
 *
 * @see IRaptorDataMessage#getRelevantTime()
 */
public class RaptorDataMessageComparator implements Comparator<IRaptorDataMessage>
{
    @Override
    public int compare(IRaptorDataMessage m1, IRaptorDataMessage m2)
    {
        return Long.compare(m1.getRelevantTime(), m2.getRelevantTime());
    }
}
