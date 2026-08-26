package gov.tak.core.map.layer;

import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Interface for classes that can provide objects in response to a pick.
 *
 * @since 5.2
 */
public interface IPickedObjectProvider
{
    /**
     * Produce a list of objects for the provided {@link IPickContext}.
     *
     * @param context the pick context
     * @return the {@link PickedObject} collection of objects from this provider
     */
    @Nonnull
    List<PickedObject> pick(@Nonnull IPickContext context);
}
