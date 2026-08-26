package gov.takx.core.globe.tak.features;

import com.atakmap.spi.PriorityServiceProvider;

/**
 * Service provider interface for {@link IFeatureProvider}.
 *
 * @since 3.5
 */
public interface FeatureFactorySpi extends PriorityServiceProvider<IFeatureProvider<?, ?>, Object>
{
}
