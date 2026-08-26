package gov.takx.core.database.provider.hypersonic;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.core.database.provider.ADatabaseConnectionManager;
import jakarta.inject.Inject;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Map;

/**
 * Connection manager for HSQLDB.
 *
 * @since 3.0 (was previously in the Hypersonic plugin)
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class HypersonicDatabaseConnectionManager extends ADatabaseConnectionManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private CustomSqlFunctions customSqlFunctions;

    /**
     * Construct a connection manager with the given settings.
     *
     * @param settings Settings for the connection
     */
    public HypersonicDatabaseConnectionManager(Map<String, Object> settings)
    {
        super(settings);
    }

    @Override
    public void dropTables()
    {
        logger.debug("Dropping tables...");

        final Session sess = openSession();
        final Transaction transaction = sess.beginTransaction();
        //noinspection SqlResolve
        sess.createNativeMutationQuery("DROP SCHEMA PUBLIC CASCADE").executeUpdate();
        transaction.commit();
        closeSession(sess);
    }

    @Override
    protected void doCreateFunctions(Session session)
    {
        customSqlFunctions.defineCustomFunctions(session);
    }
}
