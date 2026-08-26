package gov.takx.api.state;

/**
 * Interface for persistable map support.
 *
 * @since 1.48
 */
public interface IRestorableMapSupport
{
    /**
     * Prepare for storage.  Add any relevant data to the map for persistence.
     *
     * @param map restorable map of interest.
     */
    void store(IRestorableMap map);

    /**
     * Restore from data in a state object.
     *
     * @param map restorable map of interest.
     */
    void restore(IRestorableMap map);
}
