package gov.takx.api.emergency;

import gov.tak.api.annotation.IncubatingApi;
import gov.tak.api.cot.CotTypes;

import static gov.tak.api.cot.CotTypes.PREFIX_EMERGENCY;

/**
 * Emergency Alert types, including a description and associated CoT type.
 *
 * @since 5.7
 */
@IncubatingApi(since = "5.7")
public enum EmergencyAlertType
{
    NineOneOne("911 Alert", PREFIX_EMERGENCY + "-o-tbl"),
    GeoFenceBreach("Geo-fence Breached", PREFIX_EMERGENCY + "-o-g"),
    RingTheBell("Ring The Bell", PREFIX_EMERGENCY + "-o-pan"),
    InContact("In Contact", PREFIX_EMERGENCY + "-o-opn"),
    Cancel("Cancel Alert", CotTypes.EMERGENCY_CANCEL),
    ;

    private final String cotType;
    private final String description;

    EmergencyAlertType(String description, String cotType)
    {
        this.cotType = cotType;
        this.description = description;
    }

    /**
     * @return The CoT type for this alert type
     */
    public String getCotType()
    {
        return cotType;
    }

    /**
     * @return The description of this alert type
     */
    public String getDescription()
    {
        return description;
    }

    @Override
    public String toString()
    {
        return description;
    }
}
