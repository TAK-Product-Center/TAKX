package gov.takx.api.io;

import gov.takx.api.plugin.io.IImportExportPlugin;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * The result of an Import/Export operation. This object is used to report how many map objects and data points were imported
 * or exported, and any errors that occurred.
 *
 * @since 1.3
 */
@SuppressWarnings("UnusedDeclaration")
public class ImportExportResult
{
    private int mapObjectCount;
    private int dataPointCount;
    private boolean success = true;
    private String[] messages;

    /**
     * A map in which a wizard can place results, statistics, etc.
     *
     * @since 1.14
     */
    private final Map<String, Object> affectedObjects = new HashMap<String, Object>();

    private static final int ERROR_REPORT_LIMIT = 10;

    /**
     * Create the result object.
     */
    public ImportExportResult()
    {
    }

    /**
     * Set the number of data points.
     *
     * @param count New data point count.
     */
    public void setDataPointCount(int count)
    {
        dataPointCount = count;
    }

    /**
     * Get the number of data points in the result.
     *
     * @return Number of data points.
     */
    public int getDataPointCount()
    {
        return dataPointCount;
    }

    /**
     * Increment the number of data points.
     */
    public void incrementDataPointCount()
    {
        dataPointCount++;
    }

    /**
     * Set the number of map objects.
     *
     * @param count New map object count.
     */
    public void setMapObjectCount(int count)
    {
        mapObjectCount = count;
    }

    /**
     * Get the number of map objects in the result.
     *
     * @return The number of map objects in the result.
     */
    public int getMapObjectCount()
    {
        return mapObjectCount;
    }

    /**
     * Increment the number of map objects in the result.
     *
     * @return new count
     */
    public int incrementMapObjectCount()
    {
        return ++mapObjectCount;
    }

    /**
     * Get any messages generated during the operation.
     *
     * @return Array of messages.
     */
    public String[] getMessages()
    {
        return messages;
    }

    /**
     * Get the message from the result. If more than ten messages have been set, messages beyond ten will
     * not be included, and the string will end with "And n more...".
     *
     * @return Message string
     */
    public String getMessage()
    {
        return getMessage(ERROR_REPORT_LIMIT);
    }

    /**
     * Get the messages as a single string. If more than one message has been set the messages are combined,
     * separated by new lines. {@code reportLimit} determines how many messages will be included in the string.
     * <p/>
     * For example, if eight messages have been set in the result, the code:
     * <br>
     * {@code result.getMessage(3);}
     * might return
     * <pre>
     *  Message 1
     *  Message 2
     *  Message 3
     *  And 5 more...
     * </pre>
     *
     * @param reportLimit The maximum number of messages to include.
     * @return Message string, {@code null} if no messages
     */
    public String getMessage(int reportLimit)
    {
        if (messages == null) return null;

        final StringBuilder msg = new StringBuilder();

        int i = 0;
        for (String m : messages)
        {
            if (i < reportLimit)
            {
                msg.append(m).append('\n');
                i++;
            } else
            {
                break;
            }
        }

        if (i >= reportLimit)
        {
            msg.append("And ")
                    .append(messages.length - reportLimit)
                    .append(" more...");
        }

        return msg.toString();
    }

    /**
     * Set the message in the result.
     *
     * @param msg New message
     */
    public void setMessage(String msg)
    {
        setMessage(new String[]{msg});
    }

    /**
     * Set the messages in the result.
     *
     * @param messages Array of messages
     */
    public void setMessage(String[] messages)
    {
        this.messages = messages;
    }

    /**
     * Set the success flag.
     *
     * @param success True if the operation succeeded, or false if it failed.
     */
    public void setSuccess(boolean success)
    {
        this.success = success;
    }

    /**
     * Did the operation succeed?
     *
     * @return true if the operation succeeded, otherwise false.
     */
    public boolean getSuccess()
    {
        return success;
    }

    /**
     * Returns a map in which a wizard can place results, statistics, etc. accessible via keys as
     * JavaDoc'd in the {@link IImportExportPlugin} producing the results.
     *
     * @return A map in which a wizard can place results, statistics, etc.
     * @since 1.14
     */
    public Map<String, Object> getAffectedObjects()
    {
        return affectedObjects;
    }

    @Override
    public String toString()
    {
        return "ImportExportResult{" +
                "mapObjectCount=" + mapObjectCount +
                ", dataPointCount=" + dataPointCount +
                ", success=" + success +
                ", messages=" + Arrays.toString(messages) +
                ", affectedObjects=" + affectedObjects +
                '}';
    }
}
