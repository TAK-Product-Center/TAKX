package gov.takx.api.terrain.experimental;

import com.atakmap.map.elevation.ElevationChunk;
import com.atakmap.map.elevation.ElevationChunk.Data;
import com.atakmap.map.elevation.ElevationManager;
import com.atakmap.map.elevation.ElevationSource;
import com.atakmap.map.elevation.ElevationSource.QueryParameters.Order;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.feature.geometry.GeometryFactory;
import com.atakmap.map.layer.feature.geometry.Point;
import com.atakmap.map.layer.model.Models;
import gov.tak.api.engine.map.coords.GeoPoint;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.engine.map.coords.IProjection;
import gov.tak.api.engine.math.IGeometryModel;
import gov.tak.api.engine.math.PointD;
import gov.tak.api.engine.math.Ray;
import gov.tak.api.engine.math.Vector;
import gov.tak.platform.engine.map.coords.ProjectionFactory;
import gov.tak.platform.marshal.MarshalManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A collection of utility methods for working with {@link ElevationSource}.
 * @since 5.2
 */
public class ElevationSourceUtils
{
    private static final int EQUIRECTANGULAR_PROJECTION_SRID = 4326;

    /**
     * Calculate intersection points in an area from a collection of rays. Rays should be in
     * longitude/latitude/elevation coordinates. The returned map contains all the rays submitted
     * for evaluation along with a list of zero or more intersections points with respect to the passed limit.
     *
     * @param elevationSource the elevation source
     * @param envelope the bounding area to search for intersections
     * @param rays a collection of rays to search for intersections
     * @param limit a limit of intersection results per-ray to return, or 0 for no limit
     * @return a map of rays to zero or more intersection locations
     */
    @Nonnull
    public static Map<Ray, Collection<IGeoPoint>> intersect(@Nonnull ElevationSource elevationSource, @Nonnull Envelope envelope, @Nonnull Collection<Ray> rays, int limit)
    {
        Map<Integer, IProjection> projCache = new HashMap<>();
        ElevationSource.QueryParameters params = new ElevationSource.QueryParameters();
        params.order = List.of(Order.ResolutionDesc);
        params.spatialFilter = GeometryFactory.createRectangle(new Point(envelope.minX, envelope.minY, envelope.minZ), new Point(envelope.maxX, envelope.maxY, envelope.maxZ), GeometryFactory.Algorithm.wgs84.ordinal());
        var results = new HashMap<Ray, Collection<IGeoPoint>>();
        rays.forEach(r -> results.put(r, new ArrayList<>()));
        try (ElevationSource.Cursor query = elevationSource.query(params))
        {
            while (query.moveToNext())
            {
                ElevationChunk elevationChunk = query.get();
                try
                {
                    Data data = elevationChunk.createData();
                    if (data == null || data.value == null) continue;
                    // Potential improvement - for accuracy across large distances, always reproject ray and mesh into ecef
                    IProjection proj = data.srid != EQUIRECTANGULAR_PROJECTION_SRID ? projCache.computeIfAbsent(data.srid, ProjectionFactory::getProjection) : null;
                    IGeometryModel geomModel = MarshalManager.marshal(Models.createGeometryModel(data.value, data.localFrame), com.atakmap.math.GeometryModel.class, IGeometryModel.class);
                    for (Ray ray : rays)
                    {
                        // Potential improvement - for accuracy across large distances, always reproject ray and mesh into ecef
                        Ray activeRay = ray;
                        do
                        {
                            Collection<IGeoPoint> locations = results.get(ray);
                            // Check existing intersection results from other chunks for this ray to see if we've reached our limit
                            if (limit != 0 && locations.size() >= limit) break;
                            // Check to see if our ray is outside our chunk
                            PointD intersect = geomModel.intersect(activeRay);
                            if (intersect == null || Double.isNaN(intersect.x) || Double.isNaN(intersect.y) || Double.isNaN(intersect.z)) break;
                            // Check to see if we are outside the search area
                            GeoPoint geoPoint = GeoPoint.createMutable();
                            if (proj != null)
                            {
                                proj.inverse(intersect, geoPoint);
                            } else
                            {
                                geoPoint.set(intersect.y, intersect.x, intersect.z);
                            }
                            if (envelope.minX > geoPoint.getLongitude() || envelope.maxX < geoPoint.getLongitude()
                                    || envelope.minY > geoPoint.getLatitude() || envelope.maxY < geoPoint.getLatitude())
                                break;
                            locations.add(geoPoint);
                            // Advance ray by 1 beyond intersection
                            activeRay = new Ray(new PointD(intersect.x + ray.direction.x, intersect.y + ray.direction.y, intersect.z + ray.direction.z), ray.direction);
                        } while (true);
                    }
                } finally
                {
                    elevationChunk.dispose();
                }
            }
        }
        return results;
    }

    private static Ray createRay(@Nullable IProjection proj, @Nonnull IGeoPoint locationA, @Nonnull IGeoPoint locationB)
    {
        PointD rayOrg;
        PointD rayTgt;
        if (proj != null)
        {
            rayOrg = new PointD();
            rayTgt = new PointD();
            proj.forward(new GeoPoint(locationA.getLatitude(), locationA.getLongitude(), locationA.getAltitude()), rayOrg);
            proj.forward(new GeoPoint(locationB.getLatitude(), locationB.getLongitude(), locationB.getAltitude()), rayTgt);
        } else
        {
            rayOrg = new PointD(locationA.getLongitude(), locationA.getLatitude(), locationA.getAltitude());
            rayTgt = new PointD(locationB.getLongitude(), locationB.getLatitude(), locationB.getAltitude());
        }

        return new Ray(rayOrg, new Vector(rayTgt.x - rayOrg.x, rayTgt.y - rayOrg.y, rayTgt.z - rayOrg.z));
    }

    /**
     * Returns the elevation at a specified location for the given {@link ElevationSource}.
     *
     * @param elevationSource the elevation source
     * @param latitude the latitude of the location
     * @param longitude the longitude of the location
     * @param targetResolution the target resolution of the result, use {@link Double#MIN_VALUE} to get the highest resolution available
     * @param replacementValue the value to use if elevation data is not available, or {@code null} to use {@link Double#NaN}
     *
     * @return the elevation at the specified location, or {@code replacementValue} if not available
     */
    public static double queryElevation(ElevationSource elevationSource, double latitude, double longitude, double targetResolution, Double replacementValue)
    {
        ElevationSource.QueryParameters params = new ElevationSource.QueryParameters();
        params.spatialFilter = new Point(longitude, latitude);
        params.order = List.of(Order.ResolutionDesc);
        params.maxResolution = targetResolution;
        double sample = replacementValue != null ? replacementValue : Double.NaN;
        try (ElevationSource.Cursor query = elevationSource.query(params))
        {
            // Use first result
            if (query.moveToNext())
            {
                ElevationChunk elevationChunk = query.get();
                // sample() returns NaN on no data
                double localSample = elevationChunk.sample(latitude, longitude);
                if (!Double.isNaN(localSample)) sample = localSample;
                elevationChunk.dispose();
            }
        }
        return sample;
    }

    /**
     * Returns the elevations at a specified locations for the given {@link ElevationSource}.
     *
     * @param elevationSource the elevation source
     * @param latlons the list of locations to retrieve
     * @param targetResolution the target resolution of the result, use {@link Double#MIN_VALUE} to get the highest resolution available
     * @param buffer a buffer to return the results
     * @param replacementValue the value to use if elevation data is not available, or {@code null} to use {@link Double#NaN}
     */
    public static void queryElevations(ElevationSource elevationSource, List<IGeoPoint> latlons, double targetResolution, double[] buffer, Double replacementValue)
    {
        double[] lla = new double[latlons.size() * 3];
        for (int a = 0; a < lla.length; a += 3)
        {
            IGeoPoint geoPoint = latlons.get(a / 3);
            lla[a] = geoPoint.getLongitude();
            lla[a + 1] = geoPoint.getLatitude();
            lla[a + 2] = Double.NaN;
        }

        queryElevations(elevationSource, targetResolution, lla);

        for (int i = 2, bufferIdx = 0; i < lla.length; i += 3, bufferIdx++)
        {
            double altitude = lla[i];
            if (Double.isNaN(altitude) && replacementValue != null) altitude = replacementValue;
            buffer[bufferIdx] = altitude;
        }
    }

    private static void queryElevations(ElevationSource elevationSource, double targetResolution, double[] llaBuffer)
    {
        ElevationSource.QueryParameters params = new ElevationSource.QueryParameters();
        params.order = List.of(Order.ResolutionDesc);
        params.maxResolution = targetResolution;
        int llaIndex = 0;
        // Query on individual points and use returned ElevationChunk to fill as many elevation requests as it can.
        // Find next unresolved point and repeat.
        // Continue until all points are resolved.
        while (llaIndex < llaBuffer.length)
        {
            double lon = llaBuffer[llaIndex];
            double lat = llaBuffer[llaIndex + 1];
            double alt = llaBuffer[llaIndex + 2];
            if (Double.isNaN(alt))
            {
                params.spatialFilter = new Point(lon, lat);
                try (ElevationSource.Cursor query = elevationSource.query(params))
                {
                    if (query.moveToNext())
                    {
                        ElevationChunk elevationChunk = query.get();
                        elevationChunk.sample(llaBuffer, llaIndex / 3, (llaBuffer.length - llaIndex) / 3);
                        elevationChunk.dispose();
                    }
                }
            }
            llaIndex += 3;
        }
    }

    /**
     * Produce a heightmap of {@code numPostsLng/numPostsLat} gridded elevation values starting from {@code bounds.minX/bounds.minY} inclusive to {@code bounds.maxX/bounds.maxY} inclusive.
     * Results are returned as an array of LLA triples. Altitude results of {@link Double#NaN} indicate that elevation data was not available.
     *
     * @param elevationSource the elevation source
     * @param targetResolution the target resolution of the result, use {@link Double#MIN_VALUE} to get the highest resolution available
     * @param params the heightmap configuration
     * @param llaBuffer an array of {@code numPostsLat * numPostsLng * 3} doubles to store the result
     * @since 5.2
     */
    public static void createHeightmapLLA(ElevationSource elevationSource, double targetResolution, ElevationManager.HeightmapParams params, double[] llaBuffer)
    {
        double cellHeightLat = ((params.bounds.maxY - params.bounds.minY) / (params.numPostsLat - 1));
        double cellWidthLng = ((params.bounds.maxX - params.bounds.minX) / (params.numPostsLng - 1));

        int post = 0;
        for (int postLat = 0; postLat < params.numPostsLat; postLat++)
        {
            double ptLat = params.invertYAxis ? params.bounds.minY + cellHeightLat * postLat : params.bounds.maxY - cellHeightLat * postLat;
            // flip over pole if necessary
            if (ptLat > 90.0) ptLat = 180.0 - ptLat;
            if (ptLat < -90.0) ptLat = -180.0 - ptLat;
            for (int postLng = 0; postLng < params.numPostsLng; postLng++)
            {
                double ptLng = params.bounds.minX + cellWidthLng * postLng;
                // flip over anti-meridian if necessary
                if (ptLng < -180.0) ptLng += 360.0;
                if (ptLng > 180.0) ptLng -= 360.0;
                // lat/longs go into queryElevations() as x,y coordinates
                llaBuffer[post * 3] = ptLng;
                llaBuffer[post * 3 + 1] = ptLat;
                llaBuffer[post * 3 + 2] = Double.NaN;
                post++;
            }
        }

        queryElevations(elevationSource, targetResolution, llaBuffer);

        // lat/longs go out of this method as y,x coordinates
        for (int i = 0; i < params.numPostsLat * params.numPostsLng; i++)
        {
            double lng = llaBuffer[i * 3];
            llaBuffer[i * 3] = llaBuffer[i * 3 + 1];
            llaBuffer[i * 3 + 1] = lng;
        }
    }
}
