package gov.takx.dataservices.shapes;

import com.atakmap.map.layer.feature.style.TextureFillStyle;
import com.fasterxml.jackson.annotation.JsonIgnore;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IShapeMapObject;
import gov.takx.api.shapes.IMutableShape;
import gov.takx.api.shapes.IShape;
import gov.takx.api.shapes.IShapeListener;
import gov.takx.api.shapes.ShapeProperties;
import gov.takx.commons.lang.IPropertyChangeDelegateSupport;
import gov.takx.platform.ui.UIResources;
import gov.takx.platform.utils.PrioritizedPropertyChangeSupport;
import gov.takx.platform.utils.ShapeUtils;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.OperationNotSupportedException;

import java.awt.Color;
import java.lang.invoke.MethodHandles;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Abstract base class for all shape instances.  This class handles the shape creation and basic attribute getters/setters.
 */
@NoPluginImplementations
public abstract class AShape implements IMutableShape, IPropertyChangeDelegateSupport
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int DASHED_STIPPLE_FACTOR = 6;
    private static final short DASHED_STIPPLE_PATTERN = 0x5555;
    private static final int DEFAULT_STIPPLE_FACTOR = 0;
    private static final short DEFAULT_STIPPLE_PATTERN = 0;
    private static final String CROSS_PATTERN_URI = UIResources.getUrl(PLATFORM_ICONS, "cross_pattern.png").toString();

    protected static final int MIN_CORNER = ShapeUtils.MIN_CORNER;
    protected static final int MAX_CORNER = ShapeUtils.MAX_CORNER;

    @JsonIgnore
    protected final PrioritizedPropertyChangeSupport propertyChangeSupport = new PrioritizedPropertyChangeSupport(this);

    private String key;
    private ILocation location = GeoPoint.UNKNOWN_POINT;
    private String name = "";
    private String description;
    private String remarks = "";
    private AltitudeMode altitudeMode = AltitudeMode.ClampToGround;

    private int fillColor = new Color(0, 255, 255, (int) (255 * .7)).getRGB(); // Cyan at 70% opacity
    private int lineColor = Color.cyan.getRGB();
    private double lineWidth = 3.0;
    private int lineStippleFactor;
    private short lineStipplePattern;
    private TextureFillParams textureFillParams;

    private boolean visible = true;
    private boolean orientable = true;
    private boolean editable = true;
    private boolean allowDrag = true;
    private boolean published;

    @JsonIgnore
    private int shapeListenerCount;

    // The map object representing this shape
    @JsonIgnore
    private IShapeMapObject<?> shapeMapObject;

    /**
     * Default constructor.
     */
    protected AShape()
    {
    }

    @Override
    public void setShapeMapObject(IShapeMapObject<?> shapeMapObject)
    {
        this.shapeMapObject = shapeMapObject;
        key = shapeMapObject.getKey();
        location = shapeMapObject.getLocation();
    }

    @Nonnull
    @Override
    public ILocation getLocation()
    {
        return location;
    }

    @Override
    public void setLocation(ILocation location)
    {
        propertyChangeSupport.updateAndFire(ShapeProperties.LOCATION, this.location, location, () -> this.location = location);
    }

    @Override
    public Color getLineColor()
    {
        return new Color(lineColor, true);
    }

    @Override
    public void setLineColor(Color color)
    {
        final int newColor = 0xff000000 | color.getRGB(); // No alpha
        propertyChangeSupport.updateAndFire(ShapeProperties.LINE_COLOR, lineColor, newColor, () -> lineColor = newColor);
    }

    @Override
    public double getLineWidth()
    {
        return lineWidth;
    }

    @Override
    public void setLineWidth(double lineWidth)
    {
        propertyChangeSupport.updateAndFire(ShapeProperties.LINE_WIDTH, this.lineWidth, lineWidth,
                () -> this.lineWidth = lineWidth);
    }

    @Override
    public int getLineStippleFactor()
    {
        return lineStippleFactor;
    }

    @Override
    public void setLineStippleFactor(int lineStippleFactor)
    {
        propertyChangeSupport.updateAndFire(ShapeProperties.LINE_STIPPLE_FACTOR, this.lineStippleFactor, lineStippleFactor,
                () -> this.lineStippleFactor = lineStippleFactor);
    }

    @Override
    public short getLineStipplePattern()
    {
        return lineStipplePattern;
    }

    @Override
    public void setLineStipplePattern(short lineStipplePattern)
    {
        propertyChangeSupport.updateAndFire(ShapeProperties.LINE_STIPPLE_PATTERN, this.lineStipplePattern, lineStipplePattern,
                () -> this.lineStipplePattern = lineStipplePattern);
    }

    @Override
    public Color getFillColor()
    {
        return new Color(fillColor, true);
    }

    @Override
    public void setFillColor(Color color)
    {
        final int newColor = color.getRGB();
        propertyChangeSupport.updateAndFire(ShapeProperties.FILL_COLOR, fillColor, newColor, () -> fillColor = newColor);
    }

    @Override
    public TextureFillParams getTextureFillParams()
    {
        return textureFillParams;
    }

    @Override
    public void setTextureFillParams(TextureFillParams textureFillParams)
    {
        propertyChangeSupport.updateAndFire(ShapeProperties.TEXTURE_FILL_PARAMS, this.textureFillParams, textureFillParams, () -> this.textureFillParams = textureFillParams);
    }

    @Override
    public int getOpacity()
    {
        return (int) Math.ceil(getFillColor().getAlpha() / 255.0 * 100);
    }

    @Override
    public void setOpacity(int opacity) throws IllegalArgumentException
    {
        if (opacity < 0 || opacity > 100)
        {
            throw new IllegalArgumentException("Opacity must be between 0 and 100, got: " + opacity);
        }

        propertyChangeSupport.updateAndFire(ShapeProperties.OPACITY, getOpacity(), opacity,
                () -> setFillColor(makeColorWithOpacity(getFillColor(), opacity)));
    }

    /**
     * Construct a Color object from the base (pure) color and adding in the opacity as the alpha channel.
     *
     * @param color base color
     * @param opacity opacity to set in alpha channel, value ranges from 0 to 100
     * @return newly minted Color object with alpha channel set to opacity scaled to 0 - 255
     */
    private Color makeColorWithOpacity(Color color, int opacity)
    {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) ((opacity / 100d) * 255));
    }

    @Override
    public boolean isVisible()
    {
        return visible;
    }

    @Override
    public boolean isOrientable()
    {
        return orientable;
    }

    @Override
    public void setOrientable(boolean orientable)
    {
        propertyChangeSupport.updateAndFire(ShapeProperties.ORIENTABLE, this.orientable, orientable,
                () -> this.orientable = orientable);
    }

    @Override
    public boolean isEditable()
    {
        return editable;
    }

    @Override
    public void setEditable(boolean editable)
    {
        propertyChangeSupport.updateAndFire(ShapeProperties.EDITABLE, this.editable, editable,
                () -> {
                    this.editable = editable;
                    setAllowDrag(editable); // Draggable follows from editable for shapes
                });
    }

    @Override
    public boolean isAllowDrag()
    {
        return allowDrag;
    }

    @Override
    public void setAllowDrag(boolean allowDrag)
    {
        propertyChangeSupport.updateAndFire(ShapeProperties.ALLOW_DRAG, this.allowDrag, allowDrag, () -> this.allowDrag = allowDrag);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <SMO extends IShapeMapObject<?>> SMO getShapeMapObject()
    {
        return (SMO) shapeMapObject;
    }

    @Override
    public void setPublished(boolean published)
    {
        propertyChangeSupport.updateAndFire(ShapeProperties.PUBLISHED, this.published, published,
                () -> {
                    this.published = published;
                    updateShapeGraphic();
                });
    }

    @Override
    public boolean isPublished()
    {
        return published;
    }

    @Override
    public String getKey()
    {
        return key;
    }

    @Override
    public void setKey(String key)
    {
        if (this.key != null) throw new IllegalStateException("'key' may not be changed.");
        this.key = key;
    }

    @Override
    public void setName(@Nonnull String name)
    {
        propertyChangeSupport.updateAndFire(ShapeProperties.NAME, this.name, name, () -> this.name = name);
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public void setDescription(@Nonnull String description)
    {
        final String descriptionToUse;

        if (description.length() > 255)
        {
            logger.warn("Description will be trimmed to 255 characters on {}", this);
            descriptionToUse = description.substring(0, 255);
        } else
        {
            descriptionToUse = description;
        }

        propertyChangeSupport.updateAndFire(IMapObject.DESCRIPTION, this.description, descriptionToUse,
                () -> this.description = descriptionToUse);
    }

    @Override
    public String getDescription()
    {
        return description;
    }

    @Override
    public void addShapeListener(@Nonnull IShapeListener shapeListener)
    {
        propertyChangeSupport.addPropertyChangeListener(shapeListener);
        shapeListenerCount++;
    }

    @Override
    public void removeShapeListener(@Nonnull IShapeListener shapeListener)
    {
        propertyChangeSupport.removePropertyChangeListener(shapeListener);
        shapeListenerCount--;
    }

    @Override
    public int getShapeListenerCount()
    {
        return shapeListenerCount;
    }

    @Override
    public String getType()
    {
        return getClass().getSimpleName();
    }

    @Override
    public void setVisible(boolean visible)
    {
        propertyChangeSupport.updateAndFire(ShapeProperties.VISIBILITY, this.visible, visible, () -> this.visible = visible);
    }

    @Override
    public boolean contains(ILocation location) throws OperationNotSupportedException
    {
        throw new OperationNotSupportedException();
    }

    @Override
    public boolean contains(IShape shape) throws OperationNotSupportedException
    {
        throw new OperationNotSupportedException();
    }

    @Override
    public boolean intersects(IShape shape) throws OperationNotSupportedException
    {
        throw new OperationNotSupportedException();
    }

    @Override
    public boolean canUseVerticalHeight()
    {
        return false;
    }

    @Override
    public void setVerticalHeight(double verticalHeight)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public double getVerticalHeight()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setVerticalHeightEnabled(boolean enable)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isVerticalHeightEnabled()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public AltitudeMode getAltitudeMode()
    {
        return altitudeMode;
    }

    @Override
    public void setAltitudeMode(@Nonnull AltitudeMode altitudeMode)
    {
        propertyChangeSupport.updateAndFire(ShapeProperties.ALTITUDE_MODE, this.altitudeMode, altitudeMode, () -> this.altitudeMode = altitudeMode);
    }

    @Override
    public void setRemarks(@Nonnull String remarks)
    {
        propertyChangeSupport.updateAndFire(ShapeProperties.REMARKS, this.remarks, remarks, () -> this.remarks = remarks);
    }

    @Override
    public String getRemarks()
    {
        return remarks;
    }

    /**
     * @since 5.7
     */
    @Nonnull
    @Override
    public PrioritizedPropertyChangeSupport getPropertyChangeListenerSupport()
    {
        return propertyChangeSupport;
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName() + "{name=" + name + ", key=" + key + ", SMO=" + shapeMapObject + " @ " + location + '}';
    }

    /**
     * Update the outline stipple pattern and fill pattern based on whether the shape is published.  If the shape is
     * published, then set the outline to stipple and fill to a crosshatch pattern, otherwise use a solid outline and
     * no fill.
     * @since 5.5 (moved from GlobeShapeMapObject)
     */
    private void updateShapeGraphic()
    {
        final boolean published = isPublished();
        final short stipplePattern = published ? DASHED_STIPPLE_PATTERN : DEFAULT_STIPPLE_PATTERN;
        final int stippleFactor = published ? DASHED_STIPPLE_FACTOR : DEFAULT_STIPPLE_FACTOR;

        TextureFillParams textureFillParams = published
                ? new TextureFillParams(CROSS_PATTERN_URI, TextureFillStyle.REPEATTYPE_REPEAT, TextureFillStyle.REPEATTYPE_REPEAT, 32, 32, 0, true)
                : null;

        setTextureFillParams(textureFillParams);
        setLineStipplePattern(stipplePattern);
        setLineStippleFactor(stippleFactor);
    }

    /**
     * Contains the parameters for rendering a textured fill pattern.
     *
     * @param uri         the uri of the texture image
     * @param repeatTypeX the repeat type for the x-axis, either {@link TextureFillStyle#REPEATTYPE_REPEAT} or {@link TextureFillStyle#REPEATTYPE_STRETCH}
     * @param repeatTypeY the repeat type for the y-axis, either {@link TextureFillStyle#REPEATTYPE_REPEAT} or {@link TextureFillStyle#REPEATTYPE_STRETCH}
     * @param width       texture width to repeat on the x-axis if applicable
     * @param height      texture height to repeat on the y-axis if applicable
     * @param rotation    the texture rotation
     * @param isAbsolute  {@code true} if rotation is absolute (relative to north) or {@code false} (relative to screen)
     * @since 5.5.1
     */
    public record TextureFillParams(String uri, int repeatTypeX, int repeatTypeY, int width, int height, float rotation, boolean isAbsolute)
    {

        private static final String ICONS_STR = ".jar!/icons/";
        private static final int ICONS_STR_LENGTH = ICONS_STR.length();

        public TextureFillParams {
            // We need to rewrite URIs that directly reference our platform jar.
            // This is needed because versions < 5.7.0 were using absolute jar references, which fail when crossing
            // versions because the jar file name changes.
            if (uri.contains("/platform-") && uri.contains(".jar!/"))
            {
                final int index = uri.indexOf(ICONS_STR);
                if (index != -1)
                {
                    uri = UIResources.getUrl(PLATFORM_ICONS, uri.substring(index + ICONS_STR_LENGTH)).toString();
                } else
                {
                    logger.warn("Don't know how to update URL '{}'; it may not be readable", uri);
                }
            }
        }
    }
}
