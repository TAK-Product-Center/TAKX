package gov.takx.core.globe.tak.features;

import com.atakmap.map.layer.feature.AttributeSet;
import com.atakmap.map.layer.feature.Feature.AltitudeMode;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.feature.style.Style;
import gov.takx.api.layer.feature.ISharedFeatureDataStore;
import jakarta.annotation.Nonnull;

import java.util.Arrays;

/**
 * Produces TAK features from extracted properties of {@link gov.takx.api.globe.IGlobeMapObject} and legacy renderable objects.
 * Implementations must be thread-safe.
 * @param <T> the object type served by this provider
 * @param <StateObject> the state object type produced by this provider
 * @since 3.5
 */
public interface IFeatureProvider<T, StateObject>
{
    Geometry[] EMPTY_GEOMETRY = {null};

    /**
     * Create a state object that represents the current state of the provided object.
     * @param object the object
     * @return a state object
     */
    @Nonnull
    StateObject createState(T object);

    /**
     * Get the number of features an object state will produce. All array-based results will be of this length.
     *
     * @param state the object state
     * @return the number of features
     */
    int getFeatureCount(@Nonnull StateObject state);

    /**
     * Get whether the feature set is visible.
     * @param state the object state
     * @return {@code true} if the feature set is visible
     */
    boolean isVisible(@Nonnull StateObject state);

    /**
     * Get an array of feature name results for the provided object state.
     *
     * @param state the object state
     * @return an array of name results
     */
    @Nonnull
    default String[] createNames(@Nonnull StateObject state)
    {
        final String[] names = new String[getFeatureCount(state)];
        Arrays.fill(names, state.getClass().getSimpleName());
        return names;
    }

    /**
     * Get an array of {@link Geometry} results for the provided object state.
     * The individual elements of the array may be null.
     *
     * @param state the object state
     * @return an array of Geometry results
     */
    @Nonnull
    Geometry[] createGeometry(@Nonnull StateObject state);

    /**
     * Get an array of {@link Style} results for the provided object state.
     * The individual elements of the array may be null.
     *
     * @param state the object state
     * @return an array of Style results
     */
    @Nonnull
    Style[] createStyle(@Nonnull StateObject state);

    /**
     * Get an array of {@link AttributeSet} results for the provided object state.
     *
     * @param state the object state
     * @return an array of AttributeSet results
     */
    @Nonnull
    default AttributeSet[] createAttributes(@Nonnull StateObject state)
    {
        int featureCount = getFeatureCount(state);
        AttributeSet[] attributeSets = new AttributeSet[featureCount];
        Arrays.setAll(attributeSets, i -> new AttributeSet());
        return attributeSets;
    }

    /**
     * Get an array of {@link AltitudeMode} results for the provided object state.
     *
     * @param state the object state
     * @return an array of AltitudeMode results
     */
    @Nonnull
    AltitudeMode[] createAltitudeModes(@Nonnull StateObject state);

    /**
     * Get an array of extrusion results for the provided object state.
     *
     * @param state the object state
     * @return an array of extrusion results
     */
    @Nonnull
    default double[] createExtrusions(@Nonnull StateObject state)
    {
        int featureCount = getFeatureCount(state);
        double[] extrude = new double[featureCount];
        Arrays.fill(extrude, 0.0);
        return extrude;
    }

    /**
     * Get an array of unique Objects that identify each of the individual features produced by this object.
     * Each object in this array must be !o.equals() to the other elements.  This is used when performing
     * updates on features to ensure that each update goes to the proper feature. It is sufficient to supply
     * a simple indexed integer for each feature if each generated feature appears in the same position in
     * the results every time.
     *
     * @param state the object state
     * @return an array of unique Objects
     */
    @Nonnull
    default Object[] createObjectKey(StateObject state)
    {
        int featureCount = getFeatureCount(state);
        String[] groupingNames = createGroupingNames(state);
        Object[] objectKeys = new Object[featureCount];
        for (int i = 0; i < featureCount; i++) objectKeys[i] = groupingNames[i] + i;
        return objectKeys;
    }

    /**
     * Get an array of grouping names that identify which group each of the individual features produced by this object
     * belongs to. Grouping names can be used to organize features into feature sets.
     * @param state the object state
     * @return an array of grouping names
     * @since 5.3
     */
    @Nonnull
    default String[] createGroupingNames(StateObject state)
    {
        int featureCount = getFeatureCount(state);
        String[] groupingNames = new String[featureCount];
        Arrays.fill(groupingNames, ISharedFeatureDataStore.ALL_GLOBES_FEATURE_SET_TYPE);
        return groupingNames;
    }
}
