package gov.takx.core.project;

import gov.takx.api.platform.IProgressMonitor;

import java.io.IOException;

/**
 * Stub (empty) implementation of a {@link Project} used for testing and static tools that need a project session.
 * It overrides the behavior of the standard project to avoid any real "opening" but it allows access to
 * project preferences.
 *
 */
public class EmptyProject extends Project
{
    public EmptyProject(String projectPath, boolean mostRecentProject)
    {
        super(projectPath);
    }

    @Override
    public void open() throws IOException
    {
        createProjectSetupIfNeeded();

        setProjectUuidIfNeeded(false); // Ensure we have a project UUID
    }

    @Override
    public void close(IProgressMonitor progressMonitor)
    {
        // Do nothing
    }

    @Override
    public void saveProjectPreferences()
    {
    }

    @Override
    public boolean isNew()
    {
        return false;
    }

    @Override
    public void setNew(boolean val)
    {
    }

    @Override
    public long getProjectCreationTime()
    {
        return System.currentTimeMillis();
    }
}
