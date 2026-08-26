package gov.takx.core.preferences;

import gov.takx.api.event.project.ProjectClosed;
import gov.takx.api.preferences.IPreferenceEntry;
import gov.takx.api.preferences.IProjectPreferenceManager;
import gov.takx.api.project.IProjectManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Typed;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;

import java.io.File;

/**
 * Implementation of project preference manager. Provides access to project preference node and a method to save the
 * current, in-memory, preferences out to disk.
 * <p>
 * Currently, project preferences can not be injected (as the User and Program preferences can be), so
 * {@link #getPreferenceEntryFromInjection(InjectionPoint)} always return {@code null}.
 *
 * @since 2.35
 */
@ApplicationScoped
@Typed({ProjectPreferenceManager.class, IProjectPreferenceManager.class})
public class ProjectPreferenceManager extends APreferenceManager implements IProjectPreferenceManager
{
    private static final String DEFAULT_PROJECT_PREFERENCES_XML = "conf/DefaultUserPreferences.xml";
    private static final String PROJECT_NODE_KEY = "project";

    @Inject
    private IProjectManager projectManager;

    ProjectPreferenceManager()
    {
        super(PROJECT_NODE_KEY, DEFAULT_PROJECT_PREFERENCES_XML);
    }

    @Override
    protected File getPreferencesFile()
    {
        return new File(projectManager.getCurrentOpenProject().getProjectFilePath());
    }

    @Override
    protected IPreferenceEntry getPreferenceEntryFromInjection(InjectionPoint injectionPoint)
    {
        return null;
    }

    /**
     * Unload the preferences when the project is closed.
     */
    private void onProjectClosed(@Observes ProjectClosed ignored)
    {
        unloadPreferences();
    }
}
