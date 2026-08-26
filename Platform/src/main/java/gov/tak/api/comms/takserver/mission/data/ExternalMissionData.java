package gov.tak.api.comms.takserver.mission.data;

/**
 * @param uid Unique identifier of the data source.
 * @param name Name of the data source.
 * @param tool Name of the tool that created the mission.
 * By default, if no value is specified {@code public} is used. Special cases may change this value.
 * @param urlData URL to external data source.
 * @param urlView URL to external visualization of the external data source.
 * @param notes Associated notes for the data.
 *
 * @since 3.5
 */
public record ExternalMissionData(String uid, String name, String tool, String urlData, String urlView, String notes)
{
}
