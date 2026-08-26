package gov.takx.api.event;

import gov.takx.api.platform.IProgressMonitor;

/**
 * CDI Event fired after all the project-level services have started, and the plugin's should be started.
 * This will cause Map Objects to be restored (and have their plugin controllers created), apps to be started, etc.
 *
 * @since 2.23
 */
public class StartPlugins extends ProgressAwareEvent
{
    public StartPlugins(IProgressMonitor progressMonitor)
    {
        super(progressMonitor);
    }
}
