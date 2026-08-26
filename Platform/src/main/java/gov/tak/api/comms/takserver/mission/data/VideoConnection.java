package gov.tak.api.comms.takserver.mission.data;

import java.util.List;

/**
 * <a href="https://github.com/TAK-Product-Center/Server/blob/main/src/takserver-core/takserver-war/src/main/java/com/bbn/marti/video/VideoConnection.java">VideoConnection.java</a>
 *
 * @since 5.0
 */
public record VideoConnection(String uuid, boolean active, String alias, String thumbnail, String classification, List<Feed> feeds)
{
}
