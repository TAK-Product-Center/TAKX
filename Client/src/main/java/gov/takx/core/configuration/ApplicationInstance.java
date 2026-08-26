package gov.takx.core.configuration;

import gov.takx.api.annotation.CallSign;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.InstanceNumber;
import gov.takx.api.annotation.NodeUUID;
import gov.takx.api.annotation.PlatformUnderTest;
import gov.takx.api.configuration.IApplicationInstance;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.utils.StringUtils;
import gov.takx.commons.utils.UUIDUtils;
import gov.takx.platform.preferences.PreferenceEntry;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

/**
 * Manages the identifying details of the instance (execution) of the application.
 *
 */
@InjectableService(registerAs = IApplicationInstance.class)
@ApplicationScoped
public class ApplicationInstance extends BareApplicationInstance implements IApplicationInstance
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final File NODE_UUID_FILE = new File("nodeUUID.txt");

    private static final String ERROR_CALLSIGN = "ERROR";
    private static final String CALLSIGN_ERROR_MESSAGE = "{} does not exist or is not readable;" +
            " using '" + ERROR_CALLSIGN + "' as callsign!";

    private final boolean platformUnderTest;

    private UUID nodeUUID; // This Node's (machine) UUID
    private String callSign; // This node's call sign
    private IPreferenceMonitor<String> callSignMonitor;

    @Inject
    public ApplicationInstance(@PlatformUnderTest boolean platformUnderTest)
    {
        this.platformUnderTest = platformUnderTest;
    }

    /**
     * @return Current instance number
     */
    @Produces
    @InstanceNumber
    public int produceInstanceNumber()
    {
        return getInstanceNumber();
    }

    @Override
    @Produces
    @NodeUUID
    public UUID getNodeUUID()
    {
        return nodeUUID != null ? nodeUUID : (nodeUUID = initNodeUUID());
    }

    /**
     * @return Node UUID as a String
     * @since 5.1
     */
    @Produces
    @NodeUUID
    private String produceNodeUuidAsString()
    {
        return getNodeUUID().toString();
    }

    @Override
    @Produces
    @CallSign
    public String getCallSign()
    {
        // Can't allocate this monitor during construction or in PostConstruct, or we get container failures
        if (callSignMonitor == null) callSignMonitor = PreferenceEntry.CALL_SIGN.newStringMonitor(this::updateCallsign);

        if (callSign == null) updateCallsign(getBaseCallSign());
        return callSign;
    }

    /**
     * @return The file where the node UUID is stored
     * @since 5.7
     */
    @Nonnull
    public File getNodeUuidFile()
    {
        return NODE_UUID_FILE;
    }

    /**
     * Update our instance-specific callsign from a new base callsign.
     * @since 5.4
     */
    private void updateCallsign(String callSign)
    {
        logger.debug("update callsign '{}'", callSign);
        this.callSign = isDefaultInstance() ? callSign : callSign + getInstanceNumber();
    }

    /**
     * We need a consistent node UUID for this node. Previously, we used to generate the UUID from the MAC address so we
     * have a single node UUID for the machine. However, in the presence of some VPN software, every client machine in
     * the VPN can be assigned the *same* MAC address.
     * <p>
     * Now, we just generate a UUID one time, the first time it is requested. On that first request, we generate a
     * time-based "base" UUID (this still uses the MAC address, but it has additional time data in it that should ensure
     * that we don't generate 2 identical base UUIDs). If the current instance of TAKX is not the default instance, then
     * we add the instance number to the base UUID string, to give us a unique string per instance, and then generate a
     * Type 3 Node UUID from that.
     * <p>
     * This mechanism avoids the problem of VPNs MAC address munging, and it still allows multiple instances of TAKX to
     * run on a single machine.
     */
    private UUID initNodeUUID()
    {
        String preferenceUuid = PreferenceEntry.NODE_UUID.getStringValue();

        if (preferenceUuid == null)
        {
            preferenceUuid = readOrCreateNodeUuid();

            if (!isDefaultInstance())
            {
                final String instanceName = preferenceUuid + getInstanceNumber();
                preferenceUuid = UUIDUtils.generateType3UUID(instanceName).toString();

                logger.debug("Generated node UUID for instance: {}", preferenceUuid);
            }

            PreferenceEntry.NODE_UUID.putAndSave(preferenceUuid);
        } else
        {
            logger.debug("Use existing node UUID: {}", preferenceUuid);
        }

        logger.debug("NodeUUID: {}", preferenceUuid);

        return UUID.fromString(preferenceUuid);
    }

    /**
     * In order to provide a stable UUID, regardless of whether the ProjectData directory is cleared, we store the
     * node UUID in a file in the execution directory, and also in the preferences so it can be easily accessed
     * from within the code.
     * <p/>
     * If the node UUID can't be read, then one is generated and written out.
     * 
     * @return node UUID read from disk, or generated and written to the file if it could not initially be read
     * @since 5.3
     */
    @Nonnull
    private String readOrCreateNodeUuid()
    {
        if (NODE_UUID_FILE.canRead())
        {
            try
            {
                return Files.readString(NODE_UUID_FILE.toPath(), StandardCharsets.UTF_8);
            } catch (IOException e)
            {
                logger.error("Failed to read UUID from {}", NODE_UUID_FILE.getAbsolutePath(), e);
            }
        }

        // Attempt to delete it, and then recreate it with a new UUID
        if (NODE_UUID_FILE.exists() && !NODE_UUID_FILE.delete()) logger.warn("Failed to delete: {}", NODE_UUID_FILE.getAbsolutePath());

        final UUID uuid = UUIDUtils.generateType1UUID(); // Generate one

        logger.debug("Generated node UUID: {}", uuid);

        try
        {
            Files.writeString(NODE_UUID_FILE.toPath(), uuid.toString(), StandardCharsets.UTF_8);
        } catch (IOException e)
        {
            logger.error("Failed to write UUID '{}' to {}", uuid, NODE_UUID_FILE.getAbsolutePath());
        }

        return uuid.toString();
    }

    /**
     * Get or generate the base Call Sign for this installation of TAKX. If it is not already in the program properties,
     * then we generate a new one and store it out.
     *
     * @return Base Call Sign for this TAKX installation
     * @since 3.0
     */
    private String getBaseCallSign()
    {
        String callSign = callSignMonitor.getCurrentValue();

        if (StringUtils.isEmpty(callSign) || ERROR_CALLSIGN.equals(callSign))
        {
            final File callSignsFile = new File("resources/tak-assets/txt/callsigns.txt");

            if (callSignsFile.canRead())
            {
                final String[] callSigns = FileUtils.readLines(callSignsFile);

                // Select one using the current host name as a randomizer
                callSign = callSigns.length > 0
                        ? callSigns[getModulusFromHostName() % callSigns.length]
                        : ERROR_CALLSIGN;
            } else
            {
                callSign = ERROR_CALLSIGN;

                if (platformUnderTest)
                {
                    // This file is often missing when running plugin tests; log at debug since this is not a real error
                    logger.debug(CALLSIGN_ERROR_MESSAGE, callSignsFile);
                } else
                {
                    logger.error(CALLSIGN_ERROR_MESSAGE, callSignsFile);
                }
            }

            logger.debug("Generated call sign: {}", callSign);

            PreferenceEntry.CALL_SIGN.putAndSave(callSign);
        } else
        {
            logger.debug("Existing call sign: {}", callSign);
        }

        return callSign;
    }

    /**
     * Try to get a modulus value for use as a "randomizer" so that if TAKX is uninstalled and then reinstalled, we
     * <em>should</em> generate the same modulus for selecting a call sign. This is done by using the hash code of the
     * current host name. If we fail to obtain the hostname, then we use the current time.
     *
     * @return Call sign modulus
     * @since 3.0
     */
    private int getModulusFromHostName()
    {
        int modulus;

        try
        {
            modulus = InetAddress.getLocalHost().getHostName().hashCode();
        } catch (Exception e)
        {
            logger.warn("Failed to get host name", e);
            modulus = (int) System.currentTimeMillis();
        }

        return modulus & 0x7FFFFFFF; // Ensure it is positive
    }
}
