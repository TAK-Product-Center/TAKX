package gov.tak.core.map.layer.feature.datastore.memory;

import com.atakmap.map.EngineLibrary;
import com.atakmap.map.layer.feature.DataStoreException;
import com.atakmap.map.layer.feature.FeatureDataStore2;
import com.atakmap.map.layer.feature.FeatureDataStore3;
import gov.tak.core.map.layer.feature.datastore.DataStoreTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InMemoryFeatureDataStore}.
 * TODO TAKX-2678 immutability tests for the in-memory implementation
 *
 * @since 5.2
 */
public class InMemoryFeatureDataStoreTest extends DataStoreTest
{
    @Override
    public FeatureDataStore3 createDataStore()
    {
        EngineLibrary.initialize();
        return new InMemoryFeatureDataStore();
    }

    @Test
    public void testFeatureIdCounter() throws DataStoreException
    {
        TestData ignored = populateData(dataStore);
        var params = new FeatureDataStore2.FeatureQueryParameters();
        params.altitudeMode = null;
        params.order = List.of(new FeatureDataStore2.FeatureQueryParameters.Order.ID());
        List<FeatureWrapper> features = cursorToList(dataStore.queryFeatures(params));
        assertThat(features.size()).isEqualTo(features.get(features.size() - 1).getId());
    }
}