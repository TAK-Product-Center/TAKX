package gov.takx.core.globe.tak.features;

import com.atakmap.map.layer.feature.AttributeSet;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.feature.style.Style;
import jakarta.annotation.Nonnull;

/**
 * A {@link IFeatureProvider} that marshals an object and its state for submission to another IFeatureProvider.
 *
 * @param <T> the input object type
 * @param <TStateObject> the input object state type
 * @param <U> the marshaled object type
 * @param <UStateObject> the output object state type
 * @since 5.1
 */
public abstract class AMarshalingFeatureProvider<T, TStateObject, U, UStateObject> implements IFeatureProvider<T, TStateObject>
{
    private final IFeatureProvider<U, UStateObject> featureProvider;

    protected AMarshalingFeatureProvider(IFeatureProvider<U, UStateObject> featureProvider)
    {
        this.featureProvider = featureProvider;
    }

    /**
     * Marshal object from {@link T} to {@link U}
     * @param object the object to marshal
     * @return the marshaled object
     */
    abstract protected U marshalObject(T object);

    /**
     * Marshal object state from {@link TStateObject} to {@link UStateObject}
     * @param state the object state to marshal
     * @return the marshaled object state
     */
    abstract protected UStateObject marshalState(TStateObject state);

    @Override
    public int getFeatureCount(@Nonnull TStateObject state)
    {
        return featureProvider.getFeatureCount(marshalState(state));
    }

    @Override
    public boolean isVisible(@Nonnull TStateObject state)
    {
        return featureProvider.isVisible(marshalState(state));
    }

    @Nonnull
    @Override
    public TStateObject createState(T object)
    {
        return createState(object, featureProvider.createState(marshalObject(object)));
    }

    /**
     * Create state for object {@link T}. This typically includes the state for the marshaled object {@link U}.
     * @param object the original object
     * @param stateObject the state for the marshaled object
     * @return the state object
     */
    protected abstract TStateObject createState(T object, UStateObject stateObject);

    @Nonnull
    @Override
    public String[] createNames(@Nonnull TStateObject state)
    {
        return featureProvider.createNames(marshalState(state));
    }

    @Nonnull
    @Override
    public Geometry[] createGeometry(@Nonnull TStateObject state)
    {
        return featureProvider.createGeometry(marshalState(state));
    }

    @Nonnull
    @Override
    public Style[] createStyle(@Nonnull TStateObject state)
    {
        return featureProvider.createStyle(marshalState(state));
    }

    @Nonnull
    @Override
    public AttributeSet[] createAttributes(@Nonnull TStateObject state)
    {
        return featureProvider.createAttributes(marshalState(state));
    }

    @Nonnull
    @Override
    public Feature.AltitudeMode[] createAltitudeModes(@Nonnull TStateObject state)
    {
        return featureProvider.createAltitudeModes(marshalState(state));
    }

    @Nonnull
    @Override
    public double[] createExtrusions(@Nonnull TStateObject state)
    {
        return featureProvider.createExtrusions(marshalState(state));
    }

    @Nonnull
    @Override
    public Object[] createObjectKey(TStateObject state)
    {
        return featureProvider.createObjectKey(marshalState(state));
    }

    @Nonnull
    @Override
    public String[] createGroupingNames(TStateObject state)
    {
        return featureProvider.createGroupingNames(marshalState(state));
    }
}
