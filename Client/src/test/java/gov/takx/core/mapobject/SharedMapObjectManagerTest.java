package gov.takx.core.mapobject;

import gov.takx.api.mapobject.ISharedMapObjectManager;
import gov.takx.test.datasim.BloodHoundSim;
import gov.takx.test.extension.DependsOn;
import gov.takx.test.extension.DependsOnOrdering;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test the IShareMapObjectManager concrete implementation.
 */
@TestMethodOrder(DependsOnOrdering.class)
public class SharedMapObjectManagerTest
{
    private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String DEVICE_A_FAMILY = BloodHoundSim.BLOODHOUND_FAMILY;
    private static final String DEVICE_A = BloodHoundSim.BLOODHOUND_TYPE;
    private static final String DEVICE_A_ID_1 = "1";
    private static final String DEVICE_A_ID_2 = "2";

    private static final String DEVICE_B_FAMILY = "gov.takx.Foxhound";
    private static final String DEVICE_B = "foxhound";
    private static final String DEVICE_B_ID_1 = "1";
    private static final String DEVICE_B_ID_2 = "2";

    private final ISharedMapObjectManager manager = new SharedMapObjectManager();

    @BeforeAll
    public static void setup()
    {
        if (logger.isDebugEnabled()) logger.debug("{} is starting", SharedMapObjectManagerTest.class.getCanonicalName());
    }

    @Test
    public void registrationTest()
    {
        manager.registerShare(DEVICE_A_FAMILY, DEVICE_A, DEVICE_A_ID_1, DEVICE_B_FAMILY, DEVICE_B, DEVICE_B_ID_1);
        boolean isRegistered = manager.isShared(DEVICE_A_FAMILY, DEVICE_A, DEVICE_A_ID_1, DEVICE_B_FAMILY, DEVICE_B, DEVICE_B_ID_1);
        assertThat(isRegistered).withFailMessage("Registration Failed").isTrue();
    }

    @Test
    public void unregisterTest()
    {
        manager.registerShare(DEVICE_A_FAMILY, DEVICE_A, DEVICE_A_ID_2, DEVICE_B_FAMILY, DEVICE_B, DEVICE_B_ID_1);
        boolean isRegistered = manager.isShared(DEVICE_A_FAMILY, DEVICE_A, DEVICE_A_ID_2, DEVICE_B_FAMILY, DEVICE_B, DEVICE_B_ID_1);
        assertThat(isRegistered).withFailMessage("Registration Failed").isTrue();

        manager.unregisterShare(DEVICE_A_FAMILY, DEVICE_A, DEVICE_A_ID_2, DEVICE_B_FAMILY, DEVICE_B, DEVICE_B_ID_1);
        isRegistered = manager.isShared(DEVICE_A_FAMILY, DEVICE_A, DEVICE_A_ID_2, DEVICE_B_FAMILY, DEVICE_B, DEVICE_B_ID_1);
        assertThat(!isRegistered).withFailMessage("Unregistration Failed").isTrue();
    }

    @Test
    @DependsOn({"registrationTest", "unregisterTest"})
    public void oneWayCommunicationTest()
    {
        boolean isAuthorized = manager.isShared(DEVICE_B_FAMILY, DEVICE_B, DEVICE_B_ID_1, DEVICE_A_FAMILY, DEVICE_A, DEVICE_A_ID_1);
        assertThat(!isAuthorized).withFailMessage("One Way Comms Security Failure").isTrue();
    }

    @Test
    @DependsOn({"registrationTest", "unregisterTest"})
    public void unauthorizedPeerCommunicationTest()
    {
        boolean isAuthorized = manager.isShared(DEVICE_A_FAMILY, DEVICE_A, DEVICE_A_ID_1, DEVICE_B_FAMILY, DEVICE_B, DEVICE_B_ID_2);
        assertThat(!isAuthorized).withFailMessage("Unauthorized Peer Security Failure").isTrue();
    }
}
