package gov.takx.api.tasks;

import gov.nasa.worldwind.geom.Sector;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.plugin.io.IImportExportProgressMonitor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.beans.PropertyChangeListener;

/**
 * Contract for task status handlers, which are responsible for tracking and making available the latest
 * status (progress) reported by the managed task. Getter methods for the current (last reported) progress messages
 * and the MapObject/Location/Sector of interest reported by the task.
 * <p/>
 * It extends {@link IImportExportProgressMonitor} so it can easily participate in the import/export wizard feedback process.
 * <p/>
 * All property updates are reported via standard property change events.
 *
 * @see ITaskManager
 * @since 1.42
 */
public interface ITaskStatusHandler extends IImportExportProgressMonitor
{
    String MIN_PROGRESS = "MIN_PROGRESS";
    String MAX_PROGRESS = "MAX_PROGRESS";
    String PROGRESS = "PROGRESS";
    String MESSAGE = "MESSAGE";
    String COMPLETION_STATUS = "COMPLETION_STATUS";
    String PROGRESS_INDETERMINATE = "PROGRESS_INDETERMINATE";
    String MAP_OBJECT_KEY = "MAP_OBJECT_KEY";
    String SECTOR_OF_INTEREST = "SECTOR_OF_INTEREST";
    String LOCATION_OF_INTEREST = "LOCATION_OF_INTEREST";

    /**
     * @return The task being handled by this handler
     */
    IManagedTask getTask();

    /**
     * @return The last reported progress value
     */
    int getProgress();

    /**
     * @return The minimum progress value
     */
    int getMinProgress();

    /**
     * @return The maximum progress value
     */
    int getMaxProgress();

    /**
     * @return {@code true} if {@link #setProgressIndeterminate(boolean)} was called with a {@code true} value
     */
    boolean isProgressIndeterminate();

    /**
     * @return The last message issues with {@link #printMessage(String)}
     */
    String getMessage();

    /**
     * Set the completion status of the task.
     *
     * @param completionStatus Completion status
     */
    void setCompletionStatus(@Nonnull TaskCompletionStatus completionStatus);

    /**
     * @return The recorded completion status, {@code null} if not set or the task has not finished execution yet
     */
    @Nullable
    TaskCompletionStatus getCompletionStatus();

    /**
     * Call this method to indicate that the task has terminated due to an exception.  The exception will be
     * recorded and from this point on {@link #isComplete()} will return {@code true}.
     *
     * @param exception The exception that caused termination
     */
    void setException(Throwable exception);

    /**
     * @return Any exception set by {@link #setException(Throwable)}, {@code null} if not called
     */
    Throwable getException();

    /**
     * Completion may be due to normal termination, an exception, or cancellation -- in all of these cases,
     * this method will return {@code true}.
     *
     * @return {@code true} if {@link #setComplete()} has been called
     */
    boolean isComplete();

    /**
     * @return {@code true} if the task has been canceled, {@code false} otherwise
     * @since 5.6
     */
    default boolean isCanceled()
    {
        return getCompletionStatus() == TaskCompletionStatus.CANCELED;
    }

    /**
     * @return The location set using {@link #setLocationOfInterest(ILocation)}, {@code null} if it has not been called
     */
    ILocation getLocationOfInterest();

    /**
     * @return The sector set using {@link #setSectorOfInterest(Sector)}, {@code null} if it has not been called
     */
    Sector getSectorOfInterest();

    /**
     * @return The MapObject key set using {@link #setMapObjectKey(String)}, {@code null} if it has not been called
     */
    String getMapObjectKey();

    /**
     * Add a PropertyChangeListener to the listener list.
     * The listener is registered for all properties.
     * The same listener object may be added more than once, and will be called
     * as many times as it is added.
     * If {@code listener} is {@code null}, no exception is thrown and no action is taken.
     *
     * @param listener The PropertyChangeListener to be added
     */
    void addPropertyChangeListener(@Nonnull PropertyChangeListener listener);

    /**
     * Remove a PropertyChangeListener from the listener list. This removes a PropertyChangeListener that was registered
     * for all properties. If {@code listener} was added more than once to the same event source, it will be notified
     * one less time after being removed. If {@code listener} is {@code null}, or was never added, no exception is
     * thrown and no action is taken.
     *
     * @param listener The PropertyChangeListener to be removed
     */
    void removePropertyChangeListener(@Nonnull PropertyChangeListener listener);
}
