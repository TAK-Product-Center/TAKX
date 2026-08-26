package gov.takx.core.project;

import gov.takx.api.project.IProjectDelegate;
import gov.takx.api.project.IProjectManager;
import gov.takx.api.project.IRaptorProjectDescriptor;
import gov.takx.client.platform.ApplicationController;
import gov.takx.core.view.gui.screenimageutils.ProjectSnapshotService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 */
@ApplicationScoped
public class ProjectDelegate implements IProjectDelegate
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final IProjectManager projectManager;
    private final ApplicationController applicationController;
    private final ProjectSnapshotService snapshotService;

    @Inject
    ProjectDelegate(IProjectManager projectManager, ApplicationController applicationController, ProjectSnapshotService snapshotService)
    {
        this.projectManager = projectManager;
        this.applicationController = applicationController;
        this.snapshotService = snapshotService;
    }

    @Override
    public boolean saveCurrentProject()
    {
        try
        {
            projectManager.saveProject(null);
        } catch (Exception e)
        {
            logger.error("Error saving current project session", logger.isDebugEnabled() ? e : null);
            return false;
        }
        return true;
    }

    @Override
    public void openExistingProject(String name)
    {
        applicationController.openExistingProject(name);
    }

    @Override
    public IRaptorProjectDescriptor getProjectDescriptor()
    {
        return projectManager.getCurrentOpenProject();
    }

    /**
     * Create a "snapshot.jpg" snapshot of the globe in the root project directory for the current session.
     */
    @Override
    public void saveSnapshot()
    {
        snapshotService.createProjectSnapShot();
    }
}
