package gov.tak.core.map.layer.feature;

import gov.tak.core.map.layer.IPickContext;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Provides a mapping from feature ids to user objects.
 * 
 * @since 5.2
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
