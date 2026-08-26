package gov.takx.api.surfaces;

import gov.nasa.worldwind.geom.LatLon;

/**
 * This interface defines a service for creating surfaces (custom visual layers) that
 * render on the globe.
 *
 * @since 1.4
 */
public interface IAnalyticSurfaceService
{
    /**
     * Provides access to a set of mathematical routines useful for creating more complex surfaces.
     *
     * @return Analytic surface utils
     */
    IAnalyticSurfaceUtils getAnalyticSurfaceUtils();

    /**
     * Constructs a new AnalyticSurface with the specified min and max corners, base altitude in meters,
     * grid dimensions, and iterable of GridPointAttributes. The iterable should contain at least
     * {@code with * height} non-null GridPointAttributes.
     *
     * @param displayName the name of the surface (used in the layer table)
     * @param minCorner   the min corner of the surface's geographic region.
     * @param maxCorner   the max corner of the surface's geographic region.
     * @param altitude    the base altitude to place the surface at, in meters.
     * @param width       the surface grid width, in number of grid points.
     * @param height      the surface grid height, in number of grid points.
     * @param values      the attributes associated with each grid point.
     * @return new IAnalyticSurface instance
     * @throws IllegalArgumentException if the minCorner or MaxCorner are {@code null}, if the width is less than 1,
     *                                  if the height is less than 1, or if the iterable is {@code null}.
     */
    IAnalyticSurface createAnalyticSurface(String displayName, LatLon minCorner, LatLon maxCorner,
                                           double altitude, int width, int height,
                                           Iterable<? extends IAnalyticSurface.GridPointAttributes> values);

    /**
     * Constructs a new AnalyticSurface with the specified min and max corners, base altitude in meters, and grid dimensions.
     * The new AnalyticSurface has the default {@link IAnalyticSurface.GridPointAttributes}.
     *
     * @param displayName the name of the surface (used in the layer table)
     * @param minCorner   the min corner of the surface's geographic region.
     * @param maxCorner   the max corner of the surface's geographic region.
     * @param altitude    the base altitude to place the surface at, in meters.
     * @param width       the surface grid width, in number of grid points.
     * @param height      the surface grid height, in number of grid points.
     * @return new IAnalyticSurface instance
     * @throws IllegalArgumentException if the minCorner or MaxCorner are {@code null}, if the width is less than 1,
     *                                  or if the height is less than 1.
     * @see IAnalyticSurface#setSurfaceValues(Iterable)
     */
    IAnalyticSurface createAnalyticSurface(String displayName, LatLon minCorner, LatLon maxCorner,
                                           double altitude, int width, int height);

    /**
     * Delete the analytic surface and remove the layer use to display it.
     *
     * @param surface The surface to delete
     * @return {@code true} if the surface was deleted, {@code false} if the surface was not found, which can happen
     * if the same surface is deleted more than once.
     */
    boolean deleteAnalyticSurface(IAnalyticSurface surface);

    /**
     * Constructs a new AnalyticSurface with the specified min and max corners, base altitude in meters,
     * grid dimensions, and iterable of GridPointAttributes. The iterable should contain at least
     * {@code width * height} non-null GridPointAttributes.
     *
     * @param displayName    the name of the surface (used in the layer table)
     * @param minCorner      the min corner of the surface's geographic region.
     * @param maxCorner      the max corner of the surface's geographic region.
     * @param altitude       the base altitude to place the surface at, in meters.
     * @param width          the surface grid width, in number of grid points.
     * @param height         the surface grid height, in number of grid points.
     * @param values         the attributes associated with each grid point.
     * @param useDisplayList use OpenGL display lists to render the surface rather than a vertex array pointer if the graphics
     *                       card fails to render correctly using vertex array pointers
     * @return new IAnalyticSurface instance
     * @throws IllegalArgumentException if the minCorner or MaxCorner are {@code null}, if the width is less than 1,
     *                                  if the height is less than 1, or if the iterable is {@code null}.
     */
    IAnalyticSurface createAnalyticSurface(String displayName, LatLon minCorner, LatLon maxCorner, double altitude,
                                           int width, int height, Iterable<? extends IAnalyticSurface.GridPointAttributes> values, boolean useDisplayList);
}
