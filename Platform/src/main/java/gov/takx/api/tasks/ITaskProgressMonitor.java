package gov.takx.api.tasks;

/**
 * Base interface for tasks that are submitted for execution and progress display on the status bar.
 *
 * @since 2.14
 */
public interface ITaskProgressMonitor
{
    /**
     * Print a message to the Import/Export status console.
     *
     * @param msg Message to print.
     * @since 1.3
     */
    void printMessage(String msg);

    /**
     * Set the progress as an integer value. Allowable values are zero to the max progress value.
     *
     * @param progress New progress value
     * @see #setMaxProgress(int)
     * @since 1.3
     */
    void setProgress(int progress);

    /**
     * Set whether or not the progress is indeterminate.
     *
     * @param value True if the progress is indeterminate, and the graphical progress bar
     *              should be set to an indeterminate state, or false if the progress will be
     *              tracked.
     * @since 1.3
     */
    void setProgressIndeterminate(boolean value);

    /**
     * Set the minimum progress value.
     *
     * @param value Min progress value
     * @since 1.42
     */
    void setMinProgress(int value);

    /**
     * Set the maximum progress value.
     *
     * @param value Max progress value.
     * @since 1.3
     */
    void setMaxProgress(int value);

    /**
     * Set the progress monitor to a "completed" state.  Required to call this method to disposed of the progress
     * for the associated task.
     *
     * @since 1.3
     */
    void setComplete();
}
