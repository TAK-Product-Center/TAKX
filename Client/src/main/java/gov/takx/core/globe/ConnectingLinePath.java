package gov.takx.core.globe;

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.WWUtil;
import gov.nasa.worldwindx.examples.util.DirectedPath;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.core.pathsimulation.PathMeasurementTool;
import gov.takx.platform.utils.ILengthMeasurer;
import gov.takx.platform.utils.WWUtils;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Trivial subclass of {@link DirectedPath} to allow for control over rendering the directional arrow heads, limit
 * the number of subsegments generated during tesselation, and avoid using 0,0 as a reference point (which can
 * lead to very jagged lines due to resolution issues).
 *
 * @since 1.14
 */
public class ConnectingLinePath extends DirectedPath
{
    /**
     * This value limits the length of a path segment that will be subdivided using the standard algorithm.
     * Any segment longer than this will be forced to simple subdivision into 100 subsegments.
     */
    private static final int MAX_STANDARD_SEGMENT_LENGTH = 10_000; // 10 Km
    private static final int SUBSEGMENT_LIMIT = 100;
    private boolean connectingLine = true; // Whether this line is acting as a connecting line
    private boolean drawArrowHead = true;
    private boolean centerArrowHead = true;
    private Position referencePosition = null; // Current reference position for path

    /**
     * Set whether the arrow head should be centered on the line (i.e. -->--). If this is false, the arrow head will be
     * rendered at the tip of the arrow (i.e. ---->). The default value is true (centered on the line).
     *
     * @param centerArrowHead Pass {@code true} to draw arrow heads in the center of the line
     * @since 3.0
     */
    public void setCenterArrowHead(boolean centerArrowHead)
    {
        this.centerArrowHead = centerArrowHead;
    }

    /**
     * Set whether the arrow head is being drawn or not (default is true).
     *
     * @param drawArrowHead Pass {@code true} to draw directional arrow heads
     */
    public void setDrawArrowHead(boolean drawArrowHead)
    {
        this.drawArrowHead = drawArrowHead;
    }

    /**
     * @return Whether this path is drawing the arrow head.
     * @since 3.5
     */
    public boolean isDrawArrowHead()
    {
        return drawArrowHead;
    }

    /**
     * @return Whether this path is representing a connecting line between track points.
     * @since 3.0
     */
    public boolean isConnectingLine()
    {
        return connectingLine;
    }

    /**
     * Set whether this path is representing a connecting line between track points (default is true).
     *
     * @param connectingLine True if this class represents a connecting line
     * @since 3.0
     */
    public void setConnectingLine(boolean connectingLine)
    {
        this.connectingLine = connectingLine;
    }

    /**
     * If we should draw arrow heads, then dispatch to our super class.  If not, then just do nothing.
     */
    @Override
    protected void drawDirectionArrows(DrawContext dc, PathData pd)
    {
        if (drawArrowHead) super.drawDirectionArrows(dc, pd);
    }

    /**
     * Overridden to avoid performing the arrow calculations if we are not going to render them.
     */
    @Override
    protected void computeDirectionArrows(DrawContext drawContext, PathData pathData)
    {
        if (drawArrowHead) super.computeDirectionArrows(drawContext, pathData);
    }

    /**
     * Overridden to allow drawing the arrow heads at the end vs the center if desired.
     *
     * @since 3.0
     */
    @Override
    protected void computeArrowheadGeometry(DrawContext dc, int poleA, int poleB, Vec4 polePtA, Vec4 polePtB,
                                            FloatBuffer buffer, PathData pathData)
    {
        if (centerArrowHead)
        {
            super.computeArrowheadGeometry(dc, poleA, poleB, polePtA, polePtB, buffer, pathData);
            return;
        }

        double arrowLength = getArrowLength(); // Length of the arrow head
        double arrowBase = arrowLength * getArrowAngle().tanHalfAngle(); // Width of the arrow head base
        double poleDistance = polePtA.distanceTo3(polePtB); // Length of the arrow pole
        Vec4 parallel = polePtA.subtract3(polePtB); // Vector parallel to the arrow pole
        Vec4 surfaceNormal = dc.getGlobe().computeSurfaceNormalAtPoint(polePtB);
        Vec4 perpendicular = surfaceNormal.cross3(parallel); // Vector perpendicular to the arrow pole on globe surface
        Vec4 endPoint = polePtB; // Use the arrow pole end as the tip of the arrow head

        if (!isArrowheadSmall(dc, endPoint, 1))
        {
            View view = dc.getView();
            double midpointDistance = view.getEyePoint().distanceTo3(endPoint);
            double pixelSize = view.computePixelSizeAtDistance(midpointDistance);
            if (arrowLength / pixelSize > maxScreenSize)
            {
                arrowLength = maxScreenSize * pixelSize;
                arrowBase = arrowLength * getArrowAngle().tanHalfAngle();
            }

            if (poleDistance <= arrowLength) return; // Bail if length of the arrow head is longer than the arrow pole

            perpendicular = perpendicular.normalize3().multiply3(arrowBase); // Vector for half the arrow head base
            parallel = parallel.normalize3().multiply3(arrowLength); // Vector for the length of the arrow head
            Vec4 vertex1 = endPoint.add3(parallel).add3(perpendicular);
            Vec4 vertex2 = endPoint.add3(parallel).add3(perpendicular.multiply3(-1.0D));
            Vec4 referencePoint = pathData.getReferencePoint();
            buffer.put((float) (vertex1.x - referencePoint.x));
            buffer.put((float) (vertex1.y - referencePoint.y));
            buffer.put((float) (vertex1.z - referencePoint.z));
            buffer.put((float) (vertex2.x - referencePoint.x));
            buffer.put((float) (vertex2.y - referencePoint.y));
            buffer.put((float) (vertex2.z - referencePoint.z));
            buffer.put((float) (endPoint.x - referencePoint.x));
            buffer.put((float) (endPoint.y - referencePoint.y));
            buffer.put((float) (endPoint.z - referencePoint.z));
        }
    }

    /**
     * Override to avoid using [0,0] as a reference position.  Fix for CORE-2171.
     *
     * @return Reference position for path
     */
    @Override
    public Position getReferencePosition()
    {
        // If we don't have a reference position yet, then find it.  We are looking for the first non-[0,0]
        // position.  [0,0] often results from bogus data and when the rest of the positions come through
        // properly, they are typically across the globe from [0,0] resulting in a very jagged line rendering
        // due to resolution problems, since a float is used for line location calculations.

        if (referencePosition == null && positions != null)
        {
            for (Position position : positions)
            {
                //noinspection OrredNotEqualExpression
                if (position.latitude.degrees != 0.0 || position.longitude.degrees != 0.0)
                {
                    // Found a non-[0,0] position, record it and break out.
                    referencePosition = position;
                    break;
                }
            }
        }

        return referencePosition;
    }

    /**
     * Update the position list for this path.  Nuke our current reference position so it will be recalculated.
     *
     * @param positions New position list
     */
    @Override
    public void setPositions(Iterable<? extends Position> positions)
    {
        referencePosition = null; // throw away any previous reference position since we have a new set of positions
        super.setPositions(positions);
    }

    /**
     * Overridden to limit the number of subsegments generated for very long segments.
     * <p/>
     * {@inheritDoc}
     */
    @Override
    protected void makeSegment(DrawContext dc, Position posA, Position posB, Vec4 ptA, Vec4 ptB, Color colorA,
                               Color colorB, int ordinalA, int ordinalB, PathData pathData)
    {
        // This method does not add the first position of the segment to the position list. It adds only the
        // subsequent positions, including the segment's last position.

        boolean straightLine = AVKey.LINEAR.equals(getPathType()) && !isFollowTerrain();

        double arcLength;
        if (straightLine)
        {
            arcLength = ptA.distanceTo3(ptB);
        } else
        {
            arcLength = computeSegmentLength(dc, posA, posB);
        }

        if (arcLength <= 0 || straightLine)
        {
            if (!ptA.equals(ptB))
            {
                addTessellatedPosition(posB, colorB, ordinalB, pathData);
            }
            return;
        }

        final boolean followTerrain = isFollowTerrain();
        final boolean limitSubsegments = followTerrain && arcLength > MAX_STANDARD_SEGMENT_LENGTH;
        final int nSubsegments = limitSubsegments ? SUBSEGMENT_LIMIT : numSubsegments;

        // Variables for great circle and rhumb computation.
        Angle segmentAzimuth = null;
        Angle segmentDistance = null;

        for (double s = 0, p = 0; s < 1;)
        {
            if (followTerrain && !limitSubsegments)
            {
                p += terrainConformance * dc.getView().computePixelSizeAtDistance(
                        ptA.distanceTo3(dc.getView().getEyePoint()));
            } else
            {
                p += arcLength / nSubsegments;
            }

            Position pos;
            Color color;

            s = p / arcLength;
            if (s >= 1)
            {
                pos = posB;
                color = colorB;
            } else if (AVKey.LINEAR.equals(pathType))
            {
                if (segmentAzimuth == null)
                {
                    segmentAzimuth = LatLon.linearAzimuth(posA, posB);
                    segmentDistance = LatLon.linearDistance(posA, posB);
                }
                Angle distance = Angle.fromRadians(s * segmentDistance.radians);
                LatLon latLon = LatLon.linearEndPosition(posA, segmentAzimuth, distance);
                pos = new Position(latLon, (1 - s) * posA.getElevation() + s * posB.getElevation());
                color = (colorA != null && colorB != null) ? WWUtil.interpolateColor(s, colorA, colorB) : null;
            } else if (AVKey.RHUMB_LINE.equals(pathType) || AVKey.LOXODROME.equals(pathType))
            {
                if (segmentAzimuth == null)
                {
                    segmentAzimuth = LatLon.rhumbAzimuth(posA, posB);
                    segmentDistance = LatLon.rhumbDistance(posA, posB);
                }
                Angle distance = Angle.fromRadians(s * segmentDistance.radians);
                LatLon latLon = LatLon.rhumbEndPosition(posA, segmentAzimuth, distance);
                pos = new Position(latLon, (1 - s) * posA.getElevation() + s * posB.getElevation());
                color = (colorA != null && colorB != null) ? WWUtil.interpolateColor(s, colorA, colorB) : null;
            } else // GREAT_CIRCLE
            {
                if (segmentAzimuth == null)
                {
                    segmentAzimuth = LatLon.greatCircleAzimuth(posA, posB);
                    segmentDistance = LatLon.greatCircleDistance(posA, posB);
                }
                Angle distance = Angle.fromRadians(s * segmentDistance.radians);
                LatLon latLon = LatLon.greatCircleEndPosition(posA, segmentAzimuth, distance);
                pos = new Position(latLon, (1 - s) * posA.getElevation() + s * posB.getElevation());
                color = (colorA != null && colorB != null) ? WWUtil.interpolateColor(s, colorA, colorB) : null;
            }

            addTessellatedPosition(pos, color, s >= 1 ? ordinalB : null, pathData);

            ptA = ptB;
        }
    }

    /**
     * @return the center point along the path
     */
    public ILocation getCenterPoint()
    {
        List<? extends LatLon> positionList;
        if (positions instanceof List<? extends LatLon> arrayList)
        {
            positionList = arrayList;
        } else
        {
            ArrayList<LatLon> latLonList = new ArrayList<>();
            positions.forEach(latLonList::add);
            positionList = latLonList;
        }
        PathMeasurementTool pathMeasurementTool = new PathMeasurementTool(positionList);
        double length = pathMeasurementTool.computeLength();
        if (length == 0) return WWUtils.latLonToRaptorLocation(positionList.get(0));

        pathMeasurementTool.setMeasurementPathType(ILengthMeasurer.MeasurementPathType.GreatCircle);
        pathMeasurementTool.setMaxSegmentLength(length / 2);
        List<ILocation> pathLocations = pathMeasurementTool.getPathLocations();
        return pathLocations.get(pathLocations.size() / 2);
    }
}
