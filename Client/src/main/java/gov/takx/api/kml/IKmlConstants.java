package gov.takx.api.kml;

/**
 * Misc. KML constants used for consistency in the KML map object importer and exporter.
 */
public interface IKmlConstants
{
    String UNKNOWN_FORMAT = "UNKNOWN";

    String RDM_ROOT_ELEMENT = "RaptorDataMessage";
    String MO_ROOT_ELEMENT = "MapObject";

    String MESSAGES = "Messages";

    // Core RDM features, minus time and position
    String FAMILY = "Family";
    String TYPE = "Type";
    String UNITID = "UnitId";
    String NAME = "Name";
    String SPEED = "Speed";
    String HEADING = "Heading";
    String MOTION = "Motion";
    String PITCH = "Pitch";
    String ROLL = "Roll";

    String KML_MESSAGE_TYPE = "KML Track Point";
    String KML_MESSAGE_VERSION = "1.0.0";
}
