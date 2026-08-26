package gov.takx.core.persist.breadcrumb;

import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.takx.api.breadcrumb.IBreadcrumb;
import gov.takx.api.persist.IPersistable;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a marker's location with additional metadata.
 *
 * @since 5.3
 */
@Entity
@Table(indexes = {@Index(name = "breadcrumb_uid", columnList = "uid", unique = true)})
public class Breadcrumb implements IBreadcrumb, IPersistable
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(nullable = false)
    private String uid;
    @Column(nullable = false)
    private GeoPointMetaData geoPointMetaData;
    @Column(nullable = false)
    @Basic
    private Instant time;
    private Double bearing;
    private Double speed;
    @Column(nullable = false)
    private String trackUid;

    @SuppressWarnings("unused") // Hibernate
    protected Breadcrumb()
    {
    }

    /**
     * @param breadcrumb The breadcrumb to copy
     */
    @SuppressWarnings("CopyConstructorMissesField") // id not needed b/c copies are not persisted
    public Breadcrumb(@Nonnull Breadcrumb breadcrumb)
    {
        this(breadcrumb.geoPointMetaData, breadcrumb.time, breadcrumb.bearing, breadcrumb.speed, breadcrumb.trackUid);
    }

    /**
     * @param geoPointMetaData The {@link gov.tak.api.maps.Marker marker's} location and location metadata
     * @param time The time the location was recorded
     * @param bearing The {@link gov.tak.api.maps.Marker marker's} bearing. Pass {@code null} if unavailable
     * @param speed The {@link gov.tak.api.maps.Marker marker's} speed in m/s. Pass {@code null} if unavailable
     * @param trackUid The uid of the {@link gov.takx.api.breadcrumb.ITrack} that contains this breadcrumb
     * @throws IllegalArgumentException if {@link #geoPointMetaData} does not contain a valid {@link GeoPoint}
     */
    public Breadcrumb(@Nonnull GeoPointMetaData geoPointMetaData, @Nonnull Instant time, @Nullable Double bearing,
                      @Nullable Double speed, @Nonnull String trackUid)
            throws IllegalArgumentException
    {
        if (!geoPointMetaData.get().isValid()) throw new IllegalArgumentException("A valid GeoPoint is required");

        uid = UUID.randomUUID().toString();
        this.geoPointMetaData = new GeoPointMetaData(geoPointMetaData);

        this.time = time;
        this.bearing = bearing;
        this.speed = speed;
        this.trackUid = trackUid;
    }

    /**
     * @param bearing sets the breadcrumb's bearing
     */
    public void setBearing(double bearing)
    {
        this.bearing = bearing;
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
    public String getTrackUid()
    {
        return trackUid;
    }

    @Nonnull
    @Override
    public GeoPointMetaData getGeoPointMetaData()
    {
        return geoPointMetaData;
    }

    @Nonnull
    @Override
    public Instant getTime()
    {
        return time;
    }

    @Nullable
    @Override
    public Double getBearing()
    {
        return bearing;
    }

    @Override
    public boolean hasBearing()
    {
        return bearing != null;
    }

    @Nullable
    @Override
    public Double getSpeedInMps()
    {
        return speed;
    }

    @Override
    public boolean hasSpeed()
    {
        return speed != null;
    }
}
