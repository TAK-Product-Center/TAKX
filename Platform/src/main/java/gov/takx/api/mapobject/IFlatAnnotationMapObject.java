package gov.takx.api.mapobject;

import gov.takx.api.annotation.NoPluginImplementations;

import java.awt.Color;
import java.awt.Font;
import java.util.Arrays;
import java.util.List;

/**
 * Contract for FlatAnnotationMapObjects
 *
 */
@NoPluginImplementations
public interface IFlatAnnotationMapObject extends IMapObject
{
    // Map Object Type
    String FLAT_ANNOTATION_TYPE = "Flat Annotation";

    // Properties
    String FLAT_ANNOTATION_TEXT = "FLAT_ANNOTATION_TEXT";
    String FLAT_ANNOTATION_FONT = "FLAT_ANNOTATION_FONT";
    String FLAT_ANNOTATION_FONT_NAME = "FLAT_ANNOTATION_FONT_NAME";
    String FLAT_ANNOTATION_FONT_STYLE = "FLAT_ANNOTATION_FONT_STYLE";
    String FLAT_ANNOTATION_TEXT_COLOR = "FLAT_ANNOTATION_TEXT_COLOR";
    String FLAT_ANNOTATION_BG_COLOR = "FLAT_ANNOTATION_BG_COLOR";
    String FLAT_ANNOTATION_BG_FILL_ENABLED = "FLAT_ANNOTATION_BG_FILL_ENABLED";
    String FLAT_ANNOTATION_BORDER_COLOR = "FLAT_ANNOTATION_BORDER_COLOR";
    String FLAT_ANNOTATION_ENABLE_BORDER = "FLAT_ANNOTATION_ENABLE_BORDER";
    String FLAT_ANNOTATION_OPACITY = "FLAT_ANNOTATION_OPACITY";
    String FLAT_ANNOTATION_SIZE = "FLAT_ANNOTATION_SIZE";
    String FLAT_ANNOTATION_BORDER_THICKNESS = "FLAT_ANNOTATION_BORDER_THICKNESS";
    String FLAT_ANNOTATION_ORIENTATION = "FLAT_ANNOTATION_ORIENTATION";
    String FLAT_ANNOTATION_TEXT_ALIGNMENT = "FLAT_ANNOTATION_TEXT_ALIGNMENT";

    // List of properties to watch
    List<String> PROPERTIES_TO_WATCH = Arrays.asList(FLAT_ANNOTATION_TEXT, FLAT_ANNOTATION_FONT,
            FLAT_ANNOTATION_FONT_NAME, FLAT_ANNOTATION_FONT_STYLE,
            FLAT_ANNOTATION_TEXT_COLOR, FLAT_ANNOTATION_BG_COLOR, FLAT_ANNOTATION_BG_FILL_ENABLED,
            FLAT_ANNOTATION_BORDER_COLOR, FLAT_ANNOTATION_ENABLE_BORDER, FLAT_ANNOTATION_OPACITY,
            FLAT_ANNOTATION_SIZE, FLAT_ANNOTATION_BORDER_THICKNESS, FLAT_ANNOTATION_ORIENTATION,
            FLAT_ANNOTATION_TEXT_ALIGNMENT);

    String getText();

    Font getFont();

    Color getTextColor();

    Color getBackgroundColor();

    boolean isBackgroundFillEnabled();

    Color getBorderColor();

    boolean isBorderEnabled();

    double getOpacity();

    double getSize();

    double getOrientation();

    String getTextAlignment();

    float getBorderThickness();

    void setText(String text);

    void setFont(Font font);

    void setTextColor(Color color);

    void setBackgroundColor(Color color);

    void setBackgroundFillEnabled(boolean enable);

    void setBorderColor(Color color);

    void setBorderEnabled(boolean borderEnabled);

    void setOpacity(double opacity);

    void setOrientation(double orientation);

    void setTextAlignment(String stringAlignment);

    void setSize(double sizeInMeters);

    void setBorderThickness(float thickness);
}
