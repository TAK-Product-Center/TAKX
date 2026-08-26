package gov.takx.api.project;

import gov.takx.api.annotation.NoPluginImplementations;

/**
 */
@NoPluginImplementations
public interface IProjectMapObjectMetadata
{
    String getFamily();

    String getType();

    String getUnitID();

    String getName();

    String getKey();

    /**
     * @return {@code true} if the unit Id has the form of a UUID
     * @since 3.0
     */
    boolean isUnitIdAUuid();

    /**
     * @return Useful string to display to humans, format {@code "type:unitId:(name)"}.  The {@code unitId}
     * is converted to an empty string if it looks like a UUID.
     * @see #isUnitIdAUuid()
     * @since 3.0
     */
    String getDisplayText();
}
