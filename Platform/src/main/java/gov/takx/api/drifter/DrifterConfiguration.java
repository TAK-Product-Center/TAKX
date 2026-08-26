package gov.takx.api.drifter;

import com.google.auto.value.AutoBuilder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * A record representing drifter configuration for an {@link gov.takx.api.mapobject.IMapObject}.
 *
 * @param speed the speed in mps that the map object should move
 * @param bearing the constant bearing to move, ignored if {@link #routeUid} is non {@code null}
 * @param paused {@code true} if the map object is paused and should not drift, otherwise {@code false}
 * @param routeUid the uid of a {@link gov.takx.api.mapobject.IRouteMapObject} to drift on, or {@code null} to use a constant bearing
 * @param routePoint the current route point we are headed towards
 * @since 5.6
 */
public record DrifterConfiguration(double speed, double bearing, boolean paused, int routePoint, @Nullable String routeUid)
{

    /**
     * @return a builder with no properties set
     */
    @Nonnull
    public static Builder builder()
    {
        return new AutoBuilder_DrifterConfiguration_Builder();
    }

    /**
     * @param configuration the configuration
     * @return a builder with properties copied from the supplied configuration
     */
    @Nonnull
    public static Builder builder(@Nullable DrifterConfiguration configuration)
    {
        return configuration == null ? builder() : builder()
                .speed(configuration.speed)
                .bearing(configuration.bearing)
                .paused(configuration.paused)
                .routePoint(configuration.routePoint)
                .routeUid(configuration.routeUid);
    }

    /**
     * Builder for {@link DrifterConfiguration} instances.
     */
    @SuppressWarnings("NullableProblems")
    @AutoBuilder
    public abstract static class Builder
    {
        /**
         * @param speed see {@link DrifterConfiguration#speed}
         */
        @Nonnull
        public abstract Builder speed(double speed);

        /**
         * @param bearing see {@link DrifterConfiguration#bearing}
         */
        @Nonnull
        public abstract Builder bearing(double bearing);

        /**
         * @param routeUid see {@link DrifterConfiguration#routeUid}
         */
        @Nonnull
        public abstract Builder routeUid(@Nullable String routeUid);

        /**
         * @param routePoint see {@link DrifterConfiguration#routePoint}
         */
        @Nonnull
        public abstract Builder routePoint(int routePoint);

        /**
         * @param paused see {@link DrifterConfiguration#paused}
         */
        @Nonnull
        public abstract Builder paused(boolean paused);

        /**
         * @return the instance constructed using the properties set on this builder
         */
        @Nonnull
        public abstract DrifterConfiguration build();
    }
}
