package gov.takx.core.tasks;

import gov.nasa.worldwind.geom.Sector;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.tasks.IManagedTask;
import gov.takx.api.tasks.ITaskStatusHandler;
import gov.takx.api.tasks.TaskCompletionStatus;
import gov.takx.commons.utils.StringUtils;
import jakarta.annotation.Nonnull;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * Concrete implementation of the task status handler.  It manages all the state and change notification.
 * Each handler operates on a unique task instance.
 *
 * @since 1.42
 */
public class TaskStatusHandler implements ITaskStatusHandler
{
    protected final IManagedTask task;
    protected boolean indeterminate = false;
    protected int minProgress = 0;
    protected int maxProgress = 100;
    protected int progress = 0;
    protected String statusMessage;
    protected String mapObjectKey = null;
    protected ILocation locationOfInterest = null;
    protected Sector sectorOfInterest = null;
    protected TaskCompletionStatus completionStatus = TaskCompletionStatus.INCOMPLETE;
    protected Throwable exception;

    protected final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    /**
     * Construct a status handler for a managed task.
     *
     * @param task The task we are handling
     */
    public TaskStatusHandler(IManagedTask task)
    {
        this.task = task;
    }

    /**
     * @return The task being handled by this handler
     */
    @Override
    public IManagedTask getTask()
    {
        return task;
    }

    @Override
    public String getMapObjectKey()
    {
        return mapObjectKey;
    }

    @Override
    public int getProgress()
    {
        return progress;
    }

    @Override
    public int getMinProgress()
    {
        return minProgress;
    }

    @Override
    public int getMaxProgress()
    {
        return maxProgress;
    }

    @Override
    public boolean isProgressIndeterminate()
    {
        return indeterminate;
    }

    @Override
    public void setException(Throwable exception)
    {
        this.exception = exception;
        setCompletionStatus(TaskCompletionStatus.EXECPTION);
    }

    @Override
    public Throwable getException()
    {
        return exception;
    }

    @Override
    public boolean isComplete()
    {
        return completionStatus != TaskCompletionStatus.INCOMPLETE;
    }

    @Override
    public ILocation getLocationOfInterest()
    {
        return locationOfInterest;
    }

    @Override
    public Sector getSectorOfInterest()
    {
        return sectorOfInterest;
    }

    @Override
    public void printMessage(String msg)
    {
        statusMessage = msg;

        pcs.firePropertyChange(MESSAGE, null, msg);
    }

    @Override
    public String getMessage()
    {
        return statusMessage;
    }

    @Override
    public void setProgress(int progress)
    {
        if (this.progress != progress)
        {
            int oldValue = this.progress;
            this.progress = progress;

            pcs.firePropertyChange(PROGRESS, oldValue, progress);
        }
    }

    @Override
    public void setProgressIndeterminate(boolean indeterminate)
    {
        if (this.indeterminate != indeterminate)
        {
            boolean oldValue = this.indeterminate;
            this.indeterminate = indeterminate;

            pcs.firePropertyChange(PROGRESS_INDETERMINATE, oldValue, indeterminate);
        }
    }

    @Override
    public void setMinProgress(int minProgress)
    {
        if (this.minProgress != minProgress)
        {
            int oldValue = this.minProgress;
            this.minProgress = minProgress;

            pcs.firePropertyChange(MIN_PROGRESS, oldValue, minProgress);
        }
    }

    @Override
    public void setMaxProgress(int maxProgress)
    {
        if (this.maxProgress != maxProgress)
        {
            int oldValue = this.maxProgress;
            this.maxProgress = maxProgress;

            pcs.firePropertyChange(MAX_PROGRESS, oldValue, maxProgress);
        }
    }

    @Override
    public void setCompletionStatus(@Nonnull TaskCompletionStatus completionStatus)
    {
        if (this.completionStatus != completionStatus)
        {
            TaskCompletionStatus oldValue = this.completionStatus;
            this.completionStatus = completionStatus;

            pcs.firePropertyChange(COMPLETION_STATUS, oldValue, completionStatus);
        }
    }

    @Override
    public TaskCompletionStatus getCompletionStatus()
    {
        return completionStatus;
    }

    @Override
    public void setComplete()
    {
        // This method is here for consistency with the Import/Export world.  If this is called before a completion
        // status is set, then assume things went well.

        if (completionStatus == TaskCompletionStatus.INCOMPLETE)
        {
            setCompletionStatus(TaskCompletionStatus.DONE);
        }
    }

    @Override
    public void setMapObjectKey(String key) throws IllegalArgumentException
    {
        if (StringUtils.isEmpty(key)) throw new IllegalArgumentException("'key' must be non-null and non-empty");

        if (!key.equals(mapObjectKey))
        {
            String oldValue = mapObjectKey;
            mapObjectKey = key;

            pcs.firePropertyChange(MAP_OBJECT_KEY, oldValue, key);
        }
    }

    @Override
    public void setLocationOfInterest(@Nonnull ILocation location)
    {
        if (!location.equals(locationOfInterest))
        {
            ILocation oldValue = locationOfInterest;
            locationOfInterest = location;

            pcs.firePropertyChange(LOCATION_OF_INTEREST, oldValue, location);
        }
    }

    @Override
    public void setSectorOfInterest(@Nonnull Sector sector)
    {
        if (!sector.equals(sectorOfInterest))
        {
            Sector oldValue = sectorOfInterest;
            sectorOfInterest = sector;

            pcs.firePropertyChange(SECTOR_OF_INTEREST, oldValue, sector);
        }
    }

    @Override
    public void addPropertyChangeListener(@Nonnull PropertyChangeListener listener)
    {
        pcs.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(@Nonnull PropertyChangeListener listener)
    {
        pcs.removePropertyChangeListener(listener);
    }

    public String toString()
    {
        return getClass().getSimpleName() + "[task=" + task + "]";
    }
}
