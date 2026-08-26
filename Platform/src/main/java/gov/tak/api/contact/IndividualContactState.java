package gov.tak.api.contact;

/**
 * Defines a contact's activity states
 *
 * @since 5.1
 */
public enum IndividualContactState
{
    /**
     * Indicates that a contact is active, e.g. contact messages have been received recently. The contact will be
     * displayed in the Contacts panel with an active state icon.
     */
    active,
    /**
     * Indicates that a contact is inactive, e.g. if no contact messages have been received for some time. The
     * contact will be displayed in the Contacts panel with an inactive state icon.
     */
    inactive,
    /**
     * Indicates that a contact is considered gone even if no deletion message has been received, e.g. if no contact
     * messages have been received for a long time. The contact will not be displayed in the Contacts panel while
     * in this state but will reappear if the state is changed to {@link #active}.
     */
    gone
}
