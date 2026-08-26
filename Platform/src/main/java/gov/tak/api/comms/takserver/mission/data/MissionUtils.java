package gov.tak.api.comms.takserver.mission.data;

import jakarta.annotation.Nonnull;
import org.apache.commons.lang3.ObjectUtils;

import java.time.Instant;
import java.util.List;

/**
 * Utility class that performs common {@link Mission} operations.
 *
 * @since 5.0
 */
public class MissionUtils
{
    /**
     * Returns the mission's last update time. If {@link MissionChange}s exist, the max change timestamp will be
     * returned. Otherwise, the mission creation time will be returned.
     *
     * @param mission The mission
     * @return The mission's last update time
     */
    public static Instant getLastUpdateTime(@Nonnull Mission mission)
    {
        final List<MissionChange> missionChanges = mission.missionChanges();
        final Instant missionCreationTime = ObjectUtils.defaultIfNull(mission.createTime(), Instant.EPOCH);

        return missionChanges.isEmpty()
                ? missionCreationTime
                // Handle edge case where faulty system clocks result in timestamps older than the mission creation time
                : max(missionChanges.get(missionChanges.size() - 1).timestamp(), missionCreationTime);
    }

    /**
     * @param instant1 The first {@code Instant} to compare 
     * @param instant2 The second {@code Instant} to compare
     * @return The max {@code Instant}
     */
    private static Instant max(@Nonnull Instant instant1, @Nonnull Instant instant2)
    {
        return instant1.compareTo(instant2) > 0 ? instant1 : instant2;
    }

    private MissionUtils()
    {
    }
}
