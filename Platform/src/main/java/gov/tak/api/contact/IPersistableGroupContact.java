package gov.tak.api.contact;

import gov.tak.api.annotation.NonNull;
import gov.takx.api.persist.IPersistable;
import jakarta.annotation.Nullable;

/**
 * Interface adding {@link IPersistable} to {@link IGroupContact}, and needed mutator methods.
 *
 * @since 3.2
 */
public interface IPersistableGroupContact extends IGroupContact, IPersistable
{
    /**
     * @return Unique Id of parent contact, or {@code null} if none
     */
    @Nullable
    String getParentUid();

    /**
     * Set the display name of this group contact.
     * @since 3.4 (renamed from setDisplayName)
     */
    void setName(@NonNull String name);
}
