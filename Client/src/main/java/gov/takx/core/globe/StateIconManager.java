package gov.takx.core.globe;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.globe.IGlobeDelegate;
import gov.takx.api.globe.IGlobeMarkerMapObject;
import gov.takx.commons.constants.State;
import gov.takx.commons.swing.GraphicsUtils;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.platform.utils.StateIconUtils;
import jakarta.inject.Inject;

import javax.swing.Timer;

import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 */
@PseudoBean
public class StateIconManager
{
    private static final double SCALE_FACTOR = 0.6; // The factor that the pulsing images scale is increased by each frame.
    private static final int NUM_FRAMES = 4; // The number of frames it takes to cycle through a pulsing image
    private static final int FRAME_RATE = 500; // MS between animation frames

    @Inject
    private IExtendedGlobeManager globeManager;

    @Inject
    private StateIconUtils stateIconUtils;

    private final Map<IGlobeMarkerMapObject, IconState> animatedIcons = new ConcurrentHashMap<>();
    private Timer animationTimer;
    private final Map<State, IconState> iconStates = new EnumMap<>(State.class);
    private boolean iconsInitialized;

    StateIconManager()
    {
    }

    public void changeState(IGlobeMarkerMapObject globeMarkerMapObject, State state)
    {
        initIcons();

        IconState iconState = iconStates.get(state);
        if (iconState == null) return;

        // Update our animation handling based on the action of this icons state
        if (iconState.action() == Action.NONE)
        {
            removeAnimatedIcon(globeMarkerMapObject);
        } else
        {
            addAnimatedIcon(globeMarkerMapObject, iconState);
        }

        // Moved this back out of the condition below, because the new state was not getting
        // drawn over the previous state if there was only a SINGLE data monitor for the icon.  In the case
        // of a critical being cleared, the background image would stay in place.
        //
        // Don't install the primary shape image (full opacity) if we're animating it
        if (!animatedIcons.containsKey(globeMarkerMapObject)) globeMarkerMapObject.setBackgroundImage(iconState.image());

        if (iconState.hasStateShapeImage()) globeMarkerMapObject.setBackgroundScale(2);
    }

    private void startIconAnimations()
    {
        // Lazy creation of timer
        if (animationTimer == null)
        {
            animationTimer = new javax.swing.Timer(FRAME_RATE, new ActionListener()
            {
                int scaleIndex;

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    synchronized (animatedIcons)
                    {
                        for (var entry : animatedIcons.entrySet())
                        {
                            final var marker = entry.getKey();
                            marker.setBackgroundImage(entry.getValue().getImage(scaleIndex));
                            marker.setBackgroundScale(1.4 + ((scaleIndex) * SCALE_FACTOR));
                        }
                    }
                    scaleIndex = (scaleIndex + 1) % NUM_FRAMES;
                    globeManager.getAllGlobeDelegates().forEach(IGlobeDelegate::redraw);
                }
            });
        }

        animationTimer.start();
    }

    private void addAnimatedIcon(final IGlobeMarkerMapObject wwIcon, IconState iconState)
    {
        animatedIcons.put(wwIcon, iconState);
        updateAnimationTimer();
    }

    private void removeAnimatedIcon(IGlobeMarkerMapObject wwIcon)
    {
        animatedIcons.remove(wwIcon);
        updateAnimationTimer();
    }

    /**
     * Either start or stop the animation timer as needed.
     */
    private void updateAnimationTimer()
    {
        if (!animatedIcons.isEmpty())
        {
            startIconAnimations();
        } else
        {
            if (animationTimer != null)
            {
                animationTimer.stop();
            }
        }
    }

    private synchronized void initIcons()
    {
        if (iconsInitialized) return;

        for (State state : State.values())
        {
            // Get the color, shape, and animation settings for this state
            final Color color = stateIconUtils.getColorForState(state, null);
            final GraphicsUtils.Shape shape = stateIconUtils.getShapeForState(state, null);
            final Action action = stateIconUtils.isPulsingForState(state) ? Action.PULSING : Action.NONE;

            IconState iconState = (shape != null && color != null)
                    ? createIconState(shape, color, action)
                    : createIconState(null, null, Action.NONE);

            iconStates.put(state, iconState);
        }

        iconsInitialized = true;
    }

    private IconState createIconState(GraphicsUtils.Shape shape, Color color, Action action)
    {
        BufferedImage image = null;
        List<BufferedImage> pulsingImages = null;

        if (shape != null && color != null)
        {
            // Create our state image
            image = switch (shape)
            {
                case CIRCLE -> GraphicsUtils.createCircle(ImageUtils.FEATURE_ICON_HEIGHT, 100, color);
                case TRIANGLE -> createCenteredTriangleImage(color);
                case OCTAGON -> GraphicsUtils.createOctagon(ImageUtils.FEATURE_ICON_HEIGHT, 200, color);
                default -> null;
            };

            if (Action.PULSING == action) pulsingImages = createPulsingImages(shape, color);
        }
        return new IconState(shape, action, image, pulsingImages);
    }

    /**
     * Create a list of images that fade over time according to the frame count.
     *
     * @param shape Image shape to create
     * @param color Color of image to draw
     * @return image list
     * @since 5.7
     */
    private static List<BufferedImage> createPulsingImages(GraphicsUtils.Shape shape, Color color)
    {
        List<BufferedImage> pulsingImages = new ArrayList<>(NUM_FRAMES);
        int initialOpacity = 100;
        int opacity;

        for (int i = 0; i < NUM_FRAMES; i++)
        {
            opacity = initialOpacity - ((initialOpacity / NUM_FRAMES) * i);
            pulsingImages.add(GraphicsUtils.createShape(shape, ImageUtils.FEATURE_ICON_WIDTH, opacity, color));
        }

        return pulsingImages;
    }

    /**
     * Create an image of a triangle that is centered within the given size (causing it's base to be shifted up).
     * @since 5.7
     */
    private static BufferedImage createCenteredTriangleImage(Color color)
    {
        final var image = new BufferedImage(ImageUtils.FEATURE_ICON_WIDTH, ImageUtils.FEATURE_ICON_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        final var g = image.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 160));
        g.fill(makeTrianglePath(ImageUtils.FEATURE_ICON_HEIGHT));
        g.dispose();

        return image;
    }

    @SuppressWarnings("SameParameterValue")
    private static Path2D makeTrianglePath(int size)
    {
        final double side = size * 0.95; // leave margin
        final double height = Math.sqrt(3) / 2 * side;

        final double cx = size / 2.0;
        final double cy = size / 2.0;

        // Vertices relative to centroid
        final double y1 = cy - (2.0 / 3.0) * height;
        final double x2 = cx - side / 2;
        final double y2 = cy + (1.0 / 3.0) * height;
        final double x3 = cx + side / 2;

        final var triangle = new Path2D.Double();
        triangle.moveTo(cx, y1);
        triangle.lineTo(x2, y2);
        triangle.lineTo(x3, y2);
        triangle.closePath();
        return triangle;
    }

    private record IconState(GraphicsUtils.Shape shape, Action action, BufferedImage image, List<BufferedImage> images)
    {
        BufferedImage getImage(int index)
        {
            return images.get(index);
        }

        boolean hasStateShapeImage()
        {
            return shape != null && image != null;
        }
    }

    private enum Action
    {
        NONE, PULSING
    }
}
