package gov.tak.api.comms.takserver.mission.data;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import gov.tak.api.comms.takserver.mission.MissionContentKeysSerializer;
import gov.takx.api.mapobject.IMapObject;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * Outlines content to attach to a {@link Mission}.
 *
 * @param uids The list of {@link Resource#uid() resource uids} or {@link IMapObject#getUid() map item uids} to reference. List value is taken as-is and is not copied.
 * @param hashes The list of {@link Resource#hash() resource sha256 hashes} to reference. List value is taken as-is and is not copied.
 *
 * @see gov.tak.api.comms.takserver.mission.MissionEndpoints#addMissionContent(String, MissionContentKeys)
 * @since 3.5
 */
@JsonSerialize(using = MissionContentKeysSerializer.class)
public record MissionContentKeys(@Nullable List<String> uids, @Nullable List<String> hashes)
{
    /**
     * @return {@code true} when there are no uids or hashes provided.
     */
    public boolean isEmpty()
    {
        return (uids == null || uids.isEmpty()) && (hashes == null || hashes.isEmpty());
    }
}
