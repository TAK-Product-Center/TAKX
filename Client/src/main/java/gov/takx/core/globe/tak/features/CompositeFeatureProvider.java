package gov.takx.core.globe.tak.features;

import android.util.Pair;
import com.atakmap.map.layer.feature.AttributeSet;
import com.atakmap.map.layer.feature.Feature.AltitudeMode;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.feature.style.Style;
import jakarta.annotation.Nonnull;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * A {@link IFeatureProvider} that composites multiple providers to a single result.
 *
 * @since 5.0
 */
public class CompositeFeatureProvider<T> implements IFeatureProvider<T, Object[]>
{
    protected IFeatureProvider<T, ?>[] featureProviders;

    @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
    @SafeVarargs
    public CompositeFeatureProvider(IFeatureProvider<T, ?>... featureProviders)
    {
        this.featureProviders = featureProviders;
    }

    @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
    protected IFeatureProvider<T, ?>[] getFeatureProviders()
    {
        return featureProviders;
    }

    @Nonnull
    @Override
    public Object[] createState(T object)
    {
        return Arrays.stream(getFeatureProviders()).map(p -> p.createState(object)).toArray(Object[]::new);
    }

    @Override
    public int getFeatureCount(@Nonnull Object[] state)
    {
        return pair(state)
                .mapToInt(p -> p.first.getFeatureCount(p.second))
                .sum();
    }

    @Override
    public boolean isVisible(@Nonnull Object[] state)
    {
        return pair(state)
                .anyMatch(p -> p.first.isVisible(p.second));
    }

    @Nonnull
    @Override
    public String[] createNames(@Nonnull Object[] state)
    {
        return createArrayStream(String[].class, p -> p.first.createNames(p.second), state);
    }

    @Nonnull
    @Override
    public Geometry[] createGeometry(@Nonnull Object[] state)
    {
        return createArrayStream(Geometry[].class, p -> p.first.createGeometry(p.second), state);
    }

    @Nonnull
    @Override
    public Style[] createStyle(@Nonnull Object[] state)
    {
        return createArrayStream(Style[].class, p -> p.first.createStyle(p.second), state);
    }

    @Nonnull
    @Override
    public AttributeSet[] createAttributes(@Nonnull Object[] state)
    {
        return createArrayStream(AttributeSet[].class, p -> p.first.createAttributes(p.second), state);
    }

    @Nonnull
    @Override
    public AltitudeMode[] createAltitudeModes(@Nonnull Object[] state)
    {
        return createArrayStream(AltitudeMode[].class, p -> p.first.createAltitudeModes(p.second), state);
    }

    @Nonnull
    @Override
    public double[] createExtrusions(@Nonnull Object[] state)
    {
        return createDoubleArrayStream(r -> r.first.createExtrusions(r.second), state);
    }

    @Nonnull
    @Override
    public String[] createGroupingNames(Object[] state)
    {
        return createArrayStream(String[].class, p -> p.first.createGroupingNames(p.second), state);
    }

    private <U> U[] createArrayStream(Class<U[]> clazz, Function<Pair<IFeatureProvider<T, Object>, Object>, U[]> func, Object[] state)
    {
        return pair(state).map(func).flatMap(Arrays::stream)
                .toArray(a -> clazz.cast(Array.newInstance(clazz.getComponentType(), a)));
    }

    private double[] createDoubleArrayStream(Function<Pair<IFeatureProvider<T, Object>, Object>, double[]> func, Object[] state)
    {
        return pair(state).map(func).flatMapToDouble(Arrays::stream).toArray();
    }

    /**
     * Create a stream of pairs of {@link IFeatureProvider} and the provided state objects.
     * @param state the state objects
     * @return the stream of pairs
     */
    private Stream<Pair<IFeatureProvider<T, Object>, Object>> pair(Object[] state)
    {
        //noinspection unchecked
        return IntStream.range(0, state.length)
                .mapToObj(i -> Pair.create((IFeatureProvider<T, Object>) featureProviders[i], state[i]));
    }
}
