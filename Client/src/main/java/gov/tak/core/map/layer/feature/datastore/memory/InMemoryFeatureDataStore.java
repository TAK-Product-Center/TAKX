package gov.tak.core.map.layer.feature.datastore.memory;

import android.util.Pair;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.layer.feature.AbstractFeatureDataStore3;
import com.atakmap.map.layer.feature.AttributeSet;
import com.atakmap.map.layer.feature.DataStoreException;
import com.atakmap.map.layer.feature.Feature.AltitudeMode;
import com.atakmap.map.layer.feature.FeatureDefinition;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.feature.geometry.GeometryFactory;
import com.atakmap.map.layer.feature.ogr.style.FeatureStyleParser;
import com.atakmap.map.layer.feature.style.Style;
import com.atakmap.math.MathUtils;
import com.atakmap.math.Rectangle;
import gov.takx.api.globe.IGlobeDelegate;
import gov.takx.api.layer.feature.ISharedFeatureDataStore;
import gov.takx.core.globe.tak.FeatureRendererManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.mutable.MutableLong;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * An in-memory implementation of FeatureDataStore. Stores {@link com.atakmap.map.layer.feature.Feature} and {@link com.atakmap.map.layer.feature.FeatureSet} data in HashMaps.
 * Two HashMaps are used to prevent having to filter queries based on class. Ids are generated from a simple counter.<br>
 * <p/>
 * Note that there is a single instance of this data store used across all globes to render features, as created in
 * {@link FeatureRendererManager#createFeatureRenderer(IGlobeDelegate)}, and that layer has a "type" filter specified
 * to only render feature sets that have a {@link com.atakmap.map.layer.feature.FeatureSet#getType() type} of a special
 * name of {@link ISharedFeatureDataStore#ALL_GLOBES_FEATURE_SET_TYPE}, a string of the integer globe ID, or the empty string.
 * If any other value is used for the type of the feature set, it will not be rendered.
 * <p/>
 *
 * @implNote Keep in mind that lock acquisition order matters. There are two maps in this class that require synchronized access.
 * Operations that need to access both maps should <i>almost always</i> acquire the locks in the same order (currently FeatureSet -> Feature)
 * to prevent deadlocks.
 * @since 5.2
 */
@Singleton
public class InMemoryFeatureDataStore extends AbstractFeatureDataStore3 implements ISharedFeatureDataStore
{
    private final static int DEFAULT_VISIBILITY_FLAGS = VISIBILITY_SETTINGS_FEATURESET | VISIBILITY_SETTINGS_FEATURE;
    private final static int DEFAULT_MODIFICATION_FLAGS =
            MODIFY_FEATURESET_INSERT |
                    MODIFY_FEATURESET_DELETE |
                    MODIFY_FEATURESET_UPDATE |
                    MODIFY_FEATURESET_DISPLAY_THRESHOLDS |
                    MODIFY_FEATURESET_NAME |
                    MODIFY_FEATURESET_FEATURE_INSERT |
                    MODIFY_FEATURESET_FEATURE_DELETE |
                    MODIFY_FEATURESET_FEATURE_UPDATE |
                    MODIFY_FEATURE_NAME |
                    MODIFY_FEATURE_GEOMETRY |
                    MODIFY_FEATURE_STYLE |
                    MODIFY_FEATURE_ATTRIBUTES;
    private static final Comparator<Feature> ID_COMPARATOR = Comparator.comparing(IDataStoreEntity::id);
    private static final Comparator<Feature> NAME_COMPARATOR = Comparator.comparing(Feature::name);
    private static final Comparator<Feature> NOOP_COMPARATOR = Comparator.comparing(o -> 0);

    private final MutableLong featureIdCounter = new MutableLong(0L);
    private final MutableLong featureSetIdCounter = new MutableLong(0L);
    private final Map<Long, Feature> featureMap = new ConcurrentHashMap<>();
    private final ClusteredList clusteredFeatures = new ClusteredList();
    private final Map<Long, FeatureSet> featureSetMap = new ConcurrentHashMap<>();
    private final Map<Long, FeatureSet> visibleFeatureSets = new ConcurrentHashMap<>();

    /**
     * Constructs new InMemoryFeatureDataStore with default modification and visibility flags.
     */
    public InMemoryFeatureDataStore()
    {
        super(DEFAULT_MODIFICATION_FLAGS, DEFAULT_VISIBILITY_FLAGS);
    }

    @Override
    @Nonnull
    protected Pair<Long, Long> insertFeatureImpl(long featureSetId, long fid, String name, int geomCoding, Object rawGeom, int styleCoding, Object rawStyle, AttributeSet attributes, long timestamp, long version) throws DataStoreException
    {
        return insertFeatureImpl(featureSetId, fid, name, geomCoding, rawGeom, styleCoding, rawStyle, attributes, AltitudeMode.ClampToGround, 0.0d, timestamp, version);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Inserts the new {@link com.atakmap.map.layer.feature.Feature} into the map, by id.
     */
    @Override
    @Nonnull
    protected Pair<Long, Long> insertFeatureImpl(long fsid, long fid, final String name, final int geomCoding, final Object rawGeom, final int styleCoding, final Object rawStyle, final AttributeSet attributes, final AltitudeMode altitudeMode, final double extrude, final long timestamp, long version) throws DataStoreException
    {
        if (!featureSetMap.containsKey(fsid)) return Pair.create(0L, version);

        if (version == FEATURE_VERSION_NONE) version = 1L;

        // Create objects if necessary objects
        Style style = createStyle(styleCoding, rawStyle);
        Geometry geometry = createGeometry(geomCoding, rawGeom);

        final long id;
        if (fid == FEATURE_ID_NONE)
        {
            // Need manual synchronization here because we have to scan the map for unused ids to compute the next fid
            synchronized (featureMap)
            {
                id = findId(fid, featureIdCounter, featureMap);
                final var feature = new Feature(id, version, true, fsid, name, geometry, style, attributes, altitudeMode, extrude, timestamp);
                featureMap.put(id, feature);
                clusteredFeatures.add(feature);
            }
        } else
        {
            // Otherwise if we know the feature has a requested ID we just need to check if it conflicts or not
            id = isIdAvailable(fid, featureMap);
            final var feature = new Feature(id, version, true, fsid, name, geometry, style, attributes, altitudeMode, extrude, timestamp);
            featureMap.put(id, feature);
            clusteredFeatures.add(feature);
        }
        return new Pair<>(id, version);
    }

    private long findId(long requested, @Nonnull MutableLong counter, @Nonnull Map<Long, ?> map) throws DataStoreException
    {
        // Check if we need to compute an available fid
        if (requested == FEATURE_ID_NONE)
        {
            long id;
            do
            {
                id = counter.incrementAndGet();
            } while (map.containsKey(id));
            return id;
        }

        return isIdAvailable(requested, map);
    }

    /**
     * Check if the requested feature ID is available for use.
     * @param requested Requested feature ID.
     * @param map Map to search in.
     * @return The validated unique feature ID (same as passed)
     * @throws DataStoreException When the map already contains the feature ID.
     * @since 5.8
     */
    private long isIdAvailable(long requested, @Nonnull Map<Long, ?> map) throws DataStoreException
    {
        if (map.containsKey(requested))
        {
            throw new DataStoreException("Entity already exists with id: " + requested);
        } else
        {
            return requested;
        }
    }

    @Override
    protected long insertFeatureSetImpl(com.atakmap.map.layer.feature.FeatureSet featureSet) throws DataStoreException
    {
        final long fsid = featureSet.getId();
        final long id;
        if (fsid == FEATURESET_ID_NONE)
        {
            // Need manual synchronization here because we have to scan the map for unused ids
            synchronized (featureSetMap)
            {
                id = findId(FEATURESET_ID_NONE, featureSetIdCounter, featureSetMap);
                final var newFeatureSet = new FeatureSet(id, featureSet);
                featureSetMap.put(id, newFeatureSet);
                visibleFeatureSets.put(id, newFeatureSet);
            }
        } else
        {
            // Otherwise if we know the feature-set has a requested ID we just need to check if it conflicts or not
            id = isIdAvailable(fsid, featureSetMap);
            final var newFeatureSet = new FeatureSet(id, featureSet);
            featureSetMap.put(id, newFeatureSet);
            visibleFeatureSets.put(id, newFeatureSet);
        }
        return id;
    }

    /**
     * {@inheritDoc}
     * Uses {@link AltitudeMode#ClampToGround} as the default altitude mode, and {@code 0.0} as the default extrude value.
     */
    @Override
    protected void updateFeatureImpl(long fid, int updatePropertyMask, String name, Geometry geometry, Style style, AttributeSet attributes, int attrUpdateType)
    {
        updateFeatureImpl(fid, updatePropertyMask, name, geometry, style, attributes, AltitudeMode.ClampToGround, 0.0, attrUpdateType);
    }

    @Override
    protected void updateFeatureImpl(long fid, int updatePropertyMask, String name, Geometry geometry, Style style,
                                     AttributeSet attributes, AltitudeMode altitudeMode, double extrude, int attrUpdateType)
    {
        featureMap.computeIfPresent(fid, (key, feature) -> {
            if (MathUtils.hasBits(updatePropertyMask, PROPERTY_FEATURE_NAME)) feature.setName(name);
            if (MathUtils.hasBits(updatePropertyMask, PROPERTY_FEATURE_GEOMETRY)) feature.setGeometry(geometry);
            if (MathUtils.hasBits(updatePropertyMask, PROPERTY_FEATURE_STYLE)) feature.setStyle(style);
            if (MathUtils.hasBits(updatePropertyMask, PROPERTY_FEATURE_ATTRIBUTES)) feature.setAttributes(attributes);
            if (MathUtils.hasBits(updatePropertyMask, PROPERTY_FEATURE_ALTITUDE_MODE)) feature.setAltitudeMode(altitudeMode);
            if (MathUtils.hasBits(updatePropertyMask, PROPERTY_FEATURE_EXTRUDE)) feature.setExtrude(extrude);

            return feature;
        });
    }

    @Override
    public void updateFeatureSet(long fsid, @Nullable String name, double minResolution, double maxResolution)
    {
        featureSetMap.computeIfPresent(fsid, (key, featureSet) -> {
            if (name != null) featureSet.setName(name);
            if (minResolution >= 0) featureSet.setMinGsd(minResolution);
            if (maxResolution >= 0) featureSet.setMaxGsd(maxResolution);

            return featureSet;
        });
    }

    /**
     * {@inheritDoc}
     * Uses {@code -1} as the default for minResolution and maxResolution to indicate no update.
     */
    @Override
    public void updateFeatureSet(long fsid, String name)
    {
        updateFeatureSet(fsid, name, -1, -1);
    }

    @Override
    public void updateFeatureSet(long fsid, double minResolution, double maxResolution)
    {
        updateFeatureSet(fsid, null, minResolution, maxResolution);
    }

    @Override
    protected boolean deleteFeatureImpl(long fid)
    {
        synchronized (featureMap)
        {
            final var removed = featureMap.remove(fid);
            if (removed != null) clusteredFeatures.remove(removed);
            return removed != null;
        }
    }

    @Override
    protected boolean deleteFeatureSetImpl(long fsid)
    {
        synchronized (featureSetMap)
        {
            final var removed = featureSetMap.remove(fsid);
            boolean deleted = removed != null;
            if (deleted)
            {
                if (removed.visible()) visibleFeatureSets.remove(fsid);

                // cascade delete all features
                synchronized (featureMap)
                {
                    final var bounds = clusteredFeatures.getFeatureSetBounds(fsid);
                    if (bounds != null)
                    {
                        for (int i = bounds.lower; i <= bounds.upper; i++)
                        {
                            featureMap.remove(clusteredFeatures.getFid(i));
                        }
                    }
                }
            }
            return deleted;
        }
    }

    @Override
    protected boolean setFeatureVisibleImpl(long id, boolean visible)
    {
        return featureMap.computeIfPresent(id, (key, feature) -> {
            if (feature.visible() != visible) feature.setVisible(visible);
            return feature;
        }) != null;
    }

    /**
     * @return {@code true} if the feature with the given {@code id} is visible
     * @since 5.7
     */
    @Override
    public boolean isFeatureVisible(long id)
    {
        final var feature = featureMap.get(id);
        return feature != null && feature.visible();
    }

    @Override
    protected boolean setFeatureSetVisibleImpl(long id, boolean visible)
    {
        final var updatedFeatureSet = featureSetMap.computeIfPresent(id, (key, featureSet) -> {
            if (visible != featureSet.visible()) featureSet.setVisible(visible);
            return featureSet;
        });

        if (updatedFeatureSet != null)
        {
            if (visible)
            {
                visibleFeatureSets.put(id, updatedFeatureSet);
            } else
            {
                visibleFeatureSets.remove(id);
            }
            return true;
        } else
        {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Overridden to optimize implementation.
     *
     * @since 5.5.1
     */
    @Override
    public int queryFeatureSetsCount(FeatureSetQueryParameters params)
    {
        return (int) queryFeatureSets(params, params != null && params.visibleOnly).count();
    }

    /**
     * {@inheritDoc}
     * Searches the {@link com.atakmap.map.layer.feature.Feature} map.
     * This implementation ignores the following filters:<ul>
     * <li>attributeFilters</li>
     * <li>timeout</li>
     * <li>extrudedOnly</li>
     * <li>geometryTypes</li>
     * </ul>
     */
    @Override
    public synchronized com.atakmap.map.layer.feature.FeatureCursor queryFeatures(FeatureQueryParameters featureQueryParameters)
    {
        if (featureMap.isEmpty()) return new FeatureCursor(List.of());

        var stream = featureMap.values().stream();
        if (featureQueryParameters != null)
        {
            final var featureSetIds = getFeatureSetIdsFromQueryParams(featureQueryParameters);

            stream = stream.filter(f -> (featureQueryParameters.ids == null || featureQueryParameters.ids.contains(f.id())) &&
                    (featureSetIds == null || Arrays.binarySearch(featureSetIds, f.featureSetId()) >= 0) &&
                    // featureQueryParameters.altitudeMode is always set to ClampToGround, so we have to ignore this for now
                    //(featureQueryParameters.altitudeMode == null || featureQueryParameters.altitudeMode == f.altitudeMode()) &&
                    (featureQueryParameters.names == null || featureQueryParameters.names.contains(f.name())) &&
                    (!featureQueryParameters.visibleOnly || f.visible()) &&
                    (featureQueryParameters.minimumTimestamp == TIMESTAMP_NONE || f.timestamp() >= featureQueryParameters.minimumTimestamp) &&
                    (featureQueryParameters.maximumTimestamp == TIMESTAMP_NONE || f.timestamp() <= featureQueryParameters.maximumTimestamp) &&
                    (featureQueryParameters.spatialFilter == null || intersects(featureQueryParameters.spatialFilter, f.geometry())));

            if (featureQueryParameters.spatialOps != null)
            {
                throw new UnsupportedOperationException("Cannot apply spatial operations"); // TODO TAKX-2677 handle spatial operations
            }

            stream = applyOrdering(featureQueryParameters, stream);

            if (featureQueryParameters.offset > 0) stream = stream.skip(featureQueryParameters.offset);
            if (featureQueryParameters.limit > 0) stream = stream.limit(featureQueryParameters.limit);
        }
        // Synchronize the terminal operation.
        synchronized (featureSetMap)
        {
            return new FeatureCursor(stream.toList());
        }
    }

    /**
     * Apply any ordering specified in {@link FeatureQueryParameters#order} to the given stream, returning an updated stream.
     *
     * @since 5.5.1
     */
    @Nonnull
    private static Stream<Feature> applyOrdering(@Nonnull FeatureQueryParameters params, @Nonnull Stream<Feature> stream)
    {
        if (params.order != null && !params.order.isEmpty())
        {
            var allOrders = NOOP_COMPARATOR;
            for (var order : params.order)
            {
                final Comparator<Feature> orderComp;
                if (order instanceof FeatureQueryParameters.Order.ID)
                {
                    orderComp = ID_COMPARATOR;
                } else if (order instanceof FeatureQueryParameters.Order.Name)
                {
                    orderComp = NAME_COMPARATOR;
                } else if (order instanceof FeatureQueryParameters.Order.Distance distance)
                {
                    orderComp = Comparator.comparing(f0 -> f0.geometry() == null ? null : distance.point.distanceTo(center(f0.geometry())),
                            Comparator.nullsFirst(Comparator.naturalOrder()));
                } else
                {
                    throw new UnsupportedOperationException("Can't order on " + order);
                }
                allOrders = allOrders.thenComparing(orderComp);
            }
            stream = stream.sorted(allOrders);
        }
        return stream;
    }

    /**
     * Return an array of the ids of the feature sets that match {@link FeatureQueryParameters#featureSetFilter}.
     * The returned array has the ids sorted by increasing value, so a binary search can be used to test for inclusion.
     *
     * @since 5.5.1
     */
    private long[] getFeatureSetIdsFromQueryParams(@Nonnull FeatureQueryParameters params)
    {
        final boolean featureSetVisibleOnly = params.featureSetFilter != null && params.featureSetFilter.visibleOnly;
        final boolean visibleOnly = params.visibleOnly || featureSetVisibleOnly;
        return queryFeatureSets(params.featureSetFilter, visibleOnly)
                .mapToLong(FeatureSet::id)
                .sorted().toArray();
    }

    @Override
    public com.atakmap.map.layer.feature.FeatureSetCursor queryFeatureSets(@Nullable FeatureSetQueryParameters params)
    {
        return new FeatureSetCursor(queryFeatureSets(params, params != null && params.visibleOnly).toList());
    }

    /**
     * There are cases where the kernel queries for features with {@code visibleOnly = true} specified, but the
     * {@code FeatureSetQueryParameters} specify {@code visibleOnly = false}.  Since an invisible FeatureSet can't
     * have any visible features, there's no reason to include the invisible feature set in the results.  This method
     * offers that by taking an override {@code visibleOnly}, and we ignore the one in the given {@code params}.
     *
     * @param params      query parameters
     * @param visibleOnly Pass {@code true} to only return visible feature sets
     * @return Stream providing matching feature sets
     * @since 5.5.1
     */
    private Stream<FeatureSet> queryFeatureSets(@Nullable FeatureSetQueryParameters params, boolean visibleOnly)
    {
        var stream = visibleOnly ? visibleFeatureSets.values().stream() : featureSetMap.values().stream();
        if (params != null)
        {
            stream = stream.filter(f -> (params.ids == null || params.ids.contains(f.id())) &&
                    (params.names == null || params.names.contains(f.name())) &&
                    (params.types == null || params.types.contains(f.type())) &&
                    (params.providers == null || params.providers.contains(f.provider())) &&
                    (Double.isNaN(params.minResolution) || params.minResolution >= f.maxGsd()) &&
                    (Double.isNaN(params.maxResolution) || params.maxResolution <= f.minGsd()));

            if (params.offset > 0) stream = stream.skip(params.offset);
            if (params.limit > 0) stream = stream.limit(params.limit);
        }
        return stream;
    }

    /**
     * @return false
     */
    @Override
    public boolean hasTimeReference()
    {
        return false;
    }

    /**
     * @return 0
     */
    @Override
    public long getMinimumTimestamp()
    {
        return 0;
    }

    /**
     * @return 0
     */
    @Override
    public long getMaximumTimestamp()
    {
        return 0;
    }

    /**
     * @return empty string
     */
    @Override
    public String getUri()
    {
        return "";
    }

    /**
     * @return {@code true}
     */
    @Override
    public boolean supportsExplicitIDs()
    {
        return true;
    }

    /**
     * @return false
     */
    @Override
    public boolean hasCache()
    {
        return false;
    }

    @Override
    public void clearCache()
    {
    }

    /**
     * @return 0
     */
    @Override
    public long getCacheSize()
    {
        return 0;
    }

    /**
     * {@inheritDoc}
     * Clears the Maps that hold {@link Feature} and {@link FeatureSet} entities.
     */
    @Override
    public void dispose()
    {
        synchronized (featureMap)
        {
            featureMap.clear();
            clusteredFeatures.clear();
        }
        synchronized (featureSetMap)
        {
            featureSetMap.clear();
        }
    }

    /**
     * This method uses a simple bounding envelope to check for {@link Geometry} intersection. This may have to be improved
     * in the future since it does not exactly match the results from the Spatialite DB intersection calculation, but it
     * is sufficient for the current use.
     */
    private static boolean intersects(Geometry geo1, Geometry geo2)
    {
        if (geo1 == null || geo2 == null) return false;
        Envelope env1 = geo1.getEnvelope();
        Envelope env2 = geo2.getEnvelope();
        return Rectangle.intersects(env1.minX, env1.minY, env1.maxX, env1.maxY, env2.minX, env2.minY, env2.maxX, env2.maxY);
    }

    /**
     * Calculates the center of a {@link Geometry}.
     *
     * @param geo the geometry object
     * @return the center
     */
    @Nonnull
    public static GeoPoint center(@Nonnull Geometry geo)
    {
        Envelope env = geo.getEnvelope();
        return new GeoPoint((env.maxY - env.minY) / 2, (env.maxX - env.minX) / 2, (env.maxZ - env.minZ) / 2);
    }

    private static Style createStyle(int styleCoding, Object rawStyle)
    {
        return switch (styleCoding)
        {
            case FeatureDefinition.STYLE_OGR -> FeatureStyleParser.parse2((String) rawStyle);
            case FeatureDefinition.STYLE_ATAK_STYLE -> ((Style) rawStyle);
            default -> throw new UnsupportedOperationException();
        };
    }

    private static Geometry createGeometry(int geomCoding, Object rawGeom)
    {
        return switch (geomCoding)
        {
            case FeatureDefinition.GEOM_SPATIALITE_BLOB -> GeometryFactory.parseSpatiaLiteBlob((byte[]) rawGeom);
            case FeatureDefinition.GEOM_WKB -> GeometryFactory.parseWkb((byte[]) rawGeom);
            case FeatureDefinition.GEOM_WKT -> GeometryFactory.parseWkt((String) rawGeom);
            case FeatureDefinition.GEOM_ATAK_GEOMETRY -> ((Geometry) rawGeom);
            default -> throw new UnsupportedOperationException();
        };
    }

    /**
     * This is the least CPU intensive and least memory consuming mechanism I can come up with to associate a FeatureSet
     * with all the Features it contains.  This class manages an ArrayList of tuples {@code [FID, FSID]}, and keeps
     * the list ordered by FSID.  We don't care about the ordering of the FIDs within an FSID cluster.  Binary searches
     * are used for locating an entry within a desired FSID cluster (which one is found is not important).
     *
     * @implNote Other options were things like a {@code Map<FSID, List<FID>>}, but that approach allocates a Map and
     * a List for every FeatureSet.
     * @since 5.5.1
     */
    private static class ClusteredList
    {
        private static final Comparator<IdTuple> FSID_COMPARATOR = Comparator.comparing(IdTuple::fsid);

        private final List<IdTuple> elements = new ArrayList<>(1_000);

        long getFid(int index)
        {
            return elements.get(index).fid;
        }

        /**
         * Addition of a Feature performs a binary search for the Feature's FSID (it doesn't matter which FID within the
         * FSID is actually located).  If there is no match, the FID,FSID tuple is just added to the end of the list. If
         * there is a match, then the FID,FSID tuple is inserted at the search result index.
         *
         * @apiNote Do not add the same FID,FSID more than once, or it will end up in the list multiple times.
         */
        void add(@Nonnull Feature feature)
        {
            final var tuple = new IdTuple(feature.id(), feature.featureSetId());
            final int index = Collections.binarySearch(elements, tuple, FSID_COMPARATOR);
            if (index < 0)
            {
                elements.add(tuple); // FS not present, add to the end
            } else
            {
                elements.add(index, tuple); // Add with the others in the same FS
            }
        }

        /**
         * Remove the given Feature's id from the list, if it is present.
         */
        void remove(@Nonnull Feature feature)
        {
            final int index = indexOf(feature.id(), feature.featureSetId());
            if (index >= 0) elements.remove(index);
        }

        /**
         * @return the list {@link Bounds} of the given {@code fsid}, or {@code null} if not found
         */
        @Nullable
        Bounds getFeatureSetBounds(long fsid)
        {
            final int index = Collections.binarySearch(elements, new IdTuple(0, fsid), FSID_COMPARATOR);
            return index < 0 ? null : getBounds(index, fsid);
        }

        /**
         * Locate the FSID cluster for {@code fsid}, and then search within the bounds of that cluster to find the
         * specific {@code fid} of interest.
         *
         * @return index of existing entry, or -1 if not found
         */
        private int indexOf(long fid, long fsid)
        {
            final int index = Collections.binarySearch(elements, new IdTuple(fid, fsid), FSID_COMPARATOR);

            if (index < 0) return -1; // not found

            // Search for the specific fid within the cluster bounds
            final var bounds = getBounds(index, fsid);
            for (int i = bounds.lower; i <= bounds.upper; i++)
            {
                if (elements.get(i).fid == fid) return i;
            }
            return -1;
        }

        /**
         * Determine the cluster bounds of a given {@code fsid} based on a given start {@code index} that may be
         * anywhere within the FSID cluster.
         *
         * @param index Starting index
         * @param fsid  Feature set id of interest
         * @return Index bounds of the {@code fsid} cluster
         */
        @Nonnull
        private Bounds getBounds(int index, long fsid)
        {
            int lower = index;
            while (lower > 0 && elements.get(lower - 1).fsid == fsid) lower--;

            int upper = index;
            int size = elements.size();
            while (upper < size - 1 && elements.get(upper + 1).fsid == fsid) upper++;

            return new Bounds(lower, upper);
        }

        void clear()
        {
            elements.clear();
        }

        private record IdTuple(long fid, long fsid)
        {
        }

        private record Bounds(int lower, int upper)
        {
        }
    }
}
