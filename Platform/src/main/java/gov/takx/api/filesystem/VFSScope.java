package gov.takx.api.filesystem;

/**
 * Scope qualifier for a virtual file system.
 *
 */
public enum VFSScope
{
    /**
     * This a project level VFS. The files in this VFS will only be available to the current project.
     */
    Project,

    /**
     * This a global level VFS. The files in this VFS will be available to all projects.
     */
    Global,
}
