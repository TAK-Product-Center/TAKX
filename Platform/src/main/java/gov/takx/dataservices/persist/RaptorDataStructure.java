package gov.takx.dataservices.persist;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.messages.IDataField;
import gov.takx.api.messages.IDataStructure;
import gov.takx.api.messages.IMapEntity;
import gov.takx.api.messages.IPrePersistRaptorCommandMessage;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IPrePersistRaptorMessage;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.messages.IStructure;
import gov.takx.api.persist.IMapObjectPersist;
import gov.takx.platform.KeyUtils;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Each {@link IRaptorDataStructure} (RDS) is an association between a map object family and type (usually defined by a
 * plugin), and an underlying core {@link IStructure} which defines the custom fields.
 * <p>
 * RDS instances serve as a factory for creating instances of an {@link IRaptorDataMessage} (RDM), defined by the
 * underlying structure.
 *
 * @see IStructure
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class RaptorDataStructure
        implements Serializable, Comparable<RaptorDataStructure>, IRaptorDataStructure
{
    @Inject
    private IMapObjectPersist mapObjectPersist;

    private volatile int hashCode = 0;

    private final String family;
    private final String type;
    private final IStructure structure;

    /**
     * Convert a collection of IRaptorDataStructure's to a List of IStructure's.
     *
     * @param rdsCollection Collection to convert
     * @return List of Structures for the input RDS collection
     */
    public static List<IStructure> convertToStructureCollection(Collection<IRaptorDataStructure> rdsCollection)
    {
        return rdsCollection.stream()
                .map(IDataStructure::getStructure)
                .collect(Collectors.toList());
    }

    /**
     * Construct with the given family/type and structure.
     *
     * @param family    Map Object family
     * @param type      Map Object type
     * @param structure Underlying structure
     */
    public RaptorDataStructure(String family, String type, IStructure structure)
    {
        this.family = family;
        this.type = type;
        this.structure = structure;
    }

    @Override
    public IPrePersistRaptorDataMessage createRaptorDataMessage(@Nonnull IMapObject mapObject)
    {
        return applyDefaultValues(new RaptorDataMessage(structure, mapObject));
    }

    @Override
    public IPrePersistRaptorDataMessage createRaptorDataMessage(String unitId)
    {
        return internalCreateRdm(family, type, unitId);
    }

    @Override
    public IPrePersistRaptorDataMessage createRaptorDataMessage(String type, String unitId)
    {
        return internalCreateRdm(family, type, unitId);
    }

    @Override
    public IPrePersistRaptorDataMessage createRaptorDataMessage(@Nonnull String family, String type, String unitId)
    {
        return internalCreateRdm(family, type, unitId);
    }

    private IPrePersistRaptorDataMessage internalCreateRdm(String family, String type, String unitId)
    {
        final IMapObject mapObject = mapObjectPersist.findMapObject(KeyUtils.getUniqueKey(family, type, unitId));

        return mapObject != null
                ? applyDefaultValues(new RaptorDataMessage(structure, mapObject))
                : applyDefaultValues(new RaptorDataMessage(structure, family, type, unitId));
    }

    @Override
    public IPrePersistRaptorCommandMessage createRaptorCommandMessage(String unitId)
    {
        final IMapObject mapObject = mapObjectPersist.findMapObject(KeyUtils.getUniqueKey(family, type, unitId));

        return mapObject != null
                ? applyDefaultValues(new RaptorCommandMessage(structure, mapObject))
                : applyDefaultValues(new RaptorCommandMessage(structure, family, type, unitId));
    }

    /**
     * Apply all default values specified in this structure to the message instance.
     *
     * @param message The message (RDM or RCM) to process
     * @param <T>     Type of message
     * @return message
     */
    private <T extends IPrePersistRaptorMessage> T applyDefaultValues(T message)
    {
        structure.getFieldsWithDefaultValues()
                .forEach(field -> message.setField(field.getFieldName(), field.getDefaultValue()));

        return message;
    }

    @Override
    public String getMapObjectFamily()
    {
        return family;
    }

    @Override
    public String getMapObjectType()
    {
        return type;
    }

    @Override
    public String toString()
    {
        return family + "/" + type + "/" + structure;
    }

    @Override
    public int hashCode()
    {
        if (0 == hashCode)
        {
            int result = family.hashCode();
            result = 31 * result + type.hashCode();
            result = 31 * result + structure.hashCode();

            hashCode = result;
        }

        return hashCode;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RaptorDataStructure rds = (RaptorDataStructure) o;

        return family.equals(rds.family)
                && type.equals(rds.type)
                && structure.equals(rds.structure);
    }

    /**
     * Comparison is delegated to the underlying structure.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public int compareTo(RaptorDataStructure o)
    {
        return structure.compareTo(o.getStructure());
    }

    @Override
    public IMapEntity getMapEntity()
    {
        return mapObjectPersist.findMapEntity(family, type);
    }

    @Override
    public Collection<IDataField> getFields()
    {
        return structure.getFields();
    }

    @Override
    public IDataField getField(String name)
    {
        return structure.getField(name);
    }

    @Override
    public String getMessageType()
    {
        return structure.getMessageType();
    }

    @Override
    public String getVersion()
    {
        return structure.getVersion();
    }

    @Override
    public IStructure getStructure()
    {
        return structure;
    }

    @Override
    public Integer getId()
    {
        return structure.getId();
    }
}
