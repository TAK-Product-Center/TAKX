package gov.takx.api.persist;

/**
 * Contract for an <em>extended</em> lifecycle, providing a callback when ALL objects of a
 * given table/type have been pulled from persistence.  This allows for arbitrary inter-entity
 * relationships to be re-wired without requiring Hibernate to manage that relationship.
 *
 * @since 3.0
 */
public interface IExtendedLifecycle
{
    /**
     * Called when all objects of the same type, e.g. MapObject, have been loaded from persistence.
     */
    void onAllLoaded();
}
