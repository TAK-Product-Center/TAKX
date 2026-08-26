package gov.takx.core.globe.utils;

import com.koloboke.collect.map.IntObjMap;
import com.koloboke.collect.map.hash.HashIntObjMaps;
import jakarta.annotation.Nullable;

import java.util.BitSet;

/**
 * Map of {@link TSDAssignment} entries keyed by RDM Id.  A bit set of mp object ids is also maintained based on
 * the entries added to this set/map.
 * <p/>
 * Use the {@link #add(TSDAssignment)} method, not the standard {@code put} method, to add entries.
 *
 * @since 1.14
 */
public class TSDAssignmentSet
{
    private final BitSet moIds = new BitSet();
    private final IntObjMap<TSDAssignment> assignmentMap = HashIntObjMaps.newUpdatableMap();

    /**
     * Default constructor.
     */
    public TSDAssignmentSet()
    {
    }

    /**
     * @return Size of this assignment set
     * @since 2.18
     */
    public int size()
    {
        return assignmentMap.size();
    }

    /**
     * Add the TSDAssignment to the map using it's rdmId as the key.
     *
     * @param tsda TSDAssignment to add to the set
     * @return Previous entry with that RDM Id, if any
     */
    public TSDAssignment add(TSDAssignment tsda)
    {
        moIds.set(tsda.moId);
        return assignmentMap.put(tsda.rdmId, tsda);
    }

    /**
     * Returns the assignment for the given rdmId, or {@code null} if this assignment set
     * contains no mapping for the rdmId.
     *
     * @param rdmId RDM Id of assignment to retrieve
     * @return TSDAssignment or {@code null} if not found
     * @since 2.18
     */
    @Nullable
    public TSDAssignment get(int rdmId)
    {
        return assignmentMap.get(rdmId);
    }

    /**
     * Determine if this set has any entry that references the given Map Object Id.
     *
     * @param moId ID of MO in question
     * @return {@code true} if an entry exists with that MO Id, {@code false} otherwise
     */
    public boolean containsMO(int moId)
    {
        return moIds.get(moId);
    }

    @Override
    public String toString()
    {
        return "TSDAssignmentSet{" +
                "moIds=" + moIds +
                ", " + assignmentMap.toString() +
                '}';
    }
}
