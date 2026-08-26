package gov.takx.api.messages;

/**
 * Collection of well-known metadata field names.
 *
 * @since 1.5 (renamed in 3.0)
 */
public final class MetadataKey
{
    /**
     * Standard metadata key name used to provide a human-readable description.
     */
    public static final String KEY_DESCRIPTION = "gov.takx.Description";

    /**
     * Standard metadata key name used to represent a unit of measure.
     */
    public static final String KEY_UOM = "gov.takx.UnitsOfMeasure";

    private MetadataKey()
    {
    }
}
