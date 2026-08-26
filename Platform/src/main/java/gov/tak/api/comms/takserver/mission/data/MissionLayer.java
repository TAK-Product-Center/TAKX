package gov.tak.api.comms.takserver.mission.data;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * @param uid Unique id of the layer.
 * @param name Name of the layer.
 * @param parentUid Parent id of another {@link MissionLayer}.
 * @param after Uid of another {@link MissionLayer} that this occurs after.
 * @param type The layer type, determining which kind of content is expected.
 * @param children Child mission layers.
 * @param uids List of map item uids associated with the layer.
 * @param contents List of {@link Resource resources} associated with the layer.
 * @param maplayers List of {@link MapLayer map layers} associated with the layer.
 *
 * @since 3.5
 */
public record MissionLayer(String uid,
        String name,
        String parentUid,
        String after,
        MissionLayerType type,
        @JsonProperty("mission_layers") List<MissionLayer> children,
        List<MissionAdd.TString> uids,
        List<MissionAdd.TResource> contents,
        List<MissionAdd.TMapLayer> maplayers)
{
}
