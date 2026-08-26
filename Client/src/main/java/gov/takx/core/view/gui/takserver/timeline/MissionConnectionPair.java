package gov.takx.core.view.gui.takserver.timeline;

import gov.tak.api.comms.takserver.ITakServerConnection;
import jakarta.annotation.Nonnull;

/**
 * Wrapper for a mission and its defining connection.
 *
 * @param connection Connection where the mission is defined on.
 * @param mission Name of the mission.
 *
 * @since 5.0
 */
public record MissionConnectionPair(@Nonnull ITakServerConnection connection, @Nonnull String mission)
{
    @Override
    public String toString()
    {
        return mission;
    }

    /**
     * @param otherMission Other mission name.
     * @return Name comparison, ignoring case.
     */
    public int compareToIgnoreCase(@Nonnull String otherMission)
    {
        return mission.compareToIgnoreCase(otherMission);
    }
}
