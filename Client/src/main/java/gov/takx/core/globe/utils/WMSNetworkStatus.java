package gov.takx.core.globe.utils;

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.util.NetworkStatus;
import gov.nasa.worldwind.util.WWIO;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.UserPreferenceType;
import gov.takx.api.preferences.PreferenceEntryChange;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.preferences.UserPreferenceEntry;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.event.Observes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Taken nearly verbatim from WW class BasicNetworkStatus.  When that class is enhanced to make it more
 * extensible or configurable, we should ditch this class and use the WW class. However, a significant difference between
 * this class and BasicNetworkStatus is that the isHostUnavailable method is not synchronized in this class. This method
 * was made non-synchronized to fix a problem checking network status on a closed network. If we replace this class
 * with BasicNetworkStatus we need to make sure that the WW class does not re-introduce the bug. See CORE-2027.
 * <p/>
 * The list of hosts that we want to test for connectivity is the set of currently configured WMS servers.  This
 * provides better support for operation on closed networks where the internet is not reachable.  As long as
 * one of the configured WMS servers is available, then we consider the network to be available.
 *
 */
@PseudoBean(singletonObserver = true)
public class WMSNetworkStatus extends AVListImpl implements NetworkStatus
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final long DEFAULT_TRY_AGAIN_INTERVAL = TimeUnit.SECONDS.toMillis(60);
    private static final int DEFAULT_ATTEMPT_LIMIT = 4; // number of unavailable events to declare host unavailable
    private static final Set<String> networkTestSites = new HashSet<>();
    private static final long NETWORK_STATUS_REPORT_INTERVAL = TimeUnit.SECONDS.toMillis(60);

    private static class HostInfo
    {
        private final long tryAgainInterval;
        private final int attemptLimit;
        private final AtomicInteger logCount = new AtomicInteger(1);
        private final AtomicLong lastLogTime = new AtomicLong(System.currentTimeMillis());

        private HostInfo(int attemptLimit, long tryAgainInterval)
        {
            this.tryAgainInterval = tryAgainInterval;
            this.attemptLimit = attemptLimit;
        }

        private boolean isUnavailable()
        {
            return this.logCount.get() >= this.attemptLimit;
        }

        private boolean isTimeToTryAgain()
        {
            return System.currentTimeMillis() - this.lastLogTime.get() >= this.tryAgainInterval;
        }
    }

    private final ConcurrentHashMap<String, HostInfo> hostMap = new ConcurrentHashMap<>();

    private final AtomicLong tryAgainInterval = new AtomicLong(DEFAULT_TRY_AGAIN_INTERVAL);
    private final AtomicInteger attemptLimit = new AtomicInteger(DEFAULT_ATTEMPT_LIMIT);

    // Fields for determining overall network status.
    private volatile boolean offlineMode;
    private final AtomicLong lastUnavailableLogTime = new AtomicLong(System.currentTimeMillis());
    private final AtomicLong lastAvailableLogTime = new AtomicLong(System.currentTimeMillis() + 1);
    private final AtomicLong lastNetworkCheckTime = new AtomicLong(System.currentTimeMillis());
    private final AtomicLong lastNetworkStatusReportTime = new AtomicLong(0);
    private final AtomicBoolean lastNetworkUnavailableResult = new AtomicBoolean(false);

    // List of servers to exclude when calculating network status timeout.
    private final List<String> excludedUnresponsiveServers =
            Arrays.asList(PreferenceEntry.EXCLUDED_UNRESPONSIVE_SERVERS.getStringValue().split(","));

    public WMSNetworkStatus()
    {
        String oms = Configuration.getStringValue(AVKey.OFFLINE_MODE, "false");
        offlineMode = oms.startsWith("t") || oms.startsWith("T");
    }

    /**
     * Update our test sites when the WMS imagery servers change.
     *
     * @since 2.35
     */
    private void handleImageryServerChange(@Observes @UserPreferenceType(UserPreferenceEntry.WMS_IMAGERY_SERVERS) PreferenceEntryChange ignored)
    {
        refreshTestSites();
    }

    /**
     * Update our test sites when the WMS elevation servers change.
     *
     * @since 2.35
     */
    private void handleElevationServerChange(@Observes @UserPreferenceType(UserPreferenceEntry.WMS_ELEVATION_SERVERS) PreferenceEntryChange ignored)
    {
        refreshTestSites();
    }

    /**
     * Update the list of test sites from the given string containing a list of comma-separated WMS entries.
     */
    protected synchronized void refreshTestSites()
    {
        networkTestSites.clear();

        addTestSites(UserPreferenceEntry.WMS_IMAGERY_SERVERS.getStringValue().split(","));
        addTestSites(UserPreferenceEntry.WMS_ELEVATION_SERVERS.getStringValue().split(","));

        logger.debug("wmsList: {}", networkTestSites);
    }

    private void addTestSites(String[] serverNames)
    {
        for (String serverName : serverNames)
        {
            String hostUrl = serverName.trim();
            try
            {
                if (!hostUrl.isEmpty())
                {
                    final String host = new URL(hostUrl).getHost();
                    networkTestSites.add(host);
                    logger.debug("Server to test: {}", host);
                }
            } catch (MalformedURLException e)
            {
                logger.warn("Invalid WMS host URL: {} {}", hostUrl, e.getMessage());
            }
        }
    }

    /**
     * Indicates whether World Wind will attempt to connect to the network to retrieve data or for other reasons.
     *
     * @return {@code true} if World Wind is in off-line mode, {@code false} if not.
     */
    public boolean isOfflineMode()
    {
        return offlineMode;
    }

    /**
     * Indicate whether World Wind should attempt to connect to the network to retrieve data or for other reasons.
     * The default value for this attribute is {@code false}, indicating that the network should be used.
     *
     * @param offlineMode {@code true} if World Wind should use the network, {@code false} otherwise
     */
    public void setOfflineMode(boolean offlineMode)
    {
        this.offlineMode = offlineMode;
    }

    /**
     * Set the number of times a host must be logged as unavailable before it is marked unavailable in this class.
     *
     * @param limit the number of log-unavailability invocations necessary to consider the host unreachable.
     * @throws IllegalArgumentException if the limit is less than 1.
     */
    public void setAttemptLimit(int limit)
    {
        if (limit < 1)
        {
            String message = "Invalid limit in setAttemptLimit";
            logger.error(message);
            throw new IllegalArgumentException(message);
        }

        this.attemptLimit.set(limit);
    }

    /**
     * Set the length of time to wait until a host is marked as not unreachable subsequent to its being marked
     * unreachable.
     *
     * @param interval The length of time, in milliseconds, to wait to unmark a host as unreachable.
     * @throws IllegalArgumentException if the interval is less than 0.
     */
    public void setTryAgainInterval(long interval)
    {
        if (interval < 0)
        {
            String message = "Invalid interval in setTryAgainInterval";
            logger.error(message);
            throw new IllegalArgumentException(message);
        }

        this.tryAgainInterval.set(interval);
    }

    /**
     * Returns the number of times a host must be logged as unavailable before it is marked unavailable in this class.
     *
     * @return the limit.
     */
    public int getAttemptLimit()
    {
        return this.attemptLimit.get();
    }

    /**
     * Returns the length of time to wait until a host is marked as not unreachable subsequent to its being marked
     * unreachable.
     *
     * @return the interval, in milliseconds.
     */
    public long getTryAgainInterval()
    {
        return this.tryAgainInterval.get();
    }

    /**
     * Log a host as unavailable. Each invocation increments the host's attempt count. When the count equals or exceeds
     * the attempt limit, the host is marked as unavailable.
     *
     * @param url a url containing the host to mark as unavailable.
     */
    public synchronized void logUnavailableHost(@Nonnull URL url)
    {
        if (this.offlineMode)
        {
            return;
        }

        logger.debug("Host is unavailable: {}", url);

        String hostName = url.getHost();
        HostInfo hi = this.hostMap.get(hostName);
        if (hi != null)
        {
            if (!hi.isUnavailable())
            {
                hi.logCount.incrementAndGet();
                if (hi.isUnavailable()) // host just became unavailable
                {
                    this.firePropertyChange(NetworkStatus.HOST_UNAVAILABLE, null, url);
                }
            }
            hi.lastLogTime.set(System.currentTimeMillis());
        } else
        {
            hi = new HostInfo(this.attemptLimit.get(), this.tryAgainInterval.get());
            hi.logCount.set(1);
            if (hi.isUnavailable()) // the attempt limit may be as low as 1, so handle that case here
            {
                this.firePropertyChange(NetworkStatus.HOST_UNAVAILABLE, null, url);
            }
            this.hostMap.put(hostName, hi);
        }

        // If specified in program properties, ignore this server when calculating timeout for network status
        if (!excludedUnresponsiveServers.contains(hostName))
        {
            lastUnavailableLogTime.set(System.currentTimeMillis());
        }
    }

    /**
     * Log a host as available. Each invocation causes the host to no longer be marked as unavailable. Its
     * unavailability count is effectively set to 0.
     *
     * @param url a url containing the host to mark as available.
     */
    public synchronized void logAvailableHost(@Nonnull URL url)
    {
        if (this.offlineMode)
        {
            return;
        }

        logger.debug("Host is available: {}", url);

        String hostName = url.getHost();
        HostInfo hi = this.hostMap.get(hostName);
        if (hi != null)
        {
            this.hostMap.remove(hostName); // host is available again
            firePropertyChange(NetworkStatus.HOST_AVAILABLE, null, url);
        }

        this.lastAvailableLogTime.set(System.currentTimeMillis());
    }

    /**
     * Indicates whether the host has been marked as unavailable. To be marked unavailable a host's attempt count must
     * exceed the specified attempt limit.
     *
     * @param url a url containing the host to check for availability.
     * @return true if the host is marked as unavailable, otherwise false.
     */
    // This method is synchronized in the WW BasicNetworkStatus class. However the method does not need to be synchronized,
    // and making it synchronized causes problems on closed networks. See CORE-2027.
    public boolean isHostUnavailable(@Nonnull URL url)
    {
        if (this.offlineMode)
        {
            return true;
        }

        String hostName = url.getHost();
        HostInfo hi = this.hostMap.get(hostName);
        if (hi == null)
        {
            return false;
        }

        if (hi.isTimeToTryAgain())
        {
            hi.logCount.set(0); // info removed from table in logAvailableHost
            return false;
        }

        return hi.isUnavailable();
    }

    /**
     * Indicates whether a public network can be reached or has been reached in the previous five seconds.
     *
     * @return false if the network can be reached or has been reached in the previous five seconds, otherwise true.
     */
    public boolean isNetworkUnavailable()
    {
        return this.offlineMode || this.isNetworkUnavailable(5000L);
    }

    /**
     * Indicates whether a public network can be reached or has been reached in a specified previous amount of time.
     *
     * @param checkInterval the number of milliseconds in the past used to determine whether the server was available
     *                      recently.
     * @return false if the network can be reached or has been reached in a specified time, otherwise true.
     */
    public synchronized boolean isNetworkUnavailable(long checkInterval)
    {
        if (this.offlineMode)
        {
            return true;
        }

        // If there's been success since failure, network assumed to be reachable.
        if (this.lastAvailableLogTime.get() > this.lastUnavailableLogTime.get())
        {
            this.lastNetworkUnavailableResult.set(false);
            return this.lastNetworkUnavailableResult.get();
        }

        long now = System.currentTimeMillis();

        // If there's been success recently, network assumed to be reachable.
        if (!this.lastNetworkUnavailableResult.get() && now - this.lastAvailableLogTime.get() < checkInterval)
        {
            return this.lastNetworkUnavailableResult.get();
        }

        // If query comes too soon after an earlier one that addressed the network, return the earlier result.
        if (now - this.lastNetworkCheckTime.get() < checkInterval)
        {
            return this.lastNetworkUnavailableResult.get();
        }

        this.lastNetworkCheckTime.set(now);

        logger.debug("Testing network WMS sites");

        for (String testHost : networkTestSites)
        {
            if (isHostReachable(testHost))
            {
                {
                    this.lastNetworkUnavailableResult.set(false); // network not unreachable
                    return this.lastNetworkUnavailableResult.get();
                }
            }
        }

        if (now - this.lastNetworkStatusReportTime.get() > NETWORK_STATUS_REPORT_INTERVAL)
        {
            this.lastNetworkStatusReportTime.set(now);
            logger.info("Network is unreachable");
        }

        this.lastNetworkUnavailableResult.set(true); // if no successful contact then network is unreachable
        return this.lastNetworkUnavailableResult.get();
    }

    /**
     * Indicates whether the NASA World Wind servers can be reached.
     *
     * @return false if the servers can be reached, otherwise true.
     */
    @Override
    public boolean isWorldWindServerUnavailable()
    {
        return this.offlineMode || !isHostReachable("worldwind.arc.nasa.gov");
    }

    private static boolean isHostReachable(String hostName)
    {
        try
        {
            // Assume host is unreachable if we can't get its dns entry without getting an exception
            //noinspection ResultOfMethodCallIgnored
            InetAddress.getByName(hostName);
        } catch (UnknownHostException e)
        {
            logger.debug("WMS Host is unreachable: {}", hostName);
            return false;
        } catch (Exception e)
        {
            logger.info("Error testing WMS Host reachability: {}", hostName, e);
            return false;
        }

        // Was able to get internet address, but host still might not be reachable because the address might have been
        // cached earlier when it was available. So need to try something else.

        URLConnection connection = null;
        try
        {
            URL url = new URL("http://" + hostName);
            Proxy proxy = WWIO.configureProxy();
            if (proxy != null)
            {
                connection = url.openConnection(proxy);
            } else
            {
                connection = url.openConnection();
            }

            connection.setConnectTimeout(2000);
            connection.setReadTimeout(5000);
            String ct = connection.getContentType();
            if (ct != null)
            {
                logger.debug("WMS server contacted: {}", url);
                return true;
            }
        } catch (IOException e)
        {
            logger.info("Error contacting host: {} {}", hostName, e.getMessage());
        } finally
        {
            if (connection instanceof HttpURLConnection httpURLConnection) httpURLConnection.disconnect();
        }

        return false;
    }

    // TODO: investigate whether or not this subclass can be eliminated by using the latest NetworkStatus extensibility
    // TODO: improvements, including the methods below. See notes in class docs concerning isHostUnavailable before
    // TODO: replacing this class with the WW implementation.

    /**
     * A no-op implementation of {@link gov.nasa.worldwind.util.NetworkStatus#getNetworkTestSites()} which always
     * returns {@code null}.
     */
    public List<String> getNetworkTestSites()
    {
        return null;
    }

    /**
     * A no-op implementation of {@link gov.nasa.worldwind.util.NetworkStatus#setNetworkTestSites(java.util.List)} which
     * does nothing.
     */
    public void setNetworkTestSites(List<String> strings)
    {
    }
}
