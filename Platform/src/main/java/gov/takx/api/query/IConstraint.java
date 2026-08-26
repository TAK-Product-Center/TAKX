package gov.takx.api.query;

import gov.takx.api.annotation.Internal;
import gov.takx.platform.query.SqlState;

import java.util.Set;

/**
 * Tag interface for all Constraint types within the query service.
 *
 * @since 1.49
 */
public interface IConstraint
{
    /**
     * Determine if this constraint uses/references any extended structure fields.
     *
     * @return {@code true} if a structure field is referenced, {@code false} if only core fields are referenced
     */
    boolean usesExtendedFields();

    /**
     * Get the set of extended structures that are referenced by this constraint.
     *
     * @return Collection of referenced structures, may be empty
     */
    Set<String> getReferencedStructures();

    /**
     * Generate SQL for this Constraint using the provided SQL state.
     *
     * @param state Sql generation state for mapping field names and parameter binding
     */
    @Internal
    void generateSQL(SqlState state);
}
