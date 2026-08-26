package gov.takx.core.globe.utils;

import gov.takx.core.globe.render.RenderingStyleContextManager;

/**
 * Class used as a key for tracking common styling contexts for RDMs.  Any time you need to keep track of items
 * that share the same track styling context, you can use this class as the key.  It encodes the tuple to make
 * unique values for the combination of an RDM's TSD ID (which may be {@code null}) and a map object (which should be
 * the MO associated with the RDM in question).
 * <p/>
 * This key provides us with the "sharing domain" for GlobeMarkerAttributes.  We can share a GMA across the
 * set of RDM's that have the same TSD directly applied (including {@code null}) and the same owning map object.  This
 * sharing domain is needed to support the proper inheritance context for TSD's.
 * <p/>
 * You might think that the key should just be a tuple {RDM TSD key, MO TSD Key}, but since TSD's can be shared,
 * we can't use the MO TSD key since it doesn't uniquely relate to a single MO, and the TSD ona  MO can change
 * over time.
 * <p/>
 * With experience, it has turned out that the allocation time on creating instances of this class as we need them
 * is too high (1 million ould be created while rendering a single complex scene.  So, the
 * {@link RenderingStyleContextManager} is now responsible for creating and handling out
 * keys.  That way, there is exactly one key in memory for a given moId/rdmTsdId tuple.
 * <p/>
 * Also, instead of encoding the data into a single long, we'll hold it in two ints and make the fields public.
 * This avoids a method call to extract the values, consumers just access the members directly. Yep, we're into serious
 * low-level optimization land here.
 * <p>
 * As of 2.2, {@link #mapKey} was added to optimize operation allowing primitive map keys by combining our
 * two int components into a single long value.
 */
public final class StylingContextKey
{
    public final int mapObjectId;
    public final int rdmTsdId;
    public final int hashCode;
    public final long mapKey;

    /**
     * NOTE: You should not be calling this method!  Only the context manager should.
     * <p/>
     * Construct a "key" from the provided ids.
     *
     * @param mapObjectId ID of the map object owning the RDM
     * @param rdmTsdId    ID of the TSD associated with an RDM
     * @see RenderingStyleContextManager#getStylingContextKey(int, int)
     */
    public StylingContextKey(int mapObjectId, int rdmTsdId)
    {
        this.mapObjectId = mapObjectId;
        this.rdmTsdId = rdmTsdId;
        this.hashCode = (mapObjectId * 31) + rdmTsdId;
        this.mapKey = (((long) mapObjectId) << 32) | (rdmTsdId & 0xffffffffL);
    }

    /**
     * Given a map key, extract the Map Object Id (upper 32 bits).
     *
     * @param mapKey Key from which to extract MO Id
     * @return Map Object Id
     */
    public static int getMapObjectIdFromMapKey(long mapKey)
    {
        return (int) (mapKey >> 32);
    }

    /**
     * Given a map key, extract the RDM TSD Id (lower 32 bits).
     *
     * @param mapKey Key from which to extract RDM TSD Id
     * @return RDM TSD Id
     */
    public static int getRdmTsdIdFromMapKey(long mapKey)
    {
        return (int) mapKey;
    }

    /**
     * Construct a new StylingContextKey from the given map key.  This method should be used infrequently, mainly
     * for debugging.
     *
     * @param mapKey Map key to parse
     * @return new StylingContextKey made from components of mapKey
     */
    public static StylingContextKey fromMapKey(long mapKey)
    {
        return new StylingContextKey(getMapObjectIdFromMapKey(mapKey), getRdmTsdIdFromMapKey(mapKey));
    }

    @Override
    public String toString()
    {
        return String.format("[SCKey: moId=%d rdmTsdId=%d]", mapObjectId, rdmTsdId);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        //noinspection SimplifiableIfStatement
        if (o == null || getClass() != o.getClass()) return false;

        StylingContextKey otherKey = (StylingContextKey) o;
        return mapObjectId == otherKey.mapObjectId && rdmTsdId == otherKey.rdmTsdId;
    }

    @Override
    public int hashCode()
    {
        return hashCode;
    }
}
