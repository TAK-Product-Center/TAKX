package gov.tak.api.comms.takserver.mission.data;

import java.util.List;

/**
 * @see <a href="https://github.com/TAK-Product-Center/Server/blob/main/src/takserver-core/takserver-war/src/main/java/com/bbn/marti/video/VideoCollections.java">VideoCollections.java</a>
 *
 * @since 5.0
 */
public record VideoConnections(List<VideoConnection> videoConnections)
{
}
