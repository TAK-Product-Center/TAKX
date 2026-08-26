package gov.tak.core.map.layer.feature.datastore.memory;

import com.atakmap.map.EngineLibrary;
import com.atakmap.map.layer.feature.DataStoreException;
import com.atakmap.map.layer.feature.FeatureCursor;
import com.atakmap.map.layer.feature.FeatureDataStore3;
import gov.tak.core.map.layer.feature.datastore.DataStoreTestBase;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Disabled
public class InMemoryDataStorePerformanceTest extends DataStoreTestBase
{
    static FeatureDataStore3 dataStore;
    static TestData data;

    @Test
    @Order(0)
    @Timeout(3)
    public void testConstruct()
    {
        EngineLibrary.initialize();
        dataStore = new InMemoryFeatureDataStore();
    }

    @Test
    @Order(1)
    public void testCreateData()
    {
        data = createData(10, 500000);
    }

    @Test
    @Order(2)
    @Timeout(6)
    public void testInsert() throws DataStoreException
    {
        insertData(dataStore, data);
    }

    @Test
    @Order(3)
    @Timeout(2)
    public void testQueryFeatures() throws DataStoreException
    {
        FeatureCursor cursor = dataStore.queryFeatures(null);
        while (cursor.moveToNext())
        {
            cursor.get();
        }
    }
}