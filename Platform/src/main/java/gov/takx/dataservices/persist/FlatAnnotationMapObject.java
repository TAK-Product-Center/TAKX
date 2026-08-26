package gov.takx.dataservices.persist;

import gov.nasa.worldwind.avlist.AVKey;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.mapobject.IFlatAnnotationMapObject;
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
public class FlatAnnotationMapObject extends MapObject implements IFlatAnnotationMapObject
{
    @Column(length = 2048)
    private String text = "";
    private Font font = GlobeTextUtils.DEFAULT_FLAT_ANNOTATION_FONT;
    private Color textColor = Color.GRAY;
    private Color backgroundColor = Color.WHITE;
    private boolean backgroundFillEnabled = true;
    private Color borderColor = Color.BLUE;
    private boolean borderEnabled = true;
    private double opacity = .8;
    private double size = 1000;
    private double orientation = 0;
    private float borderThickness = 3;
    private String textAlignment = AVKey.CENTER;

    /**
     * For Hibernate
     */
    protected FlatAnnotationMapObject()
    {
    }

    public FlatAnnotationMapObject(MapObjectSpec mapObjectSpec, IPluginDefinition<? extends IRaptorPlugin> pluginDefinition)
    {
        super(mapObjectSpec, pluginDefinition);

        setMetaBoolean(StandardAttributeNames.NEVER_COT, true);
    }

    @Override
    public void setText(String text)
    {
        propertyChangeSupport.updateAndFire(FLAT_ANNOTATION_TEXT, this.text, text, () -> this.text = text);
    }

    @Override
    public void setFont(Font font)
    {
        propertyChangeSupport.updateAndFire(FLAT_ANNOTATION_FONT, this.font, font, () -> this.font = font);
    }

    @Override
    public void setTextColor(Color textColor)
    {
        propertyChangeSupport.updateAndFire(FLAT_ANNOTATION_TEXT_COLOR, this.textColor, textColor,
                () -> this.textColor = textColor);
    }

    @Override
    public void setBackgroundColor(Color backgroundColor)
    {
        propertyChangeSupport.updateAndFire(FLAT_ANNOTATION_BG_COLOR, this.backgroundColor, backgroundColor,
                () -> this.backgroundColor = backgroundColor);
    }

    @Override
    public void setBackgroundFillEnabled(boolean backgroundFillEnabled)
    {
        propertyChangeSupport.updateAndFire(FLAT_ANNOTATION_BG_FILL_ENABLED,
                this.backgroundFillEnabled, backgroundFillEnabled,
                () -> this.backgroundFillEnabled = backgroundFillEnabled);
    }

    @Override
    public void setBorderColor(Color borderColor)
    {
        propertyChangeSupport.updateAndFire(FLAT_ANNOTATION_BORDER_COLOR, this.borderColor, borderColor,
                () -> this.borderColor = borderColor);
    }

    @Override
    public void setBorderEnabled(boolean borderEnabled)
    {
        propertyChangeSupport.updateAndFire(FLAT_ANNOTATION_ENABLE_BORDER, this.borderEnabled, borderEnabled,
                () -> this.borderEnabled = borderEnabled);
    }

    @Override
    public void setOpacity(double opacity)
    {
        propertyChangeSupport.updateAndFire(FLAT_ANNOTATION_OPACITY, this.opacity, opacity,
                () -> this.opacity = opacity);
    }

    @Override
    public void setSize(double size)
    {
        propertyChangeSupport.updateAndFire(FLAT_ANNOTATION_SIZE, this.size, size, () -> this.size = size);
    }

    @Override
    public void setBorderThickness(float borderThickness)
    {
        propertyChangeSupport.updateAndFire(FLAT_ANNOTATION_BORDER_THICKNESS, this.borderThickness, borderThickness,
                () -> this.borderThickness = borderThickness);
    }

    @Override
    public void setOrientation(double orientation)
    {
        propertyChangeSupport.updateAndFire(FLAT_ANNOTATION_ORIENTATION, this.orientation, orientation,
                () -> this.orientation = orientation);
    }

    @Override
    public void setTextAlignment(String textAlignment)
    {
        propertyChangeSupport.updateAndFire(FLAT_ANNOTATION_TEXT_ALIGNMENT, this.textAlignment, textAlignment,
                () -> this.textAlignment = textAlignment);
    }

    @Override
    public String getText()
    {
        return text;
    }

    @Override
    public Font getFont()
    {
        return font;
    }

    @Override
    public Color getTextColor()
    {
        return textColor;
    }

    @Override
    public Color getBackgroundColor()
    {
        return backgroundColor;
    }

    @Override
    public Color getBorderColor()
    {
        return borderColor;
    }

    @Override
    public boolean isBorderEnabled()
    {
        return borderEnabled;
    }

    @Override
    public double getOpacity()
    {
        return opacity;
    }

    @Override
    public double getSize()
    {
        return size;
    }

    @Override
    public float getBorderThickness()
    {
        return borderThickness;
    }

    @Override
    public double getOrientation()
    {
        return orientation;
    }

    @Override
    public String getTextAlignment()
    {
        return textAlignment;
    }

    @Override
    public boolean isBackgroundFillEnabled()
    {
        return backgroundFillEnabled;
    }
}
