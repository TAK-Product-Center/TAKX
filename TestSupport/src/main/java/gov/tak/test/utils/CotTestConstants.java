package gov.tak.test.utils;

import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.shapes.ShapeType;
import gov.takx.platform.KeyUtils;
import org.assertj.core.data.Offset;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;

import static gov.takx.platform.KeyUtils.TOOL_FAMILY;

/**
 * Constants for use in CoT import/export tests, mostly values found in the various CoT resources.
 *
 * @since 3.0
 */
public class CotTestConstants
{
    // Comparison tolerance for floating point numbers
    public static final Offset<Double> EPSILON = Offset.offset(1e-6);

    public static final RaptorLocation ZERO_LOC = new RaptorLocation(0, 0);

    public static final String ARROW_UID = "36551a8c-f62b-4195-82d0-3578efeee4ee";
    public static final String ARROW_KEY = KeyUtils.getUniqueKey(TOOL_FAMILY, ShapeType.ARROW.toString(), ARROW_UID);
    public static final String ARROW_NAME = "Arrow 1";

    public static final String CIRCLE_UID = "21c57f66-d135-4180-a771-f2ede16629dc";
    public static final String CIRCLE_KEY = KeyUtils.getUniqueKey(TOOL_FAMILY, ShapeType.CIRCLE.toString(), CIRCLE_UID);
    public static final String CIRCLE_NAME = "Circle 1";

    public static final String ELLIPSE_UID = "31c57f66-d135-4180-a771-f2ede16629dc";
    public static final String ELLIPSE_KEY = KeyUtils.getUniqueKey(TOOL_FAMILY, ShapeType.ELLIPSE.toString(), ELLIPSE_UID);
    public static final String ELLIPSE_NAME = "Ellipse 1";

    public static final String POLYLINE_UID = "b112202e-dd33-4fc7-8d3d-09a14e296011";
    public static final String POLYLINE_KEY = KeyUtils.getUniqueKey(TOOL_FAMILY, ShapeType.POLYLINE.toString(), POLYLINE_UID);
    public static final String POLYLINE_NAME = "Polyline 1";

    public static final String POLYGON_UID = "c112202e-dd33-4fc7-8d3d-09a14e296011";
    public static final String POLYGON_KEY = KeyUtils.getUniqueKey(TOOL_FAMILY, ShapeType.POLYGON.toString(), POLYGON_UID);
    public static final String POLYGON_NAME = "Polygon 1";

    public static final String RECTANGLE_UID = "b8878fee-d040-486a-96cb-b5d45dc2fcb6";
    public static final String RECTANGLE_KEY = KeyUtils.getUniqueKey(TOOL_FAMILY, ShapeType.RECTANGLE.toString(), RECTANGLE_UID);
    public static final String RECTANGLE_NAME = "Rectangle 1";

    public static final Color SHAPE_LINE_COLOR = new Color(-256, true);
    public static final Color SHAPE_FILL_COLOR = new Color(-1761607681, true);
    public static final double SHAPE_LINE_WIDTH = 4;

    public static final Color UPDATED_SHAPE_LINE_COLOR = new Color(0xfeeeee00, false); // int value: -17895936, no alp  ha
    public static final Color UPDATED_SHAPE_FILL_COLOR = new Color(0x96feeeee, true);
    public static final double UPDATED_SHAPE_LINE_WIDTH = 8;

    public static final double SHAPE_CENTER_LAT = 50;
    public static final double SHAPE_CENTER_LON = -112;

    public static final double UPDATED_SHAPE_CENTER_LAT = 25;
    public static final double UPDATED_SHAPE_CENTER_LON = -90;

    // The major and minor axes should be drawn at half the value sent in the COT messages
    public static final double SEMI_MINOR_AXIS = 25_000;
    public static final double SEMI_MAJOR_AXIS = 35_000;

    public static final double ELLIPSE_ORIENTATION = 230;
    // The orientation is offset by 90 degrees because WorldWind orients around the minor axis while TAK orients around
    //   the major axis
    public static final double ELLIPSE_ORIENTATION_WITH_OFFSET = 320;
    public static final double BEARING = 230;
    public static final double RANGE = 50_000;

    // The major and minor axes should be drawn at half the value sent in the COT messages
    public static final double UPDATED_SEMI_MINOR_AXIS = 12_500;
    public static final double UPDATED_SEMI_MAJOR_AXIS = 17_500;

    public static final double CIRCLE_RADIUS = 35_000;
    public static final double LARGE_CIRCLE_RADIUS = 70_000;

    // The orientation is offset by 90 degrees because WorldWind orients around the minor axis while TAK orients around
    // the major axis
    public static final double UPDATED_ELLIPSE_ORIENTATION = 205;
    public static final double UPDATED_BEARING = 115;
    public static final double UPDATED_RANGE = 25_000;

    public static final List<GeoPoint> POLYGON_POINTS = Arrays.asList(
            new GeoPoint(40, -70),
            new GeoPoint(50, -80),
            new GeoPoint(60, -90),
            new GeoPoint(70, -100),
            new GeoPoint(40, -70));

    public static final List<GeoPoint> POLYLINE_POINTS = POLYGON_POINTS.subList(0, POLYGON_POINTS.size() - 1);

    public static final List<GeoPoint> RECTANGLE_CORNERS = Arrays.asList(
            new GeoPoint(31, -122),
            new GeoPoint(30, -122),
            new GeoPoint(30, -120),
            new GeoPoint(31, -120));

    // Allow for some clock ticks between creation and testing of "now" values
    public static final Offset<Long> CLOCK_SLOP = Offset.offset(10L);

    private CotTestConstants()
    {
    }
}
