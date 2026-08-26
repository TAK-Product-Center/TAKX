package gov.takx.api.layer.pick;

import gov.tak.api.annotation.IncubatingApi;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;

/**
 * Provides picking functionality.
 *
 * @since 5.2 (moved to platform in 5.8)
 */
@IncubatingApi(since = "5.8")
@NoPluginImplementations
@InjectableService
public interface IPickedObjectManager
{
    /**
     * Gets the latest {@link PickedObjectList}.
     *
     * @return the picked object list
     */
    @Nonnull
    PickedObjectList getPickedObjectList();

    /**
     * Add an {@link IPickedObjectProvider} to provide picking results.
     *
     * @param provider the provider that produces picked objects
     */
    void addPickedObjectProvider(@Nonnull IPickedObjectProvider provider);

    /**
     * Remove an {@link IPickedObjectProvider}.
     *
     * @param provider the provider to remove
     * @see IPickedObjectManager#addPickedObjectProvider(IPickedObjectProvider)
     */
    void removePickedObjectProvider(@Nonnull IPickedObjectProvider provider);

    /**
     * Add an {@link IPickedObjectDeconflictor} to provide pick deconfliction.
     *
     * @param provider the provider that produces pick deconfliction
     * @since 5.5
     */
    void addPickedObjectDeconflictor(@Nonnull IPickedObjectDeconflictor provider);

    /**
     * Remove an {@link IPickedObjectDeconflictor}.
     *
     * @param provider the provider to remove
     * @see IPickedObjectManager#addPickedObjectDeconflictor(IPickedObjectDeconflictor)
     * @since 5.5
     */
    void removePickedObjectDeconflictor(@Nonnull IPickedObjectDeconflictor provider);
}
