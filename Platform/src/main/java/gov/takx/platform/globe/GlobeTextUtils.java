package gov.takx.platform.globe;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.render.AnnotationAttributes;
import gov.nasa.worldwind.render.GlobeAnnotation;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;

import static gov.takx.platform.ui.FontUtils.DEFAULT_FONT_SIZE;

/**
 * Utility class containing methods and constants related to text and labels on the globe.
 *
 * @since 3.3
 */
public final class GlobeTextUtils
{
    public static final Font DEFAULT_GLOBE_FONT = new Font("Arial", Font.PLAIN, DEFAULT_FONT_SIZE);
    public static final Font DEFAULT_BOLD_GLOBE_FONT = DEFAULT_GLOBE_FONT.deriveFont(DEFAULT_GLOBE_FONT.getStyle() | Font.BOLD);
    public static final Font DEFAULT_MONOSPACE_GLOBE_FONT = new Font("Lucida Console", Font.PLAIN, DEFAULT_FONT_SIZE);

    public static final Font ANALYTIC_SURFACE_LEGEND_FONT = DEFAULT_BOLD_GLOBE_FONT;
    public static final Font ANALYTIC_SURFACE_LEGEND_TITLE_FONT = DEFAULT_BOLD_GLOBE_FONT.deriveFont(DEFAULT_BOLD_GLOBE_FONT.getSize() + 4f);
    public static final Font DEFAULT_ANNOTATION_FONT = DEFAULT_GLOBE_FONT;
    public static final Font DEFAULT_FLAT_ANNOTATION_FONT = DEFAULT_GLOBE_FONT.deriveFont(DEFAULT_GLOBE_FONT.getSize() + 2f);
    public static final Font DEFAULT_PROJECT_LABEL_FONT = DEFAULT_BOLD_GLOBE_FONT.deriveFont(DEFAULT_BOLD_GLOBE_FONT.getSize() + 12f);
    public static final Font ELEVATION_CONTOUR_LABEL_FONT = DEFAULT_GLOBE_FONT;
    public static final Font ICON_STACK_LABEL_FONT = DEFAULT_BOLD_GLOBE_FONT.deriveFont(DEFAULT_BOLD_GLOBE_FONT.getSize() + 2f);
    public static final Font PREVIEW_ANNOTATION_FONT = DEFAULT_GLOBE_FONT;
    public static final Font RANGE_FAN_LABEL_FONT = DEFAULT_GLOBE_FONT;
    public static final Font SHAPE_LABEL_FONT = DEFAULT_BOLD_GLOBE_FONT.deriveFont(DEFAULT_BOLD_GLOBE_FONT.getSize() + 2f);
    public static final Font TILED_IMAGE_LAYER_FONT = DEFAULT_GLOBE_FONT.deriveFont(DEFAULT_GLOBE_FONT.getSize() + 1f);

    private GlobeTextUtils()
    {
    }

    /**
     * Sets a default set of attributes for a {@link GlobeAnnotation} used as a label for a shape.
     *
     * @param globeAnnotation The {@link GlobeAnnotation} to set attributes for
     */
    public static void setDefaultLabelAnnotations(GlobeAnnotation globeAnnotation)
    {
        globeAnnotation.setAlwaysOnTop(false);
        globeAnnotation.setPickEnabled(true);

        AnnotationAttributes attrs = new AnnotationAttributes();
        attrs.setAdjustWidthToText(AVKey.SIZE_FIT_TEXT);
        attrs.setFrameShape(AVKey.SHAPE_NONE);
        attrs.setEffect(AVKey.TEXT_EFFECT_OUTLINE);
        attrs.setDrawOffset(new Point(0, -35));
        attrs.setTextColor(Color.WHITE);
        attrs.setBackgroundColor(Color.BLACK);
        attrs.setFont(SHAPE_LABEL_FONT);
        attrs.setTextAlign(AVKey.CENTER);

        globeAnnotation.setAttributes(attrs);
    }
}
