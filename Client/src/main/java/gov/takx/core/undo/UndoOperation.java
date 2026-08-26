package gov.takx.core.undo;

/**
 * Interface defining undo operations.  Undo operations are generally handled by {@link UndoHandler} instances.
 *
 * @see UndoHandler
 * @since 1.26
 */
public interface UndoOperation
{
    /**
     * Apply the undo operation.
     */
    void apply();
}
