package gov.takx.platform.utils;

import gov.takx.commons.constants.State;
import gov.takx.commons.swing.GraphicsUtils;
import gov.takx.platform.preferences.PreferencesNode;
import gov.takx.platform.preferences.UserPreferenceEntry;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

/**
 * Offers utility methods for internalizing and working with the preferences defining how Map Object
 * alarm states are rendered graphically.
 * <p>
 * This class handles parsing and internalizing the user preferences "Status" node.  A typical configuration
 * looks like this:
 * <pre>
 * &lt;node name="Status"&gt;
 *   &lt;map/&gt;
 *   &lt;node name="Active"&gt;
 *     &lt;map&gt;
 *       &lt;entry key="Action" value="none"/&gt;
 *       &lt;entry key="Shape" value="circle"/&gt;
 *       &lt;entry key="Color" value="green"/&gt;
 *     &lt;/map&gt;
 *   &lt;/node&gt;
 *   &lt;node name="Critical"&gt;
 *     &lt;map&gt;
 *       &lt;entry key="Action" value="pulsing"/&gt;
 *       &lt;entry key="Shape" value="octagon"/&gt;
 *       &lt;entry key="Color" value="red"/&gt;
 *     &lt;/map&gt;
 *   &lt;/node&gt;
 *   &lt;node name="Inactive"&gt;
 *     &lt;map&gt;
 *       &lt;entry key="Action" value="none"/&gt;
 *       &lt;entry key="Shape" value="circle"/&gt;
 *       &lt;entry key="Color" value="none"/&gt;
 *     &lt;/map&gt;
 *   &lt;/node&gt;
 *   &lt;node name="Serious"&gt;
 *     &lt;map&gt;
 *       &lt;entry key="Action" value="pulsing"/&gt;
 *       &lt;entry key="Shape" value="triangle"/&gt;
 *       &lt;entry key="Color" value="yellow"/&gt;
 *     &lt;/map&gt;
 *   &lt;/node&gt;
 *   &lt;node name="Unknown"&gt;
 *     &lt;map&gt;
 *       &lt;entry key="Action" value="none"/&gt;
 *       &lt;entry key="Shape" value="none"/&gt;
 *       &lt;entry key="Color" value="none"/&gt;
 *     &lt;/map&gt;
 *   &lt;/node&gt;
 *   &lt;node name="Warning"&gt;
 *     &lt;map&gt;
 *       &lt;entry key="Action" value="none"/&gt;
 *       &lt;entry key="Shape" value="triangle"/&gt;
 *       &lt;entry key="Color" value="yellow"/&gt;
 *     &lt;/map&gt;
 *   &lt;/node&gt;
 * &lt;/node&gt;
 * </pre>
 * <p>
 * With nodes defining each of the {@link State} values.  Within each node, the map contains 3 elements:
 * <ul>
 * <li>Shape: The icon shape to use, one of {@code circle}, {@code triangle}, or {@code octagon}</li>
 * <li>Color: The color to use in an icon of the given shape, can be any color name defined as a field in {@link Color}</li>
 * <li>Action: For MapObject background, "{@code pulsing}" makes the background animate through various sizes</li>
 * </ul>
 * <p>
 * Sadly, there is no event we can observe down here in Platform that is early enough to ensure we are properly
 * initialized. So, we initialize on fist use.
 *
 * @since 2.2
 */
@ApplicationScoped
public class StateIconUtils
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int STATUS_ICON_SIZE = 28;
    private static final int SECONDARY_STATUS_OPACITY = 200;

    private StateConfigurations stateConfigurations;
    private Map<String, BufferedImage> secondaryStatusImages = new HashMap<>();

    @PostConstruct
    private void postConstruct()
    {
        stateConfigurations = UserPreferenceEntry.STATUS_NODE.getNodeAs(StateConfigurations.class, true);
    }

    /**
     * Get the color defined for the given state.
     *
     * @param state State of interest
     * @return Color for state
     */
    public Color getColorForState(State state)
    {
        return getColorForState(state, Color.gray);
    }

    /**
     * Get the color defined for the given state.  Values configured as "none" will always return the
     * supplied default value.
     *
     * @param state        State of interest
     * @param defaultColor Color to use if there is no color defined for the given state
     * @return Color for state or default if not found
     */
    public Color getColorForState(State state, Color defaultColor)
    {
        final Color color = stateConfigurations.getStateConfiguration(state).color;

        return color != null ? color : defaultColor;
    }

    /**
     * Get the shape defined for the given state.
     *
     * @param state State of interest
     * @return Shape for state
     */
    public GraphicsUtils.Shape getShapeForState(State state)
    {
        return getShapeForState(state, GraphicsUtils.Shape.CIRCLE);
    }

    /**
     * Get the shape defined for the given state.  Values configured as "none" will always return the
     * supplied default value.
     *
     * @param state        State of interest
     * @param defaultShape Shape to use if there is no color defined for the given state
     * @return Shape for state or default if not found
     */
    public GraphicsUtils.Shape getShapeForState(State state, GraphicsUtils.Shape defaultShape)
    {
        final GraphicsUtils.Shape shape = stateConfigurations.getStateConfiguration(state).shape;
        return shape != null ? shape : defaultShape;
    }

    /**
     * Determine if the "pulsing" action is configured for a given state.
     *
     * @param state State of interest
     * @return {@code true} if Action is "pulsing" for the given state
     */
    public boolean isPulsingForState(State state)
    {
        return stateConfigurations.getStateConfiguration(state).pulsing;
    }

    /**
     * Overlay a smaller image of a state that the user must acknowledge on the lower right-hand
     * side of the image of the current state.
     *
     * @param statusImage The image representing the current state of the monitor.
     * @param visualState The state that the user must acknowledge.
     * @return The original status image with a smaller image representing the state that the user
     * must acknowledge overlaid onto the lower right-hand corner of it.
     */
    public BufferedImage overlayVisualState(BufferedImage statusImage, State visualState)
    {
        final Color stateColor = getColorForState(visualState);
        final GraphicsUtils.Shape shape = getShapeForState(visualState);

        final String statusKey = shape.toString() + stateColor;

        BufferedImage secondaryImage = secondaryStatusImages.get(statusKey);

        if (secondaryImage == null)
        {
            secondaryImage = GraphicsUtils.createShape(shape, STATUS_ICON_SIZE / 2, SECONDARY_STATUS_OPACITY, stateColor);
            secondaryStatusImages.put(statusKey, secondaryImage);
        }

        return DataMonitorUtils.overlaySecondaryStatus(statusImage, secondaryImage);
    }

    /**
     * Interns the user preferences node defining the State display configurations.
     *
     * @since 2.35
     */
    private static final class StateConfigurations
    {
        private final Map<State, StateConfiguration> stateConfigurationMap = new HashMap<>();

        private StateConfigurations(PreferencesNode node)
        {
            for (State state : State.values())
            {
                final PreferencesNode stateNode = node.node(state.toString());

                final String colorString = stateNode.get("Color", "gray");
                final String shapeString = stateNode.get("Shape", "circle");
                final String actionString = stateNode.get("Action", "none");

                final Color color = colorString.equalsIgnoreCase("none")
                        ? null
                        : GraphicsUtils.getColorByName(colorString, Color.gray);

                final GraphicsUtils.Shape shape = shapeString.equalsIgnoreCase("none")
                        ? null
                        : GraphicsUtils.Shape.forName(shapeString, GraphicsUtils.Shape.CIRCLE);

                final boolean pulsing = "pulsing".equalsIgnoreCase(actionString);

                stateConfigurationMap.put(state, new StateConfiguration(shape, color, pulsing));
            }
        }

        private StateConfiguration getStateConfiguration(State state)
        {
            return stateConfigurationMap.get(state);
        }
    }

    /**
     * Simple holder for the configuration for a single State.
     *
     * @since 2.35
     */
    private static final class StateConfiguration
    {
        private final GraphicsUtils.Shape shape;
        private final Color color;
        private final boolean pulsing;

        private StateConfiguration(GraphicsUtils.Shape shape, Color color, boolean pulsing)
        {
            this.shape = shape;
            this.color = color;
            this.pulsing = pulsing;
        }
    }
}
