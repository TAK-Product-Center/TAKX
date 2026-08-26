package gov.takx.commons.file;

/**
 * Callback interface for monitoring the progress of a file copy operation.
 *
 */
public interface IFileProgress
{
    /**
     * Called to set the maximum value of the progress range.
     *
     * @param maximum Maximum value of range. The progress value passed to {@link #setFileProgress(int)} will
     *                never exceed this value.
     */
    public void setFileMaximum(int maximum);

    /**
     * Called to set the minium value of the progress range.
     *
     * @param minimum Minimum value of range. The progress value passed to {@link #setFileProgress(int)} will
     *                never be less than this value.
     */
    public void setFileMinimum(int minimum);

    /**
     * Called periodically during a file copy operation.
     *
     * @param progress Current progress. This value will always fall between the minimum and maximum values defined
     *                 by calls to {@link #setFileMinimum(int)} and {@link #setFileMaximum(int)}.
     */
    public void setFileProgress(int progress);
}
