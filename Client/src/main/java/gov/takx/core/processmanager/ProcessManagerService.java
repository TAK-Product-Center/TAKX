package gov.takx.core.processmanager;

import gov.takx.api.processmanager.IProcessManager;
import gov.takx.core.configuration.ProgramEnvironment;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.processmanager.IRemoteProcessManager;
import gov.takx.processmanager.ProcessManagerLauncher;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;

/**
 * Class responsible for starting and stopping the RemoteProcessManager and handling process registrations.
 *
 * @since 2.0.0
 */
@ApplicationScoped
public class ProcessManagerService implements IProcessManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final boolean headless;
    private final boolean underTest;
    private final String projectDataDirectory;

    private IRemoteProcessManager processManager;

    @Inject
    ProcessManagerService(ProgramEnvironment programEnvironment)
    {
        headless = programEnvironment.isHeadless();
        underTest = programEnvironment.isPlatformUnderTest();
        projectDataDirectory = programEnvironment.getProjectDataDirectory();
    }

    /**
     * Start the process manager.  In a separate method so it can be executed by a background thread.
     *
     * @since 2.37
     */
    private void startProcessManager()
    {
        try
        {
            processManager = ProcessManagerLauncher.getProcessManager(PreferenceEntry.NAME.getStringValue(), projectDataDirectory, headless);
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Stop the process manager.
     */
    @PreDestroy
    private void stopProcessManager()
    {
        if (!underTest && processManager != null)
        {
            try
            {
                processManager.shutdown();
            } catch (Exception e)
            {
                logger.warn("Unable to stop the process manager.", e);
            }
        }
    }

    @Override
    public synchronized void registerExternalProcess(long pid)
    {
        if (processManager != null)
        {
            processManager.addPID(pid);
        } else if (!underTest)
        {
            // Start it on demand
            logger.info("Starting Process Manager Service.");

            CompletableFuture.runAsync(this::startProcessManager)
                    .whenComplete((ignored, t) -> {
                        if (t != null)
                        {
                            logger.error("Unable to start process manager.", t);
                        } else
                        {
                            processManager.addPID(pid);
                        }
                    });
        }
    }
}
