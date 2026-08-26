package gov.takx.core.view.gui.maptools.elevationtools.contour;

import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.feature.geometry.LineString;
import gov.tak.api.contour.ContourLine;
import gov.tak.api.contour.ContourStyle;
import gov.tak.platform.contour.ContourLineGroup;
import jakarta.annotation.Nonnull;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Utility for rendering contour lines in a {@link ContourLineGroup}.
 *
 * @since 5.8
 */
public class ContourMapRenderer
{
    private static final int OPTIMIZATION_THRESHOLD = 100; // Vertex count of line-string to begin optimizing at
    private static final double SIMPLIFICATION_FACTOR = 0.85; // Amount in px to allow deviation from simplified lines

    /**
     * @param group Contour group to render.
     * @param size Output image size <i>(square)</i> in pixels.
     * @return Top down render of the contour group.
     * @throws IllegalArgumentException When the contours group is empty.
     */
    @Nonnull
    public static BufferedImage renderContourMap(@Nonnull ContourLineGroup group, int size)
    {
        Envelope bounds = group.getBoundingBox();
        double minX = bounds.minX;
        double maxX = bounds.maxX;
        double minY = bounds.minY;
        double maxY = bounds.maxY;
        double rangeX = maxX - minX;
        double rangeY = maxY - minY;

        return renderContourMap(group.getContourLines(), group.getStyle(), minX, minY, rangeX, rangeY, size, size);
    }

    /**
     * @param contourLines Contours to render.
     * @param style    Style to render contours with.
     * @param minX     Minimum longitude of the contours.
     * @param minY     Minimum latitude of the contours.
     * @param rangeX   Distance to maximum longitude of the contours.
     * @param rangeY   Distance to maximum longitude of the contours.
     * @param width    Output image width in pixels.
     * @param height   Output image height in pixels.
     * @return Top down render of the contours.
     * @throws IllegalArgumentException When the contours list is empty, or the ranges are {@code <= 0}.
     */
    @Nonnull
    public static BufferedImage renderContourMap(@Nonnull List<ContourLine> contourLines,
                                                 @Nonnull ContourStyle style,
                                                 double minX, double minY,
                                                 double rangeX, double rangeY,
                                                 int width, int height)
    {
        if (contourLines.isEmpty()) throw new IllegalArgumentException("Contour map is empty");
        if (rangeX <= 0 || rangeY <= 0) throw new IllegalArgumentException("All points have same X or Y coordinate");

        // Create transparent image
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        try
        {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            // Line style
            g2d.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            // Compute pixel size for degrees.
            double pixelSize = Math.max(rangeX / (width - 1.0), rangeY / (height - 1.0));
            double tolerance = pixelSize * SIMPLIFICATION_FACTOR;

            // Iterate from lowest to highest elevation (the contours are pre-sorted).
            boolean skipMajor = !style.isMajorLinesEnabled();
            boolean skipMinor = !style.isMinorLinesEnabled();
            for (ContourLine line : contourLines)
            {
                if (line.isMajor() && skipMajor) continue;
                if (!line.isMajor() && skipMinor) continue;
                g2d.setColor(new Color(style.getContourColorMapper().apply(line.getElevationMeters(), line.isMajor())));

                // Extract line vertices from the TAK kernel model so we don't
                // have to deal with JNI bridges more than once for rendering.
                // This mainly applies to larger lines, which get optimized slightly.
                LineString geometry = line.getLineGeometry();
                int pointCount = geometry.getNumPoints();
                double[] x = new double[pointCount];
                double[] y = new double[pointCount];
                for (int i = 0; i < pointCount; i++)
                {
                    x[i] = geometry.getX(i);
                    y[i] = geometry.getY(i);
                }

                int[] xPoints;
                int[] yPoints;
                int finalCount;
                if (pointCount <= OPTIMIZATION_THRESHOLD)
                {
                    // No simplification needed
                    finalCount = pointCount;
                    xPoints = new int[finalCount];
                    yPoints = new int[finalCount];

                    for (int i = 0; i < finalCount; i++)
                    {
                        double px = (x[i] - minX) / rangeX * (width - 1);
                        double py = (y[i] - minY) / rangeY * (height - 1);

                        xPoints[i] = (int) Math.round(px);
                        yPoints[i] = (int) Math.round(height - 1 - py);
                    }
                } else
                {
                    // Simplify using Douglas-Peucker
                    finalCount = simplify(x, y, minX, minY, rangeX, rangeY,
                            width, height, tolerance,
                            xPoints = new int[pointCount],
                            yPoints = new int[pointCount]);
                }

                g2d.drawPolyline(xPoints, yPoints, finalCount);
            }
        } finally
        {
            g2d.dispose();
        }

        return image;
    }

    /**
     * Simplifies a contour using the Douglas-Peucker algorithm and emits
     * the kept points to screen coordinates in one pass.
     *
     * @param x         Array of longitude values of the line.
     * @param y         Array of latitude values of the line.
     * @param minX      Minimum longitude of the view bounds.
     * @param minY      Minimum latitude of the view bounds.
     * @param rangeX    Width of the view bounds in degrees.
     * @param rangeY    Height of the view bounds in degrees.
     * @param width     Output image width in pixels.
     * @param height    Output image height in pixels.
     * @param tolerance Maximum allowed deviation in degrees.
     * @param xPoints   Output array for screen X coordinates.
     * @param yPoints   Output array for screen Y coordinates.
     * @return Number of points after simplification.
     */
    private static int simplify(double[] x, double[] y,
                                double minX, double minY,
                                double rangeX, double rangeY,
                                int width, int height,
                                double tolerance,
                                int[] xPoints, int[] yPoints)
    {

        // Setup kept point array. We will always want to keep the first and last points.
        int n = xPoints.length;
        boolean[] keep = new boolean[n];
        keep[0] = true;
        keep[n - 1] = true;

        // Simplify the line and report back which vertices to keep.
        douglasPeucker(x, y, 0, n - 1, tolerance, keep);

        // Emit only kept points back out to the xy point arrays.
        int writeIndex = 0;
        for (int i = 0; i < n; i++)
        {
            if (keep[i])
            {
                double px = (x[i] - minX) / rangeX * (width - 1);
                double py = (y[i] - minY) / rangeY * (height - 1);

                xPoints[writeIndex] = (int) Math.round(px);
                yPoints[writeIndex] = (int) Math.round(height - 1 - py);
                writeIndex++;
            }
        }
        return writeIndex;
    }

    /**
     * Recursively applies the Douglas-Peucker simplification algorithm.
     * Marks points to keep in the {@code keep} array.
     *
     * @param x         Array of longitude values of the line.
     * @param y         Array of latitude values of the line.
     * @param start     Start index of current segment.
     * @param end       End index of current segment.
     * @param tolerance Maximum allowed deviation in degrees.
     * @param keep      Output array marking which points to retain.
     */
    private static void douglasPeucker(double[] x, double[] y,
                                       int start, int end,
                                       double tolerance, boolean[] keep)
    {
        // Nothing to simplify.
        if (end - start < 2) return;

        double maxDist = 0.0;
        int index = -1;

        // Construct line segment
        double x1 = x[start];
        double y1 = y[start];
        double x2 = x[end];
        double y2 = y[end];

        // Compute the max distance of any point away from the line segment
        for (int i = start + 1; i < end; i++)
        {
            double dist = perpendicularDistance(x[i], y[i], x1, y1, x2, y2);
            if (dist > maxDist)
            {
                maxDist = dist;
                index = i;
            }
        }

        // If the point deviates too much we need to keep it.
        // Otherwise, at our tolerance level it shouldn't be noticeable if we drop it.
        if (maxDist > tolerance)
        {
            keep[index] = true;

            // We now have two line segments (joint being the worst offending point)
            // and we repeat the process for those until everything is within tolerance.
            douglasPeucker(x, y, start, index, tolerance, keep);
            douglasPeucker(x, y, index, end, tolerance, keep);
        }
    }

    /**
     * Calculates the perpendicular distance from a point to a line segment.
     *
     * @param px  Longitude of the test point.
     * @param py  Latitude of the test point.
     * @param x1  Longitude of the line start.
     * @param y1  Latitude of the line start.
     * @param x2  Longitude of the line end.
     * @param y2  Latitude of the line end.
     * @return Perpendicular distance in degrees.
     */
    private static double perpendicularDistance(double px, double py,
                                                double x1, double y1,
                                                double x2, double y2)
    {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double denom = dx * dx + dy * dy;

        if (denom == 0) return Math.hypot(px - x1, py - y1);

        double t = ((px - x1) * dx + (py - y1) * dy) / denom;
        t = Math.max(0, Math.min(1, t));

        double projX = x1 + t * dx;
        double projY = y1 + t * dy;

        return Math.hypot(px - projX, py - projY);
    }
}