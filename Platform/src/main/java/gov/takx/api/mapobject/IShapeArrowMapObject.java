package gov.takx.api.mapobject;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.shapes.IMutableArrow;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * Interface defining the arrow shape map object, which is defined by a range and bearing (and optionally anchor map
 * objects for the start and/or range).
 *
 * @since 3.0
 */
@NoPluginImplementations
public interface IShapeArrowMapObject extends IShapePathMapObject<IMutableArrow>, IMutableArrow
{
    /**
     * {@inheritDoc}
     * <p>
     * In addition, move any anchors if needed.
     */
    @Override
    void setVertices(@Nonnull List<? extends ILocation> points) throws IllegalArgumentException;

    /**
     * @return Current range anchor, if any.
     */
    @Nullable
    IMapObject getRangeAnchor();

    /**
     * Set a range anchor, referencing another map object. If that object is moved, then the range of this arrow will be
     * updated accordingly. If the range of this arrow is changed directly, then the location of the given
     * {@code rangeAnchor} will be updated accordingly.
     *
     * @param rangeAnchor Map Object to use as a range anchor, pass {@code null} to remove an existing anchor
     * @throws IllegalArgumentException if range anchor is {@code null} and arrow is anchored
     *                                  or range anchor does not have a valid location
     * @see #isAnchored()
     */
    void setRangeAnchor(@Nullable IMapObject rangeAnchor);

    /**
     * @return Current start anchor, if any.
     */
    @Nullable
    IMapObject getStartAnchor();

    /**
     * Set a start anchor, referencing another map object. If that object is moved, then the start of this arrow will be
     * updated accordingly. If the start of this arrow is changed directly, then the location of the given
     * {@code startAnchor} will be updated accordingly.
     *
     * @param startAnchor Map Object to use as a start anchor, pass {@code null} to remove an existing anchor
     * @throws IllegalArgumentException if start anchor is {@code null} and arrow is anchored
     *                                  or start anchor does not have a valid location
     * @see #isAnchored()
     */
    void setStartAnchor(@Nullable IMapObject startAnchor);

    /**
     * @return Whether the arrow requires both endpoints to be anchored to another MO.
     * @see #setStartAnchor(IMapObject)
     * @see #setRangeAnchor(IMapObject)
     * @since 5.1 (name change and inverted semantics from prior isStandalone)
     */
    boolean isAnchored();

    /**
     * Set whether the arrow requires the anchors at both ends to exist.
     * <p>
     * TODO KMB: this is needed for range and bearing lines from Bloodhound, which cannot exist alone: if an anchor is
     *  deleted, the arrow is also deleted.
     *
     * @param anchored True if arrow requires both endpoints to be anchored to another MO
     * @throws IllegalArgumentException if attempting to set to false when both anchors aren't present
     * @see #setStartAnchor(IMapObject)
     * @see #setRangeAnchor(IMapObject)
     * @since 5.1 (name change and inverted semantics from prior setStandalone)
     */
    void setAnchored(boolean anchored) throws IllegalArgumentException;
}
