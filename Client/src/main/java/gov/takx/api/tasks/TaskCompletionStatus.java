package gov.takx.api.tasks;

/**
 * Enum indicating the manner in which a managed task complete execution.
 *
 * @since 1.42
 */
public enum TaskCompletionStatus
{
    /**
     * The task has not yet completed.
     */
    INCOMPLETE,

    /**
     * The task completed normally.
     */
    DONE,

    /**
     * The task terminated with an exception.
     */
    EXECPTION,

    /**
     * The task was canceled.
     */
    CANCELED
}
