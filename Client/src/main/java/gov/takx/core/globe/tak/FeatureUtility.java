package gov.takx.core.globe.tak;

import com.atakmap.map.MapSceneModel;
import com.atakmap.map.layer.feature.DataStoreException;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.FeatureDataStore2.FeatureSetQueryParameters;
import com.atakmap.map.layer.feature.FeatureDataStore4;
import com.atakmap.map.layer.feature.FeatureSet;
import com.atakmap.map.layer.feature.FeatureSetCursor;
import com.atakmap.map.layer.feature.datastore.FeatureSetDatabase2;
import com.atakmap.map.layer.feature.style.BasicPointStyle;
import com.atakmap.map.layer.feature.style.CompositeStyle;
import com.atakmap.map.layer.feature.style.LevelOfDetailStyle;
import com.atakmap.map.layer.feature.style.Style;
import com.atakmap.map.layer.raster.osm.OSMUtils;
import com.atakmap.math.MathUtils;
import gov.tak.platform.system.SystemUtils;
import gov.takx.api.mapobject.IAnnotationMapObject;
import gov.takx.core.globe.tak.features.FeatureFactory.FeatureResult;
import gov.takx.core.globe.tak.features.FeatureFactory.FeatureSetResult;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * A utility class for dealing with TAK features.
 *
 * @since 3.5
 */
public final class FeatureUtility
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * A style that can be used to prevent the automatic label applied by the kernel.
     * @since 5.7
     */
    public static final Style EMPTY_POINT_STYLE = new BasicPointStyle(0, 12); // Used to prevent automatic adding of label

    private static final FeatureSetQueryParameters featureSetQueryParameters = new FeatureSetQueryParameters();
    private static final HashSet<Long> idSet = new HashSet<>();

    /**
     * Interpret the provided feature set result and apply it to the provided data store. No locking/transactioning is performed on the datastore by the method and instead
     * should be handled by the caller if required. The returned {@link FeatureSetRegistration} object should be retained and provided in the next call for this feature set.
     * @param featureSetResult the feature set result
     * @param state the object state
     * @param dataStore the data store
     * @param featureSetRegistration an existing registration, or null
     * @return a registration object for the feature set
     */
    static FeatureSetRegistration applyFeatureResults(@Nonnull FeatureSetResult featureSetResult, @Nullable Object state, @Nonnull FeatureDataStore4 dataStore, @Nullable FeatureSetRegistration featureSetRegistration)
    {
        FeatureResult[] featureResults = featureSetResult.featureResults();
        Map<String, Long> fsidMap;
        HashMap<Object, FeatureRegistration> featureRegistrations;
        try
        {
            // Insert or update features
            if (featureSetRegistration == null)
            {
                fsidMap = new HashMap<>();
                featureRegistrations = new HashMap<>();
                for (FeatureResult featureResult : featureResults)
                {
                    long fsid = ensureFeatureSetId(featureResult, dataStore, fsidMap, featureSetResult.visible());
                    long fid = dataStore.insertFeature(new Feature(fsid, featureResult.name(), featureResult.geometry(), featureResult.style(),
                            featureResult.attributes(), featureResult.altitudeMode(), featureResult.extrude()));
                    featureRegistrations.put(featureResult.objectId(), new FeatureRegistration(fid, featureResult.groupingName()));
                }
            } else
            {
                fsidMap = new HashMap<>(featureSetRegistration.fsidMap);
                featureRegistrations = new HashMap<>(featureSetRegistration.featureRegistrations);
                // Process incoming new and updated features
                for (FeatureResult featureResult : featureResults)
                {
                    // Call ensureFeatureSetId() to make sure latest feature set visibility is applied
                    long fsid = ensureFeatureSetId(featureResult, dataStore, fsidMap, featureSetResult.visible());
                    FeatureRegistration featureRegistration = featureRegistrations.get(featureResult.objectId());
                    if (featureRegistration != null)
                    {
                        dataStore.updateFeature(featureRegistration.fid, featureResult.updatePropertyMask(), featureResult.name(), featureResult.geometry(), featureResult.style(),
                                featureResult.attributes(), featureResult.altitudeMode(), featureResult.extrude(), FeatureDataStore4.UPDATE_ATTRIBUTES_SET);
                    } else
                    {
                        long fid = dataStore.insertFeature(new Feature(fsid, featureResult.name(), featureResult.geometry(), featureResult.style(),
                                featureResult.attributes(), featureResult.altitudeMode(), featureResult.extrude()));
                        featureRegistrations.put(featureResult.objectId(), new FeatureRegistration(fid, featureResult.groupingName()));
                    }
                }

                List<Object> incomingIds = Arrays.stream(featureResults).map(FeatureResult::objectId).toList();
                List<Object> removedIds = featureSetRegistration.featureRegistrations.keySet().stream()
                        .filter(o -> !incomingIds.contains(o)).toList();
                // Process removed features
                for (Object objectKey : removedIds)
                {
                    FeatureRegistration featureRegistration = featureRegistrations.get(objectKey);
                    dataStore.deleteFeature(featureRegistration.fid);
                    featureRegistrations.remove(objectKey);
                }
            }
        } catch (DataStoreException e)
        {
            logger.error("Error inserting feature into datastore.", e);
            return null;
        }
        return new FeatureSetRegistration(fsidMap, state, featureRegistrations);
    }

    /**
     * Ensure that the feature set id exists for the provided {@link FeatureResult} and that the visibility for the feature set is set.
     * The provided map of feature result grouping names to feature set ids will be updated if applicable.
     * @param featureResult the feature result
     * @param dataStore the data store
     * @param fsidMap the map of feature result grouping names to feature set ids
     * @param isVisible {@code true} if the feature id should be visible, {@code false} if not
     * @return the feature set id
     * @since 5.3
     */
    private synchronized static long ensureFeatureSetId(@Nonnull FeatureResult featureResult, @Nonnull FeatureDataStore4 dataStore, @Nonnull Map<String, Long> fsidMap, boolean isVisible) throws DataStoreException
    {
        Long fsid = fsidMap.get(featureResult.groupingName());
        if (fsid == null)
        {
            fsid = dataStore.insertFeatureSet(new FeatureSet("Provider", featureResult.groupingName(), "Name", Double.MAX_VALUE, 0));
            fsidMap.put(featureResult.groupingName(), fsid);
        }

        if (dataStore instanceof FeatureSetDatabase2)
        {
            // Query for current feature set visibility state
            // FeatureSetDatabase2 is caching, so it's faster to query and then set only if necessary.
            idSet.clear();
            idSet.add(fsid);
            featureSetQueryParameters.ids = idSet;
            featureSetQueryParameters.visibleOnly = true;
            boolean currentIsVisible;
            try (FeatureSetCursor featureSetCursor = dataStore.queryFeatureSets(featureSetQueryParameters))
            {
                currentIsVisible = featureSetCursor.moveToNext();
            }
            if (isVisible != currentIsVisible) dataStore.setFeatureSetVisible(fsid, isVisible);
        } else
        {
            dataStore.setFeatureSetVisible(fsid, isVisible);
        }
        return fsid;
    }

    /**
     * Convert an annotation's minimum and maximum altitude to a minimum and maximum level of detail
     * if they are not set to their default values and return the converted values back to a user defined function.
     *
     * @param minAltitude the minimum altitude in meters
     * @param maxAltitude the maximum altitude in meters
     * @param applyLodFunction the apply LOD function
     * @since 5.1
     */
    public static void applyAnnotationAltitudeAsLod(double minAltitude, double maxAltitude, BiConsumer<Integer, Integer> applyLodFunction)
    {
        if (Double.compare(minAltitude, IAnnotationMapObject.DEFAULT_MIN_ACTIVE_ALTITUDE) != 0 ||
                Double.compare(maxAltitude, IAnnotationMapObject.DEFAULT_MAX_ACTIVE_ALTITUDE) != 0)
        {
            double minGsd = MapSceneModel.gsd(minAltitude, 45, 1000);
            double maxGsd = MapSceneModel.gsd(maxAltitude, 45, 1000);
            int minLod = MathUtils.clamp(OSMUtils.mapnikTileLevel(maxGsd), LevelOfDetailStyle.MIN_LOD, LevelOfDetailStyle.MAX_LOD);
            int maxLod = MathUtils.clamp(OSMUtils.mapnikTileLevel(minGsd), LevelOfDetailStyle.MIN_LOD, LevelOfDetailStyle.MAX_LOD);
            applyLodFunction.accept(minLod, maxLod);
        }
    }

    /**
     * Sanitizes URIs from WW for use with the TAK feature renderer.
     * <ul>
     * <li>Windows paths can produce URIs with "%5C" in place of backslashes.</li>
     * <li>Windows file URIs are formatted either "file:path" or "file:/path" and TAK feature renderer expects "file://path".</li>
     * </ul>
     * @param uri the uri
     * @return the sanitized uri
     * @since 5.4
     */
    public static String sanitizeUri(String uri)
    {
        uri = URLDecoder.decode(uri, StandardCharsets.UTF_8);
        uri = uri.startsWith("file:") && uri.charAt(5) != '/' ? "file://" + uri.substring(5) : uri;
        uri = uri.startsWith("file:/") && uri.charAt(6) != '/' ? "file://" + uri.substring(6) : uri;
        if (SystemUtils.isOsLinux()) uri = uri.startsWith("file://") && uri.charAt(7) != '/' ? "file:///" + uri.substring(7) : uri;
        return uri;
    }

    /**
     * Create a composite style that suppresses the automatic addition of a label.
     *
     * @param style Base style
     * @return {@link CompositeStyle} combining the given {@code style} and a point style that suppresses the label
     * @since 5.7
     */
    public static Style makeNoLabelStyle(@Nonnull Style style)
    {
        return new CompositeStyle(new Style[]{style, EMPTY_POINT_STYLE});
    }

    /**
     * A registration object for a feature set.
     * @param fsidMap a map of grouping names to feature set ids
     * @param stateKey the state key
     * @param featureRegistrations a map of features keyed to their {@link FeatureResult#objectId()}
     */
    record FeatureSetRegistration(Map<String, Long> fsidMap, Object stateKey, HashMap<Object, FeatureRegistration> featureRegistrations)
    {
        public long[] fidsForGroupingNames(String... groupingNames)
        {
            List<String> groupingNamesList = Arrays.asList(groupingNames);
            return featureRegistrations.values().stream().filter(fr -> groupingNamesList.contains(fr.groupingName)).mapToLong(fr -> fr.fid).toArray();
        }
    }

    /**
     * A registration object for a feature.
     * @param fid the feature id
     * @param groupingName the feature grouping name
     */
    record FeatureRegistration(long fid, String groupingName)
    {
    }
}
