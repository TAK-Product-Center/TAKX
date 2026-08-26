package gov.takx.api.event.mapobject;

import gov.takx.api.messages.IMapEntity;

/**
 * Event indicating that the list of structures on a MapEntity have been updated.  This is primarily used so that
 * query services and device logs can update as needed with the new structures.
 *
 * @since 3.4
 */
public record MapEntityStructuresUpdated(IMapEntity mapEntity)
{
}
