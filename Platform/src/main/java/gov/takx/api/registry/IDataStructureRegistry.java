package gov.takx.api.registry;

import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.messages.IRaptorMessage;
import gov.takx.api.messages.NoSuchStructureException;

import java.util.Set;

/**
 * Provides a centralized repository of {@link IRaptorDataStructure}s.
 *
 */
@InjectableService
@NoPluginImplementations
public interface IDataStructureRegistry
{
    /**
     * Get a data structure by map object type, version, and message type.
     *
     * @param family        The family of map object that owns the data structure.
     * @param type          The type of the map object that owns the data structure.
     * @param structureHash Hash value that uniquely identifies the structure.
     * @return The matching data structure.
     * @throws NoSuchStructureException if structure is not found.
     */
    IRaptorDataStructure getDataStructure(String family, String type, String structureHash) throws NoSuchStructureException;

    /**
     * Get a data structure by RDM.
     *
     * @param message The RDM that owns the data structure.
     * @return The matching data structure.
     */
    IRaptorDataStructure getDataStructure(IRaptorMessage message);

    /**
     * Get all the possible data structures for a map object (given its family and type).
     *
     * @param family The family of the Map Object of interest
     * @param type   The type of the Map Object of interest
     * @return complete list of structures
     * @since 2.11
     */
    Set<IRaptorDataStructure> getDataStructuresForMapObject(String family, String type);
}
