package gov.tak.platform.comms.commo;

import gov.tak.api.cot.CotTypes;

/**
 * Enum defining the values a user may select for their device display type, with mappings to the appropriate
 * CoT type to use in our broadcast SA messages.
 *
 * @since 5.1
 */
public enum DeviceDisplayType
{
    GROUND_TROOP("Ground Troop", CotTypes.FRIENDLY_GROUND_COMBAT_UNIT),
    ARMORED_VEHICLE("Armored Vehicle", CotTypes.PREFIX_FRIENDLY + "-G-E-V-A"),
    CIVILIAN_VEHICLE("Civilian Vehicle", CotTypes.PREFIX_FRIENDLY + "-G-E-V-C"),
    GENERIC_AIR_UNIT("Generic Air Unit", CotTypes.PREFIX_FRIENDLY + "-A"),
    GENERIC_GROUND_UNIT("Generic Ground Unit", CotTypes.PREFIX_FRIENDLY + "-G-U"),
    GENERIC_SEA_SURFACE_UNIT("Generic Sea Surface Unit", CotTypes.PREFIX_FRIENDLY + "-S"),
    GENERIC_MILITARY_BASE("Generic Military Base/Facility", CotTypes.PREFIX_FRIENDLY + "-G-I-B");

    private final String displayName;
    private final String cotType;

    DeviceDisplayType(String displayName, String cotType)
    {
        this.displayName = displayName;
        this.cotType = cotType;
    }

    /**
     * @return Display name
     */
    public String getDisplayName()
    {
        return displayName;
    }

    /**
     * @return CoT type for SA messages
     */
    public String getCotType()
    {
        return cotType;
    }

    @Override
    public String toString()
    {
        return displayName;
    }
}
