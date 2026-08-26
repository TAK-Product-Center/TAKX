package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.ParameterInfo;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.data.MissionChange;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Endpoint caller to get the changelog of a {@link Mission} over a period of time.
 *
 * @since 3.5
 */
public class GetMissionChanges extends AbstractEndpointCaller<List<MissionChange>>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(
            paramOptional("squashed", true),
            paramOptional("secago", ""),
            paramOptional("start", ""),
            paramOptional("end", ""));

    /**
     * @param api API reference.
     * @param missionName Name of the mission to update.
     */
    public GetMissionChanges(@Nonnull FeignMissionApi api, @Nonnull String missionName)
    {
        super((headers, params) -> api.getMissionChanges(headers, params, missionName), PARAM_MAP);
    }

    /**
     * Enables or disabled squashing the changelog entries.
     *
     * @param squashed {@code true} to condense changelog entries.
     *
     * @return Self.
     */
    @Nonnull
    public GetMissionChanges withSquashed(boolean squashed)
    {
        return setParam("squashed", squashed);
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
    public GetMissionChanges withStart(@Nonnull Instant start)
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
    public GetMissionChanges withEnd(@Nonnull Instant end)
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
    public GetMissionChanges withSecondsAgo(long secago)
    {
        return setParam("secago", secago);
    }
}
