package gov.takx.core.globe;

import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.globe.IFavorite;
import gov.takx.api.globe.IProjectFavoritesManager;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.platform.preferences.ProjectPreferenceEntry;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import java.util.Arrays;

/**
 */
@ApplicationScoped
public class ProjectFavoritesManager extends AFavoritesManager implements IProjectFavoritesManager
{
    private static final String DEFAULT = "Default";
    private static final String LAST = "Last";

    ProjectFavoritesManager()
    {
        super(Arrays.asList(DEFAULT, LAST));
    }

    @Override
    public IFavorite getDefault()
    {
        return getById(DEFAULT);
    }

    @Override
    public IFavorite getLast()
    {
        return getById(LAST);
    }

    @Override
    public void setDefault(IFavorite bookmark)
    {
        update(bookmark, DEFAULT);
    }

    @Override
    public void setLast(IFavorite bookmark)
    {
        update(bookmark, LAST);
    }

    /**
     * Start our service.
     */
    private void startService(@Observes @Priority(ServiceOrdering.START_GlobeBookmarks) StartProjectServices ignored)
    {
        loadPreferences(ProjectPreferenceEntry.VIEW_BOOKMARKS_NODE);
    }

    /**
     * Stop our service.
     */
    private void stopService(@Observes @Priority(ServiceOrdering.STOP_GlobeService) StopProjectServices ignored)
    {
        clear();
    }
}
