package gov.takx.commons.lang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Handles a List of exceptions.  Methods are provided for seeing if any exception in the list is of a certain type
 * and generating a reasonable message from the list.
 *
 */
public class ExceptionList extends ArrayList<Throwable>
{

    /**
     * Construct from the argument list
     *
     * @param tlist initial Throwables to add to list
     */
    public ExceptionList(Throwable... tlist)
    {
        addAll(Arrays.asList(tlist));
    }

    /**
     * Construct from a generic List that contains exceptions.
     *
     * @param tlist List of Throwables
     */
    public ExceptionList(List tlist)
    {
        for (Object o : tlist)
        {
            if (!(o instanceof Throwable))
            {
                throw new IllegalArgumentException("All list entries must be of type Throwable");
            }
            add((Throwable) o);
        }
    }

    /**
     * Get a message describing the exceptions in the list.
     *
     * @return String message including all messages from all exceptions in list
     */
    public String getMessage()
    {
        return getMessage("", 0);
    }

    /**
     * Get a message describing the exceptions in the list.
     *
     * @param prefix String to prefix to each line of output (usually for indentation)
     * @return message including all messages from all exceptions in list
     */
    public String getMessage(String prefix)
    {
        return getMessage(prefix, 0);
    }

    /**
     * Get a message describing the exceptions in the list.
     *
     * @param prefix String to prefix to each line of output (usually for indentation)
     * @param limit  The maximum number of exceptions to report, 0 for all
     * @return message including all messages from all exceptions in list
     */
    public String getMessage(String prefix, int limit)
    {
        if (limit == 0) limit = size();

        final Iterator<Throwable> iter = iterator();
        final StringBuilder buffer = new StringBuilder(512);

        while (iter.hasNext() && limit-- > 0)
        {
            final Throwable t = iter.next();

            buffer.append(ExceptionUtils.getMessageWithCauses(prefix, t));

            if (iter.hasNext())
            {
                buffer.append("\n");
            }
        }

        return buffer.toString();
    }

    /**
     * Determine if any exception in the list is an instance of the given class.
     * Test all nested causal exceptions as well.
     *
     * @param classes The class(es) to match against
     * @return {@code true} if an exception matches, false otherwise
     */
    public boolean anyInstanceOf(Class... classes)
    {
        for (Throwable t : this)
        {
            if (ExceptionUtils.anyInstanceOf(t, classes))
            {
                return true;
            }
        }
        return false;
    }
}
