package gov.takx.core.globe;

import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IGlobeShapeMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IShapeMapObject;
import gov.takx.api.mapobject.MapToolType;
import gov.takx.api.shapes.IMutableShape;
import gov.takx.api.shapes.ShapeProperties;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;

import static jakarta.interceptor.Interceptor.Priority.PLATFORM_BEFORE;

/**
 * Globe object representing a surface shape.
 *
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class GlobeShapeMapObject extends AGlobeMapObject implements IGlobeShapeMapObject, PropertyChangeListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String IS_EDITING = "isEditing";

    private IMutableShape shapeModel;
    private boolean editing;
    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
    private final IMapObject mapObject;
    private final MapToolType mapToolType;

    private static final List<String> mapObjectPropertiesToWatch = List.of(
            IMapObject.NAME,
            IMapObject.ATTRIBUTE_SET);

    // NOTE: there are properties in this list that are not handled locally, but MUST be in the list since all
    // shape wrappers are listeners on this object, and we re-fire events to all listeners.
    private static final List<String> propertiesToWatch = List.of(
            IMapObject.NAME,
            ShapeProperties.ANGLE,
            ShapeProperties.EDITABLE,
            ShapeProperties.FILL_COLOR,
            ShapeProperties.GROUND_REFERENCE,
            ShapeProperties.HEIGHT,
            ShapeProperties.HOLE_ADDED,
            ShapeProperties.HOLE_REMOVED,
            ShapeProperties.LINE_COLOR,
            ShapeProperties.LINE_WIDTH,
            ShapeProperties.LOCATION,
            ShapeProperties.LOCATIONS,
            ShapeProperties.SEMI_MAJOR_AXIS,
            ShapeProperties.SEMI_MINOR_AXIS,
            ShapeProperties.ORIENTABLE,
            ShapeProperties.ORIENTATION,
            ShapeProperties.PUBLISHED,
            ShapeProperties.RADIUS,
            ShapeProperties.RING_COUNT,
            ShapeProperties.SWEEP,
            ShapeProperties.VERTICAL_HEIGHT, // unused locally
            ShapeProperties.VERTICAL_HEIGHT_ENABLED, // unused locally
            ShapeProperties.VISIBILITY, // unused locally
            ShapeProperties.WIDTH);

    /**
     * Construct an editable surface shape.  This constructor is used when the user is manually drawing a shape, or we
     * are restoring a shape from persistence. In the latter case, the {@link #setMapObject(IMapObject)}
     * method must be called to restore the properties of the shape.
     *
     * @param mapToolType Type of shape to construct
     */
    public GlobeShapeMapObject(@Nonnull MapToolType mapToolType)
    {
        this(mapToolType, null);
    }

    /**
     * Construct and initialize with a surface shape at the given location according to the map object values.
     * This constructor is used when shapes are being created programmatically, such as with the ShapeService.
     * The editability of the shape will come from the mapObject setting.
     *
     * @param mapObject The source map object
     */
    public GlobeShapeMapObject(@Nonnull IMapObject mapObject)
    {
        this(MapToolType.valueOfShapeType(mapObject.getType()), mapObject);
    }

    /**
     * Internal constructor.
     *
     * @param mapToolType Tool type (shape)
     * @param mapObject MO instance, may be {@code null}
     */
    private GlobeShapeMapObject(@Nonnull MapToolType mapToolType, @Nullable IMapObject mapObject)
    {
        super(null);
        this.mapObject = mapObject;
        this.mapToolType = mapToolType;
    }

    /**
     * Ensure globeManager is injected before registering MO PCL that may call it.
     *
     * @since 5.3
     */
    @PostConstruct
    private void postConstruct()
    {
        if (mapObject != null)
        {
            setMapObject(mapObject);
        } else
        {
            setEditable(true);
        }
    }

    @Override
    public void setMapObject(IMapObject mapObject)
    {
        logger.debug("Setting MO: {}", mapObject);

        IShapeMapObject<?> origMapObject = getMapObject();
        if (origMapObject != null) origMapObject.removePropertyChangeListener(getMapObjectPropertiesToWatch(), this);
        super.setMapObject(mapObject);
        IShapeMapObject<?> newMapObject = getMapObject();
        if (newMapObject != null) newMapObject.addPropertyChangeListener(getMapObjectPropertiesToWatch(), this);

        setShapeModel(getMapObject().getAssociatedShape());
    }

    @Override
    public IShapeMapObject<?> getMapObject()
    {
        return (IShapeMapObject<?>) super.getMapObject();
    }

    @Override
    public void setShapeModel(@Nonnull IMutableShape shapeModel)
    {
        logger.debug("Setting shape model: {}", shapeModel);
        if (this.shapeModel != null) this.shapeModel.removePropertyChangeListener(getShapePropertiesToWatch(), this);
        this.shapeModel = shapeModel;

        shapeModel.addPropertyChangeListener(getShapePropertiesToWatch(), this);
    }

    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public <T extends IMutableShape> T getShapeModel()
    {
        return (T) shapeModel;
    }

    @Override
    @Nonnull
    public <T extends IMutableShape> T requireShapeModel()
    {
        return Objects.requireNonNull(getShapeModel(), "shape model is null");
    }

    @Override
    public void peerRemoved()
    {
        super.peerRemoved();
        if (shapeModel != null) shapeModel.removePropertyChangeListener(getShapePropertiesToWatch(), this);
        if (getMapObject() != null) getMapObject().removePropertyChangeListener(getMapObjectPropertiesToWatch(), this);
    }

    @Override
    public boolean isEditable()
    {

        return shapeModel != null && shapeModel.isEditable();
    }

    @Override
    public void setEditable(boolean editable)
    {
        if (shapeModel != null) shapeModel.setEditable(editable);
    }

    @Override
    public boolean isEditing()
    {
        return editing;
    }

    @Override
    public void setEditing(boolean editing)
    {
        if (editing == this.editing) return;
        this.editing = editing;
        firePropertyChange(IS_EDITING, !editing, editing);
    }

    @Override
    public boolean isViewable()
    {
        return shapeModel != null && requireShapeModel().isVisible();
    }

    @Nonnull
    @Override
    public ILocation getLocation()
    {
        return requireShapeModel().getLocation();
    }

    @InvokeOnEDT
    @Override
    public void setLocation(final ILocation location)
    {
        // Our map object has moved, so update the shape to track it
        ILocation oldLocation = requireShapeModel().getLocation();

        if (!oldLocation.equals(location))
        {
            logger.debug("Set position to: {}", location);
            requireShapeModel().setLocation(location);

            // Notify our listener that we've moved
            refirePropertyChanged(new PropertyChangeEvent(this, IShapeMapObject.LOCATION, oldLocation, location));
        }
    }

    @Nonnull
    @Override
    public MapToolType getMapToolType()
    {
        return mapToolType;
    }

    /**
     * Handle a property change on our peer map object.  Apply the new property value onto the globe shape.
     *
     * @param evt Property change event
     */
    @InvokeOnEDT
    @Override
    public void propertyChange(@Priority(PLATFORM_BEFORE) PropertyChangeEvent evt)
    {
        // Any property not called out specifically above doesn't need processing here, but we propagate
        // every property change on to our listener
        refirePropertyChanged(evt);
    }

    /**
     * Get a list of properties to watch on the {@link gov.takx.api.shapes.IShape}.
     * @return the list of properties to watch
     * @since 5.5
     */
    protected List<String> getShapePropertiesToWatch()
    {
        return propertiesToWatch;
    }

    /**
     * Get a list of properties to watch on the {@link IMapObject}.
     * @return the list of properties to watch
     */
    protected List<String> getMapObjectPropertiesToWatch()
    {
        return mapObjectPropertiesToWatch;
    }

    @Override
    public void addPropertyChangeListener(@Nonnull PropertyChangeListener listener)
    {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener, List<String> properties)
    {
        properties.forEach(p -> propertyChangeSupport.addPropertyChangeListener(p, listener));
    }

    @Override
    public void removePropertyChangeListener(@Nonnull PropertyChangeListener listener)
    {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener, List<String> properties)
    {
        properties.forEach(p -> propertyChangeSupport.removePropertyChangeListener(p, listener));
    }

    /**
     * Re-fire a property change on the EDT.
     *
     * @param evt Event to propagate
     */
    @InvokeOnEDT
    protected final void refirePropertyChanged(final PropertyChangeEvent evt)
    {
        propertyChangeSupport.firePropertyChange(evt);
    }

    @InvokeOnEDT
    private void firePropertyChange(String propertyName, Object oldValue, Object newValue)
    {
        propertyChangeSupport.firePropertyChange(new PropertyChangeEvent(this, propertyName, oldValue, newValue));
    }
}
