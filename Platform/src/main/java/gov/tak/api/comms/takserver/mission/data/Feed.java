package gov.tak.api.comms.takserver.mission.data;

/**
 * @see  <a href="https://github.com/TAK-Product-Center/Server/blob/main/src/takserver-core/takserver-war/src/main/java/com/bbn/marti/video/FeedV2.java">FeedV2.java</a>
 *
 * @since 5.0
 */
public record Feed(String uuid, boolean active, String alias, String url, int order, String macAddress, String roverPort,
        String ignoreEmbeddedKLV, String source, String networkTimeout, String bufferTime, String rtspReliable,
        String thumbnail, String classification, String latitude, String longitude, String fov, String heading,
        String range, Integer width, Integer height, Integer bitrate)
{
}
