package gov.tak.api.comms.takserver.mission.data;

import java.time.Instant;

// TODO: TAKX-1832: Document what each property is used for.
//  - Models MOBAC format https://mobac.sourceforge.io/wiki/index.php/Custom_XML_Map_Sources
//     - Some attributes seem like they are hierarchical, but this type is flat?

/**
 * @param uid
 * @param name Name of the layer.
 * @param description Description of the layer.
 * @param creatorUid The {@link ClientEndpoint#uid()} of the user who created the layer.
 * @param path
 * @param after
 * @param createTime The timestamp of the layer's creation.
 * @param modifiedTime The timestamp of the layer's last modification.
 * @param defaultLayer
 * @param enabled
 * @param opacity
 * @param minZoom
 * @param maxZoom
 * @param north
 * @param south
 * @param east
 * @param west
 * @param type
 * @param url
 * @param tileType
 * @param serverParts
 * @param backgroundColor
 * @param tileUpdate
 * @param additionalParameters
 * @param coordinateSystem
 * @param version
 * @param layers
 * @param ignoreErrors
 * @param invertYCoordinate
 *
 * @since 3.5
 */
public record MapLayer(String uid,
        String name,
        String description,
        String creatorUid,
        String path,
        String after,
        Instant createTime,
        Instant modifiedTime,
        boolean defaultLayer,
        boolean enabled,
        int opacity,
        int minZoom, int maxZoom,
        double north, double south, double east, double west,
        String type,
        String url,
        String tileType,
        String serverParts,
        String backgroundColor,
        String tileUpdate,
        String additionalParameters,
        String coordinateSystem,
        String version,
        String layers,
        boolean ignoreErrors,
        boolean invertYCoordinate)
{
}