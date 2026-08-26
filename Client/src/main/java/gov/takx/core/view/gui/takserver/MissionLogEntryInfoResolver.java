package gov.takx.core.view.gui.takserver;

import gov.tak.api.comms.takserver.mission.data.LogEntry;
import gov.takx.platform.ui.ResourceDirectory;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;

import javax.swing.ImageIcon;

/**
 * Provides a common way to extract and resolve {@link LogEntry} event info.
 *
 * @since 5.0
 */
@ApplicationScoped
public class MissionLogEntryInfoResolver
{
    private static final ImageIcon LOG_ICON = UIResources.getActionButtonIcon(ResourceDirectory.PLATFORM_ICONS, "file.png");

    /**
     * Returns the icon that should be associated with the log entry.
     * 
     * @param change The change event
     * @return The log entry icon.
     */
    @Nonnull
    public ImageIcon getChangeIcon(@Nonnull LogEntry change)
    {
        // TODO: WinTAK shows a different icon based on if the log as an associated map item / file
        return LOG_ICON;
    }
}