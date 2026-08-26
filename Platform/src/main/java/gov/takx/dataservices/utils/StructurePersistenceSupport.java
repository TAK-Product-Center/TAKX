package gov.takx.dataservices.utils;

import gov.takx.api.annotation.Internal;
import gov.takx.api.messages.IStructure;
import gov.takx.api.persist.IMapObjectPersist;
import gov.takx.api.persist.IPersistService;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.api.project.IProjectInfoProvider;
import gov.takx.platform.utils.StringKeyLockSupport;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.locks.Lock;

/**
 * Support methods for ensuring only a single instance of a structure with a given hash is every placed into
 * persistence.
 *
 * @since 2.9
 */
@ApplicationScoped
public class StructurePersistenceSupport
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final StringKeyLockSupport hashLocks = new StringKeyLockSupport();

    @Inject
    private IPersistServiceProvider persistServiceProvider;

    @Inject
    private IProjectInfoProvider projectInfoProvider;

    @Inject
    private IMapObjectPersist mapObjectPersist;

    /**
     * Find a Structure in persistence using its hash as the key.
     *
     * @param hash Hash of structure to find
     * @return Structure if it exists in persistence, {@code null} otherwise
     */
    public IStructure findDataStructure(String hash)
    {
        // During testing, a badly written test can invoke this when there is no project session active.
        // So, detect that and report it loudly.

        if (projectInfoProvider.getProjectUUID() == null)
        {
            throw new IllegalStateException("Trying to find structure outside of a project session: " + hash);
        }

        return mapObjectPersist.findStructureByHash(hash);
    }

    /**
     * Ensure that the given structure is properly persisted, unless an existing structure with the same hash is
     * already in persistence.  If so, return the pre-existing structure.
     * <p>
     * This is an internal interface method and should not be called from plugins.
     *
     * @param struct The structure to get persisted
     * @return The input structure if it was persisted, or an existing structure pulled from persistence with the same hash value
     */
    @Internal
    public IStructure persistDataStructure(IStructure struct)
    {
        // Same as above, report illegal usage from tests.
        if (projectInfoProvider.getProjectUUID() == null)
        {
            throw new IllegalStateException("Trying to persist structure outside of a project session: " + struct);
        }

        IStructure canonicalStructure = struct;
        final IPersistService ps = persistServiceProvider.getPersistService();

        final String hashValue = struct.getHashValue();

        // Ensure that the test and create operation are atomic by locking the hash value as a lock key
        final Lock lock = hashLocks.lock(hashValue);

        try
        {
            // Try to locate the structure in persistence, if it's not there, then create it
            IStructure existingStruct = findDataStructure(hashValue);

            if (existingStruct == null)
            {
                ps.persistEntity(struct);
            } else
            {
                canonicalStructure = existingStruct;
            }
        } catch (ConstraintViolationException e)
        {
            logger.warn("ConstraintViolationException persisting structure", e);

            // This really shouldn't happen unless some other thread created the structure in the middle of
            // our test/create operation.  In any case, just read out the (now) existing structure.

            canonicalStructure = findDataStructure(hashValue);
        } finally
        {
            lock.unlock(); // Ensure we always unlock this
        }

        return canonicalStructure;
    }
}
