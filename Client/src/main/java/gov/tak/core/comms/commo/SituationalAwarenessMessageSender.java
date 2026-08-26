package gov.tak.core.comms.commo;

import com.atakmap.commoncommo.CoTSendMethod;
import gov.tak.api.comms.commo.ICommonCommoManager;
import gov.tak.api.cot.CoordinatedTime;
import gov.tak.api.cot.CotConstants;
import gov.tak.api.cot.detail.DetailConstants;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.maps.PointMapItem;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.tak.api.util.AttributeSet;
import gov.tak.api.util.StandardAttributeNames;
import gov.tak.platform.comms.commo.DeviceDisplayType;
import gov.tak.platform.comms.commo.ReportingStrategy;
import gov.takx.api.annotation.NodeUUID;
import gov.takx.api.annotation.ProgramPreference;
import gov.takx.api.event.location.LocationChanged;
import gov.takx.api.event.takserver.TakServerConnectionOpened;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.bootstrap.Version;
import gov.takx.commons.utils.StringUtils;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.utils.UnitsOfMeasureUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;

import static gov.takx.platform.preferences.PreferenceEntry.CALL_SIGN;
import static gov.takx.platform.preferences.PreferenceEntry.CONTACT_EMAIL;
import static gov.takx.platform.preferences.PreferenceEntry.SA_REPORTING_CONSTANT_INTERVAL_RELIABLE;
import static gov.takx.platform.preferences.PreferenceEntry.SA_REPORTING_CONSTANT_INTERVAL_UNRELIABLE;
import static gov.takx.platform.preferences.PreferenceEntry.SA_REPORTING_DISPLAY_TYPE;
import static gov.takx.platform.preferences.PreferenceEntry.SA_REPORTING_DYNAMIC_INTERVAL_MAXIMUM_RELIABLE;
import static gov.takx.platform.preferences.PreferenceEntry.SA_REPORTING_DYNAMIC_INTERVAL_MAXIMUM_UNRELIABLE;
import static gov.takx.platform.preferences.PreferenceEntry.SA_REPORTING_DYNAMIC_INTERVAL_MINIMUM_RELIABLE;
import static gov.takx.platform.preferences.PreferenceEntry.SA_REPORTING_DYNAMIC_INTERVAL_MINIMUM_UNRELIABLE;
import static gov.takx.platform.preferences.PreferenceEntry.SA_REPORTING_DYNAMIC_INTERVAL_STATIONARY_RELIABLE;
import static gov.takx.platform.preferences.PreferenceEntry.SA_REPORTING_DYNAMIC_INTERVAL_STATIONARY_UNRELIABLE;
import static gov.takx.platform.preferences.PreferenceEntry.SA_REPORTING_REPORT_NO_LOCATION;
import static gov.takx.platform.preferences.PreferenceEntry.SA_REPORTING_ROLE;
import static gov.takx.platform.preferences.PreferenceEntry.SA_REPORTING_SEND_LOCATION;
import static gov.takx.platform.preferences.PreferenceEntry.SA_REPORTING_STRATEGY;
import static gov.takx.platform.preferences.PreferenceEntry.SA_REPORTING_TEAM;
import static jakarta.enterprise.event.Reception.IF_EXISTS;

/**
 * Service to create and send a Situational Awareness (SA) CoT Message on a timed basis.
 *
 * @since 3.0
 */
@ApplicationScoped
public class SituationalAwarenessMessageSender
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String DEFAULT_PORT = "4242";

    // Timer delay until we send the first SA broadcast message and check to see if we can send SA again
    private static final long DELAY_SEC = 1;

    // Below this speed, use stationary reporting rate.
    private static final double MIN_SPEED_MPS = UnitsOfMeasureUtils.MPH_TO_MPS_MULTIPLIER;

    // Above this speed, use max reporting rate.
    private static final double MAX_SPEED_MPS = UnitsOfMeasureUtils.MPH_TO_MPS_MULTIPLIER * 30; // 30 MPH in meters per second

    // Speed range within which we scale the reporting rate.
    private static final double SPEED_RANGE = MAX_SPEED_MPS - MIN_SPEED_MPS;

    // Used as a buffer when determining if it's time to report
    private static final long STALE_BUFFER_MS = TimeUnit.SECONDS.toMillis(10);

    // Used as a padding in stale time
    private static final long STALE_PADDING_MS = TimeUnit.SECONDS.toMillis(15);

    // Rate multiplier for stale time calculations when using unreliable messaging.
    private static final int UNRELIABLE_RATE_MULTIPLIER = 4;

    // Rate multiplier for stale time calculations when using reliable messaging.
    private static final int RELIABLE_RATE_MULTIPLIER = 2;

    // Detail/attribute constants missing from takkernel
    private static final String EMAIL_ADDRESS = "emailAddress";
    private static final String VMF = "vmf";
    private static final String URN = "urn";

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();

    private final ICommonCommoManager commonCommoManager;
    private final ICoreExecutorService executorService;
    private final UUID nodeUuid;
    private final IMapObjectManager mapObjectManager;
    private final CotDetail takvDetail;

    private PointMapItem selfMarker;
    private GeoPointMetaData lastPosition;
    private double lastSpeed = Double.NaN;
    private double lastBearing = Double.NaN;

    // Should be set to true when relevant preferences are modified (Team Color, Callsign, etc.)
    private boolean reportImmediately = true;

    private SituationalAwarenessTimerTask situationalAwarenessTimerTask;
    private ScheduledFuture<?> scheduledTask;

    private ReportingStrategy reportingStrategy;
    private boolean sendLocation;

    private final IPreferenceMonitor<String> callSignMonitor = CALL_SIGN.newStringMonitor(v -> reportImmediately = true, false);
    private final IPreferenceMonitor<Boolean> sendLocationMonitor = SA_REPORTING_SEND_LOCATION.newBooleanMonitor(this::setSendLocation, false);
    private final IPreferenceMonitor<String> reportingStrategyMonitor = SA_REPORTING_STRATEGY.newStringMonitor(this::setReportingStrategy, false);
    private final IPreferenceMonitor<String> teamMonitor = SA_REPORTING_TEAM.newStringMonitor(v -> reportImmediately = true, false);
    private final IPreferenceMonitor<String> roleMonitor = SA_REPORTING_ROLE.newStringMonitor(v -> reportImmediately = true, false);
    private final IPreferenceMonitor<DeviceDisplayType> displayTypeMonitor = SA_REPORTING_DISPLAY_TYPE.newMonitor(DeviceDisplayType.class, v -> reportImmediately = true, false);

    @Inject
    SituationalAwarenessMessageSender(ICommonCommoManager commonCommoManager, ICoreExecutorService executorService,
                                      @NodeUUID UUID nodeUuid, IMapObjectManager mapObjectManager,
                                      @ProgramPreference(PreferenceEntry.PRODUCT_FLAVOR) String productFlavor)
    {
        this.commonCommoManager = commonCommoManager;
        this.executorService = executorService;
        this.nodeUuid = nodeUuid;
        this.mapObjectManager = mapObjectManager;

        setReportingStrategy(SA_REPORTING_STRATEGY.getStringValue());
        setSendLocation(SA_REPORTING_SEND_LOCATION.getBooleanValue());

        takvDetail = createTakvDetail(productFlavor);
    }

    private CotDetail createTakvDetail(String productFlavor)
    {
        final var detail = new CotDetail(DetailConstants.TAKV);

        final String os = String.format("%s , version %s, arch %s", SystemUtils.OS_NAME, SystemUtils.OS_VERSION, SystemUtils.OS_ARCH);

        detail.setAttribute(DetailConstants.ATTR_OS, os);
        detail.setAttribute(DetailConstants.ATTR_DEVICE, "unknown");
        detail.setAttribute(DetailConstants.ATTR_VERSION, Version.getFullVersionString());
        detail.setAttribute(DetailConstants.ATTR_PLATFORM, "TAKX-" + productFlavor);

        return detail;
    }

    /**
     * Start broadcasting our SA messages.
     */
    public void start()
    {
        // MOs are reloaded on every start, so grab a fresh reference to the self marker
        selfMarker = mapObjectManager.getSelfMarker();
        lastPosition = selfMarker.getGeoPointMetaData();

        sendLocationMonitor.start();
        reportingStrategyMonitor.start();
        callSignMonitor.start();
        teamMonitor.start();
        roleMonitor.start();
        displayTypeMonitor.start();

        startSenderTask();
    }

    /**
     * Stop broadcasting messages.
     */
    public void stop()
    {
        stopSenderTask();
        sendLocationMonitor.stop();
        reportingStrategyMonitor.stop();
        callSignMonitor.stop();
        teamMonitor.stop();
        roleMonitor.stop();
        displayTypeMonitor.stop();
    }

    /**
     * Start the task that broadcasts our SA messages.
     * @since 3.1
     */
    private void startSenderTask()
    {
        stopSenderTask();

        if (sendLocation)
        {
            situationalAwarenessTimerTask = new SituationalAwarenessTimerTask();
            scheduledTask = executorService.scheduleAtFixedRate(situationalAwarenessTimerTask, DELAY_SEC, DELAY_SEC, TimeUnit.SECONDS);
        }
    }

    /**
     * Stop broadcasting messages.
     * @since 3.1
     */
    private void stopSenderTask()
    {
        if (scheduledTask != null)
        {
            scheduledTask.cancel(true);
            scheduledTask = null;
            situationalAwarenessTimerTask.dispose();
            situationalAwarenessTimerTask = null;
        }
    }

    /**
     * Send location preference changed, start/stop the sender task as indicated.
     * @since 3.1
     */
    private void setSendLocation(boolean sendLocation)
    {
        this.sendLocation = sendLocation;

        if (sendLocation)
        {
            startSenderTask();
        } else
        {
            stopSenderTask();
        }
    }

    /**
     * @param strategy Reporting Strategy preference string
     * @since 3.1
     */
    private void setReportingStrategy(String strategy)
    {
        try
        {
            reportingStrategy = ReportingStrategy.valueOf(strategy);
        } catch (IllegalArgumentException e)
        {
            logger.error("Unknown ReportingStrategy [{}]; defaulting to Constant interval reporting.", strategy);
            reportingStrategy = ReportingStrategy.Constant;
        }

        reportImmediately = true;
    }

    /**
     * Current location has changed, update SA
     */
    private void onLocationChange(@Observes(notifyObserver = IF_EXISTS) LocationChanged event)
    {
        writeLock.lock();

        try
        {
            lastPosition = event.newLocation();

            if (lastPosition != null)
            {
                // If the user updated the location (or changed location provider), send SA immediately
                if (event.userGenerated()) reportImmediately = true;

                final AttributeSet attributeSet = lastPosition.getMetaData();
                if (attributeSet.containsAttribute(StandardAttributeNames.SPEED) || attributeSet.containsAttribute(StandardAttributeNames.BEARING))
                {
                    lastSpeed = attributeSet.getDoubleAttribute(StandardAttributeNames.SPEED, 0);
                    lastBearing = attributeSet.getDoubleAttribute(StandardAttributeNames.BEARING, 0);
                }
            }
        } finally
        {
            writeLock.unlock();
        }
    }

    /**
     * Immediately broadcast our SA message whenever a TAKServer connection is made.
     * @since 5.2 (changed to observe TakServerConnectionOpened in 5.3)
     */
    private void onTakServerConnection(@Observes(notifyObserver = IF_EXISTS) TakServerConnectionOpened ignored)
    {
        reportImmediately = true;
    }

    /**
     * Create the SA message CoT event with our current info (location).
     */
    private CotEvent createMessageEvent(ReportingState reportingState)
    {
        // Get the current location
        final GeoPointMetaData geoPointMetadata = lastPosition;
        final GeoPoint geoPoint = geoPointMetadata != null ? geoPointMetadata.get() : GeoPoint.ZERO_POINT;

        final CotDetail detail = new CotDetail();
        final String how;

        if (geoPointMetadata != null)
        {
            how = geoPointMetadata.getMetaData().getStringAttribute(StandardAttributeNames.HOW,
                    CotConstants.HOW_MACHINE_GENERATED_GARBAGE);

            if (!Double.isNaN(lastSpeed) && !Double.isNaN(lastBearing))
            {
                final CotDetail track = new CotDetail(DetailConstants.TRACK);
                track.setAttribute(DetailConstants.ATTR_SPEED, Double.toString(lastSpeed));
                track.setAttribute(DetailConstants.ATTR_COURSE, Double.toString(lastBearing));
                detail.addChild(track);
            }
        } else
        {
            how = CotConstants.HOW_MACHINE_GENERATED_GARBAGE;
        }

        final CotDetail group = new CotDetail(DetailConstants.GROUP);
        group.setAttribute(DetailConstants.ATTR_NAME, SA_REPORTING_TEAM.getStringValue());

        var attributes = selfMarker.getAttributeSet();
        group.setAttribute(DetailConstants.ATTR_ROLE, attributes.getStringAttribute(StandardAttributeNames.ATAK_ROLE_TYPE, "Team Member"));
        String customRole = attributes.getStringAttribute(StandardAttributeNames.CUSTOM_ROLE, null);
        if (customRole != null) group.setAttribute(DetailConstants.ATTR_EXTENDED_ROLE, customRole);
        String customRoleAbbrv = attributes.getStringAttribute(StandardAttributeNames.CUSTOM_ROLE_ABBREVIATION, null);
        if (customRoleAbbrv != null) group.setAttribute(DetailConstants.ATTR_EXTENDED_ROLE_ABBREVIATION, customRoleAbbrv);
        boolean useTypeIcon = attributes.getBooleanAttribute(StandardAttributeNames.USE_COT_TYPE_ICON, false);
        if (useTypeIcon) group.setAttribute(DetailConstants.ATTR_USE_TYPE_ICON, "true");

        detail.addChild(group);

        final String callSign = CALL_SIGN.getStringValue();
        final CotDetail contact = new CotDetail(DetailConstants.CONTACT);
        contact.setAttribute(DetailConstants.ATTR_CALLSIGN, callSign);

        final String emailAddress = CONTACT_EMAIL.getStringValue();
        if (!StringUtils.isEmpty(emailAddress, true)) contact.setAttribute(EMAIL_ADDRESS, emailAddress);

        try
        {
            contact.setAttribute(DetailConstants.ATTR_ENDPOINT,
                    InetAddress.getLocalHost().getHostAddress() + ":" + DEFAULT_PORT + ":tcp");
        } catch (UnknownHostException e)
        {
            logger.error("Unable to get endpoint address, skipping endpoint attribute of <contact> detail.", e);
        }

        detail.addChild(contact);

        final String urn = PreferenceEntry.CONTACT_URN.getStringValue();
        if (!StringUtils.isEmpty(urn, true)) detail.addChild(new CotDetail(VMF, URN, urn));

        detail.addChild(new CotDetail(DetailConstants.UID, DetailConstants.ATTR_DROID, callSign));

        // Desktop JDK has no way to query battery, so set to 100%
        detail.addChild(new CotDetail(DetailConstants.STATUS, DetailConstants.ATTR_BATTERY, "100"));

        detail.addChild(takvDetail);

        final CoordinatedTime now = new CoordinatedTime(reportingState.lastTime);
        final CoordinatedTime stale = new CoordinatedTime(reportingState.lastStale);

        // Get the CoT type based on the user selected display type
        final String cotType = displayTypeMonitor.getCurrentValue().getCotType();

        return new CotEvent(nodeUuid.toString(), cotType, CotEvent.VERSION_2_0, geoPoint.isValid() ? geoPoint : GeoPoint.ZERO_POINT,
                now, now, stale, how, detail, null, null, null);
    }

    /**
     * Simple timer task to invoke the SA broadcast on a regular basis.
     */
    private final class SituationalAwarenessTimerTask implements Runnable
    {
        private final DynamicReportingState dynamicReliableState;
        private final DynamicReportingState dynamicUnreliableState;

        private final ConstantReportingState constantReliableState;
        private final ConstantReportingState constantUnreliableState;

        private boolean reportNoLocation;
        private final IPreferenceMonitor<Boolean> reportNoLocationMonitor;

        SituationalAwarenessTimerTask()
        {
            reportNoLocation = SA_REPORTING_REPORT_NO_LOCATION.getBooleanValue();
            reportNoLocationMonitor = SA_REPORTING_REPORT_NO_LOCATION.newBooleanMonitor(report -> {
                reportNoLocation = report;
                reportImmediately = true;
            });

            dynamicReliableState = new DynamicReportingState(true,
                    SA_REPORTING_DYNAMIC_INTERVAL_MINIMUM_RELIABLE,
                    SA_REPORTING_DYNAMIC_INTERVAL_MAXIMUM_RELIABLE,
                    SA_REPORTING_DYNAMIC_INTERVAL_STATIONARY_RELIABLE);

            dynamicUnreliableState = new DynamicReportingState(false,
                    SA_REPORTING_DYNAMIC_INTERVAL_MINIMUM_UNRELIABLE,
                    SA_REPORTING_DYNAMIC_INTERVAL_MAXIMUM_UNRELIABLE,
                    SA_REPORTING_DYNAMIC_INTERVAL_STATIONARY_UNRELIABLE);

            constantReliableState = new ConstantReportingState(true, SA_REPORTING_CONSTANT_INTERVAL_RELIABLE);
            constantUnreliableState = new ConstantReportingState(false, SA_REPORTING_CONSTANT_INTERVAL_UNRELIABLE);
        }

        private void dispose()
        {
            reportNoLocationMonitor.dispose();
            dynamicReliableState.dispose();
            dynamicUnreliableState.dispose();
            constantReliableState.dispose();
            constantUnreliableState.dispose();
        }

        /**
         * Update SA at constant or dynamic rate determined by user preferences and speed.
         * <p>
         * In this context 'Unreliable' refers to sending SA via multicast where the packet could be dropped, and
         * 'Reliable' refers to sending SA to the TAK Server, where packet loss should not occur.
         */
        @Override
        public void run()
        {
            Thread.currentThread().setName("SA Sender");

            if (!sendLocation) return; // If the user has disabled SA messages, then exit

            readLock.lock();
            try
            {
                switch (reportingStrategy)
                {
                    case Constant ->
                    {
                        // Report SA at regular intervals specified by preferences
                        updateSA(this::testConstantRate, constantReliableState);
                        updateSA(this::testConstantRate, constantUnreliableState);
                    }
                    case Dynamic ->
                    {
                        // Report SA at a dynamic rate depending on speed
                        updateSA(this::testDynamicRate, dynamicReliableState);
                        updateSA(this::testDynamicRate, dynamicUnreliableState);
                    }
                    default -> logger.error("Unknown ReportingStrategy: {}", reportingStrategy);
                }

                reportImmediately = false;
            } finally
            {
                readLock.unlock();
            }
        }

        /**
         * Send an updated Situational Awareness message if the given predicate indicates it is time to update.
         *
         * @param shouldReport   Predicate to determine if it is time to send an updated SA
         * @param reportingState State holding last reporting time and stale times
         * @param <T>            Inferred reporting state type
         */
        private <T extends ReportingState> void updateSA(Predicate<T> shouldReport, T reportingState)
        {
            if (!shouldReport.test(reportingState)) return;

            if (reportNoLocation || (lastPosition != null && !GeoPoint.ZERO_POINT.equals(lastPosition.get())))
            {
                try
                {
                    final CotEvent event = createMessageEvent(reportingState);
                    selfMarker.setLatestCotEvent(event);
                    commonCommoManager.broadcastCoT(event.toString(), reportingState.sendMethod);
                } catch (Throwable e)
                {
                    logger.error("Failed to send SA message", e);
                }
            }
        }

        /**
         * Determines if the Message Sender should report SA at a constant rate based on the given reporting state.
         *
         * @param reportingState Reporting state to use for determining if we should update SA now
         * @return boolean value indicating whether SA should be reported
         */
        private boolean testConstantRate(ConstantReportingState reportingState)
        {
            final long currentTime = System.currentTimeMillis();
            final boolean shouldReport = reportImmediately
                    || reportingState.isInvalid()
                    || currentTime - reportingState.lastTime >= reportingState.intervalMs
                    || reportingState.lastStale - currentTime <= STALE_BUFFER_MS;

            if (shouldReport)
            {
                reportingState.setTime(currentTime, reportingState.intervalMs);
                return true;
            }

            return false;
        }

        /**
         * Determines if the Message Sender should report SA at a dynamic rate based on the given reporting state.
         *
         * @param reportingState Reporting state to use for determining if we should update SA now
         * @return boolean value indicating whether SA should be reported
         */
        private boolean testDynamicRate(DynamicReportingState reportingState)
        {
            final long currentTime = System.currentTimeMillis();

            if (reportImmediately || reportingState.isInvalid())
            {
                reportingState.setTime(currentTime, reportingState.intervalStationaryMs);
                return true;
            }

            final long rate;

            if (Double.isNaN(lastSpeed) || lastSpeed < MIN_SPEED_MPS)
            {
                rate = reportingState.intervalStationaryMs;
            } else if (lastSpeed >= MAX_SPEED_MPS)
            {
                rate = reportingState.intervalMaximumMs;
            } else
            {
                double scaledSpeed = MIN_SPEED_MPS + (((lastSpeed - MIN_SPEED_MPS) * 100D) / SPEED_RANGE);

                // scale the report rate based on the speed
                rate = (long) (reportingState.intervalMinimumMs - (scaledSpeed * (reportingState.getIntervalRangeMs())) / 100L);
            }

            final long delta = currentTime - reportingState.lastTime;

            if (delta >= rate || (reportingState.lastStale - currentTime) < STALE_BUFFER_MS)
            {
                reportingState.setTime(currentTime, rate);
                return true;
            }
            return false;
        }
    }

    /**
     * Maintains reporting state for dynamic rate reporting.
     */
    private final class DynamicReportingState extends ReportingState
    {
        long intervalMinimumMs;
        long intervalMaximumMs;
        long intervalStationaryMs;

        final IPreferenceMonitor<Long> intervalMinimumMonitor;
        final IPreferenceMonitor<Long> intervalMaximumMonitor;
        final IPreferenceMonitor<Long> intervalStationaryMonitor;

        /**
         * @param reliable               Pass {@code true} for reliable reporting, {@code false} for unreliable
         * @param intervalMinimumPref    Preference for the minimum rate at which we report, in seconds
         * @param intervalMaximumPref    Preference for the maximum rate at which we report, in seconds
         * @param intervalStationaryPref Preference for the rate to use when stationary, in seconds
         */
        DynamicReportingState(boolean reliable, PreferenceEntry intervalMinimumPref, PreferenceEntry intervalMaximumPref, PreferenceEntry intervalStationaryPref)
        {
            super(reliable);

            intervalMinimumMs = TimeUnit.SECONDS.toMillis(intervalMinimumPref.getLongValue());
            intervalMaximumMs = TimeUnit.SECONDS.toMillis(intervalMaximumPref.getLongValue());
            intervalStationaryMs = TimeUnit.SECONDS.toMillis(intervalStationaryPref.getLongValue());

            intervalMinimumMonitor = intervalMinimumPref.newLongMonitor(interval -> {
                intervalMinimumMs = TimeUnit.SECONDS.toMillis(interval);
                reportImmediately = true;
            });

            intervalMaximumMonitor = intervalMaximumPref.newLongMonitor(interval -> {
                intervalMaximumMs = TimeUnit.SECONDS.toMillis(interval);
                reportImmediately = true;
            });

            intervalStationaryMonitor = intervalStationaryPref.newLongMonitor(interval -> {
                intervalStationaryMs = TimeUnit.SECONDS.toMillis(interval);
                reportImmediately = true;
            });
        }

        /**
         * @return Current interval range in millis.
         * @since 3.1
         */
        long getIntervalRangeMs()
        {
            return intervalMinimumMs - intervalMaximumMs;
        }

        void dispose()
        {
            intervalMinimumMonitor.dispose();
            intervalMaximumMonitor.dispose();
            intervalStationaryMonitor.dispose();
        }
    }

    /**
     * Maintains reporting state for constant rate reporting.
     */
    private final class ConstantReportingState extends ReportingState
    {
        long intervalMs;
        final IPreferenceMonitor<Long> intervalMonitor;

        /**
         * @param reliable     Pass {@code true} for reliable reporting, {@code false} for unreliable
         * @param intervalPref Reporting rate
         */
        public ConstantReportingState(boolean reliable, PreferenceEntry intervalPref)
        {
            super(reliable);

            intervalMs = TimeUnit.SECONDS.toMillis(intervalPref.getLongValue());

            intervalMonitor = intervalPref.newLongMonitor(interval -> {
                intervalMs = TimeUnit.SECONDS.toMillis(interval);
                reportImmediately = true;
            });
        }

        private void dispose()
        {
            intervalMonitor.dispose();
        }
    }

    /**
     * Maintains common reporting state.
     */
    private static abstract class ReportingState
    {
        final CoTSendMethod sendMethod;
        final long staleRateMultiplier;

        long lastTime = -1;
        long lastStale = -1;

        /**
         * @param reliable Pass {@code true} for reliable reporting, {@code false} for unreliable
         */
        protected ReportingState(boolean reliable)
        {
            sendMethod = reliable ? CoTSendMethod.TAK_SERVER : CoTSendMethod.POINT_TO_POINT;
            staleRateMultiplier = reliable ? RELIABLE_RATE_MULTIPLIER : UNRELIABLE_RATE_MULTIPLIER;
        }

        /**
         * @return {@code true} if we have no recorded time or stale time
         */
        boolean isInvalid()
        {
            return lastTime == -1 || lastStale == -1;
        }

        /**
         * Set the reporting time and update the stale time based on our configuration.
         *
         * @param currentTime New time to install
         * @param rate        Reporting rate, used to calculate new stale time
         */
        void setTime(long currentTime, long rate)
        {
            lastTime = currentTime;
            lastStale = currentTime + rate * staleRateMultiplier + STALE_PADDING_MS;
        }
    }
}
