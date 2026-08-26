package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.ParameterInfo;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.Map;

/**
 * Endpoint caller to fetch a {@link Mission} instance by name.
 *
 * @see GetAllMissions Fetch all missions <i>(As if using the default values provided here, so no change-log/log-entries)</i>
 * @since 3.5
 */
public class GetMission extends AbstractEndpointCaller<Mission>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(
            paramOptional("password", ""),
            paramOptional("changes", false),
            paramOptional("logs", false),
            paramOptional("secago", 0L),
            paramOptional("start", Instant.EPOCH),
            paramOptional("end", Instant.MAX));

    /**
     * @param api API reference.
     * @param missionName Name of the mission to fetch.
     */
    public GetMission(@Nonnull FeignMissionApi api, @Nonnull String missionName)
    {
        super(unpack((headers, params) -> api.getMission(headers, params, missionName)), PARAM_MAP);
    }

    /**
     * Enables access to a password protected mission.
     *
     * @param password The password to access the mission, if it is protected by one.
     *
     * @return Self.
     */
    @Nonnull
    public GetMission withPassword(@Nullable String password)
    {
        return setParam("password", password);
    }

    /**
     * Populates the contents of the {@link Mission#missionChanges() change log} in the result value.
     * By default, changes are not included.
     *
     * @param changes Set to {@code true} to populate {@link Mission#missionChanges()} in returned value.
     *
     * @return Self.
     */
    @Nonnull
    public GetMission withChanges(boolean changes)
    {
        return setParam("changes", changes);
    }

    /**
     * Populates the contents of the {@link Mission#logs() logs} in the result value.
     * By default, logs are not included.
     *
     * @param logs Set to {@code true} to populate {@link Mission#logs()} in returned value.
     *
     * @return Self.
     */
    @Nonnull
    public GetMission withLogs(boolean logs)
    {
        return setParam("logs", logs);
    }

    /**
     * Limits the contents of the {@link Mission#missionChanges() change log} to fit within the given time frame.
     *
     * @param secago Used as an alternative to specifying a {@code start} - {@code end} range
     * with {@link #withTimeRange(Instant, Instant)}.
     *
     * @return Self.
     */
    @Nonnull
    public GetMission withSecondsAgo(long secago)
    {
        return setParam("secago", secago);
    }

    /**
     * Limits the contents of the {@link Mission#missionChanges() change log} to fit within the given time frame.
     * <br>
     * If specified, takes precedence over {@code secago}
     *
     * @param start Start time cut-off for entries of {@link Mission#missionChanges()} and {@link Mission#logs()}.
     * @param end End time cut-off for entries of {@link Mission#missionChanges()} and {@link Mission#logs()}.
     *
     * @return Self.
     */
    @Nonnull
    public GetMission withTimeRange(@Nonnull Instant start, @Nonnull Instant end)
    {
        if (start.isAfter(end)) throw new IllegalArgumentException("Start time must be before end time");
        setParam("start", start);
        return setParam("end", end);
    }
}
