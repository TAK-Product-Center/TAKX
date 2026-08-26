package gov.tak.api.contact;

import gov.tak.api.annotation.IncubatingApi;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Set;

/**
 * Contract for the {@link IIndividualContact} manager.
 *
 * @since 5.1
 * @see IIndividualContact
 */
@IncubatingApi(since = "5.1")
@InjectableService
@NoPluginImplementations
public interface IIndividualContactManager
{
    /**
     * @param listener The listener to add
     */
    void addListener(@Nonnull IIndividualContactManagerListener listener);

    /**
     * @param listener The listener to remove
     */
    void removeListener(@Nonnull IIndividualContactManagerListener listener);

    /**
     * @return All tracked {@link IIndividualContact}s
     */
    @Nonnull
    Set<IIndividualContact> getContacts();

    /**
     * @param contactUid The contact's UID
     * @return The {@link IIndividualContact}, or {@code null} if no contact is associated with the UID
     */
    @Nullable
    IIndividualContact getContact(@Nonnull String contactUid);
}
