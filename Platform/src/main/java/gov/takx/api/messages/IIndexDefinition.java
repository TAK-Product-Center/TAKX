package gov.takx.api.messages;

import java.util.List;

/**
 * Definition of a custom index associated with an {@link IRaptorDataStructure}.
 *
 * @since 1.14
 */
public interface IIndexDefinition
{
    boolean isUnique();

    /**
     * @return The list of names defining this index
     * @since 3.0
     */
    List<String> getColumnNames();
}
