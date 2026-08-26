package gov.tak.core.map.layer.feature.datastore;

import com.atakmap.map.layer.feature.DataStoreException;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.FeatureDataStore2;
import com.atakmap.map.layer.feature.FeatureDataStore3;
import com.atakmap.map.layer.feature.datastore.FeatureSetDatabase2;
import com.atakmap.map.layer.raster.osm.OSMUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Tests for {@link FeatureSetDatabase2}.
 *
 * @since 5.2
 */
public class FeatureSetDatabase2Test extends DataStoreTest
{
    @Override
    public FeatureDataStore3 createDataStore()
    {
        return new FeatureSetDatabase2(null);
    }

    @Override
    @Disabled("FDB2 does not implement paging for FeatureSets")
    public void testFeatureSetPaging() throws DataStoreException
    {
        super.testFeatureSetPaging();
    }

    @Override
    @Disabled("FDB2 Does not implement altitudeMode filter")
    public void testFeatureAltitudeModeFilter() throws DataStoreException
    {
        super.testFeatureAltitudeModeFilter();
    }

    @Override
    @Disabled("Timestamps are not filtered in FDB2")
    public void testFeatureTimestampFilter() throws DataStoreException
    {
        super.testFeatureTimestampFilter();
    }

    @Override
    @Disabled("Distance ordering algorithm is not well-understood")
    public void testFeatureOrderDistance() throws DataStoreException
    {
        super.testFeatureOrderDistance();
    }

    @Test
    @Override
    public void testFeatureDuplicateId() throws DataStoreException
    {
        // Overriding because this datastore throws a RuntimeException
        var data = populateData(dataStore);
        var params = new FeatureDataStore2.FeatureQueryParameters();
        params.ids = Set.of(data.featureWrappers.get(1).getId());
        FeatureWrapper existing = cursorToList(dataStore.queryFeatures(params)).get(0);
        // Not sure if this is the correct meaning of this exception but leaving here for now
        assertThatThrownBy(() -> dataStore.insertFeature(existing.getFeature())).isInstanceOf(RuntimeException.class);
    }

    @Test
    public void testFeatureSetDefaultOrderName() throws DataStoreException
    {
        // This datastore orders FeatureSets by Name by default
        var data = populateData(dataStore);
        List<FeatureSetWrapper> sorted = data.featureSetWrappers.stream().sorted((w0, w1) -> {
            return w0.getName().compareToIgnoreCase(w1.getName());
        }).toList();
        assertThat(cursorToList(dataStore.queryFeatureSets(null))).isEqualTo(sorted);
    }

    @Override
    public boolean equals(FeatureWrapper actualWrapper, FeatureWrapper otherWrapper)
    {
        Feature other = otherWrapper.getFeature();
        Feature actual = actualWrapper.getFeature();
        // NOTE cant compare timestamps for FDB2
        return actual.getName().equals(other.getName()) &&
                actual.getExtrude() == other.getExtrude() &&
                actual.getAltitudeMode() == other.getAltitudeMode() &&
                actual.getVersion() == other.getVersion() &&
                Objects.equals(actual.getStyle(), other.getStyle()) &&
                Objects.equals(actual.getGeometry(), other.getGeometry()) &&
                Objects.equals(actual.getAttributes(), other.getAttributes());
    }

    @Override
    public boolean equals(FeatureSetWrapper featureSet1, FeatureSetWrapper featureSet2)
    {
        // NOTE have to convert GSD to LOD and back
        return featureSet1.getName().equals(featureSet2.getName()) &&
                featureSet1.getType().equals(featureSet2.getType()) &&
                featureSet1.getProvider().equals(featureSet2.getProvider()) &&
                OSMUtils.mapnikTileResolution(OSMUtils.mapnikTileLevel(featureSet1.getMinResolution())) == OSMUtils.mapnikTileResolution(OSMUtils.mapnikTileLevel(featureSet2.getMinResolution())) &&
                OSMUtils.mapnikTileResolution(OSMUtils.mapnikTileLevel(featureSet1.getMaxResolution())) == OSMUtils.mapnikTileResolution(OSMUtils.mapnikTileLevel(featureSet2.getMaxResolution()));
    }
}
