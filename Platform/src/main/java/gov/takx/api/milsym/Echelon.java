package gov.takx.api.milsym;

import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.symbology.Amplifier;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Enum outlining the possible echelon modifiers for SIDCs.
 *
 * @since 5.2
 * @see Amplifier
 * @deprecated since = "5.5", forRemoval = true, removeAt = "5.8"
 */
@Deprecated(since = "5.5", forRemoval = true)
@DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
public enum Echelon
{
    UNSPECIFIED('*', "Unspecified"),
    TEAM_CREW('A', "Team / Crew"),
    SQUAD('B', "Squad"),
    SECTION('C', "Section"),
    PLATOON_DETACHMENT('D', "Platoon / Detachment"),
    COMPANY_BATTERY_TROOP('E', "Company / Battery / Troop"),
    BATTALION_SQUADRON('F', "Battalion / Squadron"),
    REGIMENT_GROUP('G', "Regiment / Group"),
    BRIGADE('H', "Brigade"),
    DIVISION('I', "Division"),
    CORPS_MEF('J', "Corps / MEG"),
    ARMY('K', "Army"),
    ARMY__GROUP_FRONT('L', "Army Group / Front"),
    REGION_THEATER('M', "Region / Theater"),
    COMMAND('N', "Command");

    final char code;
    final String display;

    Echelon(char code, String display)
    {
        this.code = code;
        this.display = display;
    }

    /**
     * @param c Char denoting one of the possible echelons.
     * @return Echelon instance for the associated char value, or {@link #UNSPECIFIED} if no match was found.
     */
    @Nonnull
    public static Echelon fromChar(char c)
    {
        return switch (c)
        {
            case 'A' -> Echelon.TEAM_CREW;
            case 'B' -> Echelon.SQUAD;
            case 'C' -> Echelon.SECTION;
            case 'D' -> Echelon.PLATOON_DETACHMENT;
            case 'E' -> Echelon.COMPANY_BATTERY_TROOP;
            case 'F' -> Echelon.BATTALION_SQUADRON;
            case 'G' -> Echelon.REGIMENT_GROUP;
            case 'H' -> Echelon.BRIGADE;
            case 'I' -> Echelon.DIVISION;
            case 'J' -> Echelon.CORPS_MEF;
            case 'K' -> Echelon.ARMY;
            case 'L' -> Echelon.ARMY__GROUP_FRONT;
            case 'M' -> Echelon.REGION_THEATER;
            case 'N' -> Echelon.COMMAND;
            default -> Echelon.UNSPECIFIED;
        };
    }

    /**
     * @return Character to set at the 11th index of a SIDC.
     */
    public char getCode()
    {
        return code;
    }

    /**
     * @param amplifier The Kernel {@link Amplifier} to convert to an Echelon
     * @return The Echelon equivalent, or {@code null} if it's not an Echelon
     * @since 5.5
     */
    @Nullable
    public static Echelon fromAmplifier(@Nullable Amplifier amplifier)
    {
        if (amplifier == null) return null;
        return switch (amplifier)
        {
            case Team_Crew -> TEAM_CREW; // this is not a duplicate, do not attempt to change this
            case Squad -> SQUAD;
            case Section -> SECTION;
            case Platoon_Detachment -> PLATOON_DETACHMENT;
            case Company_Battery_Troop -> COMPANY_BATTERY_TROOP;
            case Battalion_Squadron -> BATTALION_SQUADRON;
            case Regiment_Group -> REGIMENT_GROUP;
            case Brigade -> BRIGADE;
            case Division -> DIVISION;
            case Corps_MEF -> CORPS_MEF;
            case Army -> ARMY;
            case ArmyGroup_Front -> ARMY__GROUP_FRONT;
            case Region_Theater -> REGION_THEATER;
            case Command -> COMMAND;
            default -> null;
        };
    }

    /**
     * @param echelon The Echelon to convert
     * @return The {@link Amplifier} equivalent of the provided Echelon
     * @since 5.5
     */
    @Nullable
    public static Amplifier toAmplifier(@Nullable Echelon echelon)
    {
        if (echelon == null) return null;
        return switch (echelon)
        {
            case UNSPECIFIED -> null;
            case TEAM_CREW -> Amplifier.Team_Crew;
            case SQUAD -> Amplifier.Squad;
            case SECTION -> Amplifier.Section;
            case PLATOON_DETACHMENT -> Amplifier.Platoon_Detachment;
            case COMPANY_BATTERY_TROOP -> Amplifier.Company_Battery_Troop;
            case BATTALION_SQUADRON -> Amplifier.Battalion_Squadron;
            case REGIMENT_GROUP -> Amplifier.Regiment_Group;
            case BRIGADE -> Amplifier.Brigade;
            case DIVISION -> Amplifier.Division;
            case CORPS_MEF -> Amplifier.Corps_MEF;
            case ARMY -> Amplifier.Army;
            case ARMY__GROUP_FRONT -> Amplifier.ArmyGroup_Front;
            case REGION_THEATER -> Amplifier.Region_Theater;
            case COMMAND -> Amplifier.Command;
        };
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
