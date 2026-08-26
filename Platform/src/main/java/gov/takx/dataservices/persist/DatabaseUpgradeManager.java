package gov.takx.dataservices.persist;

import gov.takx.api.annotation.Internal;
import gov.takx.api.configuration.IProgramEnvironment;
import gov.takx.api.platform.IProgressMonitor;
import gov.takx.api.ui.IUserProxy;
import gov.takx.dataservices.persist.DatabaseVersionManager.DatabaseState;
import gov.takx.dataservices.persist.upgraders.AUpgrader;
import gov.takx.dataservices.persist.upgraders.UpgradeToV25;
import gov.takx.dataservices.persist.upgraders.UpgradeToV26;
import gov.takx.dataservices.persist.upgraders.UpgradeToV27;
import gov.takx.dataservices.persist.upgraders.UpgradeToV28;
import gov.takx.dataservices.persist.upgraders.UpgradeToV29;
import gov.takx.dataservices.persist.upgraders.UpgradeToV30;
import gov.takx.dataservices.persist.upgraders.UpgradeToV31;
import gov.takx.dataservices.persist.upgraders.UpgradeToV32;
import gov.takx.dataservices.persist.upgraders.UpgradeToV33;
import gov.takx.dataservices.persist.upgraders.UpgraderException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hibernate.dialect.Dialect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Manager for applying database upgraders.
 *
 * @since 3.0
 */
@Internal
@ApplicationScoped
public class DatabaseUpgradeManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final DatabaseVersionManager databaseVersionManager;
    private final IProgramEnvironment programEnvironment;
    private final IUserProxy userProxy;

    /**
     * List of upgraders to apply, in sequence. Database versions are now a simple monotonically increasing integer.
     * The ordinal position of the upgrader in this list is the "from" version of the database being upgraded minus 1.
     * The "to" version is the "from" version plus 1.
     */
    private static final List<Class<? extends AUpgrader>> UPGRADERS = List.of(
            UpgradeToV25.class,
            UpgradeToV26.class,
            UpgradeToV27.class,
            UpgradeToV28.class,
            UpgradeToV29.class,
            UpgradeToV30.class,
            UpgradeToV31.class,
            UpgradeToV32.class,
            UpgradeToV33.class);

    // The version at which DB upgrade support started.  This removes the need for a bunch of empty entries in UPGRADERS.
    static final int UPGRADE_VERSION_BASE = 24;

    /**
     * The current DB schema version.
     *
     * @implNote We calculate this to avoid manually maintaining it, which is error-prone.
     * @since 5.7.1
     */
    public static int CURRENT_VERSION = UPGRADE_VERSION_BASE + UPGRADERS.size();

    @Inject
    private DatabaseUpgradeManager(DatabaseVersionManager databaseVersionManager, IProgramEnvironment programEnvironment, IUserProxy userProxy)
    {
        this.databaseVersionManager = databaseVersionManager;
        this.programEnvironment = programEnvironment;
        this.userProxy = userProxy;
    }

    /**
     * Perform the upgrades required to bring the DB up to the current version.
     *
     * @param connection      JDBC connection
     * @param dialect         The Hibernate dialect in use
     * @param databaseState   current DB state
     * @param workingDir      Working directory (project dir for TAKX)
     * @param progressMonitor for progress updates
     * @return {@code true} if upgrade applied, {@code false} if not. Exception is thrown on error
     * @since 5.0 (signature change)
     */
    public boolean doUpgrade(Connection connection, Dialect dialect, DatabaseState databaseState,
                             File workingDir, IProgressMonitor progressMonitor)
    {
        if (!confirmUpgrade(progressMonitor)) return false;

        try
        {
            // The current DB version minus our starting version is the starting index of the upgraders to apply
            for (int i = databaseState.version() - UPGRADE_VERSION_BASE, size = UPGRADERS.size(); i < size; i++)
            {
                final String status = "Upgrading database to version " + (UPGRADE_VERSION_BASE + i + 1);

                progressMonitor.setSubstatus(status);
                logger.info(status);

                final AUpgrader upgrader = UPGRADERS.get(i).getConstructor().newInstance();

                upgrader.upgrade(dialect, databaseState, workingDir, connection);
            }

            // Finally, mark the database with the current version
            databaseVersionManager.insertCurrentVersion(connection);

            connection.commit(); // And commit all the work
        } catch (RuntimeException | ReflectiveOperationException | SQLException e)
        {
            throw new UpgraderException(e);
        }

        logger.info("Database upgrade complete");

        return true;
    }

    /**
     * Prompt user before proceeding with database upgrade.
     *
     * @param progressMonitor Progress monitor to use
     * @return {@code true} if the upgrade should be run, {@code false} otherwise
     */
    private boolean confirmUpgrade(final IProgressMonitor progressMonitor)
    {
        final String msg = "This project's database needs to be upgraded to work with version " +
                programEnvironment.getPlatformVersionString() +
                ".\nThis is an irreversible operation.\n\nDo you want to upgrade this database?";

        progressMonitor.setVisible(false);
        final boolean confirmed = userProxy.confirmDatabaseUpgrade(msg);
        progressMonitor.setVisible(true);

        return confirmed;
    }
}
