package gov.takx.core.globe.utils;

/**
 * Simple class to hold the data about one RDM TSD assignment.  This is just a data holder, so members are
 * accessed directly.
 *
 * @since 1.14
 */
public class TSDAssignment
{
    public final Integer oldTsdId;
    public final Integer newTsdId;
    public final int moId;
    public final int rdmId;

    /**
     * Construct an assignment record.
     *
     * @param oldTsdId The old TSD ID assigned to the RDM, may be {@code null}
     * @param newTsdId The new TSD ID assigned to the RDM, may be {@code null}
     * @param moId     The ID of the map object owning the RDM
     * @param rdmId    The ID of the RDM who's assignment changed
     */
    public TSDAssignment(Integer oldTsdId, Integer newTsdId, int moId, int rdmId)
    {
        this.oldTsdId = oldTsdId;
        this.newTsdId = newTsdId;
        this.moId = moId;
        this.rdmId = rdmId;
    }

    @Override
    public String toString()
    {
        return String.format("[TSDA: o=%s n=%s mo=%d rdm=%d]", oldTsdId, newTsdId, moId, rdmId);
    }
}
