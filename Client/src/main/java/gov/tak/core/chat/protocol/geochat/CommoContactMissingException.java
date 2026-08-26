package gov.tak.core.chat.protocol.geochat;

import com.atakmap.commoncommo.Commo;
import com.atakmap.commoncommo.CommoContactGoneException;
import gov.tak.api.contact.IContact;
import jakarta.annotation.Nonnull;

/**
 * Thrown when a {@link Commo} contact cannot be found via {@link Commo#getContacts()}.
 * <p>
 * TODO: This class is effectively duplicating {@link CommoContactGoneException}, but we don't have access to instantiate
 *       and throw that ourselves. We throw it to prevent unnecessary work from being done when a Contact does not
 *       exist in Commo, though ideally Commo would expose a getContact() method that allows for both querying and
 *       checking for existence of a given contact based on UID.
 *
 * @since 3.2
 */
class CommoContactMissingException extends Exception
{
    private final IContact missingContact;

    /**
     * @param missingContact The missing contact that caused this exception to be thrown
     */
    CommoContactMissingException(@Nonnull IContact missingContact)
    {
        this.missingContact = missingContact;
    }

    @Override
    public String getMessage()
    {
        return "Attempted to get Commo contact that does not exist: " + "UID=" + missingContact.getUid()
                + ", Display Name=" + missingContact.getName();
    }
}
