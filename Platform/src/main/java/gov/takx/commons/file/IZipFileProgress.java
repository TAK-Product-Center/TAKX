package gov.takx.commons.file;

/**
 * Monitors the progress of zip utilities.
 *
 * @since 1.14
 */
public interface IZipFileProgress
{
    /**
     * Set the progress bar to be indeterminate.
     */
    int INDETERMINATE = -1;

    /**
     * Increment the count by a single unit.
     */
    void increment();

    /**
     * The number of files, including directories, that will be included in the zip.
     *
     * @param count The file count.
     */
    void setFileCount(int count);

    /**
     * Set the main text message.
     *
     * @param text The main message.
     */
    void setText(String text);

    /**
     * Set the text associated with the substatus.
     *
     * @param text The substatus message.
     */
    void setSubstatusText(String text);

    /**
     * The substatus maximum.
     *
     * @param maximum The maximum value, or {@link #INDETERMINATE} if it is unknown.
     */
    @SuppressWarnings("UnusedDeclaration")
    void setSubstatusMaximum(int maximum);

    /**
     * Increment the substatus.
     */
    @SuppressWarnings("UnusedDeclaration")
    void incrementSubstatus();
}
