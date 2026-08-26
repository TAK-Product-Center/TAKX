package gov.tak.core.map.layer.feature.datastore.memory;

/**
 * Interface to be used on InMemoryDataStore records that have id, visible, and version fields.
 *
 * @since 5.2
 */
interface IDataStoreEntity
{
    /**
     * @return the unique id
     */
    Long id();

    /**
     * @return the visible flag
     */
    boolean visible();

    /**
     * @return the entity version
     */
    long version();
}