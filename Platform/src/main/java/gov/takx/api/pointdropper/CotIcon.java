package gov.takx.api.pointdropper;

import gov.tak.api.cot.CotTypes;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.stream.Stream;

/**
 * Defines the set of CoT icons available to the {@link IPointDropperService}.
 *
 * @since 5.1
 */
public enum CotIcon
{
    FriendlyGround("Friendly Ground", "Friendly", CotTypes.PREFIX_FRIENDLY + CotTypes.SUFFIX_GROUND),
    HostileGround("Hostile Ground", "Hostile", CotTypes.PREFIX_HOSTILE + CotTypes.SUFFIX_GROUND),
    NeutralGround("Neutral Ground", "Neutral", CotTypes.PREFIX_NEUTRAL + CotTypes.SUFFIX_GROUND),
    UnknownGround("Unknown Ground", "Unknown", CotTypes.PREFIX_UNKNOWN + CotTypes.SUFFIX_GROUND);

    /** Type for CoT markers.  */
    public static final String TYPE = "2525-Point";

    private final String name;
    private final String shortName;
    private final String cotType;

    /**
     * @param name The icon name, as used in the TAKX icon set
     * @param shortName Short name, for use in menus, lists, etc.
     * @param cotType The icon's CoT type string
     */
    CotIcon(@Nonnull String name, @Nonnull String shortName, @Nonnull String cotType)
    {
        this.name = name;
        this.shortName = shortName;
        this.cotType = cotType;
    }

    /**
     * @return The icon's display name, don't confuse with {@link #name()} which returns the enum constant name.
     */
    @Nonnull
    public String getName()
    {
        return name;
    }

    /**
     * @return The icon's short name.
     * @since 5.3
     */
    @Nonnull
    public String getShortName()
    {
        return shortName;
    }

    /**
     * @return The name of the icon within the TAKX icon set
     * @since 5.2
     */
    @Nonnull
    public String getIconFileName()
    {
        return name + ".png";
    }

    /**
     * @return The icon's CoT type string.
     */
    @Nonnull
    public String getCotType()
    {
        return cotType;
    }

    /**
     * @param name The icon display name
     * @return The CoT icon, or {@code null} if there is no icon with a matching display name
     */
    @Nullable
    public static CotIcon findByName(String name)
    {
        return Stream.of(values()).filter(icon -> icon.name.equalsIgnoreCase(name)).findFirst().orElse(null);
    }
}
