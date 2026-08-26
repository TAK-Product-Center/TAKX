package gov.tak.api.contact;

import gov.tak.api.annotation.IncubatingApi;
import jakarta.annotation.Nonnull;

/**
 * Contract for {@link IGroupContactManager} listeners.
 *
 * @since 3.5
 */
@IncubatingApi(since = "3.5")
public interface IGroupContactManagerListener
{
    /**
     * @param contact The contact that was added
     */
    default void contactAdded(@Nonnull IGroupContact contact)
    {
    }

    /**
     * @param contact The contact that was updated
     */
    default void contactUpdated(@Nonnull IGroupContact contact)
    {
    }

    /**
     * @param contact The contact that was removed
     */
    default void contactRemoved(@Nonnull IGroupContact contact)
    {
    }
}