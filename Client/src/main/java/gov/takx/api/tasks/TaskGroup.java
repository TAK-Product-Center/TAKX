package gov.takx.api.tasks;

/**
 * A high-level grouping of task types.  It is used to set limits on the number of concurrently executing
 * tasks within a group and to organize messages from exit-affecting tasks.
 *
 * @see IExitAffectingTask
 * @see ITaskManager#setTaskGroupExecutionLimit(TaskGroup, int)
 * @since 1.42
 */
public enum TaskGroup
{
    FILE_TRANSFER("File Transfer Tasks"),
    FILE_COPY("File Copy Tasks"),
    IMPORT_EXPORT("Import/Export Tasks"),
    RDM_PERSISTENCE("Not all RDMs have been written to the database"),
    PLUGIN("Plugin Tasks"),
    OTHER("Other Tasks"),
    MAPS_IMPORT("Maps Import"),
    SURFACE_STRUCTURE_IMPORT("Surface Structure Import");

    private final String displayName;

    TaskGroup(String displayName)
    {
        this.displayName = displayName;
    }

    public String getDisplayName()
    {
        return displayName;
    }
}
