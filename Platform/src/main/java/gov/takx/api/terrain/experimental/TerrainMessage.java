package gov.takx.api.terrain.experimental;

import gov.takx.api.annotation.Experimental;

/**
 */
@Experimental
public enum TerrainMessage
{
    LOW_RESOLUTION("Highest resolution terrain data is unavailable: using local best available data", false),
    UNUSABLE_RESOLUTION("Best available terrain data is worse than minimum usable resolution", false),
    LOCATION_MISSING("Unable to retrieve terrain data for location", true),
    REGION_MISSING("Unable to retrieve terrain data for sector", true),
    REGION_OUT_OF_BOUNDS("Region is outside elevation bounds", true),
    RETRIEVAL_TIMEOUT("Terrain data retrieval timeout", true),
    INVALID_DATA("Terrain data contains invalid data", true),
    CANCELED("Terrain data retrieval canceled", false);

    public final String messageDescription;
    public final boolean error;

    TerrainMessage(String messageDescription, boolean error)
    {
        this.messageDescription = messageDescription;
        this.error = error;
    }

    public String getMessageDescription()
    {
        return messageDescription;
    }

    public boolean isError()
    {
        return error;
    }
}
