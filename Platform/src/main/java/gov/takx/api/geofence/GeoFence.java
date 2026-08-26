package gov.takx.api.geofence;

import com.google.auto.value.AutoBuilder;
import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.Set;

/**
 * Model for a geo-fence.  An area, defined by a shape, that can be monitored for boundary crossings by other markers.
 * <p/>
 * Instances of this type are best constructed using the provided {@link Builder}.
 *
 * @param shapeUid UID of the shape that defines our boundary
 * @param trigger marker transition trigger type
 * @param monitoredType type of markers to monitor
 * @param tracking pass {@code true} the enable active monitoring of markers against this GeoFence, {@code false} to disable
 * @param monitorElevation pass {@code true} to include the map item's elevation in the boundary condition
 * @param minElevation minimum elevation, in meters, for a marker to be considered in a boundary transition, {@link Double#NaN} means no minimum
 * @param maxElevation maximum elevation, in meters, for a marker to be considered in a boundary transition, {@link Double#NaN} means no maximum
 * @param customUids collection of UIDs of map items that are to be tracked.  Only used when {@code monitoredType} is {@link MonitoredType#Custom Custom}.
 * @since 5.3
 */
public record GeoFence(@Nonnull String shapeUid, @Nonnull Trigger trigger, @Nonnull MonitoredType monitoredType, boolean tracking,
        boolean monitorElevation, double minElevation, double maxElevation, @Nonnull Set<String> customUids)
{
    /**
     * Constructor using the given values and defaulting to no elevation checking and no custom uids.
     *
     * @param shapeUid UID of the shape that defines our boundary
     * @param trigger new marker transition trigger type
     * @param monitoredType new type of markers monitored
     * @param tracking Pass {@code true} the enable active monitoring of markers against this GeoFence, {@code false} to disable
     */
    public GeoFence(@Nonnull String shapeUid, @Nonnull Trigger trigger, @Nonnull MonitoredType monitoredType, boolean tracking)
    {
        this(shapeUid, trigger, monitoredType, tracking, false, Double.NaN, Double.NaN, Collections.emptySet());
    }

    /**
     * @return Unmodifiable reference to the custom Uids collection
     */
    public Set<String> customUids()
    {
        return Collections.unmodifiableSet(customUids);
    }

    /**
     * @return {@code true} if the elevation of a marker should be included in the GeoFence transition tests.  One of
     * the min or max elevations must be a value other than {@link Double#NaN} for this to return {@code true}.
     * @see #monitorElevation()
     * @see #minElevation()
     * @see #maxElevation()
     */
    public boolean isElevationMonitored()
    {
        return monitorElevation() && (!Double.isNaN(minElevation) || !Double.isNaN(maxElevation));
    }

    /**
     * Construct a builder with default values:
     * <ul>
     *     <li>{@code shapeUid} = empty</li>
     *     <li>{@code trigger} = {@link Trigger#Entry}</li>
     *     <li>{@code monitoredType} = {@link MonitoredType#All}</li>
     *     <li>{@code tracking} = {@code true}</li>
     *     <li>{@code minElevation} = {@link Double#NaN}</li>
     *     <li>{@code maxElevation} = {@link Double#NaN}</li>
     *     <li>{@code customUids} = empty set</li>
     * </ul>
     * @return New builder
     */
    @Nonnull
    public static Builder builder()
    {
        return new AutoBuilder_GeoFence_Builder()
                .shapeUid("")
                .trigger(Trigger.Entry)
                .monitoredType(MonitoredType.All)
                .tracking(true)
                .monitorElevation(false)
                .minElevation(Double.NaN)
                .maxElevation(Double.NaN)
                .customUids(Collections.emptySet());
    }

    /**
     * Construct a builder initialized with the state from the given source {@code geoFence}.
     * 
     * @param geoFence Source GeoFence from which to pull initial property values
     * @return initialized builder
     */
    @Nonnull
    public static Builder builder(@Nonnull GeoFence geoFence)
    {
        return builder()
                .shapeUid(geoFence.shapeUid())
                .trigger(geoFence.trigger())
                .monitoredType(geoFence.monitoredType())
                .tracking(geoFence.tracking())
                .monitorElevation(geoFence.monitorElevation())
                .minElevation(geoFence.minElevation())
                .maxElevation(geoFence.maxElevation())
                .customUids(geoFence.customUids());
    }

    /**
     * Builder for construction of {@link GeoFence} instances.
     */
    @SuppressWarnings("NewClassNamingConvention")
    @AutoBuilder
    public abstract static class Builder
    {
        /**
         * @param shapeUid UID of the shape that defines our boundary
         */
        @Nonnull
        public abstract Builder shapeUid(@Nonnull String shapeUid);

        /**
         * @param trigger marker transition trigger type
         */
        @Nonnull
        public abstract Builder trigger(@Nonnull Trigger trigger);

        /**
         * @param monitoredType type of markers to monitor
         */
        @Nonnull
        public abstract Builder monitoredType(@Nonnull MonitoredType monitoredType);

        /**
         * @param monitorElevation Pass {@code true} to include the configured min/max elevations in the boundary check, {@code false} to disable
         * @see #minElevation(double)
         * @see #maxElevation(double)
         */
        @Nonnull
        public abstract Builder monitorElevation(boolean monitorElevation);

        /**
         * @param tracking Pass {@code true} the enable active monitoring of markers against this GeoFence, {@code false} to disable
         */
        @Nonnull
        public abstract Builder tracking(boolean tracking);

        /**
         * @param minElevation maximum elevation, in meters, for a marker to be considered in a boundary transition, {@link Double#NaN} means no minimum
         */
        @Nonnull
        public abstract Builder minElevation(double minElevation);

        /**
         * @param maxElevation maximum elevation, in meters, for a marker to be considered in a boundary transition, {@link Double#NaN} means no maximum
         */
        @Nonnull
        public abstract Builder maxElevation(double maxElevation);

        /**
         * Set both the min and max elevation values and set {@code monitorElevation} to {@code true}.
         *
         * @param minElevation The minimum elevation, in meters, for a marker to be considered in a boundary transition, {@link Double#NaN} means no maximum
         * @param maxElevation The maximum elevation, in meters, for a marker to be considered in a boundary transition, {@link Double#NaN} means no maximum
         *
         * @see #monitorElevation(boolean)
         * @see #minElevation(double)
         * @see #maxElevation(double)
         */
        public Builder elevationRange(double minElevation, double maxElevation)
        {
            monitorElevation(true);
            minElevation(minElevation);
            maxElevation(maxElevation);
            return this;
        }

        /**
         * @param customUids Collection of UIDs of map items that are being tracked.  Only used when {@code monitoredType} is {@link MonitoredType#Custom}
         * @see #monitoredType(MonitoredType)
         * @see MonitoredType#Custom
         */
        @Nonnull
        public abstract Builder customUids(@Nonnull Set<String> customUids);

        /**
         * @return New GeoFence from configured values
         * @throws IllegalStateException If shapeUid is blank, or if min and max elevations are specified, and the min value is >= the max value
         */
        @Nonnull
        public GeoFence build() throws IllegalStateException
        {
            if (shapeUid().isBlank()) throw new IllegalStateException("shapeUid must not be blank");

            if (minElevation() >= maxElevation())
            {
                throw new IllegalStateException("minElevation [" + minElevation() + "] must be < maxElevation [" + maxElevation() + "]");
            }

            return autoBuild();
        }

        abstract String shapeUid();

        abstract double minElevation();

        abstract double maxElevation();

        /**
         * @return New GeoFence from configured values
         */
        @Nonnull
        abstract GeoFence autoBuild();
    }
}