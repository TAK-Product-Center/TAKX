package gov.takx.core.globe.layers.spiderfy;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import gov.nasa.worldwind.View;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.takx.api.globe.spiderfy.IIconStack;
import gov.takx.api.globe.spiderfy.IconStackExpansionShape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Utility methods to support translating between WorldWind positions and Screen Coordinates.
 *
 * @since 2.26
 */
public class IconStackPositionCalculator
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // If icons are removed from the globe, they will expire from the cache
    private Cache<IIconStack, Double> iconAreaMap = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build();

    /**
     * Gets the radius of a circle with the minimum area required to encompass the {@code icons} without having the
     * icons overlap. Only the icons image sources are used; icon positions do not affect the computation.
     *
     * @param icons The icons to encompass which the circle created by the return value will encompass.
     * @return The radius of a circle with the minimum area required to encompass the parameter.
     */
    public double getRadius(Collection<IIconStack> icons)
    {
        double totalArea = icons.stream()
                .mapToDouble(this::getIconArea)
                .sum();

        return Math.sqrt(totalArea / Math.PI);
    }

    /**
     * Maps a collection of overlapping WorldWind icons to geographical positions that do not visually overlap.
     *
     * @param icons            The icons to reposition.
     * @param expansionShape   The pattern guiding the organization of the icons around the center.
     * @param center           The screen coordinate from which all the other icons will be placed around.
     * @param radius           The distance between the center and the outside of the shape.
     * @param animationPercent Scalar between 0 and 1 used to transition the icons between their initial positions
     *                         and their final position.
     * @return A collection of overlapping WWIcons to Positions to that do not overlap.
     */
    Map<IIconStack, Position> getExpandedIconPositions(WorldWindow worldWindow,
                                                       Collection<IIconStack> icons,
                                                       IconStackExpansionShape expansionShape,
                                                       Point2D.Double center, double radius,
                                                       double animationPercent)
    {
        Map<IIconStack, Position> iconPositionMap = new HashMap<>();
        List<IIconStack> iconsList = new ArrayList<>(icons);
        View view = worldWindow.getView();

        if (expansionShape == IconStackExpansionShape.CIRCLE)
        {
            iconsList.sort(Comparator.comparing(o -> o.getImageSource().toString()));

            for (int index = 0; index < icons.size(); index++)
            {
                double radianOffset = index * 2.0 * Math.PI / icons.size();

                Point2D.Double pointOffset = getCirclePointOffset(radius, animationPercent, radianOffset);
                double animatedXPoint = center.getX() + pointOffset.getX();
                double animatedYPoint = center.getY() + pointOffset.getY();

                if (view.computeRayFromScreenPoint(animatedXPoint, animatedYPoint) != null)
                {
                    Position position = view.computePositionFromScreenPoint(
                            center.getX() + pointOffset.getX(), center.getY() + pointOffset.getY());

                    iconPositionMap.put(iconsList.get(index), position);
                }
            }
        } else
        {
            if (animationPercent < 1)
            {
                icons.forEach(icon -> {
                    Point2D.Double iconScreenPoint = getScreenPoint(worldWindow, icon.getPosition());
                    Point2D.Double animationPoint =
                            new Point2D.Double(((iconScreenPoint.getX() - center.getX()) * animationPercent) + center.getX(),
                                    ((iconScreenPoint.getY() - center.getY()) * animationPercent) + center.y);
                    Position animationPosition = view.computePositionFromScreenPoint(animationPoint.getX(), animationPoint.getY());

                    if (animationPosition != null)
                    {
                        animationPosition = new Position(animationPosition.getLatitude(), animationPosition.getLongitude(), 0);

                        iconPositionMap.put(icon, animationPosition);
                    }
                });
            }
        }

        return iconPositionMap;
    }

    /**
     * Converts a WorldWind position into screen coordinates.
     *
     * @param worldWindow WorldWind utilities.
     * @param position    The position to convert to screen coordinates.
     * @return Screen coordinates corresponding to a position on the globe.
     */
    Point2D.Double getScreenPoint(WorldWindow worldWindow, Position position)
    {
        int closestYCoordinate = 0;
        double minimumDistance = Integer.MAX_VALUE;
        double screenXCoordinate = getWorldWindScreenXCoordinate(worldWindow.getModel().getGlobe(), worldWindow.getView(), position);
        View view = worldWindow.getView();

        for (int yCoordinate = 0; yCoordinate < view.getViewport().width; yCoordinate++)
        {
            Position compareTo = view.computePositionFromScreenPoint(screenXCoordinate, yCoordinate);

            if (compareTo != null)
            {
                double distance = Math.hypot(compareTo.getLatitude().degrees - position.getLatitude().degrees,
                        compareTo.getLongitude().degrees - position.getLongitude().degrees);

                if (distance < minimumDistance)
                {
                    minimumDistance = distance;
                    closestYCoordinate = yCoordinate;
                }
            }
        }

        return new Point2D.Double(screenXCoordinate, closestYCoordinate);
    }

    /**
     * Gets the measure of how much space the icon will use in pixels.
     *
     * @param icon The element to compute the area of.
     * @return The total number of pixels contained within the icon.
     */
    private double getIconArea(IIconStack icon)
    {
        return iconAreaMap.asMap().computeIfAbsent(icon, key -> {
            if (key.getSize() != null)
            {
                return key.getSize().getHeight() * key.getSize().getWidth();
            } else if (key.getImageSource() instanceof BufferedImage)
            {
                BufferedImage bufferedImage = (BufferedImage) key.getImageSource();

                return (double) bufferedImage.getWidth() * bufferedImage.getHeight();
            } else if (key.getImageSource() instanceof URL)
            {
                try
                {
                    BufferedImage bufferedImage = ImageIO.read((URL) key.getImageSource());

                    return (double) (bufferedImage.getWidth() * bufferedImage.getHeight());
                } catch (IOException exception)
                {
                    logger.warn("Failed to determine icon's area: ", exception);
                }
            }

            return 0.0;
        });
    }

    /**
     * Get the x screen coordinate from a position.
     *
     * @param globe The WW globe we're working with.
     * @param position The position to convert to screen coordinates.
     * @param view WorldWind camera.
     * @return Screen coordinates corresponding to a position on the globe.
     */
    private double getWorldWindScreenXCoordinate(Globe globe, View view, Position position)
    {
        Vec4 cartesian = globe.computePointFromPosition(position);
        Vec4 projected = view.project(cartesian);

        return projected.x;
    }

    /**
     * Gets the point on a circle where an icon should be drawn.
     *
     * @param radius           The length of a straight line from the center to the circumference of a circle.
     * @param animationPercent Scalar to reduce the radius of the circle.
     * @param radianOffset     An index into the subdivisions the circle is decomposed into.
     * @return The point on a circle where an icon should be drawn.
     */
    private Point2D.Double getCirclePointOffset(double radius, double animationPercent, double radianOffset)
    {
        double x = radius * animationPercent * Math.cos(radianOffset);
        double y = radius * animationPercent * Math.sin(radianOffset);

        return new Point2D.Double(x, y);
    }
}
