package gov.takx.core.view.gui.takserver.timeline;

import gov.tak.api.comms.takserver.mission.data.LogEntry;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.data.MissionChange;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

/**
 * Wrapper for a changelog entry or a user log, since the log UI should display both.
 *
 * @param missionChange Changelog entry. Will be {@code null} if {@link #logEntry} is present.
 * @param logEntry      User log entry. Will be {@code null} if {@link #missionChange} is present.
 *
 * @since 5.0
 */
public record MissionChangeOrLogEntry(@Nullable MissionChange missionChange, @Nullable LogEntry logEntry) implements Comparable<MissionChangeOrLogEntry>
{
    /**
     * @param mission Mission to pull change/user logs from.
     * @return Combined list of change/user log entries sorted by timestamp.
     */
    @Nonnull
    public static List<MissionChangeOrLogEntry> listFromMission(@Nonnull Mission mission)
    {
        Stream<MissionChangeOrLogEntry> changes = mission.missionChanges().stream()
                .map(c -> new MissionChangeOrLogEntry(c, null));
        Stream<MissionChangeOrLogEntry> logs = mission.logs().stream()
                .map(l -> new MissionChangeOrLogEntry(null, l));
        return Stream.concat(changes, logs)
                .sorted()
                .toList();
    }

    /**
     * @return Timestamp of the entry.
     */
    @Nonnull
    public Instant timestamp()
    {
        return missionChange != null ? missionChange.timestamp() : logEntry != null ? logEntry.dtg() : Instant.now();
    }

    @Override
    public int compareTo(MissionChangeOrLogEntry o)
    {
        return timestamp().compareTo(o.timestamp());
    }
}
