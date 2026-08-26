package gov.tak.api.contact;

import gov.tak.api.annotation.IncubatingApi;
import jakarta.annotation.Nonnull;

/**
 * Contract for {@link IIndividualContactManager} listeners
 *
 * @since 5.1 (added to core in 3.4.1)
 */
@IncubatingApi(since = "5.1")
@SuppressWarnings("unused")
public interface IIndividualContactManagerListener
{
    /**
     * @param contact the contact that was added
     */
    default void contactAdded(@Nonnull IIndividualContact contact)
    {
    }

    /**
     * @param contact the contact that was removed
     */
    default void contactRemoved(@Nonnull IIndividualContact contact)
    {
    }

    /**
     * @param contact the contact that was updated (name, icon, etc.)
     */
    default void contactUpdated(@Nonnull IIndividualContact contact)
    {
    }

    /**
     * @param contact the contact that changed state
     * @see IndividualContactState
     * @see IndividualContactUtils#getState(IIndividualContact)
     */
    default void contactStateChanged(@Nonnull IIndividualContact contact)
    {
    }
}