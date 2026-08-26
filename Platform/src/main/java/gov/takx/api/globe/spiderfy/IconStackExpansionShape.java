package gov.takx.api.globe.spiderfy;

/**
 * Defines the shape that nested icons within a {@code IIconStack} will be expanded into.
 *
 * @since 2.26
 */
public enum IconStackExpansionShape
{
    /**
     * Icons will expand out along the border of a circle that is wide enough to give icons enough space to
     * avoid overlapping.
     */
    CIRCLE,
    /**
     * Icons will expand out to their actual locations.
     */
    NONE
}
