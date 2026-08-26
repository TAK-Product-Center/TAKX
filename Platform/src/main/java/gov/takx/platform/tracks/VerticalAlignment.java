package gov.takx.platform.tracks;

/**
 * Vertical alignment used to specify how the glyph on a track point is rendered relative to the marker.
 *
 * @since 1.12
 */
public enum VerticalAlignment
{
    Top,
    Middle,
    Bottom;

    /**
     * The default vertical alignment.
     *
     * @see #Top
     */
    public static final VerticalAlignment DEFAULT = Top;
}
