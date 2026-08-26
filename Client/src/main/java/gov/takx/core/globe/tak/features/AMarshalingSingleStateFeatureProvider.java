package gov.takx.core.globe.tak.features;

/**
 * A {@link IFeatureProvider} that marshals an object for submission to another IFeatureProvider that uses the same {@link StateObject}.
 *
 * @param <T> the input object type
 * @param <U> the marhsaled object type
 * @param <StateObject> the state type
 * @since 5.1
 */

public abstract class AMarshalingSingleStateFeatureProvider<T, U, StateObject> extends AMarshalingFeatureProvider<T, StateObject, U, StateObject>
{
    protected AMarshalingSingleStateFeatureProvider(IFeatureProvider<U, StateObject> featureProvider)
    {
        super(featureProvider);
    }

    @Override
    protected StateObject marshalState(StateObject state)
    {
        return state;
    }

    @Override
    protected StateObject createState(T object, StateObject stateObject)
    {
        return stateObject;
    }
}
