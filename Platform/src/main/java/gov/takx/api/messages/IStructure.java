package gov.takx.api.messages;

import gov.takx.api.annotation.Internal;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.persist.IPersistable;
import gov.takx.api.plugin.IMetadata;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * Structures define a set of strongly typed fields, keyed by a field name.  Structures are not directly created by
 * plugins, for that you should use a {@link DataStructureBuilder}, or for device plugins, define the structure
 * using an XML specification.
 *
 * @since 1.3
 */
@NoPluginImplementations
public interface IStructure extends Comparable<IStructure>, Serializable, IPersistable
{
    /**
     * @return A collection of all the field names defined in this structure
     */
    Collection<String> getKeys();

    /**
     * Collection of fields defined by the structure.
     *
     * @return Collection of IDataField objects
     */
    Collection<IDataField> getFields();

    /**
     * @return Collection of fields that have default values.  May be empty, never {@code null}.
     * @since 2.14
     */
    Collection<IDataField> getFieldsWithDefaultValues();

    /**
     * Locate a specific data field within the structure.
     *
     * @param name field name
     * @return IDataField, or {@code null} if no field with the given name exists within the structure
     */
    IDataField getField(String name);

    /**
     * @return Structure message type
     */
    String getMessageType();

    /**
     * @return Structure version
     */
    String getVersion();

    /**
     * @return Metadata associated with this structure
     */
    IMetadata getMetadata();

    String getHashValue();

    Collection<IMapEntity> getDefinitions();

    /**
     * Return the list of indexes defined for this Structure.
     *
     * @return the index list
     */
    List<? extends IIndexDefinition> getIndexes();

    /**
     * @return The class defining the dynamically generated extended data object
     * @since 2.2
     */
    Class<? extends IExtendedDataStructure> getExtendedDataClass();

    /**
     * @return The name of the extended data class for this structure (also the DB table name)
     * @since 2.37
     */
    @Internal
    String getExtendedDataClassName();

    /**
     * Ensure that the dynamically created extended data class has been created.
     *
     * @since 2.37
     */
    @Internal
    void createExtendedDataClass();

    /**
     * Construct an instance of the extended data class for this structure.
     *
     * @since 2.37
     */
    @Internal
    IExtendedDataStructure makeExtendedDataObject();

    /**
     * @return The SQL statement to use to COPY values into the structure table
     * @since 2.37
     */
    @Internal
    String getBulkCopySql();

    boolean isRenderable();

    @Internal
    void addDefinition(IMapEntity definition);
}
