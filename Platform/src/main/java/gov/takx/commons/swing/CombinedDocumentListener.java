package gov.takx.commons.swing;

import gov.tak.api.annotation.IncubatingApi;
import jakarta.annotation.Nonnull;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * A FunctionalInterface that delegates all {@link DocumentEvent} listener methods of {@link DocumentListener} to a single
 * method which can be concisely used with method references when needed. The event type can be obtained from
 * {@link DocumentEvent#getType()}.
 * <p>
 * Note: Because duplicate method references are not equal to one another, they cannot be used when it is necessary to remove
 * the listener unless assigned to a variable first, which can then be used to remove the listener.
 *
 * @since 5.7
 */
@FunctionalInterface
@IncubatingApi(since = "5.7")
public interface CombinedDocumentListener extends DocumentListener
{
    /**
     * Notifies the listener of a document event.
     *
     * @param event the event
     */
    void documentUpdated(@Nonnull DocumentEvent event);

    @Override
    default void insertUpdate(DocumentEvent e)
    {
        documentUpdated(e);
    }

    @Override
    default void removeUpdate(DocumentEvent e)
    {
        documentUpdated(e);
    }

    @Override
    default void changedUpdate(DocumentEvent e)
    {
        documentUpdated(e);
    }
}
