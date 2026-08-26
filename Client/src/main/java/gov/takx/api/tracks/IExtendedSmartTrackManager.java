package gov.takx.api.tracks;

import gov.takx.api.mapobject.IMapObject;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Extended interface for smart track manager that provides access to registered objects by database ID.  This is
 * needed to ensure that the "single" instance is obtained by all callers to ensure that only one instance is in
 * memory (this is needed to ensure that property change notifications work as expected).
 *
 * @since 1.14
 */
public interface IExtendedSmartTrackManager extends ISmartTrackManager
{
    /**
     * Get a registered TSD given its database ID.
     *
     * @param id Database ID of the TSD to retrieve
     * @return TSD or {@code null} if not found
     */
    @Nullable
    ITrackStyleDefinition getTrackStyleDefinitionByID(Integer id);

    /**
     * Create a 100% deferring TSD for the given Map Object.
     *
     * @param mapObject Map object of interest
     * @return New TSD with no properties set (100% deferring to parent)
     * @since 2.28
     */
    ITrackStyleDefinition createDeferringTSD(@Nonnull IMapObject mapObject);

    /**
     * Create a non-visible, but otherwise 100% deferring, CLD for the given Map Object.
     * The parent will be set to the global CLD.
     *
     * @param mapObject Map object of interest
     * @return New CLD with no properties set (100% deferring to parent)
     * @since 2.28
     */
    IConnectingLineDefinition createDeferringCLD(@Nonnull IMapObject mapObject);
}
