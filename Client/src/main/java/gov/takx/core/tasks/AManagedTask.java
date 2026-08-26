package gov.takx.core.tasks;

import gov.nasa.worldwind.geom.Sector;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.systemmessage.MessageAction;
import gov.takx.api.tasks.IManagedTask;
import gov.takx.api.tasks.ITaskManager;
import gov.takx.api.tasks.ITaskStatusHandler;
import gov.takx.api.tasks.TaskGroup;
import jakarta.inject.Inject;

/**
 * Abstract base class of all managed tasks.  Every task has a name used in UI displays and messages.
 * All tasks are organized into groups for use when interacting with users to display similar tasks together.
 *
 * @since 1.42
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public abstract class AManagedTask implements IManagedTask
{
    @Inject
    private IExtendedGlobeManager globeManager;

    @Inject
    private IMapObjectManager mapObjectManager;

    private final String name;
    protected final TaskGroup taskGroup;
    protected ITaskStatusHandler taskStatusHandler;

    /**
     * Construct a task in the given group with the given name.  A status handler will be created automatically.
     *
     * @param taskGroup Group to which task belongs
     * @param name      Task name
     * @see ITaskManager#setTaskGroupExecutionLimit(TaskGroup, int)
     */
    protected AManagedTask(TaskGroup taskGroup, String name)
    {
        this.taskGroup = taskGroup;
        this.name = name;
        taskStatusHandler = new TaskStatusHandler(this);
    }

    @Override
    public TaskGroup getTaskGroup()
    {
        return taskGroup;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public void setTaskStatusHandler(ITaskStatusHandler taskStatusHandler)
    {
        this.taskStatusHandler = taskStatusHandler;
    }

    @Override
    public ITaskStatusHandler getTaskStatusHandler()
    {
        return taskStatusHandler;
    }

    @Override
    public boolean hasLocationInfo()
    {
        final ITaskStatusHandler handler = getTaskStatusHandler();

        return handler.getLocationOfInterest() != null
                || handler.getSectorOfInterest() != null
                || handler.getMapObjectKey() != null;
    }

    @SuppressWarnings("DataFlowIssue")
    @Override
    public MessageAction makeGotoAction()
    {
        final ITaskStatusHandler handler = getTaskStatusHandler();

        return new MessageAction("Go To", () -> {
            // Check for any of three location sources: location, sector, or MO

            final IApplicationGlobe globe = globeManager.getLastFocusedGlobe();

            final ILocation loi = handler.getLocationOfInterest();
            final Sector soi = handler.getSectorOfInterest();
            final String moKey = handler.getMapObjectKey();

            if (loi != null)
            {
                globe.goTo(loi);
            } else if (soi != null)
            {
                globe.goTo(soi);
            } else if (moKey != null)
            {
                final var mapObject = mapObjectManager.getMapObject(moKey);
                if (mapObject != null && mapObject.hasLocation()) globe.goTo(mapObject.getLocation());
            }
        });
    }

    public String toString()
    {
        return getClass().getSimpleName() + ": " + getName();
    }
}
