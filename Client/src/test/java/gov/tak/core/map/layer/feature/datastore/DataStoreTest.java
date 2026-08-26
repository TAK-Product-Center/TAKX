package gov.tak.core.map.layer.feature.datastore;

import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.layer.feature.DataStoreException;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.FeatureCursor;
import com.atakmap.map.layer.feature.FeatureDataStore2;
import com.atakmap.map.layer.feature.FeatureDataStore3;
import com.atakmap.map.layer.feature.FeatureSet;
import com.atakmap.map.layer.feature.FeatureSetCursor;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.math.Rectangle;
import gov.tak.core.map.layer.feature.datastore.memory.InMemoryFeatureDataStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Core test class for Data stores. Should be left sufficiently generic / extensible that it can be run for any data
 * store implementation. Implementation specific tests should go in subclasses.
 * NOTE: We usually set {@link com.atakmap.map.layer.feature.FeatureDataStore2.FeatureQueryParameters#altitudeMode} to null
 * because some implementations do not honor the default value of {@link com.atakmap.map.layer.feature.Feature.AltitudeMode#ClampToGround}
 * <br/>
 * TODO TAKX-2678 test 3D spatial filters, and spatial filters that cross multiple geometries
 *
 * @since 5.2
 */
public abstract class DataStoreTest extends DataStoreTestBase
{
    protected FeatureDataStore3 dataStore;

    @BeforeEach
    public void beforeEach()
    {
        dataStore = createDataStore();
    }

    public abstract FeatureDataStore3 createDataStore();

    @AfterEach
    public void afterEach()
    {
        dataStore.dispose();
    }

    @Test
    public void testFeatureSetInsert() throws DataStoreException
    {
        var data = populateData(dataStore);
        FeatureSetCursor fsc = dataStore.queryFeatureSets(null);
        assertThat(cursorToSet(fsc)).isEqualTo(new HashSet<>(data.featureSetWrappers));
    }

    @Test
    public void testFeatureSetUpdate() throws DataStoreException
    {
        var data = populateData(dataStore);
        for (FeatureSetWrapper setWrapper : data.featureSetWrappers)
        {
            FeatureSetWrapper updated = new FeatureSetWrapper(new FeatureSet(setWrapper.getProvider(), setWrapper.getType(), setWrapper.getName() + "Updated", 25000, 10000));
            dataStore.updateFeatureSet(setWrapper.getId(), updated.getName(), updated.getMinResolution(), updated.getMaxResolution());
            var params = new FeatureDataStore2.FeatureSetQueryParameters();
            params.ids = Collections.singleton(setWrapper.getId());
            FeatureSetWrapper result = cursorToList(dataStore.queryFeatureSets(params)).get(0);
            assertThat(result).isEqualTo(updated);
        }
    }

    @Test
    public void testFeatureSetDelete() throws DataStoreException
    {
        var data = populateData(dataStore);
        int size = 3;
        var toDel = data.featureSetWrappers.stream().limit(size).toList();
        for (var fsw : toDel)
        {
            dataStore.deleteFeatureSet(fsw.getId());
        }
        FeatureSetCursor fsc = dataStore.queryFeatureSets(null);
        assertThat(cursorToSet(fsc)).isEqualTo(data.featureSetWrappers.stream().filter(fsw -> !toDel.contains(fsw)).collect(Collectors.toSet()));
    }

    @Test
    public void testFeatureSetIdFilter() throws DataStoreException
    {
        var data = populateData(dataStore);
        var params = new FeatureDataStore2.FeatureSetQueryParameters();
        Set<Long> featureSetWrapperIds = data.featureSetWrappers.stream().skip(2).limit(2).map(FeatureSetWrapper::getId).collect(Collectors.toSet());
        params.ids = featureSetWrapperIds;
        assertThat(cursorToSet(dataStore.queryFeatureSets(params))).isEqualTo(data.featureSetWrappers.stream().filter(fw -> featureSetWrapperIds.contains(fw.getId())).collect(Collectors.toSet()));
    }

    @Test
    public void testFeatureSetNameFilter() throws DataStoreException
    {
        var data = populateData(dataStore);
        var params = new FeatureDataStore2.FeatureSetQueryParameters();
        params.names = data.featureSetWrappers.stream().skip(2).limit(2).map(FeatureSetWrapper::getName).collect(Collectors.toSet());
        assertThat(cursorToSet(dataStore.queryFeatureSets(params))).isEqualTo(data.featureSetWrappers.stream().filter(fw -> params.names.contains(fw.getName())).collect(Collectors.toSet()));
    }

    @Test
    public void testFeatureSetVisibleFilter() throws DataStoreException
    {
        var data = populateData(dataStore);
        var params = new FeatureDataStore2.FeatureSetQueryParameters();
        params.visibleOnly = true;
        FeatureSetWrapper invis = data.featureSetWrappers.get(0);
        dataStore.setFeatureSetVisible(invis.getId(), false);
        assertThat(dataStore.queryFeatureSetsCount(params)).isEqualTo(data.featureSetWrappers.size() - 1);
        // check for cascade to Feature
        var featParams = new FeatureDataStore2.FeatureQueryParameters();
        featParams.altitudeMode = null;
        featParams.visibleOnly = true;
        dataStore.setFeatureSetVisible(invis.getId(), false);
        assertThat(cursorToSet(dataStore.queryFeatures(featParams))).isEqualTo(data.featureWrappers.stream().filter(fw -> !fw.getFeatureSet().getId().equals(invis.getId())).collect(Collectors.toSet()));
    }

    @Test
    public void testFeatureSetTypeFilter() throws DataStoreException
    {
        var data = populateData(dataStore);
        var params = new FeatureDataStore2.FeatureSetQueryParameters();
        String type = data.featureSetWrappers.get(0).getType();
        params.types = Set.of(type);
        assertThat(cursorToSet(dataStore.queryFeatureSets(params))).isEqualTo(data.featureSetWrappers.stream().filter(fs -> fs.getType().equals(type)).collect(Collectors.toSet()));
    }

    @Test
    public void testFeatureSetProviderFilter() throws DataStoreException
    {
        var data = populateData(dataStore);
        var params = new FeatureDataStore2.FeatureSetQueryParameters();
        String provider = data.featureSetWrappers.get(0).getProvider();
        params.providers = Set.of(provider);
        assertThat(cursorToSet(dataStore.queryFeatureSets(params))).isEqualTo(data.featureSetWrappers.stream().filter(fs -> fs.getProvider().equals(provider)).collect(Collectors.toSet()));
    }

    @Test
    public void testFeatureSetResolutionFilter() throws DataStoreException
    {
        var data = populateData(dataStore);
        var setParams = new FeatureDataStore2.FeatureSetQueryParameters();
        for (FeatureSetWrapper setWrapper : data.featureSetWrappers)
        {
            setParams.minResolution = setWrapper.getMinResolution();
            setParams.maxResolution = setWrapper.getMaxResolution();
            var params = new FeatureDataStore2.FeatureQueryParameters();
            params.featureSetFilter = setParams;
            params.altitudeMode = null;
            Set<Long> matchingSetIds = data.featureSetWrappers.stream().filter(fsw -> setParams.minResolution >= fsw.getMaxResolution() && setParams.maxResolution <= fsw.getMinResolution()).map(FeatureSetWrapper::getId).collect(Collectors.toSet());
            Set<FeatureWrapper> matchingFeatures = data.featureWrappers.stream().filter(fw -> matchingSetIds.contains(fw.getFeatureSet().getId())).collect(Collectors.toSet());
            assertThat(cursorToSet(dataStore.queryFeatures(params))).isEqualTo(matchingFeatures);
        }
    }

    @Test
    public void testFeatureSetPaging() throws DataStoreException
    {
        var data = populateData(dataStore);
        var params = new FeatureDataStore2.FeatureSetQueryParameters();
        params.limit = 1;
        params.offset = 1;
        assertThat(dataStore.queryFeatureSetsCount(params)).isEqualTo(1);
        assertThat(cursorToList(dataStore.queryFeatureSets(params))).isEqualTo(data.featureSetWrappers.stream().skip(1).limit(1).toList());
    }

    @Test
    public void testFeatureInsert() throws DataStoreException
    {
        var data = populateData(dataStore);
        FeatureCursor fsc = dataStore.queryFeatures(null);
        assertThat(cursorToSet(fsc)).isEqualTo(new HashSet<>(data.featureWrappers));
    }

    @Test
    public void testFeatureCascadeDelete() throws DataStoreException
    {
        var data = populateData(dataStore);
        FeatureSetWrapper featureSet = data.featureSetWrappers.get(0);
        dataStore.deleteFeatureSet(featureSet.getId());
        assertThat(dataStore.queryFeaturesCount(null)).isEqualTo(data.featureWrappers.stream().filter(fw -> fw.getFeature().getFeatureSetId() != featureSet.getId()).toList().size());
    }

    @Test
    public void testFeatureNameFilter() throws DataStoreException
    {
        var data = populateData(dataStore);
        var params = new FeatureDataStore2.FeatureQueryParameters();
        params.altitudeMode = null;
        params.names = data.featureWrappers.stream().skip(1).limit(2).map(fw -> fw.getFeature().getName()).collect(Collectors.toSet());
        assertThat(cursorToSet(dataStore.queryFeatures(params))).isEqualTo(data.featureWrappers.stream().filter(fw -> params.names.contains(fw.getFeature().getName())).collect(Collectors.toSet()));
    }

    @Test
    public void testFeatureTimestampFilter() throws DataStoreException
    {
        var data = populateData(dataStore);
        var params = new FeatureDataStore2.FeatureQueryParameters();
        params.altitudeMode = null;
        params.minimumTimestamp = 10000L;
        params.maximumTimestamp = 20001L;
        assertThat(cursorToSet(dataStore.queryFeatures(params))).isEqualTo(data.featureWrappers.stream().filter(fw -> fw.getFeature().getTimestamp() <= params.maximumTimestamp && fw.getFeature().getTimestamp() >= params.minimumTimestamp).collect(Collectors.toSet()));
    }

    @Test
    @Disabled("In-memory data store does not implement altitudeMode filter")
    public void testFeatureAltitudeModeFilter() throws DataStoreException
    {
        var data = populateData(dataStore);
        var params = new FeatureDataStore2.FeatureQueryParameters();
        // test multiple altitude modes
        params.altitudeMode = Feature.AltitudeMode.Absolute;
        assertThat(cursorToSet(dataStore.queryFeatures(params))).isEqualTo(data.featureWrappers.stream().filter(fw -> fw.getFeature().getAltitudeMode() == Feature.AltitudeMode.Absolute).collect(Collectors.toSet()));
        params.altitudeMode = Feature.AltitudeMode.ClampToGround;
        assertThat(cursorToSet(dataStore.queryFeatures(params))).isEqualTo(data.featureWrappers.stream().filter(fw -> fw.getFeature().getAltitudeMode() == Feature.AltitudeMode.ClampToGround).collect(Collectors.toSet()));
    }

    @Test
    public void testFeatureVisibleFilter() throws DataStoreException
    {
        var data = populateData(dataStore);
        var params = new FeatureDataStore2.FeatureQueryParameters();
        params.visibleOnly = true;
        params.altitudeMode = null;
        FeatureWrapper invisibleFeature = data.featureWrappers.get(0);
        dataStore.setFeatureVisible(invisibleFeature.getId(), false);
        assertThat(dataStore.queryFeaturesCount(params)).isEqualTo(data.featureWrappers.size() - 1);
    }

    @Test
    public void testFeatureFeatureSetIdFilter() throws DataStoreException
    {
        var data = populateData(dataStore);
        var setParams = new FeatureDataStore2.FeatureSetQueryParameters();
        FeatureSetWrapper featureSetWrapper = data.featureSetWrappers.get(1);
        setParams.ids = Set.of(featureSetWrapper.getId());
        var featParams = new FeatureDataStore2.FeatureQueryParameters();
        featParams.featureSetFilter = setParams;
        featParams.altitudeMode = null;
        assertThat(cursorToSet(dataStore.queryFeatures(featParams))).isEqualTo(data.featureWrappers.stream().filter(fw -> featureSetWrapper.getId().equals(fw.getFeature().getFeatureSetId())).collect(Collectors.toSet()));
    }

    @Test
    public void testFeatureFeatureSetTypeFilter() throws DataStoreException
    {
        var data = populateData(dataStore);
        var setParams = new FeatureDataStore2.FeatureSetQueryParameters();
        String setType = data.featureSetWrappers.get(0).getType();
        setParams.types = Collections.singleton(setType);
        var featParams = new FeatureDataStore2.FeatureQueryParameters();
        featParams.featureSetFilter = setParams;
        featParams.altitudeMode = null;
        assertThat(cursorToSet(dataStore.queryFeatures(featParams))).isEqualTo(data.featureWrappers.stream().filter(fw -> fw.getFeatureSet().getType().equals(setType)).collect(Collectors.toSet()));
    }

    @Test
    public void testFeatureSpatialFilter() throws DataStoreException
    {
        var data = populateData(dataStore);
        var params = new FeatureDataStore2.FeatureQueryParameters();
        params.altitudeMode = null;
        for (FeatureWrapper wrapper : data.featureWrappers)
        {
            params.spatialFilter = wrapper.getFeature().getGeometry();
            Set<FeatureWrapper> actual = cursorToSet(dataStore.queryFeatures(params));
            Set<FeatureWrapper> expected = data.featureWrappers.stream().filter(fw -> {
                Envelope env1 = fw.getFeature().getGeometry().getEnvelope();
                Envelope env2 = wrapper.getFeature().getGeometry().getEnvelope();
                return Rectangle.intersects(env1.minX, env1.minY, env1.maxX, env1.maxY, env2.minX, env2.minY, env2.maxX, env2.maxY);
            }).collect(Collectors.toSet());
            assertThat(actual).isEqualTo(expected);
        }
    }

    @Test
    public void testFeatureOrderId() throws DataStoreException
    {
        var data = populateData(dataStore);
        var params = new FeatureDataStore2.FeatureQueryParameters();
        params.altitudeMode = null;
        params.order = List.of(new FeatureDataStore2.FeatureQueryParameters.Order.ID());
        assertThat(cursorToList(dataStore.queryFeatures(params))).isEqualTo(data.featureWrappers.stream().sorted(Comparator.comparing(FeatureWrapper::getId)).toList());
    }

    @Test
    public void testFeatureOrderName() throws DataStoreException
    {
        var data = populateData(dataStore);
        var params = new FeatureDataStore2.FeatureQueryParameters();
        params.altitudeMode = null;
        params.order = List.of(new FeatureDataStore2.FeatureQueryParameters.Order.Name());
        assertThat(cursorToList(dataStore.queryFeatures(params))).isEqualTo(data.featureWrappers.stream().sorted(Comparator.comparing(FeatureWrapper::getName)).toList());
    }

    @Test
    public void testFeatureOrderDistance() throws DataStoreException
    {
        var data = populateData(dataStore);
        var params = new FeatureDataStore2.FeatureQueryParameters();
        params.altitudeMode = null;
        for (FeatureWrapper feature : data.featureWrappers)
        {
            GeoPoint from = InMemoryFeatureDataStore.center(feature.getFeature().getGeometry());
            params.order = List.of(new FeatureDataStore2.FeatureQueryParameters.Order.Distance(from));
            assertThat(cursorToList(dataStore.queryFeatures(params))).isEqualTo(data.featureWrappers.stream().sorted(Comparator.comparingDouble(fw0 -> from.distanceTo(InMemoryFeatureDataStore.center(fw0.getFeature().getGeometry())))).toList());
        }
    }

    @Test
    public void testFeatureOrderDistanceNullGeometry() throws DataStoreException
    {
        var data = populateData(dataStore);
        FeatureWrapper nullGeoFeatureWrapper = new FeatureWrapper(data.featureSetWrappers.get(0), createFeatureDefinition("Feature Null Geo",
                null,
                null,
                Feature.AltitudeMode.Absolute,
                null,
                1L), 0L);
        insertFeature(dataStore, nullGeoFeatureWrapper);

        var params = new FeatureDataStore2.FeatureQueryParameters();
        params.altitudeMode = null;
        GeoPoint from = InMemoryFeatureDataStore.center(data.featureWrappers.get(0).getFeature().getGeometry());
        params.order = List.of(new FeatureDataStore2.FeatureQueryParameters.Order.Distance(from));

        // Confirm that nulls are first in return
        assertThat(cursorToList(dataStore.queryFeatures(params)).get(0)).isEqualTo(nullGeoFeatureWrapper);
    }

    @Test
    public void testFeaturePaging() throws DataStoreException
    {
        var data = populateData(dataStore);
        var params = new FeatureDataStore2.FeatureQueryParameters();
        params.limit = 3;
        params.offset = 2;
        params.altitudeMode = null;
        assertThat(dataStore.queryFeaturesCount(params)).isEqualTo(3);
        assertThat(cursorToList(dataStore.queryFeatures(params))).isEqualTo(data.featureWrappers.stream().skip(2).limit(3).toList());
    }

    @Test
    public void testFeatureDuplicateId() throws DataStoreException
    {
        var data = populateData(dataStore);
        var params = new FeatureDataStore2.FeatureQueryParameters();
        params.altitudeMode = null;
        params.ids = Set.of(data.featureWrappers.get(1).getId());
        FeatureWrapper existing = cursorToList(dataStore.queryFeatures(params)).get(0);
        assertThatThrownBy(() -> dataStore.insertFeature(existing.getFeature())).isInstanceOf(DataStoreException.class);
    }
}