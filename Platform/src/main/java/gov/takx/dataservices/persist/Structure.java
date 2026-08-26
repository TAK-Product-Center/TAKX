package gov.takx.dataservices.persist;

import gov.takx.api.annotation.Internal;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.messages.DataStructureBuilder;
import gov.takx.api.messages.IDataField;
import gov.takx.api.messages.IExtendedDataStructure;
import gov.takx.api.messages.IIndexDefinition;
import gov.takx.api.messages.IMapEntity;
import gov.takx.api.messages.IStructure;
import gov.takx.api.persist.IStructureAssociationManager;
import gov.takx.api.plugin.IMetadata;
import gov.takx.dataservices.utils.DynamicStructures;
import jakarta.inject.Inject;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MapKey;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.NaturalId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Core structure definition consisting of 0 - N strongly types data fields, optional index definitions, and metadata.
 *
 * @see DataField
 * @see DataStructureBuilder
 */
@SuppressWarnings("SameParameterValue")
@Internal
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"hashValue"}))
@PseudoBean
public class Structure extends AEntityWithMetadata implements IStructure
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String STRUCTURE_CLASS_PREFIX = "S";

    // Comparator, case-insensitive on messageType and then version
    private static final Comparator<IStructure> STRUCTURE_COMPARATOR =
            Comparator.comparing(IStructure::getMessageType, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(IStructure::getVersion, String.CASE_INSENSITIVE_ORDER);

    @Inject
    @Transient
    private IStructureAssociationManager structureAssociationManager;

    @Inject
    @Transient
    private DynamicStructures dynamicStructures;

    @SuppressWarnings("unused") // Set by hibernate
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String messageType;
    private String version;
    private boolean isRenderable;

    @NaturalId
    private String hashValue;

    // We always want to pull in the data field definitions for an RDS to avoid potential lazy load issues
    @OneToMany(mappedBy = "structure", cascade = CascadeType.ALL, fetch = FetchType.EAGER, targetEntity = DataField.class)
    @MapKey(name = "fieldName")
    @Immutable
    private final Map<String, DataField> dataMap = new HashMap<>();

    @Transient
    private final List<IMapEntity> definitions = new ArrayList<>();

    @OneToMany(mappedBy = "structure", cascade = CascadeType.ALL, targetEntity = IndexDefinition.class, fetch = FetchType.EAGER)
    @Immutable
    private final List<IndexDefinition> indexes = new ArrayList<>();

    private int calculatedHashCode = 0;

    @Transient
    private Class<? extends IExtendedDataStructure> extendedDataClass;

    @Transient
    private Constructor<? extends IExtendedDataStructure> extendedDataConstructor;

    @Transient
    private final List<IDataField> fieldsWithDefaultValues = new ArrayList<>();

    // This is used to protect access to the fieldsWithDefaultValues list while still avoiding the allocation
    // of a new object on every call to getFieldsWithDefaultValues().
    @Transient
    private final List<IDataField> unmodifiableFieldsWithDefaultValues = Collections.unmodifiableList(fieldsWithDefaultValues);

    /**
     * Construct a Structure from the given builder.
     *
     * @param builder   Builder from which to construct the structure
     * @param hashValue Hash value of Structure
     * @return new Structure
     * @since 3.0
     */
    public static Structure createFromBuilder(DataStructureBuilder builder, String hashValue)
    {
        final Structure structure = new Structure(hashValue, builder.getMessageType(), builder.getVersion(), builder.isRenderable());

        builder.getStructureMetadata().forEach(structure::setMetadataValue);
        builder.getIndexes().forEach(structure::addIndex);
        builder.getDataFields().forEach(structure::addField);

        return structure; // Return the newly created structure
    }

    /**
     * No-arg constructor for Hibernate.
     */
    protected Structure()
    {
    }

    private Structure(String hashValue, String messageType, String version, boolean isRenderable)
    {
        this.hashValue = hashValue;
        this.messageType = messageType;
        this.version = version;
        this.isRenderable = isRenderable;

        int hashCode = messageType.hashCode();
        hashCode = 31 * hashCode + version.hashCode();
        hashCode = 31 * hashCode + hashValue.hashCode();

        calculatedHashCode = hashCode;
    }

    @Override
    public Integer getId()
    {
        return id;
    }

    @Override
    public boolean isRenderable()
    {
        return isRenderable;
    }

    @Override
    public Collection<String> getKeys()
    {
        return Collections.unmodifiableCollection(dataMap.keySet());
    }

    @Override
    public Collection<IDataField> getFields()
    {
        return Collections.unmodifiableCollection(dataMap.values());
    }

    @Override
    public Collection<IDataField> getFieldsWithDefaultValues()
    {
        return unmodifiableFieldsWithDefaultValues;
    }

    @Override
    public IDataField getField(String name)
    {
        return dataMap.get(name);
    }

    @Override
    public IMetadata getMetadata()
    {
        return this;
    }

    @Override
    public String getVersion()
    {
        return version;
    }

    @Override
    public String getMessageType()
    {
        return messageType;
    }

    @Override
    public String getHashValue()
    {
        return hashValue;
    }

    @Override
    public String toString()
    {
        return id + ":" + messageType + ":" + version + ":" + hashValue;
    }

    /**
     * A more complete toString.
     *
     * @param includeMEs flag that indicates if MEs should be included
     * @return String representation of the structure without including the fields
     */
    public String fullString(boolean includeMEs)
    {
        return fullString(includeMEs, false);
    }

    /**
     * A more complete toString.
     *
     * @param includeMEs    flag that indicates if MEs should be included
     * @param includeFields flag that indicates if fields should be included
     * @return String representation of the structure
     */
    public String fullString(boolean includeMEs, boolean includeFields)
    {
        return fullString(includeMEs, includeFields, false);
    }

    public String fullString(boolean includeMEs, boolean includeFields, boolean includeMetadata)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(id).append(":").append(messageType).append("_").append(version).append(" ").append(hashValue);
        if (includeMEs)
        {
            for (IMapEntity me : definitions)
            {
                sb.append("\r    ");
                sb.append(((MapEntity) me).fullString(false));
            }
        }
        if (includeFields)
        {
            for (DataField f : dataMap.values())
            {
                sb.append("\r    ");
                sb.append(f.fullString());

                if (includeMetadata)
                {
                    sb.append("\r     ");
                    for (String metadataKey : f.getMetadataKeys())
                    {
                        sb.append(metadataKey)
                                .append(" = ")
                                .append(f.getMetadataValue(metadataKey))
                                .append(", ");
                    }
                }
            }
        }

        if (includeMetadata)
        {
            sb.append("\r    ");
            sb.append(getMetadataAsJson());
        }

        return sb.toString();
    }

    @Override
    public int hashCode()
    {
        return calculatedHashCode;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Structure structure = (Structure) o;

        // Use method calls, not direct member access, so hibernate proxies will compare as equal
        // Yes, this was tricky to find.

        return version.equals(structure.getVersion())
                && messageType.equals(structure.getMessageType())
                && hashValue.equals(structure.getHashValue());
    }

    private void addField(DataField dataField)
    {
        dataField.setStructure(this); // Take ownership
        dataMap.put(dataField.getFieldName(), dataField);

        if (dataField.getDefaultValue() != null) fieldsWithDefaultValues.add(dataField);
    }

    /**
     * Performs a case-insensitive compare based on the Message Type and version fields.
     */
    @Override
    public int compareTo(@SuppressWarnings("NullableProblems") IStructure structure)
    {
        return STRUCTURE_COMPARATOR.compare(this, structure);
    }

    @Override
    public void addDefinition(IMapEntity def)
    {
        boolean aD = internalAddMapEntity(def);
        boolean aS = ((MapEntity) def).internalAddStructure(this);

        if (logger.isDebugEnabled() && (aD || aS))
        {
            logger.debug("Adding {}MapEntity{}:{} to {}Structure{}:{}", aD ? "<<" : "", aD ? ">>" : "", def, aS ? "<<" : "", aS ? ">>" : "", this);
        }
    }

    /**
     * Internal method to add a map entity to our collection if it is not already present.
     * Package access so {@link MapEntity#addStructure(IStructure)} can also access it.
     *
     * @param mapEntity MapEntity to add
     * @return {@code true} if the MapEntity was added. {@code false} if already present
     */
    boolean internalAddMapEntity(IMapEntity mapEntity)
    {
        // If the mapEntity hasn't been persisted, then somehow a MapEntity got created without being persisted.
        if (mapEntity.getId() == null)
        {
            throw new IllegalStateException("Can't add a MapEntity that hasn't been persisted: " + mapEntity);
        }

        if (!definitions.contains(mapEntity))
        {
            definitions.add(mapEntity);
            structureAssociationManager.addAssociation(mapEntity.getId(), getId());
            return true;
        }
        return false;
    }

    @Override
    public Set<IMapEntity> getDefinitions()
    {
        return new HashSet<>(definitions);
    }

    private void addIndex(IndexDefinition indexDefinition)
    {
        indexDefinition.setStructure(this); // Take ownership
        indexes.add(indexDefinition);
    }

    @Override
    public List<IIndexDefinition> getIndexes()
    {
        return Collections.unmodifiableList(indexes);
    }

    @Override
    public void createExtendedDataClass()
    {
        if (extendedDataClass == null)
        {
            extendedDataClass = dynamicStructures.getOrGenerateClass(this);
        }
    }

    @Override
    public Class<? extends IExtendedDataStructure> getExtendedDataClass()
    {
        // Don't bother synchronizing this, worst case we perform this lookup a couple of times
        if (extendedDataClass == null) createExtendedDataClass();

        return extendedDataClass;
    }

    @Override
    public String getExtendedDataClassName()
    {
        return STRUCTURE_CLASS_PREFIX + getHashValue();
    }

    @Override
    public IExtendedDataStructure makeExtendedDataObject()
    {
        try
        {
            if (extendedDataConstructor == null) extendedDataConstructor = getExtendedDataClass().getConstructor();

            return extendedDataConstructor.newInstance();
        } catch (ReflectiveOperationException e)
        {
            logger.error("Error creating extended data object", e);
            return null;
        }
    }

    @Override
    public String getBulkCopySql()
    {
        return dynamicStructures.getCopySql(this);
    }

    @PostLoad
    private void onLoad()
    {
        // Find all the fields with default values
        for (IDataField field : dataMap.values())
        {
            if (field.getDefaultValue() != null) fieldsWithDefaultValues.add(field);
        }
    }
}
