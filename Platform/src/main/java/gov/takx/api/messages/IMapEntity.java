package gov.takx.api.messages;

import gov.takx.api.annotation.Internal;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.persist.IPersistable;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

/**
 */
@NoPluginImplementations
public interface IMapEntity extends Serializable, IPersistable
{
    /**
     * Get the unique key for the Map Entity.
     *
     * @return Unique key consisting of family and type
     */
    String getUniqueKey();

    String getFamily();

    String getType();

    @Internal
    boolean addStructure(IStructure struct);

    @Internal
    boolean addStructures(Collection<IStructure> structs);

    /**
     * @return Collection of ALL structures, including current and older versions
     */
    Set<IStructure> getDataStructures();

    /**
     * @return Collection of {@link IRaptorDataStructure}s for current (the newest version) structures.
     */
    Set<IRaptorDataStructure> getRaptorDataStructures();

    /**
     * @return Collection of {@link IRaptorDataStructure}s for ALL structures, including current and versioned structures.
     * @since 3.0
     */
    Set<IRaptorDataStructure> getAllRaptorDataStructures();

    /**
     * Get the {@link IRaptorDataStructure} for the {@link IStructure} with the given message type.
     *
     * @param messageType Message type of interest
     * @return RaptorDataStructure or {@code null} if not found
     */
    IRaptorDataStructure getRaptorDataStructure(String messageType);

    /**
     * Determine if this MapEntity has any associated structure with the given message type.
     *
     * @param messageType Message type of interest
     * @return {@code true} if a struct with the given message type is associated with this MapEntity
     * @since 2.30
     */
    boolean hasMessageType(String messageType);

    /**
     * Get a structure by name and version.
     *
     * @param name    Structure name
     * @param version Structure version
     * @return {@link IRaptorDataStructure} or {@code null} if not found
     */
    IRaptorDataStructure getRaptorDataStructure(String name, String version);
}
