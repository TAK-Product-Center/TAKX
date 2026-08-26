package gov.takx.core.filesystem.managers;

import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.event.project.ProjectClosed;
import gov.takx.api.event.project.ProjectOpened;
import gov.takx.api.event.project.ProjectOpening;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.project.IProjectManager;
import gov.takx.core.filesystem.VirtualFileSystemManager;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.io.File;

import static jakarta.interceptor.Interceptor.Priority.PLATFORM_AFTER;
import static jakarta.interceptor.Interceptor.Priority.PLATFORM_BEFORE;

/**
 * VFS Manager for {@link VFSScope#Project}, rootDirectory tracks the current project's project directory.
 *
 * @since 2.0.0
 */
@ApplicationScoped
@VFSManagerScope(VFSScope.Project)
public class ProjectVirtualFileSystemManager extends VirtualFileSystemManager
{
    private final IProjectManager projectManager;

    private File rootDirectory;

    @Inject
    public ProjectVirtualFileSystemManager(IProjectManager projectManager)
    {
        super(VFSScope.Project);
        this.projectManager = projectManager;
    }

    @Override
    public File getRootDirectory()
    {
        return rootDirectory;
    }

    /**
     * We need to get the root directory set as soon as we know the project directory.
     * @since 3.4
     */
    private void onProjectOpening(@Observes @Priority(PLATFORM_BEFORE) ProjectOpening event)
    {
        rootDirectory = projectManager.getProjectDirectory(event.getProjectName());
    }

    /**
     * If the final project directory after opening is different, then clear our file systems cache, and update root.
     * This can happen when a package is opened with the same name as an existing project, in which case the actual
     * directory name has a {@code (n)} added to the base name.
     *
     * @since 3.4
     */
    private void onProjectOpened(@Observes @Priority(PLATFORM_BEFORE) ProjectOpened event)
    {
        final File projectDirectory = projectManager.getProjectDirectory(event.getProjectName());
        if (!projectDirectory.equals(rootDirectory))
        {
            fileSystems.clear();
            rootDirectory = projectDirectory;
        }
    }

    /**
     * We need to delay clearing the current project root directory until all other event listeners have
     * had a chance to run, or they could fail to get a correct reference to the project directory.
     *
     * @since 3.4
     */
    private void onProjectClosed(@Observes @Priority(PLATFORM_AFTER) ProjectClosed event)
    {
        rootDirectory = null; // try to quickly catch any use of rootDir outside a project
    }
}
