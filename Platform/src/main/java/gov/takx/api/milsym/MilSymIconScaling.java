package gov.takx.api.milsym;

/**
 * Icon scaling strategy for {@link IMilSymIconBuilder}.
 * 
 * @since 5.5
 */
public enum MilSymIconScaling
{
    /**
     * Rendered symbols do not have any scale adjustments applied.
     *
     * @since 5.5.1
     */
    NONE,
    /**
     * Rendered symbols with unequal dimensions will downscale the image such that
     * the largest dimension is the value specified via {@link IMilSymIconBuilder#setResolution(int)}.
     */
    SCALE_TO_MAX_DIMENSION,
    /**
     * Rendered symbols with unequal dimensions will upscale the image such that
     * the smallest dimension is the value specified via {@link IMilSymIconBuilder#setResolution(int)}.
     */
    SCALE_TO_MIN_DIMENSION
}
