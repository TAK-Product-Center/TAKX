package gov.takx.core.globe.utils;

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.avlist.AVKey;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.test.AContainerTest;
import gov.takx.test.utils.CdiTestUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Network status is determined by the reachability of WMS servers that are configured to pull from as documented
 * in {@link WMSNetworkStatus}.
 *
 * @since 3.4 (cherry picked from 2.39.1)
 */
public class WMSNetworkStatusTest extends AContainerTest
{
    @BeforeEach
    public void setupOfflineMode()
    {
        // We need offline mode to be false for these tests to work
        Configuration.setValue(AVKey.OFFLINE_MODE, "false");
    }

    @AfterAll
    public void restoreDefaults()
    {
        // Restore the default value so other tests are unaffected by this test
        Configuration.setValue(AVKey.OFFLINE_MODE, "true");
    }

    /**
     * Default mode should be online (since we set it above in setupOfflineMode).
     */
    @Test
    public void testOfflineModeDefault()
    {
        assertThat(new WMSNetworkStatus().isOfflineMode()).isFalse();
    }

    /**
     * Test the lastUnavailableLogTime is updated when logging an unavailable host that is not in the list of
     * unavailable servers to skip.
     */
    @Test
    public void testWithoutUnresponsiveServerList() throws MalformedURLException, InterruptedException
    {
        PreferenceEntry.EXCLUDED_UNRESPONSIVE_SERVERS.put("");
        WMSNetworkStatus networkStatus = new WMSNetworkStatus();

        AtomicLong lastUnavailableLogTime = CdiTestUtils.getField(networkStatus, "lastUnavailableLogTime");
        AtomicLong lastAvailableLogTime = CdiTestUtils.getField(networkStatus, "lastAvailableLogTime");
        assertThat(lastAvailableLogTime.get()).isEqualTo(lastUnavailableLogTime.get() + 1);

        // Sleep is necessary because lastAvailableLogTime is initialized to be lastUnavailableLogTime + 1 (see above)
        TimeUnit.MILLISECONDS.sleep(2);
        URL wwDataUrl = new URL("https://data.worldwind.arc.nasa.gov");
        networkStatus.logUnavailableHost(wwDataUrl);

        // lastUnavailableLogTime time should have been updated and > lastAvailableLogTime
        assertThat(lastAvailableLogTime.get()).isLessThanOrEqualTo(lastUnavailableLogTime.get());

        TimeUnit.MILLISECONDS.sleep(2);
        networkStatus.logAvailableHost(wwDataUrl);
        // lastAvailableLogTime time should have been updated and now > lastUnavailableLogTime
        assertThat(lastAvailableLogTime.get()).isGreaterThan(lastUnavailableLogTime.get());
    }

    /**
     * Test calls to logUnavailableHost do not update the lastUnavailableLogTime when the servers are included
     * in the list of unresponsive servers to skip.
     *
     * @throws MalformedURLException error in the protocol forming the URL
     * @throws InterruptedException sleep interrupted
     */
    @Test
    public void testWithUnresponsiveServerList() throws MalformedURLException, InterruptedException
    {
        PreferenceEntry.EXCLUDED_UNRESPONSIVE_SERVERS.put("data.worldwind.arc.nasa.gov,boneless.chicken.ranch");
        WMSNetworkStatus networkStatus = new WMSNetworkStatus();

        TimeUnit.MILLISECONDS.sleep(2);
        URL wwDataUrl = new URL("https://data.worldwind.arc.nasa.gov");
        networkStatus.logUnavailableHost(wwDataUrl);
        AtomicLong lastUnavailableLogTime = CdiTestUtils.getField(networkStatus, "lastUnavailableLogTime");
        AtomicLong lastAvailableLogTime = CdiTestUtils.getField(networkStatus, "lastAvailableLogTime");

        // lastUnavailableLogTime time should not have been updated
        assertThat(lastAvailableLogTime.get()).isGreaterThan(lastUnavailableLogTime.get());

        TimeUnit.MILLISECONDS.sleep(2);
        URL ranchUrl = new URL("https://boneless.chicken.ranch");
        networkStatus.logUnavailableHost(ranchUrl);
        // lastUnavailableLogTime time should not have been updated
        assertThat(lastAvailableLogTime.get()).isGreaterThan(lastUnavailableLogTime.get());

        TimeUnit.MILLISECONDS.sleep(2);
        URL hobbitUrl = new URL("https://second.breakfast");
        networkStatus.logUnavailableHost(hobbitUrl);
        // This time, lastUnavailableLogTime should have been updated
        assertThat(lastAvailableLogTime.get()).isLessThanOrEqualTo(lastUnavailableLogTime.get());
    }

    /**
     * Test that unavailable hosts are tracked until they are available again.
     *
     * @throws MalformedURLException error in the protocol forming the URL
     */
    @Test
    public void testHostAvailable() throws MalformedURLException
    {
        WMSNetworkStatus networkStatus = new WMSNetworkStatus();
        URL wwDataUrl = new URL("https://data.worldwind.arc.nasa.gov");
        assertThat(networkStatus.isHostUnavailable(wwDataUrl)).isFalse();

        networkStatus.logUnavailableHost(wwDataUrl);
        Map<?, ?> hostMap = CdiTestUtils.getField(networkStatus, "hostMap");
        assertThat(hostMap).hasSize(1);
        networkStatus.logAvailableHost(wwDataUrl);
        assertThat(hostMap).hasSize(0);
    }
}
