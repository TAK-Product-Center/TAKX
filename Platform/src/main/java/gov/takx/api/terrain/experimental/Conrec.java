package gov.takx.api.terrain.experimental;

/**
 * Conrec.java
 * <p>
 * Created on 5 August 2001, 15:03
 * <p>
 * Copyright (c) 1996-1997 Nicholas Yue
 * <p>
 * This software is copyrighted by Nicholas Yue. This code is base on the work of
 * Paul D. Bourke CONREC.F routine
 * <p>
 * The authors hereby grant permission to use, copy, and distribute this
 * software and its documentation for any purpose, provided that existing
 * copyright notices are retained in all copies and that this notice is included
 * verbatim in any distributions. Additionally, the authors grant permission to
 * modify this software and its documentation for any purpose, provided that
 * such modifications are not distributed without the explicit consent of the
 * authors and that existing copyright notices are retained in all copies. Some
 * of the algorithms implemented by this software are patented, observe all
 * applicable patent law.
 * <p>
 * IN NO EVENT SHALL THE AUTHORS OR DISTRIBUTORS BE LIABLE TO ANY PARTY FOR
 * DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT
 * OF THE USE OF THIS SOFTWARE, ITS DOCUMENTATION, OR ANY DERIVATIVES THEREOF,
 * EVEN IF THE AUTHORS HAVE BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * <p>
 * THE AUTHORS AND DISTRIBUTORS SPECIFICALLY DISCLAIM ANY WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE, AND NON-INFRINGEMENT.  THIS SOFTWARE IS PROVIDED ON AN
 * "AS IS" BASIS, AND THE AUTHORS AND DISTRIBUTORS HAVE NO OBLIGATION TO PROVIDE
 * MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 */

import gov.takx.api.annotation.Experimental;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Conrec a straightforward method of contouring some surface represented a regular triangular mesh.
 * <p/>
 * Ported from the C++ code by Nicholas Yue (see above copyright notice).
 *
 * @author Bradley White
 * @version 1.0
 * @see <a href="http://local.wasp.uwa.edu.au/~pbourke/papers/conrec">for full description of code and original C++ source</a>
 */
@Experimental
public class Conrec
{
    public static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    public static double[] contours = new double[]{100};
    public static Map<Double, Color> colorContourThresholdMap = new HashMap<Double, Color>();

    static
    {
        for (int i = 0, contoursLength = contours.length; i < contoursLength; i++)
        {
            double contour = contours[i];
            Color c = new Color(
                    ((contoursLength - i) * Color.blue.getRed() + i * Color.red.getRed()) / contoursLength,
                    ((contoursLength - i) * Color.blue.getGreen() + i * Color.red.getGreen()) / contoursLength,
                    ((contoursLength - i) * Color.blue.getBlue() + i * Color.red.getBlue()) / contoursLength);
            colorContourThresholdMap.put(contour, c);
        }
    }

    /**
     * contour is a contouring subroutine for rectangularily spaced data
     * <p/>
     * It emits calls to a line drawing subroutine supplied by the user which draws a contour map corresponding to
     * real*4data on a randomly spaced rectangular grid. The coordinates emitted are in the same units given in the
     * xAxisCoords() and yAxisCoords() arrays.
     * <p/>
     * Any number of contour levels may be specified but they must be in order of increasing value.
     *
     * @param data              - matrix of data to contour
     * @param xAxisCoordinates  - data matrix column coordinates
     * @param yAxisCoordinates  - data matrix row coordinates
     * @param contourThresholds - contour levels in increasing order.
     */
    public static Map<Double, List<List<Double[]>>> buildContourLines(double[][] data,
                                                                      double[] xAxisCoordinates,
                                                                      double[] yAxisCoordinates,
                                                                      double[] contourThresholds)
    {

        //TODO: This is a fix to get the 100% contour to draw correctly.  There should be a fix in conrec for this.
        double[] alteredThresholds = new double[contourThresholds.length + 1];
        int i = 0;
        for (; i < contourThresholds.length; i++)
        {
            alteredThresholds[i] = contourThresholds[i];
        }
        alteredThresholds[i] = getMaxValue(data, contourThresholds) + 1;
        Map<Double, java.util.List<Double[]>> unorderedSegments = Conrec.buildContourSegments(data, xAxisCoordinates,
                yAxisCoordinates, alteredThresholds);
        return connectSegments(unorderedSegments);
    }

    private static double getMaxValue(double[][] data, double[] contourThresholds)
    {
        double max = data[0][0];
        for (double[] dataLine : data)
        {
            for (double value : dataLine)
            {
                if (value > max)
                {
                    max = value;
                }
            }
        }

        for (double t : contourThresholds)
        {
            if (t > max)
            {
                max = t;
            }
        }
        return max;
    }

    private static Double[] isNextConnectingSegment(Double[] currentPathEndPoint,
                                                    Double[] potentialNextSegment)
    {
        if (currentPathEndPoint[0].equals(potentialNextSegment[X1]) &&
                currentPathEndPoint[1].equals(potentialNextSegment[Y1]))
        {
            return new Double[]{potentialNextSegment[X2], potentialNextSegment[Y2]};
        } else if (currentPathEndPoint[0].equals(potentialNextSegment[X2]) &&
                currentPathEndPoint[1].equals(potentialNextSegment[Y2]))
        {
            return new Double[]{potentialNextSegment[X1], potentialNextSegment[Y1]};
        }
        return null;
    }

    private static Double[] isPreviousConnectingSegment(Double[] currentPathStartPoint,
                                                        Double[] potentialPreviousSegment)
    {
        if (currentPathStartPoint[0].equals(potentialPreviousSegment[X2]) &&
                currentPathStartPoint[1].equals(potentialPreviousSegment[Y2]))
        {
            return new Double[]{potentialPreviousSegment[X1], potentialPreviousSegment[Y1]};
        } else if (currentPathStartPoint[0].equals(potentialPreviousSegment[X1]) &&
                currentPathStartPoint[1].equals(potentialPreviousSegment[Y1]))
        {
            return new Double[]{potentialPreviousSegment[X2], potentialPreviousSegment[Y2]};
        } else
        {
            return null;
        }
    }

    /**
     * Creates a map of Thresholds as the key and a list of coordinates representing an ordered path as the value.
     *
     * @param unorderedSegmentsMap - a map of thresholds and ordered paths
     * @return The paths can be shapes or a polyline
     */
    private static Map<Double, List<List<Double[]>>> connectSegments(Map<Double, List<Double[]>> unorderedSegmentsMap)
    {
        Map<Double, List<List<Double[]>>> connectedSegments = new HashMap<Double, List<List<Double[]>>>();
        for (Double threshold : unorderedSegmentsMap.keySet())
        {
            List<Double[]> unorderedSegments = unorderedSegmentsMap.get(threshold);
            if (unorderedSegments.size() == 0)
            {
                continue;
            }
            List<List<Double[]>> orderedSegments = new ArrayList<List<Double[]>>();
            connectedSegments.put(threshold, orderedSegments);

            List<Double[]> currentOrderedSegments = new ArrayList<Double[]>();
            orderedSegments.add(currentOrderedSegments);
            Double[] currentPath = unorderedSegments.remove(0);
            Double[] currentPathStartPoint = {currentPath[X1], currentPath[Y1]};
            Double[] currentPathEndPoint = {currentPath[X2], currentPath[Y2]};
            currentOrderedSegments.add(currentPath);

            Double[] nextSegment = null;
            Double[] previousSegment = null;

            // Continue until we have added all of the unordered segments
            while (!unorderedSegments.isEmpty())
            {
                for (Double[] currentSegment : unorderedSegments)
                {
                    // Determine if the segment needs to go before the 1st point in the path
                    if (isPreviousConnectingSegment(currentPathStartPoint, currentSegment) != null)
                    {
                        currentPathStartPoint = isPreviousConnectingSegment(currentPathStartPoint, currentSegment);
                        // Do we have a backwards segment?
                        if (!currentPathStartPoint[0].equals(currentSegment[X1]) ||
                                !currentPathStartPoint[1].equals(currentSegment[Y1]))
                        {
                            double x1 = currentSegment[X2];
                            double y1 = currentSegment[Y2];
                            // Switch the indices since they are backwards
                            currentSegment[X2] = currentSegment[X1];
                            currentSegment[Y2] = currentSegment[Y1];
                            currentSegment[X1] = x1;
                            currentSegment[Y1] = y1;
                        }
                        previousSegment = currentSegment;
                        break;
                    }
                    // Determine if the segment needs to go after the last point in the path
                    if (isNextConnectingSegment(currentPathEndPoint, currentSegment) != null)
                    {
                        currentPathEndPoint = isNextConnectingSegment(currentPathEndPoint, currentSegment);
                        // Do we have a backwards segment?
                        if (!currentPathEndPoint[0].equals(currentSegment[X2]) ||
                                !currentPathEndPoint[1].equals(currentSegment[Y2]))
                        {
                            double x1 = currentSegment[X2];
                            double y1 = currentSegment[Y2];
                            // Switch the indices since they are backwards
                            currentSegment[X2] = currentSegment[X1];
                            currentSegment[Y2] = currentSegment[Y1];
                            currentSegment[X1] = x1;
                            currentSegment[Y1] = y1;
                        }
                        nextSegment = currentSegment;
                        break;
                    }
                }
                if (nextSegment != null)
                {
                    // Add the new segment to the end of the path
                    currentOrderedSegments.add(nextSegment);
                    unorderedSegments.remove(nextSegment);
                    nextSegment = null;
                } else if (previousSegment != null)
                {
                    // Add the new segment to the beginning of the path
                    currentOrderedSegments.add(0, previousSegment);
                    unorderedSegments.remove(previousSegment);
                    previousSegment = null;
                } else
                {
                    // No more segments found for this path so start a new path
                    if (!unorderedSegments.isEmpty())
                    {
                        currentOrderedSegments = new ArrayList<Double[]>();
                        orderedSegments.add(currentOrderedSegments);
                        currentPath = unorderedSegments.remove(0);
                        currentPathStartPoint[0] = currentPath[X1];
                        currentPathStartPoint[1] = currentPath[Y1];
                        currentPathEndPoint[0] = currentPath[X2];
                        currentPathEndPoint[1] = currentPath[Y2];
                        currentOrderedSegments.add(currentPath);
                    }
                }
            }
        }

        return connectedSegments;
    }

    // These constants are just fields 0-3 of an array representing the coordinates of a line segment
    public static final int X1 = 0;
    public static final int Y1 = 1;
    public static final int X2 = 2;
    public static final int Y2 = 3;

    // Graph size
    private static final int SIZE = 1000;

    /**
     * contour is a contouring subroutine for rectangularily spaced data
     * <p/>
     * It emits calls to a line drawing subroutine supplied by the user which draws a contour map corresponding to
     * real*4data on a randomly spaced rectangular grid. The coordinates emitted are in the same units given in the
     * yAxisCoords() and xAxisCoords() arrays.
     * <p/>
     * Any number of contour levels may be specified but they must be in order of increasing value.
     *
     * @param data              - matrix of data to contour
     * @param yAxisCoords       - data matrix column coordinates
     * @param xAxisCoords       - data matrix row coordinates
     * @param contourThresholds - contour levels in increasing order.
     */
    @SuppressWarnings("IdentityBinaryExpression") // Line 444 - test for bottom line equality may be wrong
    private static Map<Double, List<Double[]>> buildContourSegments(double[][] data,
                                                                    final double[] xAxisCoords,
                                                                    final double[] yAxisCoords,
                                                                    double[] contourThresholds)
    {
        double[] h = new double[5];
        int[] sh = new int[5];
        double[] xh = new double[5];
        double[] yh = new double[5];

        Map<Double, List<Double[]>> unorderedResults = new HashMap<Double, List<Double[]>>();
        int m1;
        int m2;
        int m3;
        int case_value;
        double dmin;
        double dmax;
        double x1 = 0.0;
        double x2 = 0.0;
        double y1 = 0.0;
        double y2 = 0.0;
        int row, col, thresholdIndex, m;

        // The indexing of im and jm should be noted as it has to start from zero
        // unlike the fortran counter part
        int[] im = {0, 1, 1, 0};
        // mapping from vertex numbers to y offsets
        int[] jm = {0, 0, 1, 1};

        // Note that castab is arranged differently from the FORTRAN code because
        // Fortran and C/C++ arrays are transposed of each other, in this case
        // it is more tricky as castab is in 3 dimension
        int[][][] castab =
                {
                        {
                                {0, 0, 8}, {0, 2, 5}, {7, 6, 9}
                        },
                        {
                                {0, 3, 4}, {1, 3, 1}, {4, 3, 0}
                        },
                        {
                                {9, 6, 7}, {5, 2, 0}, {8, 0, 0}
                        }
                };

        for (col = (xAxisCoords.length - 2); col >= 0; col--)
        {
            for (row = 0; row <= yAxisCoords.length - 2; row++)
            {
                double temp1, temp2;
                temp1 = Math.min(data[row][col], data[row][col + 1]);
                temp2 = Math.min(data[row + 1][col], data[row + 1][col + 1]);
                dmin = Math.min(temp1, temp2);
                temp1 = Math.max(data[row][col], data[row][col + 1]);
                temp2 = Math.max(data[row + 1][col], data[row + 1][col + 1]);
                dmax = Math.max(temp1, temp2);

                if (dmax >= contourThresholds[0] && dmin <= contourThresholds[contourThresholds.length - 1])
                {
                    //TODO: This is a fix to get the 100% contour to draw correctly.  Above we
                    // add a 101 on to the end of thresholds array.  We now want to ignore that value.  This
                    // can be fixed in the future.
                    for (thresholdIndex = 0; thresholdIndex < contourThresholds.length - 1; thresholdIndex++)
                    {
                        if (contourThresholds[thresholdIndex] >= dmin && contourThresholds[thresholdIndex] <= dmax)
                        {
                            for (m = 4; m >= 0; m--)
                            {
                                if (m > 0)
                                {
                                    // The indexing of im and jm should be noted as it has to start from zero
                                    h[m] = data[row + im[m - 1]][col + jm[m - 1]] - contourThresholds[thresholdIndex];
                                    xh[m] = yAxisCoords[row + im[m - 1]];
                                    yh[m] = xAxisCoords[col + jm[m - 1]];
                                } else
                                {
                                    h[0] = 0.25 * (h[1] + h[2] + h[3] + h[4]);
                                    xh[0] = 0.5 * (yAxisCoords[row] + yAxisCoords[row + 1]);
                                    yh[0] = 0.5 * (xAxisCoords[col] + xAxisCoords[col + 1]);
                                }
                                if (h[m] > 0.0)
                                {
                                    sh[m] = 1;
                                } else if (h[m] < 0.0)
                                {
                                    sh[m] = -1;
                                } else
                                {
                                    sh[m] = 0;
                                }
                            }
                            boolean drawSinglePoint = false;

                            double thresholdValue = contourThresholds[thresholdIndex];
                            double nextThresholdValue;
                            if (thresholdIndex + 1 < contourThresholds.length)
                            {
                                nextThresholdValue = contourThresholds[thresholdIndex + 1];
                            } else
                            {
                                nextThresholdValue = 100;
                            }

                            // Check to see if this is a single point instead of a
                            // line by checking the surrounding points
                            if ((data[row][col] == thresholdValue) &&
                            // Check to see if all adjacent values are < threshold
                            // Right
                                    (((col + 1 == xAxisCoords.length - 2 ||
                                            data[row][col + 1] < thresholdValue) &&
                                    // Top
                                            (row + 1 == yAxisCoords.length - 2 ||
                                                    data[row + 1][col] < thresholdValue)
                                            &&
                                            // Bottom
                                            (row - 1 < 0 || data[row - 1][col] < thresholdValue) &&
                                            // Left
                                            (col - 1 < 0 || data[row][col - 1] < thresholdValue)) ||
                                    // OR Check to see if all adjacent values are > threshold
                                    // Right
                                            ((col + 1 == xAxisCoords.length - 2 ||
                                                    data[row][col + 1] > thresholdValue) &&
                                            // Top
                                                    (row + 1 == yAxisCoords.length - 2 ||
                                                            data[row + 1][col] > thresholdValue)
                                                    &&
                                                    // Bottom
                                                    (row - 1 < 0 || data[row - 1][col] > thresholdValue) &&
                                                    // Left
                                                    (col - 1 < 0 || data[row][col - 1] > thresholdValue))))
                            {
                                drawSinglePoint = true;
                            }

                            boolean drawTopLine = true;
                            boolean drawRightLine = true;
                            boolean drawBottomLine = true;
                            boolean drawLeftLine = true;

                            // Check any side of our box to see if they have the same values as the max threshold
                            // If they do then we may need to skip drawing some of the sides
                            if (
                            // Top line
                            (data[row][col] == thresholdValue && data[row][col + 1] == thresholdValue)
                                    ||
                                    // Right line
                                    (data[row][col + 1] ==
                                            thresholdValue && data[row + 1][col + 1] == thresholdValue)
                                    ||
                                    // Left line
                                    (data[row][col] == thresholdValue && data[row + 1][col] == thresholdValue)
                                    ||
                                    //Bottom line
                                    (data[row + 1][col + 1] == thresholdValue &&
                                            data[row + 1][col + 1] == thresholdValue))
                            {
                                // Value above vertex 4
                                double aboveV4;
                                // Value above vertex 3
                                double aboveV3;
                                // Value below vertex 2
                                double belowV2;
                                // Value below vertex 1
                                double belowV1;
                                // Value to the right of vertex 3
                                double rightV3;
                                // Value to the right of vertex 2
                                double rightV2;
                                // Value to the left of vertex 1
                                double leftV1;
                                // Value to the left of vertex 4
                                double leftV4;

                                // Look above v4 and v3
                                if (row - 1 >= 0)
                                {
                                    aboveV4 = data[row - 1][col];
                                    aboveV3 = data[row - 1][col + 1];
                                    double belowV4 = data[row + 1][col];
                                    double belowV3 = data[row + 1][col + 1];

                                    if (areSegmentsWithinThreshold(
                                            aboveV4,
                                            aboveV3,
                                            belowV4,
                                            belowV3,
                                            thresholdValue,
                                            nextThresholdValue))
                                    {
                                        drawTopLine = false;
                                    }
                                }

                                // Look below v1 and v2
                                if (row + 2 < yAxisCoords.length)
                                {
                                    belowV1 = data[row + 2][col];
                                    belowV2 = data[row + 2][col + 1];
                                    double aboveV1 = data[row][col];
                                    double aboveV2 = data[row][col + 1];
                                    if (areSegmentsWithinThreshold(
                                            belowV1,
                                            belowV2,
                                            aboveV1,
                                            aboveV2,
                                            thresholdValue,
                                            nextThresholdValue))
                                    {
                                        drawBottomLine = false;
                                    }
                                }

                                // Look to the right of v3
                                if (col + 2 < xAxisCoords.length)
                                {
                                    rightV3 = data[row][col + 2];
                                    rightV2 = data[row + 1][col + 2];
                                    double leftV3 = data[row][col];
                                    double leftV2 = data[row + 1][col];
                                    if (areSegmentsWithinThreshold(
                                            rightV3,
                                            rightV2,
                                            leftV3,
                                            leftV2,
                                            thresholdValue,
                                            nextThresholdValue))
                                    {
                                        drawRightLine = false;
                                    }
                                }

                                // Look to the left of v4
                                if (col - 1 >= 0)
                                {
                                    leftV4 = data[row][col - 1];
                                    leftV1 = data[row + 1][col - 1];
                                    double rightV4 = data[row][col + 1];
                                    double rightV1 = data[row + 1][col + 1];
                                    if (areSegmentsWithinThreshold(
                                            leftV4,
                                            leftV1,
                                            rightV4,
                                            rightV1,
                                            thresholdValue,
                                            nextThresholdValue))
                                    {
                                        drawLeftLine = false;
                                    }
                                }
                            }

                            //
                            // Note: at this stage the relative heights of the corners and the
                            // centre are in the h array, and the corresponding coordinates are
                            // in the xh and yh arrays. The centre of the box is indexed by 0
                            // and the 4 corners by 1 to 4 as shown below.
                            // Each triangle is then indexed by the parameter m, and the 3
                            // vertices of each triangle are indexed by parameters m1,m2,and
                            // m3.
                            // It is assumed that the centre of the box is always vertex 2
                            // though this is important only when all 3 vertices lie exactly on
                            // the same contour level, in which case only the side of the box
                            // is drawn.
                            //
                            //
                            //      vertex 4 +-------------------+ vertex 3
                            //               | \               / |
                            //               |   \    m-3    /   |
                            //               |     \       /     |
                            //               |       \   /       |
                            //               |  m=2    X   m=2   |       the centre is vertex 0
                            //               |       /   \       |
                            //               |     /       \     |
                            //               |   /    m=1    \   |
                            //               | /               \ |
                            //      vertex 1 +-------------------+ vertex 2
                            //
                            //
                            //
                            //               Scan each triangle in the box
                            //
                            for (m = 1; m <= 4; m++)
                            {
                                m1 = m;
                                m2 = 0;
                                if (m != 4)
                                {
                                    m3 = m + 1;
                                } else
                                {
                                    m3 = 1;
                                }
                                case_value = castab[sh[m1] + 1][sh[m2] + 1][sh[m3] + 1];
                                // We only want to do this once
                                if (drawSinglePoint && m == 1)
                                {
                                    case_value = 10;
                                }
                                if (case_value != 0)
                                {
                                    switch (case_value)
                                    {
                                        case 1: // Line between vertices 1 and 2
                                            x1 = xh[m1];
                                            y1 = yh[m1];
                                            x2 = xh[m2];
                                            y2 = yh[m2];
                                            break;
                                        case 2: // Line between vertices 2 and 3
                                            x1 = xh[m2];
                                            y1 = yh[m2];
                                            x2 = xh[m3];
                                            y2 = yh[m3];
                                            break;
                                        case 3: // Line between vertices 3 and 1
                                            x1 = xh[m3];
                                            y1 = yh[m3];
                                            x2 = xh[m1];
                                            y2 = yh[m1];
                                            // Skip top
                                            if (x1 == x2 && (m3 == 1 && m1 == 4 && !drawTopLine))
                                            {
                                                continue;
                                            }
                                            // Skip bottom line
                                            if (m3 == 3 && m1 == 2 && !drawBottomLine)
                                            {
                                                continue;
                                            }
                                            // Skip left line
                                            if (y1 == y2 && (m3 == 2 && m1 == 1 && !drawLeftLine))
                                            {
                                                continue;
                                            }
                                            // Skip right line
                                            if (m3 == 4 && m1 == 3 && !drawRightLine)
                                            {
                                                continue;
                                            }
                                            break;
                                        case 4: // Line between vertex 1 and side 2-3
                                            x1 = xh[m1];
                                            y1 = yh[m1];
                                            x2 = xsect(m2, m3, h, xh);
                                            y2 = ysect(m2, m3, h, yh);
                                            break;
                                        case 5: // Line between vertex 2 and side 3-1
                                            x1 = xh[m2];
                                            y1 = yh[m2];
                                            x2 = xsect(m3, m1, h, xh);
                                            y2 = ysect(m3, m1, h, yh);
                                            break;
                                        case 6: //  Line between vertex 3 and side 1-2
                                            x1 = xh[m3];
                                            y1 = yh[m3];
                                            x2 = xsect(m1, m2, h, xh);
                                            y2 = ysect(m1, m2, h, yh);
                                            break;
                                        case 7: // Line between sides 1-2 and 2-3
                                            x1 = xsect(m1, m2, h, xh);
                                            y1 = ysect(m1, m2, h, yh);
                                            x2 = xsect(m2, m3, h, xh);
                                            y2 = ysect(m2, m3, h, yh);
                                            break;
                                        case 8: // Line between sides 2-3 and 3-1
                                            x1 = xsect(m2, m3, h, xh);
                                            y1 = ysect(m2, m3, h, yh);
                                            x2 = xsect(m3, m1, h, xh);
                                            y2 = ysect(m3, m1, h, yh);
                                            break;
                                        case 9: // Line between sides 3-1 and 1-2
                                            x1 = xsect(m3, m1, h, xh);
                                            y1 = ysect(m3, m1, h, yh);
                                            x2 = xsect(m1, m2, h, xh);
                                            y2 = ysect(m1, m2, h, yh);
                                            break;
                                        case 10: // Draw a dot at vertex 1
                                            int index = 1;
                                            x1 = xh[index];
                                            y1 = yh[index];
                                            x2 = xh[index];
                                            y2 = yh[index];
                                            break;
                                        default:
                                            break;
                                    }
                                    List<Double[]> segmentList = unorderedResults.get(
                                            contourThresholds[thresholdIndex]);
                                    if (segmentList == null)
                                    {
                                        segmentList = new ArrayList<Double[]>();
                                        unorderedResults.put(contourThresholds[thresholdIndex], segmentList);
                                    }
                                    // Add our new segment to the list
                                    segmentList.add(new Double[]{y1, x1, y2, x2});
                                }
                            }
                        }
                    }
                }
            }
        }
        return unorderedResults;
    }

    private static boolean areSegmentsWithinThreshold(double segment1Point1,
                                                      double segment1Point2,
                                                      double segment2Point1,
                                                      double segment2Point2,
                                                      double thresholdValue,
                                                      double nextThresholdValue)
    {
        // Are the points in the first segment within our thresholds?
        return (segment1Point1 >= thresholdValue && segment1Point1 < nextThresholdValue) &&
                (segment1Point2 >= thresholdValue && segment1Point2 < nextThresholdValue)
                // Are the points in the second segment within our thresholds?
                && (segment2Point1 >= thresholdValue && segment2Point1 < nextThresholdValue) &&
                (segment2Point2 >= thresholdValue && segment2Point2 < nextThresholdValue);
    }

    private static double xsect(int p1, int p2, double[] h, double[] xh)
    {
        return (h[p2] * xh[p1] - h[p1] * xh[p2]) / (h[p2] - h[p1]);
    }

    private static double ysect(int p1, int p2, double[] h, double[] yh)
    {
        return (h[p2] * yh[p1] - h[p1] * yh[p2]) / (h[p2] - h[p1]);
    }
}
