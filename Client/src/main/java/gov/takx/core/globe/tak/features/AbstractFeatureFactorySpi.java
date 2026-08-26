package gov.takx.core.globe.tak.features;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Supplier;

/**
 * Service provider interface for {@link IFeatureProvider}.
 *
 * @since 3.5
 */
public abstract class AbstractFeatureFactorySpi implements FeatureFactorySpi
{
    /**
     * Default priority for feature providers.
     */
    public static final int DEFAULT_PRIORITY = 0;
    /**
     * Given to feature providers that handle Renderable objects.
     */
    public static final int RENDERABLE_PRIORITY = 1;
    /**
     * Given to feature providers that handle IGlobeMapObject objects.
     */
    public static final int GLOBE_MAP_OBJECT_PRIORITY = 2;
    /**
     * Given to feature providers that require top priority.
     * @since 5.1
     */
    public static final int TOP_PRIORITY = 3;

    private final Class<?> managedType;
    private final Supplier<IFeatureProvider<?, ?>> featureSupplier;
    private final int priority;

    /**
     * @param priority Priority at which to register this SPI
     * @param managedType The type of source objects from which to create feature provider instances
     * @see #createFeatureProvider(Object)
     */
    protected AbstractFeatureFactorySpi(int priority, @Nonnull Class<?> managedType)
    {
        this(priority, managedType, null);
    }

    /**
     *
     * @param priority Priority at which to register this SPI
     * @param managedType The type of source objects from which to create feature provider instances
     * @param featureSupplier Supplier to invoke to obtain the feature provider
     */
    protected AbstractFeatureFactorySpi(int priority, @Nonnull Class<?> managedType, @Nullable Supplier<IFeatureProvider<?, ?>> featureSupplier)
    {
        this.priority = priority;
        this.managedType = managedType;
        this.featureSupplier = featureSupplier;
    }

    @Override
    public int getPriority()
    {
        return priority;
    }

    @Override
    public IFeatureProvider<?, ?> create(Object object)
    {
        return managedType.isInstance(object) ? createFeatureProvider(object) : null;
    }

    /**
     * Create the feature provider instance.  The given {@code object} will be an instance of the managed type
     * specified in the constructor for this SPI.  The default implementation uses the {@code featureSupplier}
     * provided at construction.
     *
     * @param object Source object from which to create the feature provider
     * @return feature provider or {@code null} if no feature(s) should be created for the given source object
     */
    @Nullable
    protected IFeatureProvider<?, ?> createFeatureProvider(@Nonnull Object object)
    {
        return featureSupplier.get();
    }
}
