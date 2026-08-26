package gov.takx.api.milsym;

import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.symbology.Status;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Enum outlining the possible presence modifiers for SIDCs.
 *
 * @since 5.2
 * @see Status
 * @deprecated since = "5.5", forRemoval = true, removeAt = "5.8"
 */
@Deprecated(since = "5.5", forRemoval = true)
@DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
public enum Presence
{
    UNSPECIFIED('-', "Unspecified"),
    ANTICIPATED_PLANNED('A', "ANTICIPATED / PLANNED"),
    PRESENT('P', "PRESENT"),
    PRESENT_FULLY_CAPABLE('C', "PRESENT / FULLY CAPABLE"),
    PRESENT_DAMAGED('D', "PRESENT / DAMAGED"),
    PRESENT_DESTROYED('X', "PRESENT / DESTROYED"),
    PRESENT_FULL_CAPACITY('F', "PRESENT / FULL TO CAPACITY");

    final char code;
    final String display;

    Presence(char code, String display)
    {
        this.code = code;
        this.display = display;
    }

    /**
     * @param c Char denoting one of the possible presence amplifiers.
     * @return Presence instance for the associated char value, or {@link #UNSPECIFIED} if no match was found.
     */
    @Nonnull
    public static Presence fromChar(char c)
    {
        return switch (c)
        {
            case 'A' -> Presence.ANTICIPATED_PLANNED;
            case 'P' -> Presence.PRESENT;
            case 'C' -> Presence.PRESENT_FULLY_CAPABLE;
            case 'D' -> Presence.PRESENT_DAMAGED;
            case 'X' -> Presence.PRESENT_DESTROYED;
            case 'F' -> Presence.PRESENT_FULL_CAPACITY;
            default -> Presence.UNSPECIFIED;
        };
    }

    /**
     * @param status The {@link Status} to convert to a Presence
     * @return The equivalent Presence to the provided Status
     * @since 5.5
     */
    @Nonnull
    public static Presence fromStatus(@Nullable Status status)
    {
        if (status == null) return PRESENT;
        return switch (status)
        {
            case Present -> PRESENT;
            case PlannedAnticipatedSuspect -> ANTICIPATED_PLANNED;
            case PresentFullyCapable -> PRESENT_FULLY_CAPABLE;
            case PresentDamaged -> PRESENT_DAMAGED;
            case PresentDestroyed -> PRESENT_DESTROYED;
            case PresentFullToCapacity -> PRESENT_FULL_CAPACITY;
        };
    }

    /**
     * @param presence The {@link Presence} to convert to a {@link Status}
     * @return The equivalent Status to the provided Presence
     * @since 5.5
     */
    @Nonnull
    public static Status toStatus(@Nullable Presence presence)
    {
        if (presence == null) return Status.Present;
        return switch (presence)
        {
            case UNSPECIFIED, PRESENT -> Status.Present;
            case ANTICIPATED_PLANNED -> Status.PlannedAnticipatedSuspect;
            case PRESENT_FULLY_CAPABLE -> Status.PresentFullyCapable;
            case PRESENT_DAMAGED -> Status.PresentDamaged;
            case PRESENT_DESTROYED -> Status.PresentDestroyed;
            case PRESENT_FULL_CAPACITY -> Status.PresentFullToCapacity;
        };
    }

    /**
     * @return Character to set at the 3rd index of a SIDC.
     */
    public char getCode()
    {
        return code;
    }

    /**
     * @return Display name for UI usage.
     */
    @Nonnull
    public String getDisplay()
    {
        return display;
    }
}
