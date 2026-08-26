package gov.takx.core.navigation;

import gov.takx.platform.ui.UIResources;

import javax.swing.ImageIcon;

import java.net.URL;

import static gov.takx.platform.ui.ResourceDirectory.TAK_ASSETS_PNG;

/**
 * Constants for {@link gov.takx.core.navigation.NavigationManager} and associated classes.
 *
 * @since 5.7
 */
public class NavigationConstants
{
    /**
     * Duration format used when displaying navigation durations.
     */
    public static final String ETA_FORMAT = "[H'h' m'm' ]s's'";
    /**
     * Resource URL for Navigate-To icon.
     */
    public static final URL NAVIGATE_TO_ICON_URL = UIResources.getUrl(TAK_ASSETS_PNG, "Navigate-to-SPI_48x48.png");
    /**
     * Action button sized navigate-to icon.
     */
    public static final ImageIcon NAVIGATE_TO_ACTION_BUTTON_ICON = UIResources.getActionButtonIcon(NAVIGATE_TO_ICON_URL);
    /**
     * Standard sized navigate-to icon.
     */
    public static final ImageIcon NAVIGATE_TO_ICON = UIResources.getStandardSizedIcon(NAVIGATE_TO_ICON_URL);
}
