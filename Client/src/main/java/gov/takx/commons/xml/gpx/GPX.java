package gov.takx.commons.xml.gpx;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * GPX data model used to convert a route or track to an XML-writable format. Utilizes a builder pattern for data construction.
 * @since 5.8
 */
@JsonPropertyOrder({"xmlns", "xsd", "creator", "version", "waypoints", "gpxRoute"})
@JacksonXmlRootElement(localName = "gpx")
public class GPX
{
    @JacksonXmlProperty(isAttribute = true)
    private final String xmlns = "http://www.topografix.com/GPX/1/1/";

    @JacksonXmlProperty(isAttribute = true)
    private final String xsd = "https://www.w3.org/2001/XMLSchema";

    @JacksonXmlProperty(isAttribute = true)
    private final String version = "1.1";

    @JacksonXmlElementWrapper(useWrapping = false)
    @JsonProperty("wpt")
    private final List<GpxPoint> waypoints = new ArrayList<>();

    @SuppressWarnings("FieldCanBeLocal")
    @JacksonXmlProperty(isAttribute = true)
    private String creator;

    @JsonProperty("rte")
    private GpxRoute gpxRoute;

    /**
     * A wayPoint/routePoint data holder. Leave the {@link GpxPoint#name} and {@link GpxPoint#desc} elements unset,
     * or set to {@code null} for Route points. Populate them for Waypoints.
     */
    @JsonPropertyOrder({"ele", "name", "desc"})
    public static class GpxPoint
    {
        private final double lat;
        private final double lon;
        private final double ele;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("name")
        private String name;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("desc")
        private String desc;

        /**
         * Create either a route point or a waypoint.
         * <p/>
         * Waypoints use both name and description, Route points do not.
         * @param lat the latitude of the point, decimal degrees
         * @param lon the longitude of the point, decimal degrees
         * @param ele the elevation of the point, meters HAE
         */
        @JsonCreator
        public GpxPoint(@JacksonXmlProperty(isAttribute = true) @JsonProperty("lat") double lat,
                        @JacksonXmlProperty(isAttribute = true) @JsonProperty("lon") double lon,
                        @JsonProperty("ele") double ele)
        {
            this.lat = lat;
            this.lon = lon;
            this.ele = ele;
        }

        /**
         * Create a copy of the provided GpxPoint
         * @param gpxPoint the GpxPoint to copy
         */
        public GpxPoint(@Nonnull GpxPoint gpxPoint)
        {
            this.lat = gpxPoint.lat;
            this.lon = gpxPoint.lon;
            this.ele = gpxPoint.ele;
            this.name = gpxPoint.name;
            this.desc = gpxPoint.desc;
        }

        /**
         * Set the name of the GpxPoint
         * @param name the name to set
         * @return the GpxPoint
         */
        @Nonnull
        public GpxPoint withName(@Nonnull String name)
        {
            this.name = name;
            return this;
        }

        /**
         * Set the description of the GpxPoint
         * @param desc the description to set
         * @return the GpxPoint
         */
        @Nonnull
        public GpxPoint withDescription(@Nonnull String desc)
        {
            this.desc = desc;
            return this;
        }

        @JsonIgnore
        public double getLat()
        {
            return lat;
        }

        @JsonIgnore
        public double getLon()
        {
            return lon;
        }

        @JsonIgnore
        public double getElevation()
        {
            return ele;
        }

        @JsonIgnore
        @Nullable
        public String getName()
        {
            return name;
        }

        @JsonIgnore
        @Nullable
        public String getDesc()
        {
            return desc;
        }

        /**
         * @return if this point and the other have the same exact position (lat, lon, elevation)
         */
        public boolean samePosition(@Nonnull GpxPoint other)
        {
            return Double.compare(lat, other.lat) == 0 && Double.compare(lon, other.lon) == 0 && Double.compare(ele, other.ele) == 0;
        }
    }

    /**
     * A data holder for a list of {@link GpxPoint} tagged as {@code rtept} in the XML, and a {@code name} field.
     */
    public static class GpxRoute
    {
        private final String name;

        @JacksonXmlElementWrapper(useWrapping = false)
        @JsonProperty("rtept")
        private final List<GpxPoint> gpxPoints;

        @JsonCreator
        private GpxRoute(@JsonProperty("name") @Nonnull String name, @JsonProperty("rtept") @Nonnull List<GpxPoint> gpxPoints)
        {
            this.name = name;
            this.gpxPoints = gpxPoints;
        }

        /**
         * @return the name of the GPX Route
         */
        @JsonIgnore
        @Nonnull
        public String getName()
        {
            return name;
        }

        /**
         * @return the list of points in the GPX Route
         */
        @JsonIgnore
        @Nonnull
        public List<GpxPoint> getPoints()
        {
            return gpxPoints;
        }

        public static class Builder
        {
            private String name;
            private final List<GpxPoint> gpxPoints = new ArrayList<>();

            /**
             * Set the route name.
             * @param name the name of the route
             * @return the Builder object
             */
            @CanIgnoreReturnValue
            @Nonnull
            public GpxRoute.Builder withName(@Nonnull String name)
            {
                this.name = name;
                return this;
            }

            /**
             * Add the provided {@link GpxPoint} to the route.
             * @param gpxPoint the point to add
             * @return the Builder object
             */
            @CanIgnoreReturnValue
            @Nonnull
            public GpxRoute.Builder withPoint(@Nonnull GpxPoint gpxPoint)
            {
                gpxPoints.add(new GpxPoint(gpxPoint));
                return this;
            }

            /**
             * @return a fully constructed route object
             */
            @Nonnull
            public GpxRoute build()
            {
                return new GpxRoute(name, gpxPoints);
            }
        }
    }

    /**
     * Sets the GPX tag "creator" attribute.
     * @param creator who/what created this GPX file
     * @return the GPX object
     */
    @CanIgnoreReturnValue
    @Nonnull
    public GPX withCreator(@Nonnull String creator)
    {
        this.creator = creator;
        return this;
    }

    /**
     * Add a waypoint to the file.
     * @param lat the latitude, decimal degrees
     * @param lon the longitude, decimal degrees
     * @param ele the elevation, meters HAE
     * @param name the name of the waypoint
     * @param desc the description of the waypoint
     * @return the GPX object
     */
    @CanIgnoreReturnValue
    @Nonnull
    public GPX addWaypoint(double lat, double lon, double ele, @Nonnull String name, @Nonnull String desc)
    {
        waypoints.add(new GpxPoint(lat, lon, ele).withName(name).withDescription(desc));
        return this;
    }

    /**
     * Add a waypoint to the file.
     * @param gpxPoint the {@link GpxPoint} containing the waypoint details
     * @return the GPX object
     */
    @CanIgnoreReturnValue
    @Nonnull
    public GPX addWaypoint(@Nonnull GpxPoint gpxPoint)
    {
        waypoints.add(new GpxPoint(gpxPoint));
        return this;
    }

    /**
     * Add all waypoints to the file.
     * @param gpxPoints a list of {@link GpxPoint} containing the waypoint details
     * @return the GPX object
     */
    @CanIgnoreReturnValue
    @Nonnull
    public GPX addWaypoints(@Nonnull List<GpxPoint> gpxPoints)
    {
        waypoints.addAll(gpxPoints);
        return this;
    }

    /**
     * Add the {@link GpxRoute} to the file.
     * @param gpxRoute the route to add
     * @return the GPX object
     */
    @CanIgnoreReturnValue
    @Nonnull
    public GPX addRoute(@Nonnull GpxRoute gpxRoute)
    {
        this.gpxRoute = gpxRoute;
        return this;
    }

    /**
     * @return the route of this GPX, if one has been added
     */
    @JsonIgnore
    @Nullable
    public GpxRoute getRoute()
    {
        return gpxRoute;
    }

    /**
     * @return the list of promoted Waypoints
     */
    @JsonIgnore
    @Nonnull
    public List<GpxPoint> getWaypoints()
    {
        return waypoints;
    }
}
