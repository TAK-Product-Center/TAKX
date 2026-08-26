package gov.tak.api.comms.takserver.mission.data;

import java.util.List;

// TODO TAKX-1832: Document what each parameter is for specifically
//  - Only have one sample of a map object:
//      {type: a-n-A, callsign: U.11.193813, iconsetPath: COT_MAPPING_2525C/a-u/a-u-G }

/**
 * Additional details that may be linked to a {@link MissionAdd} pulled from CoT events.
 * Represents a map marker.
 *
 * @param name
 * @param title
 * @param type CoT type.
 * @param category
 * @param color String representation of an ARGB integer. Defaults to {@code "-1"} for white.
 * @param location Location of the represented marker.
 * @param callsign Display name.
 * @param iconsetPath Path to custom display icon. See {@link gov.tak.api.util.StandardAttributeNames#ICONSET_PATH} and {@link gov.tak.platform.cot.detail.UserIconDetailHandler}.
 * @param attachments List of SHA256 hashes of attached files.
 *
 * @since 3.5
 */
public record UidDetails(String name,
        String title,
        String type,
        String category,
        String color,
        Location location,
        String callsign,
        String iconsetPath,
        List<String> attachments)
{
}
