package gov.takx.core.dataportal;

import gov.takx.api.messages.IMapEntity;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.messages.IRaptorMessage;
import gov.takx.api.messages.IStructure;
import gov.takx.api.messages.NoSuchStructureException;
import gov.takx.api.persist.IMapObjectPersist;
import gov.takx.api.registry.IDataStructureRegistry;
import gov.takx.dataservices.persist.RaptorDataStructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Collections;
import java.util.Set;

/**
 * Implementation of IDataStructureRegistry.
 *
 */
@ApplicationScoped
public class DataStructureRegistry implements IDataStructureRegistry
{
    private final IMapObjectPersist moPersist;

    @Inject
    DataStructureRegistry(IMapObjectPersist moPersist)
    {
        this.moPersist = moPersist;
    }

    @Override
    public Set<IRaptorDataStructure> getDataStructuresForMapObject(String family, String type)
    {
        final IMapEntity mapEntity = moPersist.findMapEntity(family, type);
        return mapEntity == null
                ? Collections.emptySet()
                : mapEntity.getAllRaptorDataStructures();
    }

    @Override
    public IRaptorDataStructure getDataStructure(String family, String type, String structureHash) throws NoSuchStructureException
    {
        final IStructure structure = moPersist.findStructureByHash(structureHash);

        if (structure == null) throw new NoSuchStructureException(structureHash);

        return new RaptorDataStructure(family, type, structure);
    }

    @Override
    public IRaptorDataStructure getDataStructure(IRaptorMessage message)
    {
        return message.getMapObject().getRaptorDataStructure(message.getMessageType());
    }
}
