package gov.takx.api.mapobject;

import gov.takx.api.annotation.Internal;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.geofence.GeoFence;
import gov.takx.api.shapes.IMutableShape;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

/**
 * Contract for the base functionality for all shape map objects.
 */
@NoPluginImplementations
public interface IShapeMapObject<T extends IMutableShape> extends IMapObject, IMutableShape
{
    /**
     * Determine whether this shape can use vertical height.  This is typically a hard-coded value based upon the
     * extending shape map object type.
     *
     * @return {@code true} if height can be used on this shape, or {@code false} otherwise.
     * @since 1.29
     */
    @Override
    boolean canUseVerticalHeight();

    /**
     * Set the vertical height (in meters) on this shape.
     *
     * @param verticalHeight the vertical height (in meters).
     * @since 1.29
     */
    @Override
    void setVerticalHeight(double verticalHeight);

    /**
     * Get the vertical height (in meters) from this shape.
     *
     * @return the vertical height (in meters).
     * @since 1.29
     */
    @Override
    double getVerticalHeight();

    /**
     * Enable or disable the vertical height on this shape.
     *
     * @param enable {@code true} to enable height on this shape, or {@code false} to disable vertical height.
     * @since 1.29
     */
    @Override
    void setVerticalHeightEnabled(boolean enable);

    /**
     * Determine whether vertical height is enabled on this shape.
     *
     * @return {@code true} if height is enabled on this shape, or {@code false} otherwise.
     * @since 1.29
     */
    @Override
    boolean isVerticalHeightEnabled();

    /**
     * Get the shape associated with this Map Object.
     *
     * @return Associated shape
     * @since 3.0
     */
    @Nonnull
    T getAssociatedShape();

    /**
     * Set the associated shape, as created by a shape service, for this map object.
     *
     * @param associatedShape Associated shape
     * @since 3.0
     */
    @Internal
    void setAssociatedShape(@Nonnull IMutableShape associatedShape);

    /**
     * Set a center anchor, referencing another map object.  If that object is moved, then the location of this
     * shape will be updated accordingly.  If the location of this shape is changed directly, then the location
     * of the given {@code centerAnchor} will be updated accordingly.
     *
     * @param centerAnchor Map Object to use as a center anchor, pass {@code null} to remove an existing anchor
     * @since 3.0
     */
    void setCenterAnchor(@Nullable IMapObject centerAnchor);

    /**
     * @return Current center anchor, if any
     * @since 3.0
     */
    @Nullable
    IMapObject getCenterAnchor();

    /**
     * @return The {@link GeoFence GeoFence} associated with this shape, {@code null} if none
     * @since 5.3
     */
    @Nullable
    GeoFence getGeoFence();

    /**
     * @return The {@link GeoFence GeoFence} associated with this shape
     * @since 5.3
     * @throws NullPointerException If the current geoFence is {@code null}
     */
    @Nonnull
    default GeoFence requireGeoFence()
    {
        return Objects.requireNonNull(getGeoFence(), "geoFence is null");
    }

    /**
     * Set a GeoFence that is using this shape as its boundary definition.
     * <p/>
     * NOTE: This is an internal method, and not meant for use by plugins.
     *
     * @param geoFence The {@link GeoFence GeoFence} associated with this shape, {@code null} if none
     * @throws IllegalArgumentException If the {@code shapeUid} in the given {@code geoFence} isn't equal to our UID
     * @since 5.3
     */
    @Internal
    void setGeoFence(@Nullable GeoFence geoFence) throws IllegalArgumentException;

    /**
     * @return {@code true} if this shape has a {@link GeoFence} attached to it
     * @since 5.3
     */
    default boolean hasGeoFence()
    {
        return getGeoFence() != null;
    }
}
