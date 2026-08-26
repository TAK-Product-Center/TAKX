package gov.takx.api.persist;

/**
 * Contract for persistent entities that can have a "force update" operation applied.  Due to the semantics of how
 * a force update is performed, the normal Hibernate lifecycle annotations are not processed.  This interface offers
 * a mechanism to tell the entity to prepare for such an update operation, typically by updating internal state prior
 * to being persisted.
 *
 * @since 3.4
 */
public interface IForceUpdatable extends IPersistable
{
    /**
     * This method is called immediately prior to force updating an entity.  The implementor should prepare any internal
     * state that needs to updated prior to being persisted.
     */
    default void preForceUpdate()
    {
    }
}
