package gov.takx.core.database.provider;

import gov.takx.api.configuration.IProgramEnvironment;
import gov.takx.api.event.project.ProjectClosed;
import gov.takx.api.event.project.ProjectPackaged;
import gov.takx.api.persist.IDatabaseProvider;
import gov.takx.api.persist.IDatabaseServer;
import gov.takx.api.project.IProjectManager;
import gov.takx.commons.preferences.PreferenceUtils;
import gov.takx.core.database.exception.MissingDatabasePathException;
import gov.takx.platform.preferences.PreferencesNode;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Reception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;

/**
 * Abstract base for database providers.  A Database provider is responsible for the actual database implementation
 * (like HSQLDB or Postgres), managing DB server instances.
 *
 * @since 3.0
 */
public abstract class ADatabaseProvider implements IDatabaseProvider
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String DATABASE = "database";
    private static final String CONFIGURATION = "configuration";

    protected final IProgramEnvironment programEnvironment;
    private final IProjectManager projectManager;
    private final String providerType;
    private PreferencesNode rootPreferences;

    private IDatabaseServer coreServer; // The single core server instance

    /**
     * @param providerType The provider's type identifier
     */
    protected ADatabaseProvider(IProgramEnvironment programEnvironment, IProjectManager projectManager, String providerType)
    {
        this.programEnvironment = programEnvironment;
        this.projectManager = projectManager;
        this.providerType = providerType;
    }

    @Override
    public String getType()
    {
        return providerType;
    }

    @Override
    public IDatabaseServer getCoreServer()
    {
        return coreServer != null
                ? coreServer
                : (coreServer = createServer(makeServerSpecification()));
    }

    /**
     * Create a server specification for the current project directory and configuration.
     *
     * @return server specification
     * @see #getConfiguration()
     */
    private ServerSpecification makeServerSpecification()
    {
        return new ServerSpecification(new File(projectManager.getProjectDirectory()), getConfiguration());
    }

    /**
     * Create a DB server instance from the given specification.
     *
     * @param serverSpecification Server details
     * @return database server instance
     */
    protected abstract IDatabaseServer createServer(ServerSpecification serverSpecification);

    /**
     * @return The root preferences node for this database provider
     */
    protected PreferencesNode getRootPreferences()
    {
        // Load the root preferences, if needed
        return rootPreferences != null
                ? rootPreferences
                : (rootPreferences = loadRootPreferences(providerType));
    }

    /**
     * @return The "root" preferences for this provider type (pulled from the database properties file)
     */
    private PreferencesNode loadRootPreferences(String type)
    {
        final File preferencesFile = programEnvironment.getDatabasePropertiesFile();

        try
        {
            final Preferences preferences = PreferenceUtils.getPreferencesFromFileChecked(preferencesFile, DATABASE);
            return new PreferencesNode(preferences.node(type));
        } catch (FileNotFoundException e)
        {
            final String message = "Database Preferences file missing: " + preferencesFile.getAbsolutePath();
            logger.error(message);
            throw new MissingDatabasePathException(message);
        } catch (InvalidPreferencesFormatException | IOException e)
        {
            final String message = "Database Preferences file corrupted: " + preferencesFile.getAbsolutePath();
            logger.error(message);
            logger.error("Cause: {}", e.getMessage());
            throw new MissingDatabasePathException(message);
        }
    }

    /**
     * @return The 'configuration' node from the root preferences
     */
    protected PreferencesNode getConfiguration()
    {
        final PreferencesNode preferences = getRootPreferences();

        if (!preferences.nodeExists(CONFIGURATION))
        {
            throw new IllegalStateException("Missing preferences node: " + CONFIGURATION);
        }

        return preferences.node(CONFIGURATION);
    }

    /**
     * Handle the project being packaged.  Dump the DB contents.
     */
    protected void onProjectPackaged(@SuppressWarnings("unused") @Observes(notifyObserver = Reception.IF_EXISTS) ProjectPackaged event)
    {
        // This may look like it's in the wrong place, but it's actually not.  The way that packaging works is that
        // the database contents are being updated during the "packaging" phase.  This phase (packaged) is initiated
        // after all that work, but *before* the actual creation of the zip file.  So, this is the point at which
        // we need to get anything we want in the zip file onto disk.  Thus, we do our dump here.

        coreServer.dumpData();
    }

    /**
     * Handle the project being closed.  Call {@link #handleProjectClosed()}.
     */
    private void onProjectClosed(@SuppressWarnings("unused") @Observes(notifyObserver = Reception.IF_EXISTS) ProjectClosed event)
    {
        handleProjectClosed();
    }

    /**
     * Handle the project being closed.  The default implementation is to call {@link #shutdown()}.
     *
     * @see #shutdown()
     */
    protected void handleProjectClosed()
    {
        shutdown();
    }

    @Override
    public void shutdown()
    {
        logger.debug("SHUTDOWN");

        if (coreServer != null)
        {
            coreServer.shutdown();
            coreServer = null;
        }

        rootPreferences = null; // Force reload of preferences (some tests swap out preferences)
    }
}
