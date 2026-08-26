package gov.tak.api.maps;

/**
 * Contract for a geo-referenced marker on the map.
 * <p>
 * Bridge interface to attempt to harmonize the TAKX code with the ATAK code (where Marker is a concrete class).
 *
 * @since 3.0
 */
@SuppressWarnings("NewClassNamingConvention")
public interface Marker extends PointMapItem
{
    /**
     * Style property name.
     */
    String STYLE = "STYLE";

    /**
     * Indicates the icon should be rotated according to the track heading. The icon is assumed to
     * be naturally at heading 0 degrees and will be rotated counter clockwise from there. This is a
     * handy feature for implementing markers that are rotating arrows.
     */
    int STYLE_ROTATE_HEADING_MASK = 1;

    /**
     * The marker will smoothly interpolate between rotation changes
     */
    int STYLE_SMOOTH_ROTATION_MASK = 2;

    /**
     * The marker will smoothly interpolate between point changes
     */
    int STYLE_SMOOTH_MOVEMENT_MASK = 1 << 2;

    /**
     * Shorten long titles by marquee the text
     */
    int STYLE_MARQUEE_TITLE_MASK = 1 << 3;

    int STYLE_ALERT_MASK = 1 << 4;

    /**
     * If Rotating arrows are not desired and full rotation is desired, set the following flag.
     */
    int STYLE_ROTATE_HEADING_NOARROW_MASK = 1 << 5;

    /**
     * @return The style flags
     */
    int getStyle();

    /**
     * Sets the style of the Marker as defined by the STYLE values in the IMarker interface. After
     * this has been invoked the style listeners are notified.
     *
     * @param style a STYLE flag from the IMarker interface.
     */
    void setStyle(int style);
}
