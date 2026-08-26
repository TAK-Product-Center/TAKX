package gov.takx.api.persist;

import gov.takx.api.annotation.Internal;
import gov.takx.api.annotation.NoPluginImplementations;

/**
 * Contract for the StructureAssociationManager.  Internal use only.
 *
 * @since 2.29
 */
@Internal
@NoPluginImplementations
public interface IStructureAssociationManager
{
    /**
     * Add an association between the given MapEntity and Structure to the association table if one doesn't
     * already exist.
     *
     * @param mapEntityId Id of MapEntity
     * @param structureId Id of Structure
     */
    void addAssociation(int mapEntityId, int structureId);
}
