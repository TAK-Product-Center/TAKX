package gov.takx.api.shapes;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.ICameraEye;
import gov.takx.api.mapobject.IMapObjectController;
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
import gov.takx.api.mapobject.MapObjectOptions;
import gov.takx.api.milsym.MilStd2525Configuration;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.Color;
import java.util.List;
import java.util.function.Consumer;

/**
 * This interface defines a service for creating, finding, and removing shapes from within a plugin. Shape instances
 * are created with default values for color, opacity, and line width unless specifically overridden after
 * construction.
 * <p/>
 * To access a shape service, plugins can use a delegate for an app, device, or import/export plugin. Also, each
 * map object has an associated shape service which can be accessed via {@link IMapObjectController#getShapeService()}.
 * Finally, a shape service can be directly injected via CDI using the @ForPlugin annotation, as shown
 * in the following example:
 * <pre>{@code
 *     @Inject
 *     @ForPlugin(family = "AppFamily", type = "AppType", category = PluginCategory.AppPlugin)
 *     private IShapeService shapeService;
 * }</pre>
 * <p>
 * Note: TAKX only supports injecting a shape service for an app or import/export plugin.
 * To access the shape service associated with a device/map object, plugins can utilize
 * {@link IMapObjectController#getShapeService()}.
 * <p/>
 * Shapes are broken into two basic families: regular shapes defined by {@link IMutableRegularShape} and irregular
 * shapes defined by {@link IMutableIrregularShape}.
 * <p/>
 * All shapes created by a plugin/device instance must be given a unique name when they are created using
 * one of the factory methods defined here. It is illegal to use a shape name that is {@code null} or empty.
 * <p/>
 * All shapes are created with a boolean flag, published. When this flag is set to true, any plugin has
 * access to an immutable reference to this shape. Having this immutable reference allows you to access to various getters,
 * and also allows you to remove or add listeners for shape property change events.
 * <p/>
 * Note: <strong>All create shape methods must be called on the Event Dispatch Thread.</strong>
 * <p/>
 * This service allows you to get a complete or filtered list of immutable shapes which currently have their
 * published flag set to true. These shapes include any shapes created within the project session. A list of mutable
 * shapes that your instance of the shape service created, can also be obtained.
 * <p/>
 * This service will ensure that the name is unique among all objects on the map by automatically prefixing the name
 * supplied during creation with the device type and unit ID of the instance creating the shape.  For example, if
 * a device instance of type {@code ACME} with a unit ID of {@code 1} creates a circle with the name
 * {@code geoFence}, then the full name of the created map object is {@code Circle_ACME1_geofence}.  If
 * another instance of the ACME device, say with a unit ID of {@code 2}, also creates a circle with the name
 * {@code geoFence}, then the full name of that shape would be {@code Circle_ACME2_geoFence} and there is
 * no global name collision.
 * All of this is managed automatically by the shape service, so the calling device instance need only keep track of
 * the simple name used during creation (e.g., {@code geoFence}).
 * <p/>
 * A plugin can also register as a shape service listener in order to be notified of a user deletion of a programmatic
 * (API) created shape.
 * <p/>
 * <strong>NOTE: As of 5.2, the preferred method for creating a shape Map Object is to construct a shape model, and
 * then call the appropriate create method here.  The methods accepting individual shape properties will be phased out.
 * The {@link ShapeOptions} construct combines the published setting along with options and an initializer for use when
 * a {@link gov.takx.api.mapobject.IMapObject Map Object} is created from the model.
 * <p/>
 * Note further: the new create methods return the {@link IShapeMapObject Map Object} constructed, not the shape model
 * passed in.
 * </strong>
 *
 * @see ShapeOptions
 * @see #createArrowMapObject(IMutableArrow, ShapeOptions)
 * @see #createCircleMapObject(IMutableCircle, ShapeOptions)
 * @see #createEllipseMapObject(IMutableEllipse, ShapeOptions)
 * @see #createPolygonMapObject(IMutablePolygon, ShapeOptions)
 * @see #createPolylineMapObject(IMutablePolyline, ShapeOptions)
 * @see #createRangeFanMapObject(IMutableRangeFan, ShapeOptions)
 * @see #createRectangleMapObject(IMutableRectangle, ShapeOptions)
 * @see #createSquareMapObject(IMutableSquare, ShapeOptions)
 */
@SuppressWarnings({"UnusedDeclaration"})
@NoPluginImplementations
public interface IShapeService
{
    /**
     * Options to be used when creating a new shape.
     *
     * @param published whether this shape is published (a geofence)
     * @param mapObjectOptions Map object options to use to create the {@link gov.takx.api.mapobject.IMapObject Map Object}
     * @param initializer Initializer to apply to the {@link gov.takx.api.mapobject.IMapObject Map Object} created from these options
     * @since 5.2
     */
    record ShapeOptions(boolean published, @Nullable MapObjectOptions mapObjectOptions, @Nonnull Consumer<IShapeMapObject<?>> initializer)
    {

        /**
         * Options with {@code published = false}, no specified {@link MapObjectOptions}, and a noop initializer.
         */
        public static final ShapeOptions UNPUBLISHED = new ShapeOptions(false);

        /**
         * Options with {@code published = true}, no specified {@link MapObjectOptions}, and a noop initializer.
         */
        public static final ShapeOptions PUBLISHED = new ShapeOptions(true);

        /**
         * Create options with no specified {@link MapObjectOptions} and a noop initializer.
         *
         * @param published whether this shape is published (a geofence)
         */
        public ShapeOptions(boolean published)
        {
            this(published, null);
        }

        /**
         * Create options with a noop initializer.
         *
         * @param published whether this shape is published (a geofence)
         * @param mapObjectOptions Map object options to apply to the {@link gov.takx.api.mapobject.IMapObject Map Object} created
         */
        public ShapeOptions(boolean published, @Nullable MapObjectOptions mapObjectOptions)
        {
            this(published, mapObjectOptions, s -> {
            });
        }
    }

    /**
     * The default opacity for the line/fill colors of all shapes.
     */
    int DEFAULT_OPACITY = 70;

    /**
     * The default color for all shapes.
     */
    Color DEFAULT_COLOR = Color.cyan;

    /**
     * The default line width.
     */
    double DEFAULT_LINE_WIDTH = 3.0;

    /**
     * Create a new MIL2525 tactical graphic shape defined by the given {@code configuration}.  By default,
     * the created shape is NOT published.
     *
     * @param configuration Configuration defining the properties of the shape/graphic to create
     * @return new shape
     * @param <T> Inferred type of the shape
     * @see #createMil2525Graphic(MilStd2525Configuration, boolean)
     * @since 5.1
     */
    @Nonnull
    default <T extends IMutableShape> T createMil2525Graphic(@Nonnull MilStd2525Configuration configuration)
    {
        return createMil2525Graphic(configuration, false);
    }

    /**
     * Create a new MIL2525 tactical graphic shape defined by the given {@code configuration}.
     *
     * @param configuration Configuration defining the properties of the shape/graphic to create
     * @param published whether this shape is published
     * @return new shape
     * @param <T> Inferred type of the shape
     * @since 5.1
     */
    @Nonnull
    <T extends IMutableShape> T createMil2525Graphic(@Nonnull MilStd2525Configuration configuration, boolean published);

    /**
     * Create a new unpublished arrow from the given shape model.
     *
     * @param arrow model defining the shape properties
     * @return the original shape model
     * @throws IllegalArgumentException    If the name is {@code null} or empty
     * @throws ShapeAlreadyExistsException If the name is not unique among all shapes that have been created by a single plugin instance
     * @see gov.takx.dataservices.shapes.Arrow
     * @since 5.2
     */
    @Nonnull
    default IShapeArrowMapObject createArrowMapObject(@Nonnull IMutableArrow arrow) throws IllegalArgumentException, ShapeAlreadyExistsException
    {
        return createArrowMapObject(arrow, ShapeOptions.UNPUBLISHED);
    }

    /**
     * Create a new arrow from the given shape model.
     *
     * @param arrow model defining the shape properties
     * @param shapeOptions options to apply when creating the shape
     * @return new shape
     * @throws IllegalArgumentException    If the name is {@code null} or empty
     * @throws ShapeAlreadyExistsException If the name is not unique among all shapes that have been created by a single plugin instance
     * @see gov.takx.dataservices.shapes.Arrow
     * @since 5.2
     */
    @Nonnull
    IShapeArrowMapObject createArrowMapObject(@Nonnull IMutableArrow arrow, @Nonnull ShapeOptions shapeOptions)
            throws IllegalArgumentException, ShapeAlreadyExistsException;

    /**
     * Create a new unpublished bullseye from the given shape model.
     *
     * @param bullseye model defining the shape properties
     * @return new shape
     * @throws IllegalArgumentException    If the name is {@code null} or empty
     * @throws ShapeAlreadyExistsException If the name is not unique among all shapes that have been created by a single plugin instance
     * @see gov.takx.dataservices.shapes.Bullseye
     * @since 5.5
     */
    @Nonnull
    default IShapeBullseyeMapObject createBullseyeMapObject(@Nonnull IMutableBullseye bullseye) throws IllegalArgumentException, ShapeAlreadyExistsException
    {
        return createBullseyeMapObject(bullseye, ShapeOptions.UNPUBLISHED);
    }

    /**
     * Create a new bullseye from the given shape model.
     *
     * @param bullseye model defining the shape properties
     * @param shapeOptions options to apply when creating the shape
     * @return new shape
     * @throws IllegalArgumentException    If the name is {@code null} or empty
     * @throws ShapeAlreadyExistsException If the name is not unique among all shapes that have been created by a single plugin instance
     * @see gov.takx.dataservices.shapes.Bullseye
     * @since 5.5
     */
    @Nonnull
    IShapeBullseyeMapObject createBullseyeMapObject(@Nonnull IMutableBullseye bullseye, @Nonnull ShapeOptions shapeOptions)
            throws IllegalArgumentException, ShapeAlreadyExistsException;

    /**
     * Create a new unpublished circle from the given shape model.
     *
     * @param circle model defining the shape properties
     * @return new shape
     * @throws IllegalArgumentException    If the name is {@code null} or empty
     * @throws ShapeAlreadyExistsException If the name is not unique among all shapes that have been created by a single plugin instance
     * @see gov.takx.dataservices.shapes.Circle
     * @since 5.2
     */
    @Nonnull
    default IShapeCircleMapObject createCircleMapObject(@Nonnull IMutableCircle circle) throws IllegalArgumentException, ShapeAlreadyExistsException
    {
        return createCircleMapObject(circle, ShapeOptions.UNPUBLISHED);
    }

    /**
     * Create a new circle from the given shape model.
     *
     * @param circle       model defining the shape properties
     * @param shapeOptions options to apply when creating the shape
     * @return new shape
     * @throws IllegalArgumentException    If the name is {@code null} or empty
     * @throws ShapeAlreadyExistsException If the name is not unique among all shapes that have been created by a single plugin instance
     * @see gov.takx.dataservices.shapes.Circle
     * @since 5.2
     */
    @Nonnull
    IShapeCircleMapObject createCircleMapObject(@Nonnull IMutableCircle circle, @Nonnull ShapeOptions shapeOptions)
            throws IllegalArgumentException, ShapeAlreadyExistsException;

    /**
     * Create a new unpublished ellipse from the given shape model.
     *
     * @param ellipse model defining the shape properties
     * @return new shape
     * @throws IllegalArgumentException    If the name is {@code null} or empty
     * @throws ShapeAlreadyExistsException If the name is not unique among all shapes that have been created by a single plugin instance
     * @see gov.takx.dataservices.shapes.Ellipse
     * @since 5.2
     */
    @Nonnull
    default IShapeEllipseMapObject createEllipseMapObject(@Nonnull IMutableEllipse ellipse) throws IllegalArgumentException, ShapeAlreadyExistsException
    {
        return createEllipseMapObject(ellipse, ShapeOptions.UNPUBLISHED);
    }

    /**
     * Create a new ellipse from the given shape model.
     *
     * @param ellipse model defining the shape properties
     * @param shapeOptions options to apply when creating the shape
     * @return new shape
     * @throws IllegalArgumentException    If the name is {@code null} or empty
     * @throws ShapeAlreadyExistsException If the name is not unique among all shapes that have been created by a single plugin instance
     * @see gov.takx.dataservices.shapes.Ellipse
     * @since 5.2
     */
    @Nonnull
    IShapeEllipseMapObject createEllipseMapObject(@Nonnull IMutableEllipse ellipse, @Nonnull ShapeOptions shapeOptions)
            throws IllegalArgumentException, ShapeAlreadyExistsException;

    /**
     * Create a new unpublished polygon from the given shape model.
     *
     * @param polygon model defining the shape properties
     * @return new shape
     * @throws IllegalArgumentException    If the name is {@code null} or empty
     * @throws ShapeAlreadyExistsException If the name is not unique among all shapes that have been created by a single plugin instance
     * @see gov.takx.dataservices.shapes.Polygon
     * @since 5.2
     */
    @Nonnull
    default IShapePolygonMapObject createPolygonMapObject(@Nonnull IMutablePolygon polygon) throws IllegalArgumentException, ShapeAlreadyExistsException
    {
        return createPolygonMapObject(polygon, ShapeOptions.UNPUBLISHED);
    }

    /**
     * Create a new polygon from the given shape model.
     *
     * @param polygon model defining the shape properties
     * @param shapeOptions options to apply when creating the shape
     * @return new shape
     * @throws IllegalArgumentException    If the name is {@code null} or empty
     * @throws ShapeAlreadyExistsException If the name is not unique among all shapes that have been created by a single plugin instance
     * @see gov.takx.dataservices.shapes.Polygon
     * @since 5.2
     */
    @Nonnull
    IShapePolygonMapObject createPolygonMapObject(@Nonnull IMutablePolygon polygon, @Nonnull ShapeOptions shapeOptions)
            throws IllegalArgumentException, ShapeAlreadyExistsException;

    /**
     * Create a new unpublished polyline from the given shape model.
     *
     * @param polyline model defining the shape properties
     * @return new shape
     * @throws IllegalArgumentException    If the name is {@code null} or empty
     * @throws ShapeAlreadyExistsException If the name is not unique among all shapes that have been created by a single plugin instance
     * @see gov.takx.dataservices.shapes.Polyline
     * @since 5.2
     */
    @Nonnull
    default IShapePathMapObject<?> createPolylineMapObject(@Nonnull IMutablePolyline polyline) throws IllegalArgumentException, ShapeAlreadyExistsException
    {
        return createPolylineMapObject(polyline, ShapeOptions.UNPUBLISHED);
    }

    /**
     * Create a new polyline from the given shape model.
     *
     * @param polyline model defining the shape properties
     * @param shapeOptions options to apply when creating the shape
     * @return new shape
     * @throws IllegalArgumentException    If the name is {@code null} or empty
     * @throws ShapeAlreadyExistsException If the name is not unique among all shapes that have been created by a single plugin instance
     * @see gov.takx.dataservices.shapes.Polyline
     * @since 5.2
     */
    @Nonnull
    IShapePathMapObject<?> createPolylineMapObject(@Nonnull IMutablePolyline polyline, @Nonnull ShapeOptions shapeOptions)
            throws IllegalArgumentException, ShapeAlreadyExistsException;

    /**
     * Create a new unpublished rangeFan from the given shape model.
     *
     * @param rangeFan model defining the shape properties
     * @return new shape
     * @throws IllegalArgumentException    If the name is {@code null} or empty
     * @throws ShapeAlreadyExistsException If the name is not unique among all shapes that have been created by a single plugin instance
     * @see gov.takx.dataservices.shapes.RangeFan
     * @since 5.2
     */
    @Nonnull
    default IShapeRangeFanMapObject createRangeFanMapObject(@Nonnull IMutableRangeFan rangeFan) throws IllegalArgumentException, ShapeAlreadyExistsException
    {
        return createRangeFanMapObject(rangeFan, ShapeOptions.UNPUBLISHED);
    }

    /**
     * Create a new rangeFan from the given shape model.
     *
     * @param rangeFan model defining the shape properties
     * @param shapeOptions options to apply when creating the shape
     * @return new shape
     * @throws IllegalArgumentException    If the name is {@code null} or empty
     * @throws ShapeAlreadyExistsException If the name is not unique among all shapes that have been created by a single plugin instance
     * @see gov.takx.dataservices.shapes.RangeFan
     * @since 5.2
     */
    @Nonnull
    IShapeRangeFanMapObject createRangeFanMapObject(@Nonnull IMutableRangeFan rangeFan, @Nonnull ShapeOptions shapeOptions)
            throws IllegalArgumentException, ShapeAlreadyExistsException;

    /**
     * Create a new unpublished rectangle from the given shape model.
     *
     * @param rectangle model defining the shape properties
     * @return new shape
     * @throws IllegalArgumentException    If the name is {@code null} or empty
     * @throws ShapeAlreadyExistsException If the name is not unique among all shapes that have been created by a single plugin instance
     * @see gov.takx.dataservices.shapes.Rectangle
     * @since 5.2
     */
    @Nonnull
    default IShapeRectangleMapObject createRectangleMapObject(@Nonnull IMutableRectangle rectangle) throws IllegalArgumentException, ShapeAlreadyExistsException
    {
        return createRectangleMapObject(rectangle, ShapeOptions.UNPUBLISHED);
    }

    /**
     * Create a new rectangle from the given shape model.
     *
     * @param rectangle model defining the shape properties
     * @param shapeOptions options to apply when creating the shape
     * @return new shape
     * @throws IllegalArgumentException    If the name is {@code null} or empty
     * @throws ShapeAlreadyExistsException If the name is not unique among all shapes that have been created by a single plugin instance
     * @see gov.takx.dataservices.shapes.Rectangle
     * @since 5.2
     */
    @Nonnull
    IShapeRectangleMapObject createRectangleMapObject(@Nonnull IMutableRectangle rectangle, @Nonnull ShapeOptions shapeOptions)
            throws IllegalArgumentException, ShapeAlreadyExistsException;

    /**
     * Create a new unpublished square from the given shape model.
     *
     * @param square model defining the shape properties
     * @return new shape
     * @throws IllegalArgumentException    If the name is {@code null} or empty
     * @throws ShapeAlreadyExistsException If the name is not unique among all shapes that have been created by a single plugin instance
     * @see gov.takx.dataservices.shapes.Square
     * @since 5.2
     */
    @Nonnull
    default IShapeSquareMapObject createSquareMapObject(@Nonnull IMutableSquare square) throws IllegalArgumentException, ShapeAlreadyExistsException
    {
        return createSquareMapObject(square, ShapeOptions.UNPUBLISHED);
    }

    /**
     * Create a new square from the given shape model.
     *
     * @param square model defining the shape properties
     * @param shapeOptions options to apply when creating the shape
     * @return new shape
     * @throws IllegalArgumentException    If the name is {@code null} or empty
     * @throws ShapeAlreadyExistsException If the name is not unique among all shapes that have been created by a single plugin instance
     * @see gov.takx.dataservices.shapes.Square
     * @since 5.2
     */
    @Nonnull
    IShapeSquareMapObject createSquareMapObject(@Nonnull IMutableSquare square, @Nonnull ShapeOptions shapeOptions)
            throws IllegalArgumentException, ShapeAlreadyExistsException;

    /**
     * Retrieve a previously created arrow by name. If the arrow doesn't exist, then {@code null} is returned. A mutable
     * reference is returned. Only arrows created within this shape service instance are returned.
     * Must be called on the Event Dispatch Thread.
     *
     * @param name Name of shape used during its construction
     * @return arrow reference, {@code null} if not found
     * @throws IllegalArgumentException If the name is {@code null} or empty
     * @since 3.0
     */
    @Nullable
    default IMutableArrow getArrow(@Nonnull String name) throws IllegalArgumentException
    {
        return (IMutableArrow) getShape(name);
    }

    /**
     * Retrieve a previously created circle by name.  If the circle doesn't exist, then {@code null} is returned. A mutable
     * reference is returned. Only circles created within this shape service instance are returned.
     * Must be called on the Event Dispatch Thread.
     *
     * @param name Name of shape used during its construction
     * @return circle reference, {@code null} if not found
     * @throws IllegalArgumentException If the name is {@code null} or empty
     * @since 1.4
     */
    @Nullable
    default IMutableCircle getCircle(@Nonnull String name) throws IllegalArgumentException
    {
        return (IMutableCircle) getShape(name);
    }

    /**
     * Retrieve a previously created ellipse by name. A mutable ellipse is returned. Only ellipses created within this
     * shape service are returned.  If the ellipse doesn't exist, then {@code null} is returned. Must be called on the Event Dispatch Thread.
     *
     * @param name Name of shape used during its construction
     * @return ellipse reference, {@code null} if not found
     * @throws IllegalArgumentException If the name is {@code null} or empty
     * @since 1.4
     */
    @Nullable
    default IMutableEllipse getEllipse(@Nonnull String name) throws IllegalArgumentException
    {
        return (IMutableEllipse) getShape(name);
    }

    /**
     * Retrieve a previously created square by name. A mutable square is returned. Only squares created within this
     * shape service are returned. If the square doesn't exist, then {@code null} is returned. Must be called on the Event Dispatch Thread.
     *
     * @param name Name of shape used during its construction
     * @return square reference, {@code null} if not found
     * @throws IllegalArgumentException If the name is {@code null} or empty
     * @since 1.4
     */
    @Nullable
    default IMutableSquare getSquare(@Nonnull String name) throws IllegalArgumentException
    {
        return (IMutableSquare) getShape(name);
    }

    /**
     * Retrieve a previously created rectangle by name.  If the rectangle doesn't exist, then {@code null} is returned. A mutable
     * reference is returned. Only rectangles created within this shape service instance are returned.
     *
     * @param name Name of shape used during its construction
     * @return rectangle reference, {@code null} if not found
     * @throws IllegalArgumentException If the name is {@code null} or empty
     * @since 1.4
     */
    @Nullable
    default IMutableRectangle getRectangle(@Nonnull String name) throws IllegalArgumentException
    {
        return (IMutableRectangle) getShape(name);
    }

    /**
     * Retrieve a previously created polyline by name. A mutable polyline is returned. Only polylines created within this
     * shape service are returned. If the polyline doesn't exist, then {@code null} is returned.
     *
     * @param name Name of shape used during its construction
     * @return polyline reference, {@code null} if not found
     * @throws IllegalArgumentException If the name is {@code null} or empty
     * @since 1.4
     */
    @Nullable
    default IMutablePolyline getPolyline(@Nonnull String name) throws IllegalArgumentException
    {
        return (IMutablePolyline) getShape(name);
    }

    /**
     * Retrieve a previously created polygon by name. A mutable polygon is returned. Only polygons created within this
     * shape service instance will be returned. If the polygon doesn't exist, then {@code null} is returned.
     *
     * @param name Name of shape used during its construction
     * @return polygon reference, {@code null} if not found
     * @throws IllegalArgumentException If the name is {@code null} or empty
     * @since 1.4
     */
    @Nullable
    default IMutablePolygon getPolygon(@Nonnull String name) throws IllegalArgumentException
    {
        return (IMutablePolygon) getShape(name);
    }

    /**
     * Retrieve a previously created range fan by name. A mutable range fan is returned. Only range fans created
     * within this shape service are returned. If the range fan doesn't exist, then {@code null} is returned.
     *
     * @param name Name of shape used during its construction
     * @return range fan reference, {@code null} if not found
     * @throws IllegalArgumentException If the name is {@code null} or empty
     * @since 1.49
     */
    @Nullable
    default IMutableRangeFan getRangeFan(@Nonnull String name) throws IllegalArgumentException
    {
        return (IMutableRangeFan) getShape(name);
    }

    /**
     * Retrieve a previously created shape by name. A mutable shape is returned. Only shapes created within this shape
     * service are returned. If the shape doesn't exist, then {@code null} is returned.
     *
     * @param name Name of shape used during its construction
     * @return shape reference, {@code null} if not found
     * @throws IllegalArgumentException If the name is {@code null} or empty
     * @since 1.4
     */
    @Nullable
    IMutableShape getShape(@Nonnull String name) throws IllegalArgumentException;

    /**
     * Delete a previously created shape. Only a shape created within this shape service can be deleted.
     * If the shape doesn't exist, then an exception is thrown.
     *
     * @param name Name of shape to delete
     * @throws IllegalArgumentException if the name is {@code null} or empty
     * @throws NoSuchShapeException     if the named shape doesn't exist
     * @since 1.4
     */
    void deleteShape(@Nonnull String name) throws IllegalArgumentException, NoSuchShapeException;

    /**
     * Delete all the shapes associated with this ShapeService instance. This will ONLY delete shapes created from
     * this shape service instance and will simply ignore any published shapes that the service does not own.
     *
     * @since 1.4
     */
    void deleteAllShapes();

    /**
     * Get a list containing all the shapes for this shape service instance, as well as any currently published shapes
     * within the current project session. These will all be immutable shape references since it is possible for the
     * list to contain shapes that you do not own.
     *
     * @return a list of immutable shapes
     * @since 1.4
     */
    @Nonnull
    List<IShape> getShapes();

    /**
     * Get a filtered list containing all the shapes for this shape service instance, as well as any currently published shapes
     * within the current project session. These will all be immutable shape references since it is possible for the
     * list to contain shapes that you do not own.
     * see {@link IShapeFilter} for information on IShapeFilter's
     * see {@link IShapeFilterBuilder} for information on IShapeFilterBuilders, which create IShapeFilter's
     *
     * @param shapeFilter a shape filter
     * @return a list of filtered, immutable shapes
     * @since 1.4
     */
    @Nonnull
    List<IShape> getShapes(@Nonnull IShapeFilter shapeFilter);

    /**
     * Get a list of all the shapes owned by this shape service. These will all be mutable shape references since they
     * are guaranteed to be created from this shape service instance.
     *
     * @return list of mutable shapes
     */
    @Nonnull
    List<IMutableShape> getMutableShapes();

    /**
     * Get a filtered list of all the shapes owned by this shape service. These will all be mutable shape
     * references since they are guaranteed to be created from this shape service instance.
     * see {@link IShapeFilter} for information on IShapeFilter's
     * see {@link IShapeFilterBuilder} for information on IShapeFilterBuilders, which create IShapeFilter's
     *
     * @param shapeFilter a shape filter
     * @return list of filtered, mutable shapes
     * @since 1.4
     */
    @Nonnull
    List<IMutableShape> getMutableShapes(@Nonnull IShapeFilter shapeFilter);

    /**
     * Get the elevation (above Mean Sea Level) of the terrain at the given location (lat/lon).
     *
     * @param location Location from which to extract lat/lon coordinates
     * @return Elevation in meters of the terrain at that location
     * @since 1.4
     */
    default double getElevation(@Nonnull ILocation location)
    {
        return getElevation(location.getLatitude(), location.getLongitude());
    }

    /**
     * Get the elevation (above Mean Sea Level) of the terrain at the given lat/lon.
     *
     * @param latitude  Latitude of location of interest
     * @param longitude Longitude of location of interest
     * @return Elevation in meters of the terrain at that location
     * @since 1.4
     */
    double getElevation(double latitude, double longitude);

    /**
     * Navigates to the location on the globe where the shape is located.
     *
     * @param shape the shape to navigate to
     * @since 1.4
     */
    void gotoShape(@Nonnull IShape shape);

    /**
     * Navigates to the location on the globe where the shape is located. This method uses the latitude, longitude, and
     * elevation from the shape, and the heading, pitch, and zoom level defined by the camera eye.
     *
     * @param shape     the shape to navigate to
     * @param cameraEye the heading, pitch, and zoom level to set the camera eye to at the location of the shape
     */
    void gotoShape(@Nonnull IShape shape, @Nonnull ICameraEye cameraEye);

    /**
     * Creates and returns a new IShapeFilterBuilder object. These are used to define and create IShapeFilters.
     * <p/>
     * see {@link IShapeFilter} for information on IShapeFilter's
     * see {@link IShapeFilterBuilder} for information on IShapeFilterBuilders, which create IShapeFilter's
     *
     * @return a new IShapeFilterBuilderObject
     * @since 1.4
     */
    @Nonnull
    IShapeFilterBuilder createShapeFilterBuilder();

    /**
     * Register a listener for published shapes being created, published shapes being removed (as well as un-published
     * shapes that we own/created being removed), unpublished shapes becoming published and published shapes becoming
     * unpublished.
     *
     * @param listener the listener to be added
     * @throws IllegalArgumentException if the listener is {@code null}
     * @see IShapeServiceListener
     * @since 1.4
     */
    void addShapeServiceListener(@Nonnull IShapeServiceListener listener) throws IllegalArgumentException;

    /**
     * Unregister a listener.
     *
     * @param listener the IShapeServiceListener to be removed
     * @return {@code true} if listener was a registered listener on the ShapeService and was removed, {@code false} otherwise.
     * @since 1.4
     */
    boolean removeShapeServiceListener(@Nonnull IShapeServiceListener listener);
}
