package gov.takx.api.tasks;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

/**
 * Events fired at various lifecycle points of a managed tasks execution.
 *
 * @see IManagedTaskEvent
 * @since 2.37
 */
@ApplicationScoped
public class ManagedTaskEvents
{
    private final Event<TaskQueued> taskQueuedEvent;
    private final Event<TaskBlocked> taskBlockedEvent;
    private final Event<TaskStarted> taskStartedEvent;
    private final Event<TaskFinished> taskFinishedEvent;

    @Inject
    ManagedTaskEvents(Event<TaskQueued> taskQueuedEvent, Event<TaskBlocked> taskBlockedEvent, Event<TaskStarted> taskStartedEvent, Event<TaskFinished> taskFinishedEvent)
    {
        this.taskBlockedEvent = taskBlockedEvent;
        this.taskFinishedEvent = taskFinishedEvent;
        this.taskQueuedEvent = taskQueuedEvent;
        this.taskStartedEvent = taskStartedEvent;
    }

    /**
     * Fire a {@link TaskBlocked} event on the given task.
     */
    public void fireBlocked(IManagedTask task)
    {
        taskBlockedEvent.fire(new TaskBlocked(task));
    }

    /**
     * Fire a {@link TaskQueued} event on the given task.
     */
    public void fireQueued(IManagedTask task)
    {
        taskQueuedEvent.fire(new TaskQueued(task));
    }

    /**
     * Fire a {@link TaskStarted} event on the given task.
     */
    public void fireStarted(IManagedTask task)
    {
        taskStartedEvent.fire(new TaskStarted(task));
    }

    /**
     * Fire a {@link TaskFinished} event on the given task.
     */
    public void fireFinished(IManagedTask task)
    {
        taskFinishedEvent.fire(new TaskFinished(task));
    }

    /**
     * The given task has been blocked from execution due to a task group limit.  When the number of executing
     * tasks in that group decreases below the limit, then this task may be queued for execution.
     */
    public static final class TaskBlocked extends AManagedTaskEvent
    {
        public TaskBlocked(IManagedTask task)
        {
            super(task);
        }
    }

    /**
     * The given task has been queued for execution, but has not yet started.
     */
    public static final class TaskQueued extends AManagedTaskEvent
    {
        public TaskQueued(IManagedTask task)
        {
            super(task);
        }
    }

    /**
     * The given task has started execution.
     */
    public static final class TaskStarted extends AManagedTaskEvent
    {
        public TaskStarted(IManagedTask task)
        {
            super(task);
        }
    }

    /**
     * The given task has finished execution.  Check the task's status handler to see if it completed successfully.
     *
     * @see IManagedTask#getTaskStatusHandler()
     * @see ITaskStatusHandler#getCompletionStatus()
     */
    public static final class TaskFinished extends AManagedTaskEvent
    {
        public TaskFinished(IManagedTask task)
        {
            super(task);
        }
    }

    /**
     * Base class for all managed task events.
     */
    private static abstract class AManagedTaskEvent implements IManagedTaskEvent
    {
        private final IManagedTask task;

        AManagedTaskEvent(IManagedTask task)
        {
            this.task = task;
        }

        @Override
        public IManagedTask getTask()
        {
            return task;
        }
    }
}
