package gov.tak.core.map.layer.feature.datastore;

import com.atakmap.map.layer.feature.DataStoreException;
import com.atakmap.map.layer.feature.FeatureCursor;
import com.atakmap.map.layer.feature.FeatureDataStore3;
import com.atakmap.map.layer.feature.datastore.FeatureSetDatabase2;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Disabled
public class FeatureSetDatabase2PerformanceTest extends DataStoreTestBase
{
    static FeatureDataStore3 dataStore;
    static TestData data;

    @Test
    @Order(0)
    public void testConstruct()
    {
        dataStore = new FeatureSetDatabase2(null);
    }

    @Test
    @Order(1)
    public void testCreateData()
    {
        data = createData(10, 200000);
    }

    @Test
    @Order(2)
    @Timeout(30)
    public void testInsert() throws DataStoreException
    {
        insertData(dataStore, data);
    }

    @Test
    @Order(3)
    @Timeout(3)
    public void testQueryFeatures() throws DataStoreException
    {
        FeatureCursor cursor = dataStore.queryFeatures(null);
        while (cursor.moveToNext())
        {
            cursor.get();
        }
    }
}