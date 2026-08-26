package gov.takx.api.messages;

import java.util.Collection;

/**
 * Base data structure definition used by {@link IRaptorDataStructure}. The child class adds its own unique set of
 * creation methods to create concrete instances of a structure known as an {@link IRaptorDataMessage}.
 *
 * @since 1.5
 */
public interface IDataStructure
{
    /**
     * Collection of fields defined by the structure.
     *
     * @return Collection of IDataField objects
     */
    Collection<IDataField> getFields();

    /**
     * Locate a specific data field within the structure.
     *
     * @param name field name
     * @return IDataField, or {@code null} if no field with the given name exists within the structure
     */
    IDataField getField(String name);

    /**
     * Structure message type.
     *
     * @return message type
     */
    String getMessageType();

    /**
     * Structure version.
     *
     * @return version
     */
    String getVersion();

    /**
     * Backing structure.
     *
     * @return structure
     */
    IStructure getStructure();
}
