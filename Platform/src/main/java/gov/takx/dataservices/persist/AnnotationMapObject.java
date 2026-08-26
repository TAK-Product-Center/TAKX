package gov.takx.dataservices.persist;

import gov.takx.api.mapobject.IAnnotationMapObject;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.IRaptorPlugin;
import gov.takx.platform.globe.GlobeTextUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import java.awt.Color;
import java.awt.Font;

/**
 */
@Entity
public class AnnotationMapObject extends MapObject implements IAnnotationMapObject
{
    @Column(length = 2048)
    private String text = "";
    private double opacity = 1.00;
    private Font font = GlobeTextUtils.DEFAULT_ANNOTATION_FONT;
    private Color backgroundColor = Color.BLACK;
    private Color textColor = Color.BLACK;
    private double minActiveAltitude = DEFAULT_MIN_ACTIVE_ALTITUDE;
    private double maxActiveAltitude = DEFAULT_MAX_ACTIVE_ALTITUDE;
    private Color borderColor = Color.BLACK;
    private boolean enableLeader = true;
    private int borderShape = ANNOTATION_BORDER_SHAPE_RECTANGLE;
    private int borderRadius = DEFAULT_BORDER_RADIUS;
    private int borderThickness = DEFAULT_BORDER_THICKNESS;
    private boolean enableFixedHeight = DEFAULT_FIXED_HEIGHT_ENABLED;
    private double fixedHeight = 0;
    private int drawOffsetX = DEFAULT_DRAW_OFFSET_X;
    private int drawOffsetY = DEFAULT_DRAW_OFFSET_Y;

    private int textAlign = 1;

    /**
     * For Hibernate
     */
    protected AnnotationMapObject()
    {
    }

    @SuppressWarnings("unused")
    public AnnotationMapObject(MapObjectSpec mapObjectSpec, IPluginDefinition<? extends IRaptorPlugin> pluginDefinition)
    {
        super(mapObjectSpec, pluginDefinition);
    }

    @Override
    public String getText()
    {
        return text;
    }

    @Override
    public double getOpacity()
    {
        return opacity;
    }

    @Override
    public Font getFont()
    {
        return font;
    }

    @Override
    public Color getBackgroundColor()
    {
        return backgroundColor;
    }

    @Override
    public Color getTextColor()
    {
        return textColor;
    }

    @Override
    public int getTextAlign()
    {
        return textAlign;
    }

    @Override
    public double getMaxActiveAltitude()
    {
        return maxActiveAltitude;
    }

    @Override
    public double getMinActiveAltitude()
    {
        return minActiveAltitude;
    }

    @Override
    public Color getBorderColor()
    {
        return borderColor;
    }

    @Override
    public boolean isLeadEnabled()
    {
        return enableLeader;
    }

    @Override
    public int getBorderShape()
    {
        return borderShape;
    }

    @Override
    public int getBorderRadius()
    {
        return borderRadius;
    }

    @Override
    public int getBorderThickness()
    {
        return borderThickness;
    }

    @Override
    public boolean isHeightFixed()
    {
        return enableFixedHeight;
    }

    @Override
    public double getFixedHeight()
    {
        return fixedHeight;
    }

    @Override
    public int getDrawOffsetX()
    {
        return drawOffsetX;
    }

    @Override
    public int getDrawOffsetY()
    {
        return drawOffsetY;
    }

    @Override
    public void setMaxActiveAltitude(double maxActiveAltitude)
    {
        propertyChangeSupport.updateAndFire(ANNOTATION_MAX_ACTIVE_ALTITUDE, this.maxActiveAltitude, maxActiveAltitude,
                () -> this.maxActiveAltitude = maxActiveAltitude);
    }

    @Override
    public void setMinActiveAltitude(double minActiveAltitude)
    {
        propertyChangeSupport.updateAndFire(ANNOTATION_MIN_ACTIVE_ALTITUDE, this.minActiveAltitude, minActiveAltitude,
                () -> this.minActiveAltitude = minActiveAltitude);
    }

    @Override
    public void setText(String text)
    {
        propertyChangeSupport.updateAndFire(ANNOTATION_TEXT, this.text, text, () -> this.text = text);
    }

    @Override
    public void setFont(Font font)
    {
        propertyChangeSupport.updateAndFire(ANNOTATION_FONT, this.font, font, () -> this.font = font);
    }

    @Override
    public void setTextAlign(int textAlign)
    {
        propertyChangeSupport.updateAndFire(ANNOTATION_TEXT_ALIGN, this.textAlign, textAlign, () -> this.textAlign = textAlign);
    }

    @Override
    public void setTextColor(Color textColor)
    {
        propertyChangeSupport.updateAndFire(ANNOTATION_TEXT_COLOR, this.textColor, textColor,
                () -> this.textColor = textColor);
    }

    @Override
    public void setBackgroundColor(Color backgroundColor)
    {
        propertyChangeSupport.updateAndFire(ANNOTATION_BACKGROUND_COLOR, this.backgroundColor, backgroundColor,
                () -> this.backgroundColor = backgroundColor);
    }

    @Override
    public void setOpacity(double opacity)
    {
        propertyChangeSupport.updateAndFire(ANNOTATION_OPACITY, this.opacity, opacity, () -> this.opacity = opacity);
    }

    @Override
    public void setBorderColor(Color borderColor)
    {
        propertyChangeSupport.updateAndFire(ANNOTATION_BORDER_COLOR, this.borderColor, borderColor,
                () -> this.borderColor = borderColor);
    }

    @Override
    public void enableLeader(boolean enableLeader)
    {
        propertyChangeSupport.updateAndFire(ANNOTATION_BORDER_LEAD_ENABLE, this.enableLeader, enableLeader,
                () -> this.enableLeader = enableLeader);
    }

    @Override
    public void setBorderShape(int borderShape)
    {
        propertyChangeSupport.updateAndFire(ANNOTATION_BORDER_SHAPE, this.borderShape, borderShape,
                () -> this.borderShape = borderShape);
    }

    @Override
    public void setBorderRadius(int borderRadius)
    {
        propertyChangeSupport.updateAndFire(ANNOTATION_BORDER_RADIUS, this.borderRadius, borderRadius,
                () -> this.borderRadius = borderRadius);
    }

    @Override
    public void setBorderThickness(int borderThickness)
    {
        propertyChangeSupport.updateAndFire(ANNOTATION_BORDER_THICKNESS, this.borderThickness, borderThickness,
                () -> this.borderThickness = borderThickness);
    }

    @Override
    public void enableFixedHeight(boolean enableFixedHeight)
    {
        propertyChangeSupport.updateAndFire(ANNOTATION_ENABLE_FIXED_HEIGHT, this.enableFixedHeight, enableFixedHeight,
                () -> this.enableFixedHeight = enableFixedHeight);
    }

    @Override
    public void setFixedHeight(double fixedHeight)
    {
        propertyChangeSupport.updateAndFire(ANNOTATION_FIXED_HEIGHT, this.fixedHeight, fixedHeight,
                () -> this.fixedHeight = fixedHeight);
    }

    @Override
    public void setDrawOffsetX(int drawOffsetX)
    {
        propertyChangeSupport.updateAndFire(ANNOTATION_DRAW_OFFSET_X, this.drawOffsetX, drawOffsetX,
                () -> this.drawOffsetX = drawOffsetX);
    }

    @Override
    public void setDrawOffsetY(int drawOffsetY)
    {
        propertyChangeSupport.updateAndFire(ANNOTATION_DRAW_OFFSET_Y, this.drawOffsetY, drawOffsetY,
                () -> this.drawOffsetY = drawOffsetY);
    }
}
