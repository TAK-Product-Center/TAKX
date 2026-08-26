package gov.takx.api.tracks;

/**
 * Standard size definitions for SmartTrack attributes scaled between the values of 1and 10, inclusive. This includes
 * the Connecting Line line width and Track Style track shape and glyph size attributes.
 *
 * @since 1.12
 */
public interface ISize
{
    /**
     * Constant used to define the smallest track shape or glyph size that is possible.
     */
    public static final int MINIMUM = 1;

    /**
     * Constant used to define a small track shape or glyph size.
     */
    public static final int SMALL = 3;

    /**
     * Constant used to define a normal track shape or glyph size.
     */
    public static final int NORMAL = 5;

    /**
     * Constant used to define a large track shape or glyph size.
     */
    public static final int LARGE = 7;

    /**
     * Constant used to define the largest track shape or glyph size that is possible.
     */
    public static final int MAXIMUM = 10;
}
