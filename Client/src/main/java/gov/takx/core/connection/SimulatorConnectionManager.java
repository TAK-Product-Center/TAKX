package gov.takx.core.connection;

import gov.takx.api.annotation.InstanceNumber;
import gov.takx.api.annotation.PlatformUnderTest;
import gov.takx.api.annotation.ProgramPreference;
import gov.takx.api.commservices.ICommServiceManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

import static gov.takx.platform.preferences.PreferenceEntry.ENABLE_EXTERNAL_SIMULATOR;

/**
 * Manages opening/closing the connection listener on the simulator port.
 *
 * @since unknown, renamed from DeviceGatewayConnectionManager and reworked in 3.0
 */
@ApplicationScoped
public class SimulatorConnectionManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // The base port to open for accepting simulator input
    private static final int SIMULATOR_BASE_PORT = 1500;

    private final ICommServiceManager commServiceManager;
    private final boolean externalSimulatorEnabled;
    private final boolean platformUnderTest;

    private final int simulatorPort;

    @Inject
    SimulatorConnectionManager(ICommServiceManager commServiceManager, @InstanceNumber int instanceNumber,
                               @ProgramPreference(ENABLE_EXTERNAL_SIMULATOR) boolean externalSimulatorEnabled,
                               @PlatformUnderTest boolean platformUnderTest)
    {
        this.commServiceManager = commServiceManager;
        this.externalSimulatorEnabled = externalSimulatorEnabled;
        this.platformUnderTest = platformUnderTest;

        // Calculate the port to listen on based on the instance number
        simulatorPort = SIMULATOR_BASE_PORT + instanceNumber;
    }

    /**
     * Start listening on the simulator port, if configured and the platform is NOT under test.
     */
    public void startListening()
    {
        if (externalSimulatorEnabled && !platformUnderTest)
        {
            logger.info("Listening on simulator port: {}", simulatorPort);

            commServiceManager.openServerSocket(simulatorPort, null, null, "Simulator");
        }
    }

    /**
     * Stop listening on the simulator port.
     */
    public void stopListening()
    {
        if (externalSimulatorEnabled)
        {
            commServiceManager.closeServerSocket(simulatorPort, null);
        }
    }
}
