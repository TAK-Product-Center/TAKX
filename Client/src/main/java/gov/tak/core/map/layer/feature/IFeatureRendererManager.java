package gov.tak.core.map.layer.feature;

import com.atakmap.map.layer.feature.FeatureLayer3;
import gov.takx.api.globe.IApplicationGlobe;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Provides access to a shared feature renderer.
 *
 * @since 5.2
 */
public interface IFeatureRendererManager
{
    /**
     * Get the shared feature renderer for a specified {@link IApplicationGlobe globe} if it exists.
     * @param globe the application globe
     * @return the shared feature renderer, or null
     */
    @Nullable
    FeatureLayer3 getFeatureRenderer(@Nonnull IApplicationGlobe globe);

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
