package gov.tak.api.comms.takserver.mission.data;

import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.List;

/**
 * Base outline of changes adding content to a mission.
 *
 * @param <T> Content of addition.
 *
 * @since 3.5
 */
@NoPluginImplementations
public interface MissionAdd<T>
{
    /**
     * @return Data added.
     */
    T data();

    /**
     * @return Timestamp of addition.
     */
    Instant timestamp();

    /**
     * @return The {@link ClientEndpoint#uid()} of the user responsible for the addition.
     */
    String creatorUid();

    // TODO TAKX-1832: Document path/after
    //  - likely 'path' is used as the identifier and 'after' handles ordering of items based on 'path' values

    @Nullable
    String path();

    @Nullable
    String after();

    /**
     * @return Keywords associated with the added item.
     */
    @Nullable
    List<String> keywords();

    /**
     * @return Additional details for map item addition and modifications.
     */
    @Nullable
    UidDetails details();

    record TString(String data, Instant timestamp, String creatorUid, String path, String after,
            List<String> keywords, UidDetails details) implements MissionAdd<String>
    {
    }

    record TResource(Resource data, Instant timestamp, String creatorUid, String path, String after,
            List<String> keywords, UidDetails details) implements MissionAdd<Resource>
    {
    }

    record TMapLayer(MapLayer data, Instant timestamp, String creatorUid, String path, String after,
            List<String> keywords, UidDetails details) implements MissionAdd<MapLayer>
    {
    }
}
