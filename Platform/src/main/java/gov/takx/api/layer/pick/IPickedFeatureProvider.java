package gov.takx.api.layer.pick;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Provides a mapping from feature ids to user objects.
 * 
 * @since 5.2 (moved to platform in 5.8)
 */
public interface IPickedFeatureProvider
{
    /**
     * Returns the object that maps to the provided feature id.
     * @param context the pick context
     * @param fid the feature id
     * @return the user object if found, or {@code null} if none match
     */
    @Nullable
    Object getPickObject(@Nonnull IPickContext context, long fid);
}
