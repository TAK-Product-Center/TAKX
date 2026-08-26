package gov.tak.core.map.layer.feature.datastore.memory;

import com.atakmap.database.IteratorCursor;

import java.util.Collection;

/**
 * {@link com.atakmap.map.layer.feature.FeatureSetCursor} implementation for {@link InMemoryFeatureDataStore}.
 * This class is not thread-safe.
 *
 * @since 5.2
 */
class FeatureSetCursor extends IteratorCursor<FeatureSet> implements com.atakmap.map.layer.feature.FeatureSetCursor
{
    private com.atakmap.map.layer.feature.FeatureSet currentFeatureSet;

    /**
     * Constructs a new cursor with the given collection.
     *
     * @param collection the feature set collection
     */
    FeatureSetCursor(Collection<FeatureSet> collection)
    {
        super(collection.iterator());
    }

    @Override
    public com.atakmap.map.layer.feature.FeatureSet get()
    {
        if (currentFeatureSet == null) currentFeatureSet = getRowData().get();
        return currentFeatureSet;
    }

    @Override
    public long getId()
    {
        return getRowData().id();
    }

    @Override
    public String getType()
    {
        return getRowData().type();
    }

    @Override
    public String getProvider()
    {
        return getRowData().provider();
    }

    @Override
    public String getName()
    {
        return getRowData().name();
    }

    @Override
    public double getMinResolution()
    {
        return getRowData().minGsd();
    }

    @Override
    public double getMaxResolution()
    {
        return getRowData().maxGsd();
    }

    @Override
    public boolean moveToNext()
    {
        currentFeatureSet = null;
        return super.moveToNext();
    }
}
