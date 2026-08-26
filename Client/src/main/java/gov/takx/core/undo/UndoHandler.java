package gov.takx.core.undo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Basic undo handler for any object that wishes to track undo-able operations.
 * The operations are managed in a LIFO manner.  The object is considered to have undoable state if the
 * list of undo operations is non-empty.
 *
 * @since 1.26
 */
public class UndoHandler
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    protected final Deque<UndoOperation> undoList = new ArrayDeque<>();

    /**
     * @return {@code true} if any undo operations are queued, {@code false} otherwise
     */
    public boolean isUndoAvailable()
    {
        return !undoList.isEmpty();
    }

    /**
     * Add an undo operation to the queue.
     *
     * @param undoOperation Operation to add
     */
    protected void addUndoOperation(UndoOperation undoOperation)
    {
        undoList.push(undoOperation);
    }

    /**
     * Pop the last undo operation from the queue and apply it.
     *
     * @throws IllegalStateException If no undo operations are available
     */
    public void undo() throws IllegalStateException
    {
        if (!isUndoAvailable())
        {
            logger.error("No undo is available");
            throw new IllegalStateException("No undo operation is available");
        }

        undoList.pop().apply();
    }

    /**
     * Shutdown, clear the list of operations.
     */
    public void shutdown()
    {
        onShutdown();  // Tell derived class

        undoList.clear();
    }

    /**
     * Derived classes that need to do any cleanup when this handler is shutdown should override this method.
     */
    protected void onShutdown()
    {
    }
}
