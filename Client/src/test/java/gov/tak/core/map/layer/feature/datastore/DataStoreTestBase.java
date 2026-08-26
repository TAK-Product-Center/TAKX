package gov.tak.core.map.layer.feature.datastore;

import com.atakmap.database.RowIterator;
import com.atakmap.map.layer.feature.AttributeSet;
import com.atakmap.map.layer.feature.DataStoreException;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.FeatureCursor;
import com.atakmap.map.layer.feature.FeatureDataSource;
import com.atakmap.map.layer.feature.FeatureDataStore2;
import com.atakmap.map.layer.feature.FeatureDefinition;
import com.atakmap.map.layer.feature.FeatureDefinition3;
import com.atakmap.map.layer.feature.FeatureSet;
import com.atakmap.map.layer.feature.FeatureSetCursor;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.feature.geometry.LineString;
import com.atakmap.map.layer.feature.style.BasicStrokeStyle;
import com.atakmap.map.layer.feature.style.Style;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Base class to create and insert data for DataStore tests.
 *
 * @since 5.2
 */
public class DataStoreTestBase
{
    /**
     * Creates and inserts data into a data store.
     *
     * @param dataStore the data store
     * @return the data object
     */
    public TestData populateData(FeatureDataStore2 dataStore) throws DataStoreException
    {
        TestData data = createData(10, 25);
        insertData(dataStore, data);
        return data;
    }

    /**
     * Insert data into a data store.
     *
     * @param dataStore the data store
     * @param data the data object
     */
    public void insertData(FeatureDataStore2 dataStore, TestData data) throws DataStoreException
    {
        //insert featuresets
        for (FeatureSetWrapper wrapper : data.featureSetWrappers)
        {
            wrapper.id = dataStore.insertFeatureSet(wrapper.featureSet);
        }

        //insert features
        for (FeatureWrapper wrapper : data.featureWrappers)
        {
            insertFeature(dataStore, wrapper);
        }
    }

    /**
     * Inserts a {@link Feature} via a {@link FeatureWrapper}.
     * @param dataStore the data store
     * @param wrapper the data object
     */
    public void insertFeature(FeatureDataStore2 dataStore, FeatureWrapper wrapper) throws DataStoreException
    {
        wrapper.feature = createFeature(wrapper.featureDef, wrapper.featureSet.id, 1);
        wrapper.id = dataStore.insertFeature(wrapper.feature);
    }

    /**
     * Create data for tests.
     * Data can be altered by individual tests as necessary.
     *
     * @param featureSets number of feature sets to create
     * @param features number of features to create
     */
    public TestData createData(int featureSets, int features)
    {

        var data = new TestData();
        data.featureSetWrappers = new ArrayList<>();

        int numTypes = 5;
        int numProviders = 3;

        // creates feature sets of different types and providers
        for (int i = 1; i <= featureSets; i++)
        {
            FeatureSet featureSet = new FeatureSet(i, "Provider" + i % numProviders, "Type" + i % numTypes, "Set" + i, (i + 1) * 1000, (1 + i) * 17.561, 0);
            data.featureSetWrappers.add(new FeatureSetWrapper(featureSet, null));
        }

        // creates features for each feature set
        data.featureDefs = new ArrayList<>();
        for (int i = 1; i <= features; i++)
        {
            FeatureDataSource.FeatureDefinition def = createFeatureDefinition("Feature" + i,
                    createLineString(2,
                            new double[]{-2.49 * i, 60.50 / i},
                            new double[]{-2.50 * i, 60.51 / i},
                            new double[]{-2.51 * i, 60.52 / i}),
                    new BasicStrokeStyle(511 / i, 7.5f),
                    Feature.AltitudeMode.values()[i % Feature.AltitudeMode.values().length],
                    createAttributeSet(11, "something", new double[]{1.2, 7, 8, 4, 4.5}, createAttributeSet(1, "nest", null, null)),
                    10000L * i);
            data.featureDefs.add(def);
        }

        data.featureWrappers = new ArrayList<>();

        for (int i = 0; i < features; i++)
        {
            FeatureSetWrapper setWrapper = data.featureSetWrappers.get(i % data.featureSetWrappers.size());
            FeatureDataSource.FeatureDefinition def = data.featureDefs.get(i);
            data.featureWrappers.add(new FeatureWrapper(setWrapper, def, null));

        }

        return data;
    }

    /**
     * Simple compare of two Features for equality. Subclasses should override when necessary.
     * NOTES:<br>
     * Don't compare version because its very implementation specific
     */
    public boolean equals(FeatureWrapper actualWrapper, FeatureWrapper otherWrapper)
    {
        Feature other = otherWrapper.feature;
        Feature actual = actualWrapper.feature;
        return actual.getName().equals(other.getName()) &&
                actual.getExtrude() == other.getExtrude() &&
                actual.getAltitudeMode() == other.getAltitudeMode() &&
                actual.getTimestamp() == other.getTimestamp() &&
                Objects.equals(actual.getStyle(), other.getStyle()) &&
                Objects.equals(actual.getGeometry(), other.getGeometry()) &&
                Objects.equals(actual.getAttributes(), other.getAttributes());
    }

    /**
     * Simple compare of two FeatureSets for equality. Subclasses should override when necessary.
     * Version handling is very implementation specific, skipping it.
     */
    public boolean equals(FeatureSetWrapper featureSet1, FeatureSetWrapper featureSet2)
    {
        return featureSet1.getName().equals(featureSet2.getName()) &&
                featureSet1.getType().equals(featureSet2.getType()) &&
                featureSet1.getProvider().equals(featureSet2.getProvider()) &&
                featureSet1.getMinResolution() == featureSet2.getMinResolution() &&
                featureSet1.getMaxResolution() == featureSet2.getMaxResolution();
    }

    /**
     * Class to hold populated data for comparison.
     */
    public static class TestData
    {
        public List<FeatureDataSource.FeatureDefinition> featureDefs;
        public List<FeatureWrapper> featureWrappers;
        public List<FeatureSetWrapper> featureSetWrappers;
    }

    /**
     * Create a feature definition
     */
    public static FeatureDataSource.FeatureDefinition createFeatureDefinition(String name, Geometry geo, Style style, Feature.AltitudeMode altitudeMode, AttributeSet attributes, long timestamp)
    {
        FeatureDataSource.FeatureDefinition featureDef = new FeatureDataSource.FeatureDefinition();
        featureDef.name = name;
        featureDef.rawGeom = geo;
        featureDef.geomCoding = FeatureDefinition.GEOM_ATAK_GEOMETRY;
        featureDef.rawStyle = style;
        featureDef.styleCoding = FeatureDefinition.STYLE_ATAK_STYLE;
        featureDef.altitudeMode = altitudeMode;
        featureDef.attributes = attributes;
        featureDef.timestamp = timestamp;
        return featureDef;
    }

    /**
     * Create a set of attributes.
     */
    public static AttributeSet createAttributeSet(Integer intValue, String stringValue, double[] doubleArray, AttributeSet nested)
    {
        AttributeSet attributes = new AttributeSet();
        attributes.setAttribute("integerAtt", intValue);
        attributes.setAttribute("stringAtt", stringValue);
        attributes.setAttribute("doubleArrayAtt", doubleArray);
        attributes.setAttribute("nestedSet", nested);
        return attributes;
    }

    /**
     * We need this because the Feature class doesnt have a constructor that allows us to set the featuresetid
     */
    public static Feature createFeature(FeatureDefinition3 def, long featureSetId, long version)
    {
        return new Feature(
                featureSetId,
                FeatureDataStore2.FEATURE_ID_NONE,
                def.getName(),
                (Geometry) def.getRawGeometry(),
                (Style) def.getRawStyle(),
                def.getAttributes(),
                def.getAltitudeMode(),
                def.getExtrude(),
                def.getTimestamp(),
                version);
    }

    /**
     * Create a {@link LineString} geometry,
     */
    public static LineString createLineString(int dimension, double[]... points)
    {
        LineString ls = new LineString(dimension);
        for (double[] c : points)
        {
            if (dimension == 3)
            {
                ls.addPoint(c[0], c[1], c[2]);
            } else if (dimension == 2)
            {
                ls.addPoint(c[0], c[1]);
            }
        }
        return ls;
    }

    /**
     * Converts a {@link FeatureSetCursor} to a List.
     */
    public List<FeatureSetWrapper> cursorToList(FeatureSetCursor fsc)
    {
        return rowIteratorToList(fsc, fsc::get).stream().map(FeatureSetWrapper::new).toList();
    }

    /**
     * Converts a {@link FeatureSetCursor} to a Set. use when order does not matter.
     */
    public Set<FeatureSetWrapper> cursorToSet(FeatureSetCursor fsc)
    {
        return rowIteratorToSet(fsc, fsc::get).stream().map(FeatureSetWrapper::new).collect(Collectors.toSet());
    }

    /**
     * Converts a {@link FeatureCursor} to a Set. use when order does not matter.
     */
    public Set<FeatureWrapper> cursorToSet(FeatureCursor fsc)
    {
        return rowIteratorToSet(fsc, fsc::get).stream().map(FeatureWrapper::new).collect(Collectors.toSet());
    }

    /**
     * Converts a {@link FeatureCursor} to a List.
     */
    public List<FeatureWrapper> cursorToList(FeatureCursor fsc)
    {
        return rowIteratorToList(fsc, fsc::get).stream().map(FeatureWrapper::new).toList();
    }

    /**
     * Convert {@link RowIterator} to a Set.
     */
    public <T> Set<T> rowIteratorToSet(RowIterator iterator, Supplier<T> sup)
    {
        // This sorting ensures that tests are forced to handle/allow undefined orders
        Set<T> result = new TreeSet<>(Comparator.comparingInt(Objects::hashCode).reversed());
        while (iterator.moveToNext())
        {
            result.add(sup.get());
        }
        iterator.close();
        return result;
    }

    /**
     * Convert {@link RowIterator} to a List.
     */
    public <T> List<T> rowIteratorToList(RowIterator iterator, Supplier<T> sup)
    {
        List<T> result = new ArrayList<>();
        while (iterator.moveToNext())
        {
            result.add(sup.get());
        }
        iterator.close();
        return result;
    }

    /**
     * Class to wrap a FeatureSet so we can compare them.
     */
    public class FeatureSetWrapper
    {
        private final FeatureSet featureSet;
        private Long id;

        public FeatureSetWrapper(FeatureSet fs)
        {
            this(fs, fs.getId());
        }

        public FeatureSetWrapper(FeatureSet fs, Long id)
        {
            this.featureSet = fs;
            this.id = id;
        }

        @Override
        public int hashCode()
        {
            return 1;
        }

        @Override
        public boolean equals(Object obj)
        {
            return DataStoreTestBase.this.equals(this, (FeatureSetWrapper) obj);
        }

        @Override
        public String toString()
        {
            return "FeatureSetWrapper{" +
                    "id=" + id +
                    ", name=" + featureSet.getName() +
                    ", type=" + featureSet.getType() +
                    ", provider=" + featureSet.getProvider() +
                    ", version=" + featureSet.getVersion() +
                    ", minResolution=" + featureSet.getMinResolution() +
                    ", maxResolution=" + featureSet.getMaxResolution() +
                    '}';
        }

        public FeatureSet getFeatureSet()
        {
            return featureSet;
        }

        public Long getId()
        {
            return id;
        }

        public String getName()
        {
            return featureSet.getName();
        }

        public double getMinResolution()
        {
            return featureSet.getMinResolution();
        }

        public double getMaxResolution()
        {
            return featureSet.getMaxResolution();
        }

        public String getProvider()
        {
            return featureSet.getProvider();
        }

        public String getType()
        {
            return featureSet.getType();
        }

        public long getVersion()
        {
            return featureSet.getVersion();
        }
    }

    /**
     * Class to wrap Features so we can compare them.
     */
    public class FeatureWrapper
    {
        private FeatureSetWrapper featureSet;
        private Feature feature;
        private FeatureDataSource.FeatureDefinition featureDef;
        private Long id;

        public FeatureWrapper(Feature feature)
        {
            this(null, null, feature.getId());
            this.feature = feature;
        }

        public FeatureWrapper(FeatureSetWrapper set, FeatureDataSource.FeatureDefinition feature, Long id)
        {
            this.featureDef = feature;
            this.id = id;
            this.featureSet = set;
        }

        @Override
        public boolean equals(Object obj)
        {
            return DataStoreTestBase.this.equals(this, (FeatureWrapper) obj);
        }

        public long getId()
        {
            return id;
        }

        public String getName()
        {
            return feature.getName();
        }

        public FeatureSetWrapper getFeatureSet()
        {
            return featureSet;
        }

        public Feature getFeature()
        {
            return feature;
        }

        public FeatureDataSource.FeatureDefinition getFeatureDef()
        {
            return featureDef;
        }

        @Override
        public int hashCode()
        {
            return 1;
        }

        @Override
        public String toString()
        {
            return "FeatureWrapper{" +
                    "id=" + id +
                    ". setId=" + (featureSet != null ? featureSet.id : feature.getFeatureSetId()) +
                    ", name=" + feature.getName() +
                    ", timestamp=" + feature.getTimestamp() +
                    ", altitudeMode=" + feature.getAltitudeMode() +
                    ", timestamp=" + feature.getTimestamp() +
                    ", extrude=" + feature.getExtrude() +
                    ", setId=" + feature.getFeatureSetId() +
                    // NOTE style and geometry objects dont have nice string representations, which makes unit test failures difficult to interpret
                    ", style=" + feature.getStyle() +
                    ", geometry=" + feature.getGeometry() +
                    '}';
        }
    }
}