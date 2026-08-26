package gov.takx.core.database.provider.hypersonic;

import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.platform.utils.ShapeUtils;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;
import org.hibernate.Session;
import org.hsqldb.persist.HsqlDatabaseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.sql.Array;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Custom functions that can be called from HSQL queries. These are required to implement the spatial queries that
 * Postgres can do implicitly.
 *
 * @since 5.5
 */
@Singleton
public class CustomSqlFunctions
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // Query to determine if any of our custom functions are already defined
    private static final String FUNCTIONS_EXIST_QUERY = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.ROUTINES WHERE ROUTINE_TYPE='FUNCTION' AND ROUTINE_NAME = 'POINT_IN_CIRCLE'";

    /**
     * HSQLDB requires that a specific system property be set to allow access to custom function implementations.
     */
    static void configureAccess()
    {
        System.setProperty(HsqlDatabaseProperties.hsqldb_method_class_names, CustomSqlFunctions.class.getName() + ".*");
    }

    /**
     * Define our custom SQL functions.
     * 
     * @param session Session to use
     */
    public void defineCustomFunctions(@Nonnull Session session)
    {
        // If any of our functions are already defined, skip it
        if (session.createNativeQuery(FUNCTIONS_EXIST_QUERY, Long.class).uniqueResult() > 0) return;

        final var transaction = session.beginTransaction();

        defineFunction("pointInCircle", "point_in_circle", "pLat double, pLon double, cLat double, cLon double, radius double", "boolean", session);
        defineFunction("pointInRectangle", "point_in_rectangle", "pLat double, pLon double, minLat double, minLon double, maxLat double, maxLon double", "boolean", session);
        defineFunction("pointInPolygon", "point_in_polygon", "pLat double, pLon double, latArray double array, lonArray double array", "boolean", session);

        transaction.commit();
    }

    /**
     * Define a custom SQL function.
     *
     * @param methodName The method name in this class (which must be static)
     * @param sqlName The name of the function to be used in SQL statements
     * @param argDecl The argument declarations for the method (in SQL types)
     * @param returnDecl The return type declaration for the method (in SQL types)
     * @param session Session to use for executing the needed SQL commands
     */
    private static void defineFunction(@Nonnull String methodName, @Nonnull String sqlName, @Nonnull String argDecl, @Nonnull String returnDecl, @Nonnull Session session)
    {
        final String definition = """
                CREATE FUNCTION %s(%s) RETURNS %s
                   LANGUAGE JAVA DETERMINISTIC NO SQL
                   EXTERNAL NAME 'CLASSPATH:%s.%s'"""
                .formatted(sqlName, argDecl, returnDecl, CustomSqlFunctions.class.getName(), methodName);

        logger.debug("define: {}", definition);

        session.createNativeMutationQuery(definition).executeUpdate();
    }

    /**
     * Determine if a given point is contained in a circle defined by its center and radius.
     * 
     * @param pointLat Latitude of point to test
     * @param pointLon Longitude of point to test
     * @param centerLat Latitude of circle center
     * @param centerLon Longitude of circle center
     * @param radius radius of circle in meters
     * @return {@code true} if the point is contained in the circle
     */
    @SuppressWarnings("unused") // Called from SQL
    public static boolean pointInCircle(double pointLat, double pointLon, double centerLat, double centerLon, double radius)
    {
        return ShapeUtils.circleContainsPoint(new GeoPoint(centerLat, centerLon), radius, new GeoPoint(pointLat, pointLon));
    }

    /**
     * Determine if a given point is contained in a rectangle defined by its min/max corners.
     *
     * @param pointLat Latitude of point to test
     * @param pointLon Longitude of point to test
     * @param minLat Latitude of the minimum corner
     * @param minLon Longitude of the minimum corner
     * @param maxLat Latitude of the maximum corner
     * @param maxLon Longitude of the maximum corner
     * @return {@code true} if the point is contained in the rectangle
     */
    @SuppressWarnings("unused") // Called from SQL
    public static boolean pointInRectangle(double pointLat, double pointLon, double minLat, double minLon, double maxLat, double maxLon)
    {
        final var rectangle = ShapeUtils.calculateRectangleFromMinMaxCorners(new GeoPoint(minLat, minLon), new GeoPoint(maxLat, maxLon));
        return rectangle.contains(new GeoPoint(pointLat, pointLon));
    }

    /**
     * Determine if a given point is contained in a polygon defined by its vertices.
     *
     * @param pointLat Latitude of point to test
     * @param pointLon Longitude of point to test
     * @param latArray SQL array of the latitudes of the vertices
     * @param lonArray SQL array of the longitudes of the vertices
     * @return {@code true} if the point is contained in the polygon
     * @see ShapeUtils#polygonContainsLocation
     */
    @SuppressWarnings("unused") // Called from SQL
    public static boolean pointInPolygon(double pointLat, double pointLon, Array latArray, Array lonArray) throws SQLException
    {
        final var latitudes = (Object[]) latArray.getArray();
        final var longitudes = (Object[]) lonArray.getArray();
        final var points = new ArrayList<ILocation>(latitudes.length);

        // Construct the vertices
        for (int i = 0; i < latitudes.length; i++) points.add(new GeoPoint((Double) latitudes[i], (Double) longitudes[i]));

        return ShapeUtils.polygonContainsLocation(points, new GeoPoint(pointLat, pointLon));
    }
}
