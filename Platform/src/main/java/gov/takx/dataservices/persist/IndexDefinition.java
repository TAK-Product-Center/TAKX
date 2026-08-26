package gov.takx.dataservices.persist;

import gov.takx.api.annotation.Internal;
import gov.takx.api.messages.IIndexDefinition;
import gov.takx.api.messages.IStructure;
import gov.takx.api.persist.IPersistable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.Immutable;

import java.util.Arrays;
import java.util.List;

/**
 * @since 1.14
 */
@Internal
@Entity
@Immutable
public class IndexDefinition implements IIndexDefinition, IPersistable
{
    @SuppressWarnings("unused") // Set by hibernate
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private boolean uniqueIdx = false;
    private String[] columnNames;

    @ManyToOne(optional = false, targetEntity = Structure.class)
    private IStructure structure;

    @SuppressWarnings("unused")
    protected IndexDefinition()
    {
    }

    /**
     * Construct on the given column names.
     *
     * @param uniqueIdx   Pass {@code true} if this is a unique index
     * @param columnNames Columns to include in index
     * @since 3.0
     */
    public IndexDefinition(boolean uniqueIdx, String... columnNames)
    {
        this.uniqueIdx = uniqueIdx;
        this.columnNames = columnNames.clone();
    }

    /**
     * Set the owning structure for this data field.  Package private, only for use by {@code Structure#addIndex}.
     *
     * @param structure Owning structure
     * @since 3.0
     */
    void setStructure(IStructure structure)
    {
        this.structure = structure;
    }

    @Override
    public boolean isUnique()
    {
        return uniqueIdx;
    }

    @Override
    public List<String> getColumnNames()
    {
        return Arrays.asList(columnNames);
    }

    @Override
    public Integer getId()
    {
        return id;
    }

    @Override
    public String toString()
    {
        return "IndexDefinition{" +
                "id=" + id +
                ", structure=" + structure +
                ", uniqueIdx=" + uniqueIdx +
                ", columns=" + Arrays.toString(columnNames) +
                '}';
    }
}
