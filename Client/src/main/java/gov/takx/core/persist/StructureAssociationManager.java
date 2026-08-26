package gov.takx.core.persist;

import gov.takx.api.event.StopProjectServices;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.api.persist.IStructureAssociationManager;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.dataservices.persist.StructureAssociation;
import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to manage the association between Map Entities and Structures.  This supports the M:N relationships
 * between te objects.  We handle this ourselves so we can avoid Hibernate getting involved.
 * <p>
 * Note that associations are only added, never deleted, which makes managing them much simpler.
 *
 * @since 2.29
 */
@Singleton
public class StructureAssociationManager implements IStructureAssociationManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final IPersistServiceProvider persistServiceProvider;

    private final Set<StructureAssociation> associations = ConcurrentHashMap.newKeySet();

    @Inject
    public StructureAssociationManager(IPersistServiceProvider persistServiceProvider)
    {
        this.persistServiceProvider = persistServiceProvider;
    }

    @Override
    public void addAssociation(int mapEntityId, int structureId)
    {
        final StructureAssociation association = new StructureAssociation(mapEntityId, structureId);

        if (associations.add(association))
        {
            // Since this is a "new" association, we need to get it persisted

            persistServiceProvider.getPersistService().doWork(connection -> {
                try (Statement statement = connection.createStatement())
                {
                    statement.executeUpdate(association.getInsertSql());
                    connection.commit();
                } catch (SQLException e)
                {
                    logger.error("Failed to insert StructureAssociation {}", association, e);
                    connection.rollback();
                    throw e;
                }
            });
        }
    }

    /**
     * Load the structure associations and return a map from MapEntity ID to all its associated structure Ids.
     */
    public Map<Integer, Set<Integer>> doInitialLoad()
    {
        final Map<Integer, Set<Integer>> mapEntityStructureIds = new HashMap<>();

        if (associations.isEmpty())
        {
            persistServiceProvider.getPersistService().doWork(connection -> {
                try (PreparedStatement statement = connection.prepareStatement(StructureAssociation.LOAD_SQL);
                     ResultSet resultSet = statement.executeQuery())
                {
                    while (resultSet.next())
                    {
                        final StructureAssociation association = new StructureAssociation(resultSet);

                        associations.add(association);

                        mapEntityStructureIds.computeIfAbsent(association.getMapEntityId(), k -> new HashSet<>())
                                .add(association.getStructureId());
                    }
                    connection.commit();
                } catch (SQLException e)
                {
                    logger.error("Failed to load from StructureAssociation", e);
                    connection.rollback();
                    throw e;
                }
            });

            logger.debug("Initial SAs: {}", mapEntityStructureIds);
        } else
        {
            logger.error("Associations is NOT empty! {}", associations, new RuntimeException());
        }

        return mapEntityStructureIds;
    }

    /**
     * Cleanup - discard all our associations.
     */
    private void stopService(@Observes @Priority(ServiceOrdering.STOP_PersistServiceProvider) StopProjectServices event)
    {
        associations.clear();
    }
}
