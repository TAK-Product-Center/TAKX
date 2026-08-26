package gov.takx.core.systemmessage;

import gov.takx.api.annotation.InvokeAndWaitOnEDT;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.systemmessage.SystemMessage;

import javax.swing.table.AbstractTableModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * Custom table model to handle our message elements.  Allow editing, which gives us the capability to respond
 * to clicks on the hyperlinks in the generated HTML.
 *
 * @since 2.30
 */
class SystemMessageTableModel extends AbstractTableModel
{
    final int maxSize;
    final List<SystemMessageElement> messages = new ArrayList<>();

    final Function<SystemMessage, String> htmlGenerator;

    /**
     * @param maxSize       The maximum number of messages to allow in the model
     * @param htmlGenerator Generator function for producing HTML for a {@link SystemMessage}
     */
    public SystemMessageTableModel(int maxSize, Function<SystemMessage, String> htmlGenerator)
    {
        this.maxSize = maxSize;
        this.htmlGenerator = htmlGenerator;
    }

    /**
     * Add a collection of new messages to the table.  Ensure that we never exceed our configured max size by
     * removing messages from the front/top of the table to make room for the new messages.
     *
     * @param newMessages New messages to add
     */
    @InvokeAndWaitOnEDT
    void addMessages(Collection<SystemMessage> newMessages)
    {
        final int newMessageCount = newMessages.size();
        final int currentCount = messages.size();

        if (currentCount + newMessageCount > maxSize)
        {
            if (newMessageCount > maxSize)
            {
                // Total replacement
                messages.clear();
            } else
            {
                int toRemove = currentCount + newMessageCount - maxSize;
                while (toRemove-- > 0) messages.remove(0);
            }

            newMessages.stream().map(SystemMessageElement::new).forEach(messages::add);
            fireTableDataChanged();
        } else
        {
            newMessages.stream().map(SystemMessageElement::new).forEach(messages::add);
            fireTableRowsInserted(currentCount, currentCount + newMessageCount);
        }
    }

    /**
     * Re-add all system messages to apply changes that may have been made to the color map used by {@link #htmlGenerator}.
     *
     * @since 2.36
     */
    @InvokeOnEDT
    void refresh()
    {
        messages.replaceAll(element -> new SystemMessageElement(element.getSystemMessage()));
    }

    @Override
    public int getRowCount()
    {
        return messages.size();
    }

    @Override
    public int getColumnCount()
    {
        return 1;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex)
    {
        return String.class;
    }

    @Override
    public SystemMessageElement getValueAt(int rowIndex, int columnIndex)
    {
        return messages.get(rowIndex);
    }

    @Override
    public boolean isCellEditable(int row, int column)
    {
        return true;
    }

    /**
     * Wrapper to hold a system message reference and the HTML generated from that message.
     */
    final class SystemMessageElement
    {
        private final SystemMessage systemMessage;
        private String htmlMessageString;

        private SystemMessageElement(SystemMessage systemMessage)
        {
            this.systemMessage = systemMessage;
        }

        SystemMessage getSystemMessage()
        {
            return systemMessage;
        }

        /**
         * Generate and cache the HTML String.
         *
         * @return The System message in HTML String form
         */
        @Override
        public String toString()
        {
            if (htmlMessageString == null)
            {
                htmlMessageString = htmlGenerator.apply(systemMessage);
            }

            return htmlMessageString;
        }
    }
}
