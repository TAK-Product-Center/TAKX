package gov.takx.api.platform;

/**
 * The IProgressCallback can be used to get feedback during long running operations. It can also be used to give
 * instruction to the operation such as "cancel". In general, the interface will be extended for use in customized cases.
 * The caller (monitoring object) will either implement this interface directly or pass in an object that does.
 *
 * @since 1.13
 */
public interface IProgressCallback
{
    /**
     * Called when a "stage" of the operation is started.  The definition of a stage, or even if stages are used,
     * is up to the process using this callback.  Typically, this will be used to update a note or text on a
     * progress indicator.
     *
     * @param stage The stage that has started
     * @since 1.14
     */
    void onStageStarted(String stage);

    /**
     * onProgress will be called during the running cycle at times when the thread has an opportunity to invoke
     * the callback. There is no guarantee that this method will be called on a regular basis or even that it will be
     * called at all. For small operations, it is possible that this method is never called.
     *
     * @param progress The progress expressed as a percentage. If the the progress is indeterminate the value will
     *                 be -1.0
     */
    void onProgress(double progress);

    /**
     * An error occurred during the process. Note that one of the errors that might get thrown is that the user
     * cancelled the operation from the progress dialog.
     *
     * @param t The exception
     */
    void onError(Throwable t);

    /**
     * If the monitoring process wants to cancel the long running operation then it should return true.
     *
     * @return {@code true} if the operation should terminate early
     */
    boolean isCancel();
}
