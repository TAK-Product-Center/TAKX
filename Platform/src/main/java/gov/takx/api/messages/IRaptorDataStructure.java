package gov.takx.api.messages;

import gov.takx.api.mapobject.IMapObject;
import jakarta.annotation.Nonnull;

/**
 * @since 1.3
 */
public interface IRaptorDataStructure extends IDataStructure
{
    /**
     * Well-defined constant for structure level metadata that documents the description of the structure. This field
     * should be considered an overview of the structure.
     *
     * @since 1.8
     */
    String METADATA_STRUCTURE_DESCRIPTION = "gov.takx.StructDesc";

    /**
     * Well-defined constant for structure level metadata that documents detailed usage of the structure, including
     * nuances in the structure fields, etc.
     *
     * @since 1.8
     */
    String METADATA_STRUCTURE_DETAILS = "gov.takx.StructDetails";

    /**
     * Mark a structure to be excluded from the list of structures to export. If the value is set to "true", the
     * structure will be excluded when export list is filtered. Further, if <i>any</i> Structure is marked to be
     * excluded, the export list will be filtered <i>by default</i>, though the user will always be able to choose
     * to view all the available structures.
     *
     * @since 1.14
     */
    String METADATA_EXCLUDE_FROM_FILTERED_EXPORT_LIST = "gov.takx.ExcludeFromFilteredExportList";

    Integer getId();

    IPrePersistRaptorDataMessage createRaptorDataMessage(@Nonnull IMapObject mapObject);

    IPrePersistRaptorDataMessage createRaptorDataMessage(String mapObjectID);

    IPrePersistRaptorDataMessage createRaptorDataMessage(String mapObjectType, String mapObjectID);

    IPrePersistRaptorDataMessage createRaptorDataMessage(@Nonnull String family, String mapObjectType, String mapObjectID);

    IPrePersistRaptorCommandMessage createRaptorCommandMessage(String mapObjectID);

    String getMapObjectType();

    String getMapObjectFamily();

    /**
     * @return The Map Entity for our family/type.
     * @since 3.0
     */
    IMapEntity getMapEntity();
}
