package gov.tak.api.contact;

import gov.takx.commons.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * Provides utility functions that simplify accessing and modifying {@link IIndividualContact}s.
 *
 * @since 5.1
 */
public class IndividualContactUtils
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Returns a contact's manual state management status. If {@code true}, the contact's state will <em>not</em> be
     * managed by the core. This value is typically only used by {@link gov.tak.api.chat.protocol.IChatProtocolHandler}s
     * that create custom {@link IIndividualContact}s.
     *
     * @param contact The contact to access
     * @return {@code true} if the contact has been marked for manual state management
     */
    public static boolean isStateManagedManually(@Nonnull IIndividualContact contact)
    {
        return contact.getAttributes().getBooleanAttribute(IndividualContactAttributeNames.STATE_MANAGED_MANUALLY, false);
    }

    /**
     * Sets a contact's manual state management status. If {@code true}, the contact's state will <em>not</em> be
     * managed by the core. This value is typically only used by {@link gov.tak.api.chat.protocol.IChatProtocolHandler}s
     * that create custom {@link IIndividualContact}s.
     *
     * @param contact The contact to update
     * @param manualStatus {@code true} to indicate that the contact's state will be managed manually
     */
    public static void setStateManagedManually(@Nonnull IIndividualContact contact, boolean manualStatus)
    {
        contact.getAttributes().setAttribute(IndividualContactAttributeNames.STATE_MANAGED_MANUALLY, manualStatus);
    }

    /**
     * Returns the most recent time a message was received from a contact, in milliseconds. A value of -1 indicates that
     * the value is unset.
     *
     * @param contact The contact to access
     * @return the most recent time a message was received from a contact, in milliseconds, or -1 if the value has not
     * been set
     */
    public static long getMostRecentMessageTime(@Nonnull IIndividualContact contact)
    {
        return contact.getAttributes().getLongAttribute(IndividualContactAttributeNames.MOST_RECENT_MESSAGE_TIME, -1);
    }

    /**
     * Sets the most recent time a message was received from a contact, in milliseconds. A value of -1 indicates that
     * the value is unset. This is typically only set by the core.
     *
     * @param contact The contact to update
     * @param timeInMs The most recent message time, in milliseconds. Any value less than 0 will be converted to -1 to
     *                 indicate that the value is unset
     */
    public static void setMostRecentMessageTime(@Nonnull IIndividualContact contact, long timeInMs)
    {
        contact.getAttributes().setAttribute(IndividualContactAttributeNames.MOST_RECENT_MESSAGE_TIME, timeInMs >= 0 ? timeInMs : -1);
    }

    /**
     * Returns the contact's role. The role will affect how the contact is rendered in the UI and may cause the contact
     * to be automatically added to a role-based group.
     *
     * @param contact The contact to access
     * @return The contact's assigned role or {@code null} if the role has not been set
     */
    @Nullable
    public static String getRole(@Nonnull IIndividualContact contact)
    {
        final String role = contact.getAttributes().getStringAttribute(IndividualContactAttributeNames.ROLE, "");

        // AttributeSet does not allow null default values
        return role.isEmpty() ? null : role;
    }

    /**
     * Sets the contact's role. The role will affect how the contact is rendered in the UI and may cause the contact
     * to be automatically added to a role-based group.
     *
     * @param contact The contact to update
     * @param role The role to assign to the contact. An empty String can be used to clear the value and indicate that
     *             the role is unset.
     */
    public static void setRole(@Nonnull IIndividualContact contact, @Nonnull String role)
    {
        contact.getAttributes().setAttribute(IndividualContactAttributeNames.ROLE, role);
    }

    /**
     * Returns a contact's stale time. When the stale time is reached, the contact is considered inactive. If enough
     * time passes beyond the stale time, the contact will be considered gone.
     *
     * @param contact The contact to access
     * @return The stale time, in milliseconds, or -1 if the value is unset
     * @see IndividualContactAttributeNames#STALE_TIME
     * @see IndividualContactState
     */
    public static long getStaleTime(@Nonnull IIndividualContact contact)
    {
        return contact.getAttributes().getLongAttribute(IndividualContactAttributeNames.STALE_TIME, -1);
    }

    /**
     * Sets a contact's stale time that helps determine the contact's state. When the stale time is reached, the contact
     * is considered inactive. If enough time passes beyond the stale time, the contact will be considered gone.
     *
     * @param contact The contact to update
     * @param timeInMs The stale time, in milliseconds. Any value less than 0 will be converted to -1 to indicate that
     *                 the value is unset
     * @see IndividualContactAttributeNames#STALE_TIME
     * @see IndividualContactState
     */
    public static void setStaleTime(@Nonnull IIndividualContact contact, long timeInMs)
    {
        contact.getAttributes().setAttribute(IndividualContactAttributeNames.STALE_TIME, timeInMs >= 0 ? timeInMs : -1);
    }

    /**
     * Returns a contact's current state. The state will be used to determine how the contact should be displayed in the
     * UI or if the contact should be removed.
     *
     * @param contact the contact to access
     * @return The contact's state or {@link IndividualContactState#inactive} if the state could not be determined
     */
    public static IndividualContactState getState(@Nonnull IIndividualContact contact)
    {
        final String value = contact.getAttributes().getStringAttribute(IndividualContactAttributeNames.STATE, "");

        if (!StringUtils.isEmpty(value))
        {
            try
            {
                return IndividualContactState.valueOf(value);
            } catch (IllegalArgumentException e)
            {
                // warn is verbose, but this will only happen if an invalid state value is set directly on the AttributeSet
                logger.warn("No matching state for stored value {}. Setting state to inactive", value);

                setState(contact, IndividualContactState.inactive);
            }
        }

        return IndividualContactState.inactive;
    }

    /**
     * Sets a contact's current state. The state will be used to determine how the contact should be displayed in the
     * UI or if the contact should be removed.
     *
     * @param contact the contact to update
     * @param state the new state
     * @return {@code true} if the state changed, {@code false} if the old state already matches the new state
     */
    public static boolean setState(@Nonnull IIndividualContact contact, @Nonnull IndividualContactState state)
    {
        final String stateString = contact.getAttributes().getStringAttribute(IndividualContactAttributeNames.STATE, "");

        // Check to see if the current state already matches the new state
        if (state.toString().equals(stateString)) return false;

        contact.getAttributes().setAttribute(IndividualContactAttributeNames.STATE, state.toString());
        return true;
    }

    /**
     * Returns the contact's team. The team will affect how the contact is rendered in the UI and may cause the contact
     * to be automatically added to a team-based group.
     *
     * @param contact The contact to access
     * @return The contact's assigned team or {@code null} if the team has not been set
     */
    @Nullable
    public static String getTeam(@Nonnull IIndividualContact contact)
    {
        final String team = contact.getAttributes().getStringAttribute(IndividualContactAttributeNames.TEAM, "");

        // AttributeSet does not allow null default values
        return team.isEmpty() ? null : team;
    }

    /**
     * Sets the contact's team. The team will affect how the contact is rendered in the UI and may cause the contact
     * to be automatically added to a team-based group.
     *
     * @param contact The contact to update
     * @param team The team to assign to the contact. An empty String can be used to clear the value and indicate that
     *             the team is unset.
     */
    public static void setTeam(@Nonnull IIndividualContact contact, @Nonnull String team)
    {
        contact.getAttributes().setAttribute(IndividualContactAttributeNames.TEAM, team);
    }

    private IndividualContactUtils()
    {
    }
}
