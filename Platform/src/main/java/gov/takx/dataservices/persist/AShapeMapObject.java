package gov.takx.dataservices.persist;

import com.atakmap.coremap.maps.coords.GeoBounds;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.annotation.Internal;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.geofence.GeoFence;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IShapeMapObject;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.IRaptorPlugin;
import gov.takx.api.shapes.IMutableShape;
import gov.takx.api.shapes.IShape;
import gov.takx.api.shapes.IShapeListener;
import gov.takx.api.shapes.ShapeProperties;
import gov.takx.api.shapes.ShapeType;
import gov.takx.dataservices.shapes.AShape.TextureFillParams;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Transient;
import org.hibernate.Length;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.OperationNotSupportedException;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import java.util.Map;

/**
 */
@Internal
@Entity
public abstract class AShapeMapObject<T extends IMutableShape> extends MapObject
        implements IShapeMapObject<T>, PropertyChangeListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Column(length = Length.LONG32)
    private String modelJson = "";

    @Transient
    protected T shape;

    @Column(length = Length.LONG32)
    private String geoFenceJson;

    @Transient
    private GeoFence geoFence;

    // Supports linking the center of this shape to a given map object
    @Transient
    protected IMapObject centerAnchor;

    @Transient
    private LocationMonitor centerAnchorLocationMonitor;

    @Transient
    private Map<Integer, Double> milStdGsdHintMap;

    @Transient
    private Map<Integer, GeoBounds> milStdSceneBoundsHintMap;

    protected Integer centerAnchorId;

    protected AShapeMapObject()
    {
    }

    protected AShapeMapObject(MapObjectSpec mapObjectSpec, IPluginDefinition<? extends IRaptorPlugin> pluginDefinition)
    {
        super(mapObjectSpec, pluginDefinition);

        // In the default (just created) case, we don't have a need for the imageURI (since the image comes from
        // the pluginDefinition).  However, the way that the base class initializes leaves imageURI_t with a
        // non-null value.  This interferes the logic in loadMainIcon below, preventing the icon from being created
        // using the fill color of this object.  So, null it out.

        //noinspection AssignmentToSuperclassField
        imageURI_t = null;

        // Construct an empty shape model
        shape = ShapeType.valueForType(getType()).createEmptyModel();

        //noinspection OverriddenMethodCallDuringObjectConstruction
        prepareShape();
    }

    /**
     * Performs any shape initialization needed after being reified from JSON, or when creating a new map object.
     *
     * @since 3.3
     */
    protected void prepareShape()
    {
        final String name = getName();
        if (name != null) shape.setName(name);

        final GeoPoint location = getLocation();

        // We need to get the location on ourselves and on our shape in sync.  However, we don't want to transfer
        // an invalid location in either direction.
        if (!GeoPoint.UNKNOWN_POINT.sameAs(location))
        {
            shape.setLocation(location);
        } else
        {
            final var shapeLocation = shape.getLocation();
            if (!GeoPoint.UNKNOWN_POINT.sameAs(shapeLocation)) super.setLocation(shapeLocation);
        }

        shape.setShapeMapObject(this);
        shape.addPropertyChangeListener(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setAssociatedShape(@Nonnull IMutableShape associatedShape)
    {
        if (shape != null) shape.removePropertyChangeListener(this);

        shape = (T) associatedShape;
        shape.setShapeMapObject(this);
        shape.addPropertyChangeListener(this);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        final String propertyName = evt.getPropertyName();

        //noinspection SwitchStatementWithoutDefaultBranch
        switch (propertyName)
        {
            case ShapeProperties.ALLOW_DRAG -> super.setAllowDrag((boolean) evt.getNewValue());
            case ShapeProperties.LOCATION -> super.setLocation((ILocation) evt.getNewValue());
            case ShapeProperties.NAME -> super.setName((String) evt.getNewValue());
            case ShapeProperties.VISIBILITY -> super.setVisible((boolean) evt.getNewValue());
            case ShapeProperties.REMARKS -> super.setRemarks((String) evt.getNewValue());
        }

        // Re-fire the event, with ourselves as the source
        final PropertyChangeEvent newPce = new PropertyChangeEvent(this, propertyName, evt.getOldValue(), evt.getNewValue());
        propertyChangeSupport.firePropertyChange(newPce);
    }

    @Override
    public void setShapeMapObject(IShapeMapObject<?> shapeMapObject)
    {
        logger.error("'setShapeMapObject' should never be called on a Map Object: {} given {}", this, shapeMapObject);
    }

    @Override
    @Nullable
    public GeoFence getGeoFence()
    {
        return geoFence;
    }

    @Override
    public void setGeoFence(@Nullable GeoFence geoFence) throws IllegalArgumentException
    {
        if (geoFence != null && !getUid().equals(geoFence.shapeUid()))
        {
            throw new IllegalArgumentException("geoFence UID does not match: " + getUid() + " != " + geoFence.shapeUid());
        }
        propertyChangeSupport.updateAndFire(ShapeProperties.GEOFENCE, this.geoFence, geoFence, () -> this.geoFence = geoFence);
    }

    /**
     * Overridden to also remove all shape listeners.
     */
    @Override
    protected void onDelete()
    {
        super.onDelete();
    }

    @SuppressWarnings("JpaEntityListenerInspection")
    @PostLoad
    @Override
    protected void onLoad()
    {
        super.onLoad();

        geoFence = geoFenceJson == null ? null : GeoFenceObjectMapper.fromJson(geoFenceJson);

        final ShapeType shapeType = ShapeType.valueForType(getType());
        final var shapeModelClass = shapeType.getShapeModelClass();

        try
        {
            shape = ShapeObjectMapper.jsonToShape(shapeModelClass, modelJson);
        } catch (Exception e)
        {
            // Don't allow exceptions here to completely hose the reloading of all Map Objects.
            // Just report it and install an empty shape model.

            logger.error("Failed to reify a shape model for type {}", shapeModelClass, e);
            shape = shapeType.createEmptyModel();
        }

        prepareShape();
    }

    @PrePersist
    @PreUpdate
    protected void onSave()
    {
        modelJson = ShapeObjectMapper.shapeToJson(shape);
        geoFenceJson = geoFence == null ? null : GeoFenceObjectMapper.toJson(geoFence);
    }

    /**
     * @since 3.4
     */
    @Override
    public void preForceUpdate()
    {
        onSave(); // get the model JSON up to date
    }

    @Override
    public void onAllLoaded()
    {
        super.onAllLoaded();

        relinkMapObject(centerAnchorId, "center anchor", this::setCenterAnchor);
    }

    @Override
    public void setCenterAnchor(@Nullable IMapObject centerAnchor)
    {
        final IMapObject oldAnchor = this.centerAnchor;

        if (this.centerAnchor != null)
        {
            centerAnchorLocationMonitor.dispose();
            centerAnchorLocationMonitor = null;
            centerAnchorId = null;
        }

        this.centerAnchor = centerAnchor;

        if (centerAnchor != null)
        {
            centerAnchorId = centerAnchor.getId();

            setLocation(centerAnchor.getLocation()); // Install location from marker

            // Track its location so we can adjust our center position if/when it is moved
            centerAnchorLocationMonitor = new LocationMonitor(centerAnchor, this::setLocation);
        }

        if (oldAnchor != centerAnchor)
        {
            propertyChangeSupport.firePropertyChange(ShapeProperties.CENTER_ANCHOR,
                    oldAnchor == null ? null : oldAnchor.getKey(),
                    centerAnchor == null ? null : centerAnchor.getKey());
        }
    }

    @Nullable
    @Override
    public IMapObject getCenterAnchor()
    {
        return centerAnchor;
    }

    @Nonnull
    @Override
    public T getAssociatedShape()
    {
        return shape;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <SMO extends IShapeMapObject<?>> SMO getShapeMapObject()
    {
        return (SMO) this;
    }

    @Override
    public void setKey(String key)
    {
        logger.warn("setKey [{}] called on {}", key, this);
    }

    @Override
    public void setName(@Nonnull String name)
    {
        shape.setName(name);
    }

    @Override
    public void setTitle(String title)
    {
        if (title != null)
        {
            setMetaString("shapeName", title);
            setName(title);
            super.setTitle(title);
        } else
        {
            logger.warn("Tried to set NULL title on {}", this);
        }
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
    public boolean canUseVerticalHeight()
    {
        return false;
    }

    @Override
    public Color getLineColor()
    {
        return shape.getLineColor();
    }

    @Override
    public void setLineColor(Color color)
    {
        shape.setLineColor(color);
    }

    @Override
    public double getLineWidth()
    {
        return shape.getLineWidth();
    }

    @Override
    public void setLineWidth(double lineWidth)
    {
        shape.setLineWidth(lineWidth);
    }

    @Override
    public int getLineStippleFactor()
    {
        return shape.getLineStippleFactor();
    }

    @Override
    public void setLineStippleFactor(int stippleFactor)
    {
        shape.setLineStippleFactor(stippleFactor);
    }

    @Override
    public short getLineStipplePattern()
    {
        return shape.getLineStipplePattern();
    }

    @Override
    public void setLineStipplePattern(short stipplePattern)
    {
        shape.setLineStipplePattern(stipplePattern);
    }

    @Override
    public Color getFillColor()
    {
        return shape.getFillColor();
    }

    @Override
    public void setFillColor(Color color)
    {
        shape.setFillColor(color);
    }

    @Override
    public TextureFillParams getTextureFillParams()
    {
        return shape.getTextureFillParams();
    }

    @Override
    public void setTextureFillParams(TextureFillParams params)
    {
        shape.setTextureFillParams(params);
    }

    @Override
    public int getOpacity()
    {
        return shape.getOpacity();
    }

    @Override
    public void setOpacity(int opacity)
    {
        shape.setOpacity(opacity);
    }

    @Override
    public void setPublished(boolean published)
    {
        shape.setPublished(published);
    }

    @Override
    public boolean isPublished()
    {
        return shape.isPublished();
    }

    @Override
    public boolean isOrientable()
    {
        return shape.isOrientable();
    }

    @Override
    public void setOrientable(boolean orientable)
    {
        shape.setOrientable(orientable);
    }

    @Override
    public boolean isEditable()
    {
        return shape.isEditable();
    }

    @Override
    public void setEditable(boolean editable)
    {
        shape.setEditable(editable);
    }

    @Override
    public boolean isAllowDrag()
    {
        return shape.isAllowDrag();
    }

    @Override
    public void setAllowDrag(boolean allowDrag)
    {
        shape.setAllowDrag(allowDrag);
    }

    @Override
    public void addShapeListener(@Nonnull IShapeListener shapeListener)
    {
        shape.addShapeListener(shapeListener);
    }

    @Override
    public void removeShapeListener(@Nonnull IShapeListener shapeListener)
    {
        shape.removeShapeListener(shapeListener);
    }

    @Override
    public int getShapeListenerCount()
    {
        return shape.getShapeListenerCount();
    }

    /**
     * Overridden to expose for public API, as setLocation in MapObject is protected.
     */
    @Override
    public void setLocation(ILocation location)
    {
        // The same logic is in MapObject now, so just expose it publicly to fulfill the IMutableShape interface
        super.setLocation(location);
    }

    @Override
    protected boolean internalSetLocation(ILocation location, long time, boolean manualLocation)
    {
        if (!super.internalSetLocation(location, time, manualLocation)) return false;

        shape.setLocation(location);

        // Update center marker
        if (centerAnchor != null && !centerAnchor.getLocation().sameAs(location))
        {
            centerAnchor.getController().sendLocationChangeMessage(location);
        }

        return true;
    }

    @Override
    public boolean isVisible()
    {
        return shape.isVisible();
    }

    @Override
    public void setVisible(boolean visible)
    {
        shape.setVisible(visible);
    }

    @Override
    public boolean contains(ILocation location) throws OperationNotSupportedException
    {
        return shape.contains(location);
    }

    @Override
    public boolean contains(IShape shape) throws OperationNotSupportedException
    {
        return this.shape.contains(shape);
    }

    @Override
    public boolean intersects(IShape shape)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString()
    {
        return getName();
    }

    @Override
    public void shutdown()
    {
        if (centerAnchorLocationMonitor != null)
        {
            centerAnchorLocationMonitor.dispose();
            centerAnchorLocationMonitor = null;
        }

        shape.removePropertyChangeListener(this);
        super.shutdown();
    }

    @Override
    public AltitudeMode getAltitudeMode()
    {
        return shape.getAltitudeMode();
    }

    @Override
    public void setAltitudeMode(@Nonnull AltitudeMode altitudeMode)
    {
        shape.setAltitudeMode(altitudeMode);
    }

    @Override
    public void setRemarks(@Nonnull String remarks)
    {
        shape.setRemarks(remarks);
    }

    @Override
    public String getRemarks()
    {
        return shape.getRemarks();
    }

    /**
     * Gets a map of globe ids to GSD values. This is used by the milstd renderer to intelligently size the resulting features.
     *
     * @return the map, or {@code null} if not set
     * @since 5.5
     */
    @Nullable
    public Map<Integer, Double> getMilStdGsdHintMap()
    {
        return milStdGsdHintMap;
    }

    /**
     * Set the map of globe ids to GSD values for the milstd renderer.
     *
     * @param milStdGsdHintMap the map, or {@code null} if not set
     * @since 5.5
     */
    public void setMilStdGsdHintMap(@Nullable Map<Integer, Double> milStdGsdHintMap)
    {
        this.milStdGsdHintMap = milStdGsdHintMap;
    }

    /**
     * Gets a map of globe ids to visible {@link GeoBounds}. This is used by the milstd renderer to intelligently size the resulting features.
     *
     * @return the map, or {@code null} if not set
     * @since 5.5
     */
    @Nullable
    public Map<Integer, GeoBounds> getMilStdSceneBoundsHintMap()
    {
        return milStdSceneBoundsHintMap;
    }

    /**
     * Set the map of globe ids to visible {@link GeoBounds} for the milstd renderer.
     *
     * @param milStdSceneBoundsHintMap the map, or {@code null} if not set
     * @since 5.5
     */
    public void setMilStdSceneBoundsHintMap(@Nullable Map<Integer, GeoBounds> milStdSceneBoundsHintMap)
    {
        this.milStdSceneBoundsHintMap = milStdSceneBoundsHintMap;
    }
}
