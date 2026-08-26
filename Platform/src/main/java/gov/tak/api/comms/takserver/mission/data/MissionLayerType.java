package gov.tak.api.comms.takserver.mission.data;

/**
 * The type of layer for a {@link MissionLayer}.
 *
 * @since 3.5
 */
public enum MissionLayerType
{
    /**
     * The layer wraps a collection of other layers,
     * see: {@link MissionLayer#parentUid()} and {@link MissionLayer#children()}.
     */
    GROUP,
    /**
     * The layer defines {@link MissionLayer#uids()}.
     */
    UID,
    /**
     * The layer defines {@link MissionLayer#contents()}.
     */
    CONTENTS,
    /**
     * The layer defines {@link MissionLayer#maplayers() sub-layers}.
     */
    MAPLAYER
}
