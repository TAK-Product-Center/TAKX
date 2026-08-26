package gov.takx.api.persist;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.messages.IPluginState;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * Persistence interface for plugin state.
 *
 * @see IPluginState
 * @since 1.3
 */
@NoPluginImplementations
public interface IPluginStatePersist
{
    /**
     * Save the persistable state for an object, if it provides any.  Query the {@code persistableState}
     * object to see if it has persistable state.  If it does, save that state in the database under the
     * provided {@code stateKey}.  If there is already a row in the database with that key, update its
     * state with the new state.  Otherwise, construct a new {@code IPluginState} using the key and state
     * and store that.
     *
     * @param stateKey         Unique key for persistable state
     * @param persistableState Object for which to store the state
     * @see IPersistableState#store()
     * @see IPluginState
     */
    void saveOrUpdateState(String stateKey, IPersistableState persistableState);

    /**
     * Persist plugin state.
     *
     * @param state Object to persist.
     */
    void persistState(@Nonnull IPluginState state);

    /**
     * Find the saved state of a plugin based on its user-defined unique key.
     *
     * @param uniqueKey Unique key defined by the plugin where it stored its state
     * @return {@link IPluginState}, or {@code null} if none is found.
     */
    @Nullable
    IPluginState query(@Nonnull String uniqueKey);

    /**
     * @return ALL plugin state entries in the database.
     * @since 2.36
     */
    List<IPluginState> queryAll();

    /**
     * Delete plugin state.
     *
     * @param state Plugin state to delete
     */
    void deleteState(IPluginState state);

    /**
     * Delete plugin state with the given unique key.
     *
     * @param stateKey Key of plugin state to delete
     * @since 2.28
     */
    void deleteState(String stateKey);
}
