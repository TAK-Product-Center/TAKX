package gov.tak.core.map.layer.feature.datastore.memory;

import com.atakmap.database.IteratorCursor;
import com.atakmap.map.layer.feature.AttributeSet;
import com.atakmap.map.layer.feature.Feature.AltitudeMode;
import com.atakmap.map.layer.feature.FeatureDefinition3;

import java.util.Collection;

/**
 * {@link com.atakmap.map.layer.feature.FeatureCursor} implementation for {@link InMemoryFeatureDataStore}.
 * This class is not thread-safe.
 *
 * @since 5.2
 */
class FeatureCursor extends IteratorCursor<Feature> implements com.atakmap.map.layer.feature.FeatureCursor, FeatureDefinition3
{
    private com.atakmap.map.layer.feature.Feature currentFeature;

    /**
     * Constructs a new cursor with the given {@link Feature} collection.
     *
     * @param collection the feature collection
     */
    FeatureCursor(Collection<Feature> collection)
    {
        super(collection.iterator());
    }

    @Override
    public Object getRawGeometry()
    {
        return getRowData().geometry();
    }

    @Override
    public int getGeomCoding()
    {
        return getRowData().getGeomCoding();
    }

    @Override
    public String getName()
    {
        return getRowData().name();
    }

    @Override
    public int getStyleCoding()
    {
        return getRowData().getStyleCoding();
    }

    @Override
    public Object getRawStyle()
    {
        return getRowData().style();
    }

    @Override
    public AttributeSet getAttributes()
    {
        return getRowData().attributes();
    }

    @Override
    public com.atakmap.map.layer.feature.Feature get()
    {
        if (currentFeature == null) currentFeature = getRowData().get();
        return currentFeature;
    }

    @Override
    public long getId()
    {
        return getRowData().id();
    }

    @Override
    public long getVersion()
    {
        return getRowData().version();
    }

    @Override
    public long getFsid()
    {
        return getRowData().featureSetId();
    }

    @Override
    public AltitudeMode getAltitudeMode()
    {
        return getRowData().altitudeMode();
    }

    @Override
    public double getExtrude()
    {
        return getRowData().extrude();
    }

    @Override
    public long getTimestamp()
    {
        return getRowData().timestamp();
    }

    @Override
    public boolean moveToNext()
    {
        currentFeature = null;
        return super.moveToNext();
    }
}