package gov.takx.core.globe;

import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.globe.IUserFavoritesManager;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.platform.preferences.UserPreferenceEntry;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/**
 */
@ApplicationScoped
public class UserViewBookmarkManager extends AFavoritesManager implements IUserFavoritesManager
{
    UserViewBookmarkManager()
    {
    }

    /**
     * Start our service.
     */
    private void startService(@Observes @Priority(ServiceOrdering.START_GlobeBookmarks) StartProjectServices ignored)
    {
        loadPreferences(UserPreferenceEntry.VIEW_BOOKMARKS_NODE);
    }

    /**
     * Stop our service.
     */
    private void stopService(@SuppressWarnings("UnusedParameters") @Observes @Priority(ServiceOrdering.STOP_GlobeService) StopProjectServices event)
    {
        clear();
    }
}
