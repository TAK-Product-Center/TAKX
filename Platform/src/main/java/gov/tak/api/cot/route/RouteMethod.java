package gov.tak.api.cot.route;

import gov.takx.api.mapobject.IRouteMapObject;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * The transportation method for CoT routes.
 *
 * @since 3.0
 */
public enum RouteMethod
{
    DRIVING("Driving", IRouteMapObject.TRANSPORTATION_TYPE_VEHICLE, "End Point"),
    WALKING("Walking", IRouteMapObject.TRANSPORTATION_TYPE_ON_FOOT, "End Point"),
    FLYING("Flying", IRouteMapObject.TRANSPORTATION_TYPE_VEHICLE, "End Point"),
    SWIMMING("Swimming", IRouteMapObject.TRANSPORTATION_TYPE_ON_FOOT, "End Point"),
    WATERCRAFT("Watercraft", IRouteMapObject.TRANSPORTATION_TYPE_VEHICLE, "End Point");

    private static final Map<String, RouteMethod> textMap = new HashMap<>();

    static
    {
        for (RouteMethod method : values())
        {
            textMap.put(method.text, method);
        }
    }

    private final String text;
    private final String transportationType;
    private final String endpointDefaultLabel;

    RouteMethod(String text, String transportationType, String endpointDefaultLabel)
    {
        this.text = text;
        this.transportationType = transportationType;
        this.endpointDefaultLabel = endpointDefaultLabel;
    }

    /**
     * @return The human readable text that should be used in CoT messages and displays.
     */
    @Nonnull
    public String getDisplayText()
    {
        return text;
    }

    /**
     * @return The transportation type associated with this transportation method.
     */
    @Nonnull
    public String getTransportationType()
    {
        return transportationType;
    }

    /**
     * @return The default name of the last point for a route using this method.
     * @since 3.3
     */
    @Nonnull
    public String getEndpointDefaultLabel()
    {
        return endpointDefaultLabel;
    }

    /**
     * Gets the enum literal that corresponds to the provided text.
     *
     * @param text The text to convert
     * @return The corresponding enum literal or {@code null} if none was found.
     */
    @Nullable
    public static RouteMethod fromText(String text)
    {
        return textMap.get(text);
    }
}
