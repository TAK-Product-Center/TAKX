package gov.takx.api.terrain.experimental;

import com.atakmap.map.layer.feature.geometry.LineString;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.render.AnnotationAttributes;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.PreRenderable;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.render.ScreenAnnotation;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.render.SurfacePolyline;
import gov.takx.api.annotation.Experimental;
import gov.takx.api.mapobject.IAnnotationMapObject;
import gov.takx.platform.globe.GeometryAnnotation;
import gov.takx.platform.globe.GlobeTextUtils;

import java.awt.Color;
import java.awt.Insets;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 */
@Experimental
public class ElevationContourLines implements Renderable, PreRenderable
{
    /**
     * Maximum altitude for the elevation bounds labels.
     */
    public static final double MAX_LABEL_ALTITUDE = IAnnotationMapObject.DEFAULT_MAX_ACTIVE_ALTITUDE;
    /**
     * Meters per Kilometer
     */
    private static final double M_PER_KM = 1000;
    /**
     * Contour lines.
     */
    private final List<SurfacePolyline> lines = new ArrayList<>();
    /**
     * Labels for the lines.
     */
    private final List<Renderable> labels = new ArrayList<>();

    /**
     * Create set of elevation contour lines.
     *
     * @param gridData   Data on the point grid.
     * @param dataMatrix Matrix of elevation resolution data. The data matrix should have one cell for each grid point.
     * @param thresholds Thresholds at which to split contour lines.
     * @param colors     Colors to assign to the lines. If there are more contour levels than there are colors, then the
     *                   colors will be reused in round-robin fashion.
     */
    public ElevationContourLines(TerrainGrid gridData, double[][] dataMatrix, double[] thresholds, List<Color> colors)
    {
        double[] xAxisCoordinates = getAxisValues(gridData.getNumberOfGridPointsX(), 1);
        double[] yAxisCoordinates = getAxisValues(gridData.getNumberOfGridPointsY(), 1);

        // Compute the raw contour data
        Map<Double, List<List<Double[]>>> pathsThresholdMap = Conrec.buildContourLines(dataMatrix,
                xAxisCoordinates,
                yAxisCoordinates,
                thresholds);

        // Convert the data to ContourLevels
        List<ContourLevel> contourLevels = createContourLevels(gridData, pathsThresholdMap);

        // For each level, create lines and labels with a unique color
        int colorIndex = 0;
        for (ContourLevel level : contourLevels)
        {
            Color color = colors.get(colorIndex);

            createShapes(level.lines, level.value, color);

            colorIndex++;
            colorIndex %= colors.size();
        }
    }

    @Override
    public void preRender(DrawContext dc)
    {
        for (SurfacePolyline line : lines)
        {
            line.preRender(dc);
        }
    }

    @Override
    public void render(DrawContext dc)
    {
        for (SurfacePolyline line : lines)
        {
            line.render(dc);
        }

        for (Renderable label : labels)
        {
            label.render(dc);
        }
    }

    /**
     * Create ContourLevel objects from raw contour line data.
     *
     * @param gridData    Data on the point grid.
     * @param contourData Contour line data from conrec. The keys of the map are contour thresholds, and the values the
     *                    rings for that threshold.
     * @return List of ContourLevel that represents all of the contour lines. Each entry in the list represents all of
     * the lines for one contour threshold.
     */
    private List<ContourLevel> createContourLevels(TerrainGrid gridData, Map<Double, List<List<Double[]>>> contourData)
    {
        List<ContourLevel> levels = new ArrayList<>();

        List<Double> thresholds = new ArrayList<>(contourData.keySet());
        Collections.sort(thresholds);

        for (Double threshold : thresholds)
        {
            List<List<Double[]>> contourLines = contourData.get(threshold);
            ContourLevel level = new ContourLevel(threshold, gridData, contourLines);
            levels.add(level);
        }

        return levels;
    }

    /**
     * Create axis values to necessary to create contour lines.
     *
     * @param numberOfGridPoints Number of grid points on the axis.
     * @param stepSizeInMeters   Step size between grid points.
     * @return Array of axis values.
     */
    private double[] getAxisValues(int numberOfGridPoints, double stepSizeInMeters)
    {
        double[] axisValues = new double[numberOfGridPoints];
        for (int i = 0; i < axisValues.length; i++)
        {
            axisValues[i] = stepSizeInMeters * i;
        }
        return axisValues;
    }

    /**
     * Create lines and labels for one contour level.
     *
     * @param contourLines Locations that identify where the lines should be.
     * @param value        Value of this contour level.
     * @param color        Color to assign to the lines.
     */
    private void createShapes(List<List<LatLon>> contourLines, double value, Color color)
    {
        LineString ls = new LineString(2);
        ShapeAttributes shapeAttributes = createLineAttributes(color);
        contourLines.forEach(cl -> {
            lines.add(new SurfacePolyline(shapeAttributes, cl));
            cl.forEach(p -> ls.addPoint(p.longitude.degrees, p.latitude.degrees));
        });
        GeometryAnnotation geometryAnnotation = new GeometryAnnotation(ls);
        AnnotationAttributes annotationAttributes = geometryAnnotation.getAttributes();
        geometryAnnotation.setText(String.format("%.2f m", value));
        annotationAttributes.setBackgroundColor(color);
        annotationAttributes.setTextColor(Color.WHITE);
        annotationAttributes.getDrawOffset().setLocation(0, 0);
        labels.add(geometryAnnotation);
    }

    /**
     * Create a shape attributes bundle for a contour line.
     *
     * @param color Color of the line.
     * @return An attributes bundle to style the contour line with the specified color.
     */
    private ShapeAttributes createLineAttributes(Color color)
    {
        Material material = new Material(color);

        ShapeAttributes attr = new BasicShapeAttributes();
        attr.setOutlineMaterial(material);
        attr.setOutlineWidth(2);

        return attr;
    }

    /**
     * Create an annotation to use as a label on a contour line.
     *
     * @param text  Label to put on annotation
     * @param color Color to use for annotation background
     * @return annotation
     */
    private ScreenAnnotation makeLabelAnnotation(String text, Color color)
    {
        ScreenAnnotation ga = new ScreenAnnotation(text, new Point());
        ga.setPickEnabled(false);
        ga.setMaxActiveAltitude(MAX_LABEL_ALTITUDE);

        AnnotationAttributes attrs = new AnnotationAttributes();
        attrs.setAdjustWidthToText(AVKey.SIZE_FIT_TEXT);
        attrs.setFrameShape(AVKey.SHAPE_RECTANGLE);
        attrs.setDrawOffset(new Point(0, 10));
        attrs.setLeaderGapWidth(5);
        attrs.setTextColor(Color.WHITE);
        attrs.setBackgroundColor(color);
        attrs.setCornerRadius(5);
        attrs.setBorderColor(new Color(0xababab));
        attrs.setFont(GlobeTextUtils.ELEVATION_CONTOUR_LABEL_FONT);
        attrs.setTextAlign(AVKey.CENTER);
        attrs.setInsets(new Insets(5, 5, 5, 5));

        ga.setAttributes(attrs);

        return ga;
    }

    /**
     * The data required to draw one level of contour lines. This object combines the positions of the lines with
     * the value that those lines represent.
     */
    static class ContourLevel
    {
        /**
         * Value assigned to this contour level.
         */
        final double value;

        /**
         * The lines that make up this level.
         */
        List<List<LatLon>> lines;

        /**
         * Create a new contour level.
         *
         * @param value        Value assigned to this level.
         * @param gridData     Data on the point grid.
         * @param contourLines List of contour lines for this level. Each entry represents one line.
         */
        public ContourLevel(double value, TerrainGrid gridData, List<List<Double[]>> contourLines)
        {
            this.value = value;

            lines = new ArrayList<>();

            for (List<Double[]> contourPath : contourLines)
            {
                List<LatLon> locationPath = createPositionList(gridData, contourPath);
                lines.add(locationPath);
            }
        }

        /**
         * Create a list of geographic positions from a list of contour line segments.
         *
         * @param gridData Data on the point grid.
         * @param segments List of line segments.
         * @return List of geographic positions in one contour line.
         */
        private List<LatLon> createPositionList(TerrainGrid gridData, List<Double[]> segments)
        {
            List<LatLon> locations = new ArrayList<>();
            for (Double[] segment : segments)
            {
                addSegment(gridData, segment, locations);
            }

            return locations;
        }

        /**
         * Add a segment to a list of geographic positions.
         *
         * @param gridData      Data on the point grid.
         * @param segmentValues Values that define the line segment.
         * @param locations     List of locations that will receive the new line segment.
         */
        private void addSegment(TerrainGrid gridData, Double[] segmentValues, List<LatLon> locations)
        {
            final double stepSizeKm = gridData.getStepSizeInMeters() / M_PER_KM;

            final double x1 = segmentValues[0] * stepSizeKm;
            final double y1 = segmentValues[1] * stepSizeKm;

            final double x2 = segmentValues[2] * stepSizeKm;
            final double y2 = segmentValues[3] * stepSizeKm;

            // Add the segment start point
            locations.add(LatLon.fromDegrees(gridData.getLatFromY(y1), gridData.getLonFromX(x1)));

            // Add the segment end point
            locations.add(LatLon.fromDegrees(gridData.getLatFromY(y2), gridData.getLonFromX(x2)));
        }
    }
}
