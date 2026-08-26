package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.ParameterInfo;
import gov.tak.api.comms.takserver.mission.data.LogEntry;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Endpoint caller to get multiple {@link LogEntry} instances belonging to a {@link Mission}.
 *
 * @since 3.5
 */
public class GetLogEntries extends AbstractEndpointCaller<List<LogEntry>>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(
            paramOptional("secago", ""),
            paramOptional("start", ""),
            paramOptional("end", ""));

    /**
     * @param api API reference.
     * @param missionName Name of the mission to get the log entries of.
     */
    public GetLogEntries(@Nonnull FeignMissionApi api, @Nonnull String missionName)
    {
        super((headers, params) -> api.getLogEntries(headers, params, missionName), PARAM_MAP);
    }

    /**
     * Sets the start time cut-off for entries.
     * <br>
     * If specified, takes precedence over {@link #withSecondsAgo(long)}.
     *
     * @param start Start time cut-off for entries.
     *
     * @return Self.
     */
    @Nonnull
    public GetLogEntries withStart(@Nonnull Instant start)
    {
        return setParam("start", start);
    }

    /**
     * Sets the end time cut-off for entries.
     * <br>
     * If specified, takes precedence over {@link #withSecondsAgo(long)}.
     *
     * @param end End time cut-off for entries.
     *
     * @return Self.
     */
    @Nonnull
    public GetLogEntries withEnd(@Nonnull Instant end)
    {
        return setParam("end", end);
    }

    /**
     * Sets the start time cut-off for entries.
     * <br>
     * Used as an alternative to specifying a {@code start} - {@code end} range.
     *
     * @param secago Seconds ago to use as a start cut-off time for entries.
     *
     * @return Self.
     */
    @Nonnull
    public GetLogEntries withSecondsAgo(long secago)
    {
        return setParam("secago", secago);
    }
}
