package gov.takx.api.mapobject;

import gov.takx.api.annotation.NoPluginImplementations;

import java.awt.Color;
import java.awt.Font;

/**
 */
@NoPluginImplementations
public interface IAnnotationMapObject extends IMapObject
{
    String ANNOTATION_TYPE = "Annotation";
    String ANNOTATION_TEXT = "ANNOTATION_TEXT";
    String ANNOTATION_TEXT_ALIGN = "ANNOTATION_TEXT_ALIGN";
    String ANNOTATION_TEXT_COLOR = "ANNOTATION_TEXT_COLOR";
    String ANNOTATION_BACKGROUND_COLOR = "ANNOTATION_BACKGROUND_COLOR";
    String ANNOTATION_OPACITY = "ANNOTATION_OPACITY";
    String ANNOTATION_MIN_ACTIVE_ALTITUDE = "ANNOTATION_MIN_ACTIVE_ALTITUDE";
    String ANNOTATION_MAX_ACTIVE_ALTITUDE = "ANNOTATION_MAX_ACTIVE_ALTITUDE";
    String ANNOTATION_FONT = "ANNOTATION_FONT"; // Property change key
    String ANNOTATION_BORDER_COLOR = "ANNOTATION_BORDER_COLOR";
    String ANNOTATION_BORDER_LEAD_ENABLE = "ANNOTATION_BORDER_LEAD_ENABLE";
    String ANNOTATION_BORDER_SHAPE = "ANNOTATION_BORDER_SHAPE";
    String ANNOTATION_BORDER_RADIUS = "ANNOTATION_BORDER_RADIUS";
    String ANNOTATION_BORDER_THICKNESS = "ANNOTATION_BORDER_THICKNESS";
    String ANNOTATION_ENABLE_FIXED_HEIGHT = "ANNOTATION_ENABLE_FIXED_HEIGHT";
    String ANNOTATION_FIXED_HEIGHT = "ANNOTATION_FIXED_HEIGHT";
    String ANNOTATION_DRAW_OFFSET_X = "ANNOTATION_DRAW_OFFSET_X"; // not an entirely accurate name...
    String ANNOTATION_DRAW_OFFSET_Y = "ANNOTATION_DRAW_OFFSET_Y";

    int ANNOTATION_BORDER_SHAPE_RECTANGLE = 0;
    int ANNOTATION_BORDER_SHAPE_ELLIPSE = 1;
    int ANNOTATION_BORDER_SHAPE_NONE = 2;

    /**
     * Left align text
     * @since 5.1
     */
    int ANNOTATION_TEXT_ALIGN_LEFT = 0;
    /**
     * Center align text
     * @since 5.1
     */
    int ANNOTATION_TEXT_ALIGN_CENTER = 1;
    /**
     * Right align text
     * @since 5.1
     */
    int ANNOTATION_TEXT_ALIGN_RIGHT = 2;

    double DEFAULT_MIN_ACTIVE_ALTITUDE = -1000;
    double DEFAULT_MAX_ACTIVE_ALTITUDE = 200000000D;
    int DEFAULT_BORDER_RADIUS = 0;
    int DEFAULT_BORDER_THICKNESS = 20;
    boolean DEFAULT_FIXED_HEIGHT_ENABLED = false;
    int DEFAULT_DRAW_OFFSET_X = -15;
    int DEFAULT_DRAW_OFFSET_Y = 15;

    String getText();

    double getOpacity();

    Font getFont();

    Color getBackgroundColor();

    Color getTextColor();

    /**
     * Gets the text alignment for the annotation. Returns one of the IAnnotationMapObject text alignment constants.
     *
     * @see IAnnotationMapObject#ANNOTATION_TEXT_ALIGN_LEFT
     * @see IAnnotationMapObject#ANNOTATION_TEXT_ALIGN_CENTER
     * @see IAnnotationMapObject#ANNOTATION_TEXT_ALIGN_RIGHT
     * @return the text alignment
     */
    int getTextAlign();

    double getMinActiveAltitude();

    double getMaxActiveAltitude();

    Color getBorderColor();

    boolean isLeadEnabled();

    int getBorderShape();

    int getBorderRadius();

    int getBorderThickness();

    boolean isHeightFixed();

    double getFixedHeight();

    int getDrawOffsetX();

    int getDrawOffsetY();

    void setText(String s);

    void setFont(Font newFont);

    void setTextAlign(int annotationAlignment);

    void setTextColor(Color color);

    void setBackgroundColor(Color color);

    void setOpacity(double opacity);

    void setMinActiveAltitude(double altitude);

    void setMaxActiveAltitude(double altitude);

    void setBorderColor(Color color);

    void enableLeader(boolean enable);

    void setBorderShape(int shape);

    void setBorderRadius(int radius);

    void setBorderThickness(int thickness);

    void enableFixedHeight(boolean enable);

    void setFixedHeight(double fixedHeight);

    void setDrawOffsetX(int offsetX);

    void setDrawOffsetY(int offsetY);
}
