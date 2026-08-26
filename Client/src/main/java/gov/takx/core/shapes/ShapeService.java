package gov.takx.core.shapes;

import com.atakmap.map.elevation.ElevationManager;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.RemoveObserver;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.event.mapobject.MapObjectRemoved;
import gov.takx.api.globe.ICameraEye;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.IShapeArrowMapObject;
import gov.takx.api.mapobject.IShapeBullseyeMapObject;
import gov.takx.api.mapobject.IShapeCircleMapObject;
import gov.takx.api.mapobject.IShapeEllipseMapObject;
import gov.takx.api.mapobject.IShapeMapObject;
import gov.takx.api.mapobject.IShapePathMapObject;
import gov.takx.api.mapobject.IShapePolygonMapObject;
import gov.takx.api.mapobject.IShapeRangeFanMapObject;
import gov.takx.api.mapobject.IShapeRectangleMapObject;
import gov.takx.api.mapobject.IShapeSquareMapObject;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.milsym.IMilStd2525Service;
import gov.takx.api.milsym.IMilStd2525ServiceProvider;
import gov.takx.api.milsym.MilStd2525Configuration;
import gov.takx.api.shapes.IMutableArrow;
import gov.takx.api.shapes.IMutableBullseye;
import gov.takx.api.shapes.IMutableCircle;
import gov.takx.api.shapes.IMutableEllipse;
import gov.takx.api.shapes.IMutablePolygon;
import gov.takx.api.shapes.IMutablePolyline;
import gov.takx.api.shapes.IMutableRangeFan;
import gov.takx.api.shapes.IMutableRectangle;
import gov.takx.api.shapes.IMutableShape;
import gov.takx.api.shapes.IMutableSquare;
import gov.takx.api.shapes.IShape;
import gov.takx.api.shapes.IShapeFilter;
import gov.takx.api.shapes.IShapeFilterBuilder;
import gov.takx.api.shapes.IShapeService;
import gov.takx.api.shapes.IShapeServiceListener;
import gov.takx.api.shapes.NoSuchShapeException;
import gov.takx.api.shapes.ShapeAlreadyExistsException;
import gov.takx.commons.lang.ExceptionUtils;
import gov.takx.commons.utils.StringUtils;
import gov.takx.dataservices.shapes.AShape;
import gov.takx.dataservices.shapes.Arrow;
import gov.takx.dataservices.shapes.Bullseye;
import gov.takx.dataservices.shapes.Circle;
import gov.takx.dataservices.shapes.Ellipse;
import gov.takx.dataservices.shapes.Polygon;
import gov.takx.dataservices.shapes.Polyline;
import gov.takx.dataservices.shapes.PublishedShapeTracker;
import gov.takx.dataservices.shapes.RangeFan;
import gov.takx.dataservices.shapes.Rectangle;
import gov.takx.dataservices.shapes.ShapeFilterBuilder;
import gov.takx.dataservices.shapes.Square;
import gov.takx.platform.KeyUtils;
import gov.takx.platform.globe.CameraEyeFactory;
import gov.takx.platform.utils.UnitIdHelper;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Concrete implementation of the {@link IShapeService}.
 *
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class ShapeService implements IShapeService
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // All shape instance names have to be unique, so we provide a prefix unique to the client being
    // serviced, so they only have to provide a locally unique shape name.
    private final String shapePrefix;

    // Shapes created from this shape service instance
    private final Map<String, IMutableShape> ownedShapes = new ConcurrentHashMap<>();

    private final IShapeServiceListener publishedShapeListener = new PublishedShapeListener();

    private final List<IShapeServiceListener> registeredShapeServiceListeners = new CopyOnWriteArrayList<>();

    private final IMapObject owner;

    @Inject
    private PublishedShapeTracker publishedShapeTracker;

    @Inject
    private IMapObjectManager mapObjectManager;

    @Inject
    private IExtendedGlobeManager globeManager;

    @Inject
    private UnitIdHelper unitIdHelper;

    @Inject
    private IMilStd2525ServiceProvider milStd2525ServiceProvider;

    /**
     * Internal ShapeService constructor. Starting in 2.38, shape service instances should only be created by
     * {@link ShapeServiceFactory} so it can manage the collection of active shape services.
     *
     * @param owner  The owning map object
     * @param prefix The owner prefix
     */
    protected ShapeService(IMapObject owner, String prefix)
    {
        shapePrefix = prefix;
        this.owner = owner;

        logger.debug("ShapeService {} initialized for: {}", prefix, owner);
    }

    @PostConstruct
    private void postConstruct()
    {
        publishedShapeTracker.addShapeServiceListener(publishedShapeListener);
        reattachShapes(); // Re-initialize map of owned shapes
    }

    /**
     * Performs any necessary cleanup when the service is being disposed.
     */
    @RemoveObserver
    void dispose()
    {
        publishedShapeTracker.removeShapeServiceListener(publishedShapeListener);
        ownedShapes.clear();
    }

    /**
     * @return Our shape prefix
     * @since 3.0
     */
    String getShapePrefix()
    {
        return shapePrefix;
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    @Override
    public <T extends IMutableShape> T createMil2525Graphic(@Nonnull MilStd2525Configuration configuration, boolean published)
    {
        final var shape = configuration.shape();
        final var shapeType = shape.getType();

        // If the configuration doesn't specify a name, or it is already used, generate one from the shape's type
        final String name = configuration.name();

        if (StringUtils.isEmpty(name, true) || ownedShapes.containsKey(name))
        {
            shape.setName(shapeType + " " + unitIdHelper.getNextFriendlyUnitId(KeyUtils.TOOL_FAMILY, shapeType));
        }

        final ShapeOptions shapeOptions = new ShapeOptions(published, null, shapeMapObject -> {
            IMilStd2525Service service = milStd2525ServiceProvider.get2525ServiceForSymbol(configuration.symbolCode());
            service.setSymbolCode(shapeMapObject, configuration.symbolCode());
            configuration.modifiers().forEach(modifier -> service.setModifier(shapeMapObject, modifier.id(), modifier.value()));

            configuration.initializer().accept(shapeMapObject);
        });

        try
        {
            return (T) initAndAddShape(shape, shapeType, shapeOptions).getAssociatedShape();
        } catch (ShapeAlreadyExistsException e) // Should never really happen since we create a unique name above
        {
            throw new RuntimeException("Internal failure to create MIL2525 graphic", e);
        }
    }

    @Nonnull
    @Override
    public IShapeArrowMapObject createArrowMapObject(@Nonnull IMutableArrow arrow, @Nonnull ShapeOptions shapeOptions) throws IllegalArgumentException, ShapeAlreadyExistsException
    {
        return initAndAddShape(arrow, Arrow.class.getSimpleName(), shapeOptions);
    }

    @Nonnull
    @Override
    public IShapeBullseyeMapObject createBullseyeMapObject(@Nonnull IMutableBullseye bullseye, @Nonnull ShapeOptions shapeOptions) throws IllegalArgumentException, ShapeAlreadyExistsException
    {
        return initAndAddShape(bullseye, Bullseye.class.getSimpleName(), shapeOptions);
    }

    @Nonnull
    @Override
    public IShapeCircleMapObject createCircleMapObject(@Nonnull IMutableCircle circle, @Nonnull ShapeOptions shapeOptions)
            throws IllegalArgumentException, ShapeAlreadyExistsException
    {
        return initAndAddShape(circle, Circle.class.getSimpleName(), shapeOptions);
    }

    @Nonnull
    @Override
    public IShapeEllipseMapObject createEllipseMapObject(@Nonnull IMutableEllipse ellipse, @Nonnull ShapeOptions shapeOptions)
            throws IllegalArgumentException, ShapeAlreadyExistsException
    {
        return initAndAddShape(ellipse, Ellipse.class.getSimpleName(), shapeOptions);
    }

    @Nonnull
    @Override
    public IShapeSquareMapObject createSquareMapObject(@Nonnull IMutableSquare square, @Nonnull ShapeOptions shapeOptions)
            throws IllegalArgumentException, ShapeAlreadyExistsException
    {
        return initAndAddShape(square, Square.class.getSimpleName(), shapeOptions);
    }

    @Nonnull
    @Override
    public IShapeRectangleMapObject createRectangleMapObject(@Nonnull IMutableRectangle rectangle, @Nonnull ShapeOptions shapeOptions)
            throws IllegalArgumentException, ShapeAlreadyExistsException
    {
        return initAndAddShape(rectangle, Rectangle.class.getSimpleName(), shapeOptions);
    }

    @Nonnull
    @Override
    public IShapePathMapObject<?> createPolylineMapObject(@Nonnull IMutablePolyline polyline, @Nonnull ShapeOptions shapeOptions)
            throws IllegalArgumentException, ShapeAlreadyExistsException
    {
        return initAndAddShape(polyline, Polyline.class.getSimpleName(), shapeOptions);
    }

    @Nonnull
    @Override
    public IShapePolygonMapObject createPolygonMapObject(@Nonnull IMutablePolygon polygon, @Nonnull ShapeOptions shapeOptions)
            throws IllegalArgumentException, ShapeAlreadyExistsException
    {
        return initAndAddShape(polygon, Polygon.class.getSimpleName(), shapeOptions);
    }

    @Nonnull
    @Override
    public IShapeRangeFanMapObject createRangeFanMapObject(@Nonnull IMutableRangeFan rangeFan, @Nonnull ShapeOptions shapeOptions)
            throws IllegalArgumentException, ShapeAlreadyExistsException
    {
        return initAndAddShape(rangeFan, RangeFan.class.getSimpleName(), shapeOptions);
    }

    @Override
    public void deleteShape(@Nonnull String name) throws IllegalArgumentException, NoSuchShapeException
    {
        if (StringUtils.isEmpty(name)) throw new IllegalArgumentException("Shape name must be non-null and not empty");

        final var shape = ownedShapes.remove(name);
        if (shape == null) throw new NoSuchShapeException("Shape doesn't exist: " + name);

        mapObjectManager.deleteMapObject(shape.getShapeMapObject());
    }

    @Override
    public double getElevation(double latitude, double longitude)
    {
        final double elevationHae = ElevationManager.getElevation(latitude, longitude, null);
        return elevationHae - ElevationManager.getGeoidHeight(latitude, longitude);
    }

    @Override
    public void gotoShape(@Nonnull IShape shape)
    {
        final ILocation location = shape.getLocation();
        globeManager.getLastFocusedGlobe().goTo(location.getLatitude(), location.getLongitude());
    }

    @Override
    public void gotoShape(@Nonnull IShape shape, @Nonnull ICameraEye cameraEye)
    {
        final ICameraEye actualEye = CameraEyeFactory.createCameraEye(shape.getLocation(), cameraEye);
        globeManager.getLastFocusedGlobe().goTo(actualEye);
    }

    /**
     * Notified by {@link IMapObjectManager} when a map object is removed. If it's one of our shapes, remove it from our
     * map of owned shapes and fire the deletion event.
     *
     * @param event The event providing the removed map object
     * @since 2.38 - formerly clientMapObjectRemoved
     */
    private void onMapObjectRemoved(@Observes MapObjectRemoved event)
    {
        if (event.getMapObject() instanceof IShapeMapObject<?> shapeMapObject)
        {
            // If the shape is one of ours, but was published, then the PublishedShapeTracker will fire
            // the deletion event. So, avoid firing a duplicate event in that case.
            final IShape removedShape = removeOwnedShapeByMapObjectKey(shapeMapObject.getKey());

            if (removedShape != null && !removedShape.isPublished()) fireOnShapeDeletion(removedShape);
        }
    }

    /**
     * Find all the shapes that were created by this shape service in a previous project session and add it
     * to your map of owned shapes. We do this by finding all the map objects that have a unit ID that starts with
     * our prefix. For each shape we find, reconstitute the shape wrapper (subclass of {@link AShape}) for the
     * shape map object.
     */
    private void reattachShapes()
    {
        for (IMapObject mo : mapObjectManager.getMapObjects())
        {
            if (mo instanceof IShapeMapObject<?> shapeMapObject && mo.getUnitID().startsWith(shapePrefix))
            {
                logger.debug("connecting {} to {}", shapeMapObject, shapePrefix);
                ownedShapes.put(shapeMapObject.getName(), shapeMapObject.getAssociatedShape());
            }
        }
    }

    @Nullable
    @Override
    public IMutableShape getShape(@Nonnull String name) throws IllegalArgumentException
    {
        if (name.isBlank()) throw new IllegalArgumentException("'name' must be non-null and not empty");
        return ownedShapes.get(name);
    }

    @Override
    public void deleteAllShapes()
    {
        final var shapes = List.copyOf(ownedShapes.values());
        ownedShapes.clear();
        shapes.forEach(shape -> mapObjectManager.deleteMapObject(shape.getShapeMapObject()));
    }

    @Nonnull
    @Override
    public List<IShape> getShapes()
    {
        final var uniqueShapes = new HashSet<IShape>();
        uniqueShapes.addAll(publishedShapeTracker.getShapes());
        uniqueShapes.addAll(ownedShapes.values());
        return new ArrayList<>(uniqueShapes);
    }

    @Nonnull
    @Override
    public List<IShape> getShapes(@Nonnull IShapeFilter shapeFilter)
    {
        return getShapes().stream()
                .filter(shapeFilter::matches)
                .collect(Collectors.toList());
    }

    @Nonnull
    @Override
    public List<IMutableShape> getMutableShapes()
    {
        return new ArrayList<>(ownedShapes.values());
    }

    @Nonnull
    @Override
    public List<IMutableShape> getMutableShapes(@Nonnull IShapeFilter shapeFilter)
    {
        return ownedShapes.values().stream()
                .filter(shapeFilter::matches)
                .collect(Collectors.toList());
    }

    @Nonnull
    @Override
    public IShapeFilterBuilder createShapeFilterBuilder()
    {
        return new ShapeFilterBuilder();
    }

    @Override
    public void addShapeServiceListener(@Nonnull IShapeServiceListener listener)
    {
        registeredShapeServiceListeners.add(listener);
    }

    @Override
    public boolean removeShapeServiceListener(@Nonnull IShapeServiceListener listener)
    {
        return registeredShapeServiceListeners.remove(listener);
    }

    /**
     * Create and initialize the MapObject for the given shape model, and add it to our "owned" shapes.
     *
     * @param shape shape model to assign to the created shape map object
     * @param shapeType Type of shape to create
     * @param shapeOptions options for shape creation
     * @param <T> Inferred shape type
     * @return Initialized shape 
     * @throws IllegalArgumentException    if the shape name is {@code null} or empty
     * @throws ShapeAlreadyExistsException If the shape name has already been used
     * @see IShape#getName()
     * @since 5.2
     */
    @SuppressWarnings("unchecked")
    private <T extends IShapeMapObject<?>> T initAndAddShape(@Nonnull IMutableShape shape, @Nonnull String shapeType,
                                                             @Nonnull ShapeOptions shapeOptions)
            throws IllegalArgumentException, ShapeAlreadyExistsException
    {
        final String name = shape.getName();

        if (logger.isDebugEnabled()) logger.debug("Create {} shape '{}' at {}", shapeType, name, shape.getLocation());

        if (StringUtils.isEmpty(name, true)) throw new IllegalArgumentException("Shape name must be non-null and not empty");
        if (ownedShapes.containsKey(name)) throw new ShapeAlreadyExistsException("Shape with this name already exists: " + name);

        final String unitId = shapePrefix + name; // Make a unique unitId associating it with our instance prefix

        final MapObjectSpec spec = MapObjectSpec.builder(KeyUtils.TOOL_FAMILY, shapeType, unitId)
                .uid(unitId)
                .location(shape.getLocation())
                .name(name)
                .owner(owner)
                .mapObjectOptions(shapeOptions.mapObjectOptions())
                .initializer(mo -> {
                    final var smo = (IShapeMapObject<?>) mo;
                    shape.setKey(smo.getKey());
                    smo.setAssociatedShape(shape);
                    smo.setPublished(shapeOptions.published());
                    smo.setEditable(false);   // API created shapes default to non-editable

                    shapeOptions.initializer().accept(smo);
                })
                .build();

        final var mapObject = mapObjectManager.getOrCreateMapObject(spec);

        ownedShapes.put(name, shape);
        return (T) mapObject;
    }

    /**
     * Remove an owned shape using its MapObject key. If no matching entry is found, then return {@code null}.
     *
     * @param key the unique key of the MapObject of the shape to remove
     * @return The shape removed from the ownedShapes collection, or {@code null} if not found
     */
    private IMutableShape removeOwnedShapeByMapObjectKey(String key)
    {
        for (var entry : ownedShapes.entrySet())
        {
            final IShape shape = entry.getValue();
            if (shape.getKey().equals(key))
            {
                logger.debug("{} Remove owned shape: {}", shapePrefix, key);
                return ownedShapes.remove(entry.getKey());
            }
        }

        return null; // Not found
    }

    /**
     * Fires onShapeDeletion for all registered listeners
     *
     * @param shape The shape being deleted
     */
    private void fireOnShapeDeletion(IShape shape)
    {
        ExceptionUtils.safeDoOperation(registeredShapeServiceListeners, listener -> listener.onShapeDeletion(shape));
    }

    /**
     * Listener to receive notifications from the PublishedShapeTracker and re-fire the events to all our listeners.
     */
    private final class PublishedShapeListener implements IShapeServiceListener
    {
        @Override
        public void onShapePublished(IShape shape)
        {
            ExceptionUtils.safeDoOperation(registeredShapeServiceListeners, listener -> listener.onShapePublished(shape));
        }

        @Override
        public void onShapeUnpublished(IShape shape)
        {
            ExceptionUtils.safeDoOperation(registeredShapeServiceListeners, listener -> listener.onShapeUnpublished(shape));
        }

        @Override
        public void onShapeDefined(IShape shape)
        {
            ExceptionUtils.safeDoOperation(registeredShapeServiceListeners, listener -> listener.onShapeDefined(shape));
        }

        @Override
        public void onShapeDeletion(IShape shape)
        {
            fireOnShapeDeletion(shape);
        }
    }
}
