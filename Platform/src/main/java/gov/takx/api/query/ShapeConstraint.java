package gov.takx.api.query;

import gov.takx.api.annotation.Internal;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.shapes.ICircle;
import gov.takx.api.shapes.IEllipse;
import gov.takx.api.shapes.IPolygon;
import gov.takx.api.shapes.IRectangle;
import gov.takx.api.shapes.IShape;
import gov.takx.api.shapes.ISquare;
import gov.takx.platform.preferences.ProjectPreferenceEntry;
import gov.takx.platform.query.SqlState;
import jakarta.annotation.Nonnull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A constraint that limits selected RDMs/rows to those whose location is "within" the given shape. You can define the
 * shape parameters directly (see {@link #ShapeConstraint(ILocation, double)},
 * {@link #ShapeConstraint(ILocation, ILocation)}, and {@link #ShapeConstraint(java.util.List)}), or the shape
 * parameters can be extracted from an existing TAKX surface shape. However, not all shape types are supported. Only the
 * following shape types may be used:
 * <ul>
 * <li>{@link IRectangle}</li>
 * <li>{@link ISquare}</li>
 * <li>{@link ICircle}</li>
 * <li>{@link IEllipse}</li>
 * <li>{@link IPolygon}</li>
 * </ul>
 * It is more efficient to query for containment in a BOX (square/rectangle) or CIRCLE than it is on a POLYGON.
 * Note that an ellipse is converted to an approximating polygon.
 * <p>
 * This constraint type is only functional when running on top of Postgres.  It is not supported on any other
 * database, and will generate SQL that will not process properly.  This will generally lead to SQL parsing errors
 * when executing a query using this constraint type.
 *
 * @since 1.49
 */
public class ShapeConstraint implements IConstraint
{
    /**
     * This enum specifies which shape type this constraint is configured on.  This indicates which control values
     * are valid.
     */
    public enum ShapeType
    {
        rectangle,
        circle,
        polygon
    }

    private String shapeSql;
    private ShapeType shapeType;

    // For Square/Rectangle types
    private ILocation minCorner;
    private ILocation maxCorner;

    // For Circle
    private ILocation center;
    private double radius;

    // For Polygon (and ellipse)
    private ILocation[] vertices;

    private final boolean isPgsql = "postgres".equalsIgnoreCase(ProjectPreferenceEntry.DATABASE_TYPE.getStringValue());

    /**
     * Construct a constraint that will test for the location of an RDM being contained within the given surface shape.
     * The shape must be one of the following types:
     * <ul>
     * <li>{@link IRectangle}</li>
     * <li>{@link ISquare}</li>
     * <li>{@link ICircle}</li>
     * <li>{@link IEllipse}</li>
     * <li>{@link IPolygon}</li>
     * </ul>
     *
     * @param shape Shape to use for contains test
     * @throws IllegalArgumentException if the shape is not of a supported type
     */
    public ShapeConstraint(@Nonnull IShape shape)
    {
        if (shape instanceof final IRectangle rectangle)
        {
            prepareRectangle(rectangle.getMinCorner(), rectangle.getMaxCorner());
        } else if (shape instanceof final ICircle circle)
        {
            prepareCircle(circle.getLocation(), circle.getRadius());
        } else if (shape instanceof IEllipse ellipse)
        {
            preparePolygon(ellipse.getVertices()); // Generate a polygon from the ellipse
        } else if (shape instanceof IPolygon polygon)
        {
            preparePolygon(polygon.getVertices());
        } else
        {
            throw new IllegalArgumentException("Shape is not of a supported type: " + shape.getClass());
        }
    }

    /**
     * Constructor for specifying a BOX (square/rectangle) containment given min/max corners.
     *
     * @param minCorner Min corner
     * @param maxCorner Max corner
     */
    public ShapeConstraint(@Nonnull ILocation minCorner, @Nonnull ILocation maxCorner)
    {
        prepareRectangle(minCorner, maxCorner);
    }

    /**
     * Constructor for specifying a CIRCLE containment given a center and radius.
     *
     * @param center Center location of circle
     * @param radius Radius of circle
     */
    public ShapeConstraint(ILocation center, double radius)
    {
        prepareCircle(center, radius);
    }

    /**
     * Constructor for specifying a POLYGON containment given a list of locations (vertices on polygon).  The polygon
     * does not need to be closed, it will be automatically closed if the first and last points are not identical.
     *
     * @param vertices Polygon vertices
     */
    public ShapeConstraint(List<ILocation> vertices)
    {
        preparePolygon(vertices);
    }

    @Override
    public boolean usesExtendedFields()
    {
        return false;
    }

    @Override
    public Set<String> getReferencedStructures()
    {
        return new HashSet<>();
    }

    /**
     * Under Postgres, we produce a 'point' from the rdm to test against a spatial type generated from our configuration.
     * <br>
     * Under HSQLDB, there are no spatial types, so we use special SQL function to test for containment.
     *
     * @param state Sql generation state for mapping field names and parameter binding
     */
    @Internal
    @Override
    public void generateSQL(SqlState state)
    {
        // Only RDMs that have a location
        state.sql.append("(rdm.haslocation=TRUE and ");
        if (isPgsql) state.sql.append("point(rdm.latitude, rdm.longitude) <@ ");
        state.sql.append(shapeSql).append(')');
    }

    /**
     * @return configured shape type
     */
    public ShapeType getShapeType()
    {
        return shapeType;
    }

    /**
     * @return Circle center, only valid when {@link #getShapeType()} == {@link ShapeConstraint.ShapeType#circle}
     */
    public ILocation getCenter()
    {
        return center;
    }

    /**
     * @return Circle radius, only valid when {@link #getShapeType()} == {@link ShapeConstraint.ShapeType#circle}
     */
    public double getRadius()
    {
        return radius;
    }

    /**
     * @return Rectangle min corner, only valid when {@link #getShapeType()} == {@link ShapeConstraint.ShapeType#rectangle}
     */
    public ILocation getMaxCorner()
    {
        return maxCorner;
    }

    /**
     * @return Rectangle max corner, only valid when {@link #getShapeType()} == {@link ShapeConstraint.ShapeType#rectangle}
     */
    public ILocation getMinCorner()
    {
        return minCorner;
    }

    /**
     * @return Polygon vertices, only valid when {@link #getShapeType()} == {@link ShapeConstraint.ShapeType#polygon}
     */
    public ILocation[] getVertices()
    {
        return vertices;
    }

    /**
     * Internalize data for processing a rectangle/square.
     *
     * @param minCorner Min corner
     * @param maxCorner Max corner
     */
    private void prepareRectangle(ILocation minCorner, ILocation maxCorner)
    {
        shapeType = ShapeType.rectangle;
        this.minCorner = minCorner;
        this.maxCorner = maxCorner;

        shapeSql = getSqlForRectangle(minCorner, maxCorner);
    }

    /**
     * Internalize data for processing a circle.
     *
     * @param center Center location of circle
     * @param radius Radius of circle
     */
    private void prepareCircle(ILocation center, double radius)
    {
        shapeType = ShapeType.circle;
        this.center = center;
        this.radius = radius;

        shapeSql = getSqlForCircle(center, radius);
    }

    /**
     * Internalize data for processing a polygon.
     *
     * @param vertices Polygon vertices
     */
    private void preparePolygon(List<ILocation> vertices)
    {
        shapeType = ShapeType.polygon;
        this.vertices = vertices.toArray(new ILocation[0]);

        shapeSql = getSqlForPolygon(vertices);
    }

    /**
     * Generate the SQL for a rectangle's min/max corners.
     *
     * @param minCorner Min corner
     * @param maxCorner Max corner
     * @return SQL fragment for containment test
     */
    private String getSqlForRectangle(ILocation minCorner, ILocation maxCorner)
    {
        final String sql = isPgsql
                ? "box(point(%f,%f), point(%f,%f))"
                : "point_in_rectangle(rdm.latitude, rdm.longitude, %f, %f, %f, %f)";

        return String.format(sql, minCorner.getLatitude(), minCorner.getLongitude(), maxCorner.getLatitude(), maxCorner.getLongitude());
    }

    /**
     * Generate the SQL for a circle's center and radius.
     *
     * @param center Center location of circle
     * @param radius Radius of circle
     * @return SQL fragment for containment test
     */
    private String getSqlForCircle(ILocation center, double radius)
    {
        final String sql = isPgsql
                ? "circle(point(%f,%f), %f)"
                : "point_in_circle(rdm.latitude, rdm.longitude, %f, %f, %f)";

        return String.format(sql, center.getLatitude(), center.getLongitude(), radius);
    }

    /**
     * Generate the SQL for a polygon's vertices.
     *
     * @param vertices Polygon vertices
     * @return SQL fragment for containment test
     */
    private String getSqlForPolygon(List<ILocation> vertices)
    {
        return isPgsql ? getSqlForPolygonUsingPostgres(vertices) : getSqlForPolygonUsingHsql(vertices);
    }

    /**
     * Generate the SQL for polygon containment when using HSQLDB.
     */
    private String getSqlForPolygonUsingHsql(List<ILocation> vertices)
    {
        // SQL form: point_in_polygon(pointLat, pointLon, ARRAY[" + latitudes + "], ARRAY[" + longitudes + "])

        final var latitudes = new StringBuilder();
        final var longitudes = new StringBuilder();
        vertices.forEach(loc -> {
            if (!latitudes.isEmpty()) latitudes.append(",");
            latitudes.append(loc.getLatitude());
            if (!longitudes.isEmpty()) longitudes.append(",");
            longitudes.append(loc.getLongitude());
        });

        return "point_in_polygon(rdm.latitude, rdm.longitude, ARRAY[" + latitudes + "], ARRAY[" + longitudes + "])";
    }

    /**
     * Generate the SQL defining a polygon when using Postgres.
     */
    private String getSqlForPolygonUsingPostgres(List<ILocation> vertices)
    {
        final StringBuilder sql = new StringBuilder(1024);
        sql.append("polygon '(");

        // SQL form: polygon '((x1,y1), (x2,y2), (x3,y3),...)'

        String sep = "";
        for (ILocation vertex : vertices)
        {
            sql.append(sep);
            sql.append('(').append(vertex.getLatitude()).append(',').append(vertex.getLongitude()).append(')');
            sep = ", ";
        }

        sql.append(")'");

        return sql.toString();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ShapeConstraint that = (ShapeConstraint) o;

        if (shapeType != that.shapeType) return false;
        if (Double.compare(that.radius, radius) != 0) return false;
        if (!Objects.equals(minCorner, that.minCorner)) return false;
        if (!Objects.equals(maxCorner, that.maxCorner)) return false;
        //noinspection SimplifiableIfStatement
        if (!Objects.equals(center, that.center)) return false;
        return Arrays.equals(vertices, that.vertices);
    }

    @Override
    public int hashCode()
    {
        int result;
        result = shapeType.hashCode();
        result = 31 * result + (minCorner != null ? minCorner.hashCode() : 0);
        result = 31 * result + (maxCorner != null ? maxCorner.hashCode() : 0);
        result = 31 * result + (center != null ? center.hashCode() : 0);
        result = 31 * result + Double.hashCode(radius);
        result = 31 * result + (vertices != null ? Arrays.hashCode(vertices) : 0);

        return result;
    }

    @Override
    public String toString()
    {
        return "ShapeConstraint{" +
                "shapeType=" + shapeType +
                ", shapeSql='" + shapeSql + '\'' +
                '}';
    }
}
