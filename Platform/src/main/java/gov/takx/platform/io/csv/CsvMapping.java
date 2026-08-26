package gov.takx.platform.io.csv;

import jakarta.annotation.Nonnull;

/**
 * Instances of this class define the mapping of a single field (core or structure) of an RDM to the CSV export column.
 *
 */
public class CsvMapping
{
    final String from;
    final String to;

    /**
     * Construct a mapping where the field name and column name are the same.
     *
     * @param name Field and column name
     */
    public CsvMapping(String name)
    {
        this(name, name);
    }

    /**
     * Construct a mapping from the given field identifier to the given column name.
     *
     * @param from Field identifier
     * @param to   Column name for export
     */
    public CsvMapping(@Nonnull String from, @Nonnull String to)
    {
        this.from = from;
        this.to = to;
    }
}
