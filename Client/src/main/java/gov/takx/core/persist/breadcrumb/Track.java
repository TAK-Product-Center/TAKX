package gov.takx.core.persist.breadcrumb;

import com.atakmap.coremap.maps.coords.GeoBounds;
import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.breadcrumb.ITrack;
import gov.takx.api.breadcrumb.event.TrackVisibilityChanged;
import gov.takx.core.globe.utils.GeoBoundsUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.hibernate.Length;

import java.time.Instant;

/**
 * Defines a segment of an {@link gov.takx.api.mapobject.IMarkerMapObject marker's} path. A track contains 0 or more
 * {@link gov.takx.api.breadcrumb.IBreadcrumb breadcrumbs} and metadata about the path segment.
 *
 * @since 5.3
 */
@PseudoBean
@Entity
@Table(indexes = {@Index(name = "track_uid", columnList = "uid", unique = true), @Index(name = "marker_uid", columnList = "markerUid")})
public class Track implements ITrack
{
    @Transient
    @Inject
    private Event<TrackVisibilityChanged> trackVisibilityChangedEvent;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(nullable = false)
    private String uid;
    @Column(nullable = false)
    private String markerUid;
    @Column(length = Length.LONG32, nullable = false)
    private String name;
    private int color;
    @Enumerated(EnumType.STRING)
    private Style style;
    @Column(length = Length.LONG32)
    private String callsign;
    @Column(nullable = false)
    @Basic
    private Instant startTime;
    @Column(nullable = false)
    @Basic
    private Instant endTime;
    @Embedded
    private GeoPoint startPoint;
    @Embedded
    private GeoPoint endPoint;
    @Embedded
    private GeoPoint minElevationPoint;
    @Embedded
    private GeoPoint maxElevationPoint;
    @Embedded
    private GeoPoint maxSpeedPoint;
    private double maxSpeed;
    private double speedSum;
    private double lastElevation;
    private double elevationGain;
    private double elevationLoss;
    private double totalDistance;
    private int speedValueCount;
    private int breadcrumbCount;
    private double[] geoBoundsDegreesAndAlt;

    @Transient
    private GeoBounds geoBounds;
    @Transient
    boolean visible;

    @SuppressWarnings("unused") // Hibernate
    protected Track()
    {
    }

    /**
     * @param markerUid The {@link gov.tak.api.maps.Marker marker's} UID
     * @param name The {@link gov.tak.api.maps.Marker marker's} name
     * @param callsign The {@link gov.tak.api.maps.Marker marker's} callsign
     * @param startTime The track's start time
     * @param color The track's color
     */
    public Track(@Nonnull String uid, @Nonnull String markerUid, @Nonnull String name, @Nullable String callsign,
                 @Nonnull Instant startTime, int color)
    {
        this.uid = uid;
        this.markerUid = markerUid;
        this.name = name;
        this.callsign = callsign;
        this.startTime = startTime;
        endTime = startTime;

        startPoint = GeoPoint.UNKNOWN_POINT;
        endPoint = GeoPoint.UNKNOWN_POINT;
        minElevationPoint = GeoPoint.UNKNOWN_POINT;
        maxElevationPoint = GeoPoint.UNKNOWN_POINT;
        maxSpeedPoint = GeoPoint.UNKNOWN_POINT;

        style = Style.Arrow;
        this.color = color;
    }

    @SuppressWarnings("unused")
    @PostLoad
    private void postLoad()
    {
        if (geoBoundsDegreesAndAlt != null) geoBounds = GeoBoundsUtils.fromDegreesAndAlt(geoBoundsDegreesAndAlt);
    }

    @Override
    public Integer getId()
    {
        return id;
    }

    @Nonnull
    @Override
    public String getUid()
    {
        return uid;
    }

    @Nonnull
    @Override
    public String getMarkerUid()
    {
        return markerUid;
    }

    @Nonnull
    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public int getColor()
    {
        return color;
    }

    @Override
    public Style getStyle()
    {
        return style;
    }

    @Override
    public String getCallsign()
    {
        return callsign;
    }

    @Nonnull
    @Override
    public Instant getStartTime()
    {
        return startTime;
    }

    @Nonnull
    @Override
    public Instant getEndTime()
    {
        return endTime;
    }

    @Nullable
    @Override
    public GeoPoint getStartPoint()
    {
        return GeoPoint.UNKNOWN_POINT == startPoint ? null : startPoint;
    }

    @Nullable
    @Override
    public GeoPoint getEndPoint()
    {
        return GeoPoint.UNKNOWN_POINT == endPoint ? null : endPoint;
    }

    @Nullable
    @Override
    public GeoPoint getMinElevationPoint()
    {
        return GeoPoint.UNKNOWN_POINT == minElevationPoint ? null : minElevationPoint;
    }

    @Nullable
    @Override
    public GeoPoint getMaxElevationPoint()
    {
        return GeoPoint.UNKNOWN_POINT == maxElevationPoint ? null : maxElevationPoint;
    }

    @Override
    public double getAverageSpeed()
    {
        return speedValueCount > 0 ? speedSum / speedValueCount : 0;
    }

    @Nullable
    @Override
    public GeoPoint getMaxSpeedPoint()
    {
        return GeoPoint.UNKNOWN_POINT == maxSpeedPoint ? null : maxSpeedPoint;
    }

    @Override
    public double getMaxSpeed()
    {
        return maxSpeed;
    }

    @Override
    public double getElevationGain()
    {
        return elevationGain;
    }

    @Override
    public double getElevationLoss()
    {
        return elevationLoss;
    }

    @Override
    public double getTotalDistance()
    {
        return totalDistance;
    }

    @Override
    public int getBreadcrumbCount()
    {
        return breadcrumbCount;
    }

    @Override
    public boolean isVisible()
    {
        return visible;
    }

    /**
     *  @return This track's bounds or {@code null} if it has no locations yet
     */
    @Nullable
    public GeoBounds getGeoBounds()
    {
        return geoBounds;
    }

    /**
     * @param name The track's name
     */
    public void setName(@Nonnull String name)
    {
        this.name = name;
    }

    /**
     * @param color The track's color
     */
    public void setColor(int color)
    {
        this.color = color;
    }

    /**
     * @param style The track's style
     */
    public void setStyle(Style style)
    {
        this.style = style;
    }

    /**
     * @param callsign The marker user's callsign
     */
    public void setCallsign(@Nonnull String callsign)
    {
        this.callsign = callsign;
    }

    /**
     * @param visible {@code true} if this track should be visible on the map, {@code false} if it should be hidden
     */
    public void setVisible(boolean visible)
    {
        if (this.visible != visible)
        {
            this.visible = visible;
            trackVisibilityChangedEvent.fire(new TrackVisibilityChanged(this));
        }
    }

    /**
     * Updates this track's state with the provided {@link Breadcrumb breadcrumb's} data.
     *
     * @param breadcrumb The breadcrumb
     */
    public void update(@Nonnull Breadcrumb breadcrumb)
    {
        final Instant time = breadcrumb.getTime();
        if (time.isAfter(endTime)) endTime = time;

        final GeoPoint point = breadcrumb.getGeoPointMetaData().get();

        // Initialize
        if (startPoint == GeoPoint.UNKNOWN_POINT) startPoint = point;

        if (point.hasElevation())
        {
            final double elevation = point.getElevation();

            // Initialize all elevation values
            if (minElevationPoint == GeoPoint.UNKNOWN_POINT)
            {
                lastElevation = elevation;
                minElevationPoint = point;
                maxElevationPoint = point;
            }
            // Update min/max/gain/loss as needed
            else
            {
                if (elevation < minElevationPoint.getElevation()) minElevationPoint = point;
                if (elevation > maxElevationPoint.getElevation()) maxElevationPoint = point;

                if (elevation > lastElevation)
                {
                    elevationGain += elevation - lastElevation;
                } else
                {
                    elevationLoss += lastElevation - elevation;
                }

                lastElevation = elevation;
            }
        }

        if (breadcrumb.hasSpeed())
        {
            //noinspection DataFlowIssue
            final double speed = breadcrumb.getSpeedInMps();
            speedSum += speed;
            speedValueCount++;

            if (speed > maxSpeed)
            {
                maxSpeedPoint = point;
                this.maxSpeed = speed;
            }
        }

        if (endPoint != GeoPoint.UNKNOWN_POINT) totalDistance += GeoCalculations.slantDistance(endPoint, point);

        updateExtents(point);

        endPoint = point;
        breadcrumbCount++;
    }

    /**
     * Update the track extents with a given location.  If the given location is outside of the current extent,
     * then the extent will be expanded to include the given location.
     *
     * @param geoPoint The point to add
     */
    private void updateExtents(@Nonnull GeoPoint geoPoint)
    {
        if (geoBounds == null)
        {
            geoBounds = GeoBoundsUtils.fromGeoPoints(geoPoint);
        } else if (!GeoBoundsUtils.contains(geoBounds, geoPoint))
        {
            geoBounds = GeoBoundsUtils.union(geoBounds, geoPoint);
        }

        geoBoundsDegreesAndAlt = GeoBoundsUtils.toDegreesAndAlt(geoBounds);
    }
}
