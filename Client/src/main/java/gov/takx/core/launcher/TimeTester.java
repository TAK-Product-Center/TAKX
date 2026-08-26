package gov.takx.core.launcher;

import gov.takx.api.annotation.PlatformUnderTest;
import gov.takx.api.annotation.ProgramPreference;
import gov.takx.api.event.project.ProjectOpened;
import gov.takx.api.ui.IUserProxy;
import gov.takx.commons.utils.StringUtils;
import gov.takx.platform.preferences.PreferenceEntry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

/**
 * Simple background task to check that the local clock is not seriously skewed from the "real" time as defined
 * by NIST.  If it is, put up a dialog to alert the user.
 */
@ApplicationScoped
public final class TimeTester
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final Duration TIMEOUT_DELAY = Duration.ofSeconds(30);
    private static final long TIME_SKEW_LIMIT = TimeUnit.HOURS.toMillis(1);

    private final String timeServerHost; // Which time host are we contacting?
    private final IUserProxy userProxy;

    private final boolean doTimeTest; // Are we configured to perform the test?
    private boolean testSubmitted;

    @Inject
    TimeTester(@ProgramPreference(PreferenceEntry.TIME_SERVER_HOST) String timeServerHost, @PlatformUnderTest boolean platformUnderTest, IUserProxy userProxy)
    {
        this.timeServerHost = timeServerHost;
        this.userProxy = userProxy;

        doTimeTest = !platformUnderTest && !StringUtils.isEmpty(timeServerHost, true);
    }

    /**
     * Once the first project is opened, kick off the test, but only do it one time.
     */
    private void projectOpened(@Observes ProjectOpened ignored)
    {
        if (!doTimeTest)
        {
            logger.info("Time server connection is disabled.");
            return;
        }

        if (!testSubmitted)
        {
            testSubmitted = true;
            ForkJoinPool.commonPool().execute(this::runTest);
        }
    }

    /**
     * Contact the NIST server and determine our time skew.  If it is too large, then report it to the user.
     */
    private void runTest()
    {
        logger.info("Connecting to time server: {}", timeServerHost);

        NTPUDPClient client = new NTPUDPClient();
        client.setDefaultTimeout(TIMEOUT_DELAY);

        try
        {
            InetAddress hostAddr = InetAddress.getByName(timeServerHost);
            TimeInfo info = client.getTime(hostAddr);
            info.computeDetails();

            final Long offset = info.getOffset();

            logger.debug("Time skew: {}", offset);

            if (offset != null)
            {
                if (Math.abs(offset) > TIME_SKEW_LIMIT)
                {
                    final String msg = """
                            The local machine's time is significantly different than the current
                            time as defined by NIST.  This may prevent certain features, like imagery downloads,
                            from operating properly.  Please adjust your system clock.""";

                    userProxy.reportError("System Clock Error", msg, null);
                }
            } else
            {
                logger.warn("Failed to get time offset from {}}", timeServerHost);
            }
        } catch (UnknownHostException | NoRouteToHostException e)
        {
            logger.warn("Can't locate/reach time server at '{}' due to '{}', probably because you're on a closed network or no networking is configured.",
                    timeServerHost, e.getMessage());
        } catch (SocketTimeoutException ignored)
        {
            // Don't report the exception if it is a timeout
            logger.warn("TIMEOUT trying to check time on {}", timeServerHost);
        } catch (Exception e)
        {
            logger.warn("Failed to check time on NIST", e);
        }

        try
        {
            client.close();
        } catch (Exception ignored)
        {
            //  We tried
        }
    }
}
