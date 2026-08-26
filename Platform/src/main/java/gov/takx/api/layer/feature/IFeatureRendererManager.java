package gov.takx.api.layer.feature;

import com.atakmap.map.layer.feature.FeatureLayer3;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.globe.IGlobeDelegate;
import gov.takx.api.layer.pick.IPickedFeatureProvider;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Provides access to a shared feature renderer.
 *
 * @since 5.2 (moved to platform in 5.8)
 */
@InjectableService
public interface IFeatureRendererManager
{
    /**
     * Get the shared feature renderer for a specified {@link IGlobeDelegate globe} if it exists.
     * @param globe the application globe
     * @return the shared feature renderer, or null
     */
    @Nullable
    FeatureLayer3 getFeatureRenderer(@Nonnull IGlobeDelegate globe);

    /**
     * Adds an {@link IPickedFeatureProvider} to provide feature id to object mapping.
     * @param pickedObjectProvider the picked object provider
     */
    void addPickedFeatureProvider(@Nonnull IPickedFeatureProvider pickedObjectProvider);

    /**
     * @see IFeatureRendererManager#addPickedFeatureProvider(IPickedFeatureProvider)
     * @param pickedObjectProvider the picked object provider
     */
    void removePickedFeatureProvider(@Nonnull IPickedFeatureProvider pickedObjectProvider);
}
