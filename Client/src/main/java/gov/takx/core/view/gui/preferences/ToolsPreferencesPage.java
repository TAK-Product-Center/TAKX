package gov.takx.core.view.gui.preferences;

import gov.tak.api.comms.takserver.DeleteOption;
import gov.tak.api.comms.takserver.PublishOption;
import gov.tak.api.comms.takserver.TimeThresholdOption;
import gov.tak.api.icons.TakColor;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.milsym.MilStd2525Variant;
import gov.takx.core.preferences.ToolPreferenceManager;
import gov.takx.core.preferences.ToolPreferenceManager.ToolDefinition;
import gov.takx.core.preferences.tool.ToolPreferenceAdded;
import gov.takx.core.preferences.tool.ToolPreferenceRemoved;
import gov.takx.platform.ui.ResourceDirectory;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import javax.swing.ImageIcon;

import java.awt.Image;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static gov.takx.platform.preferences.PreferenceEntry.BLOODHOUND_AUTO_KEEP_IN_VIEW;
import static gov.takx.platform.preferences.PreferenceEntry.BLOODHOUND_AUTO_TARGET_BREADCRUMBS;
import static gov.takx.platform.preferences.PreferenceEntry.BLOODHOUND_DISPLAY_INTERCEPT;
import static gov.takx.platform.preferences.PreferenceEntry.BLOODHOUND_FLASH;
import static gov.takx.platform.preferences.PreferenceEntry.BLOODHOUND_FLASH_ETA_COLOR;
import static gov.takx.platform.preferences.PreferenceEntry.BLOODHOUND_FLASH_ETA_THRESHOLD;
import static gov.takx.platform.preferences.PreferenceEntry.BLOODHOUND_INNER_ETA_COLOR;
import static gov.takx.platform.preferences.PreferenceEntry.BLOODHOUND_INNER_ETA_THRESHOLD;
import static gov.takx.platform.preferences.PreferenceEntry.BLOODHOUND_LOW_SPEED_COLOR;
import static gov.takx.platform.preferences.PreferenceEntry.BLOODHOUND_MIDDLE_ETA_COLOR;
import static gov.takx.platform.preferences.PreferenceEntry.BLOODHOUND_MIDDLE_ETA_THRESHOLD;
import static gov.takx.platform.preferences.PreferenceEntry.BLOODHOUND_OUTER_ETA_COLOR;
import static gov.takx.platform.preferences.PreferenceEntry.DATA_PACKAGE_ENABLE_SHARING;
import static gov.takx.platform.preferences.PreferenceEntry.DATA_PACKAGE_NO_GO_THRESHOLD_MB;
import static gov.takx.platform.preferences.PreferenceEntry.DATA_PACKAGE_SUCCESS_THRESHOLD_HIGH_MB;
import static gov.takx.platform.preferences.PreferenceEntry.DATA_PACKAGE_SUCCESS_THRESHOLD_LOW_MB;
import static gov.takx.platform.preferences.PreferenceEntry.DATA_SYNC_AUTO_ACCEPT_FEED_INVITES;
import static gov.takx.platform.preferences.PreferenceEntry.DATA_SYNC_AUTO_DELETE_REMOVED;
import static gov.takx.platform.preferences.PreferenceEntry.DATA_SYNC_AUTO_DELETE_THRESHOLD;
import static gov.takx.platform.preferences.PreferenceEntry.DATA_SYNC_AUTO_DOWNLOAD_UPDATES;
import static gov.takx.platform.preferences.PreferenceEntry.DATA_SYNC_LARGE_FILE_PROMPT;
import static gov.takx.platform.preferences.PreferenceEntry.DATA_SYNC_PUBLISH_OPTIONS;
import static gov.takx.platform.preferences.PreferenceEntry.DATA_SYNC_THRESHOLD;
import static gov.takx.platform.preferences.PreferenceEntry.MIL_STD_2525_PREF;
import static gov.takx.platform.preferences.PreferenceEntry.ROUTE_CHECKPOINT_PREFIX;
import static gov.takx.platform.preferences.PreferenceEntry.ROUTE_DRIVING_CHECKPOINT_RADIUS;
import static gov.takx.platform.preferences.PreferenceEntry.ROUTE_ELEVATION_PROFILE_CENTER_ON_SEEKER;
import static gov.takx.platform.preferences.PreferenceEntry.ROUTE_FLYING_CHECKPOINT_RADIUS;
import static gov.takx.platform.preferences.PreferenceEntry.ROUTE_SWIMMING_CHECKPOINT_RADIUS;
import static gov.takx.platform.preferences.PreferenceEntry.ROUTE_WALKING_CHECKPOINT_RADIUS;
import static gov.takx.platform.preferences.PreferenceEntry.ROUTE_WATERCRAFT_CHECKPOINT_RADIUS;
import static gov.takx.platform.ui.UIResources.ROUTE_TOOLBAR_ICON;

/**
 * Preferences page showing links to sub-pages Data Packages, Data Sync, and all Tools that have been added by plugins.
 *
 * @see ToolPreferenceManager
 * @since 5.4
 */
@PseudoBean(singletonObserver = true)
public class ToolsPreferencesPage extends APreferencesPage
{
    private static final String DATA_SYNC = "Data Sync Preferences";
    private static final String DATA_PACKAGES = "Data Packages Preferences";
    private static final String MIL_STD_2525 = "MIL-STD-2525 Preferences";
    private static final String BLOODHOUND = "Bloodhound Preferences";
    private static final String ROUTE = "Route Preferences";
    private static final String PLAYBACK = "Playback Preferences";

    private static final int SIZE = ICON_SIZE.width;
    private static final ImageIcon DATA_SYNC_ICON = assetIcon("Data-Sync_32x32.png");
    private static final ImageIcon DATA_PACKAGE_ICON = assetIcon("Mission-Package_32x32.png");
    private static final ImageIcon MIL_STD_2525_ICON = UIResources.getToolbarIcon(ResourceDirectory.PLATFORM_ICONS, "icon_tac_graphics.png");
    private static final ImageIcon BLOODHOUND_ICON = assetIcon("Bloodhound_32x32.png");
    private static final ImageIcon PLAYBACK_ICON = UIResources.getToolbarIcon(ResourceDirectory.PLATFORM_ICONS, "playback.svg");

    @Inject
    private ToolPreferenceManager toolPreferenceManager;

    private final List<PageHolder> pages = new ArrayList<>();
    private final Map<ToolDefinition, Integer> toolPageIds = new ConcurrentHashMap<>();

    public ToolsPreferencesPage()
    {
        super("Tool Preferences");
    }

    @PostConstruct
    private void postConstruct()
    {
        // Even though Data Sync and Data Packages are not plugin provided tools, that's where they reside in WinTAK
        pages.add(new PageHolder(DATA_SYNC_ICON, DATA_SYNC, "Adjust " + DATA_SYNC, DataSyncPage::new));
        pages.add(new PageHolder(DATA_PACKAGE_ICON, DATA_PACKAGES, "Adjust " + DATA_PACKAGES, DataPackagesPage::new));
        pages.add(new PageHolder(MIL_STD_2525_ICON, MIL_STD_2525, "Adjust " + MIL_STD_2525, MilStd2525Page::new));
        pages.add(new PageHolder(BLOODHOUND_ICON, BLOODHOUND, "Adjust " + BLOODHOUND, BloodhoundPage::new));
        pages.add(new PageHolder(ROUTE_TOOLBAR_ICON, ROUTE, "Adjust " + ROUTE, RoutePage::new));
        pages.add(new PageHolder(PLAYBACK_ICON, PLAYBACK, "Adjust " + PLAYBACK, PlaybackPreferencesPage::new));

        toolPreferenceManager.getToolDefinitions().forEach(toolDefinition -> pages.add(makeToolPageHolder(toolDefinition)));

        pages.sort(Comparator.comparing(PageHolder::title));

        pages.forEach(page -> {
            final int pageId = addPageLink(page.icon, page.title, page.description, page.pageSupplier);
            if (page.toolDefinition != null) toolPageIds.put(page.toolDefinition, pageId);
        });
    }

    /**
     * Add a new page link for a tool preference definition that has been added.
     * @since 5.7
     */
    private void onToolPreferenceAdded(@Observes ToolPreferenceAdded event)
    {
        final var toolDefinition = event.toolDefinition();
        final int insertionIndex = getInsertionIndex(toolDefinition);
        final var page = makeToolPageHolder(toolDefinition);
        pages.add(insertionIndex, page);
        toolPageIds.put(page.toolDefinition, addPageLink(page.icon, page.title, page.description, page.pageSupplier, insertionIndex));
    }

    /**
     * @return The index at which to add a new tool definition page link
     * @since 5.7
     */
    private int getInsertionIndex(ToolDefinition toolDefinition)
    {
        final String title = toolDefinition.title();
        int index = 0;
        while (index < pages.size() && pages.get(index).title().compareTo(title) < 0) index++;
        return index;
    }

    /**
     * @return PageHolder created from the tool definition, using a properly scaled icon
     * @since 5.7
     */
    private static PageHolder makeToolPageHolder(ToolDefinition toolDefinition)
    {
        final var sizedIcon = new ImageIcon(toolDefinition.icon().getImage().getScaledInstance(SIZE, SIZE, Image.SCALE_SMOOTH));
        return new PageHolder(sizedIcon, toolDefinition);
    }

    /**
     * Remove our page link for the tool preference that was removed.
     * @since 5.7
     */
    private void onToolPreferenceRemoved(@Observes ToolPreferenceRemoved event)
    {
        final var toolDefinition = event.toolDefinition();
        pages.removeIf(page -> page.toolDefinition == toolDefinition);
        final var pageId = toolPageIds.remove(toolDefinition);
        if (pageId != null) removeActionPanel(pageId);
    }

    /**
     * Simple holder to allow us to sort a collection of pages, regardless of their source.
     */
    private record PageHolder(@Nonnull ImageIcon icon, @Nonnull String title, @Nonnull String description, @Nonnull Supplier<APreferencesPage> pageSupplier, @Nullable ToolDefinition toolDefinition)
    {
        PageHolder(@Nonnull ImageIcon icon, @Nonnull String title, @Nonnull String description, @Nonnull Supplier<APreferencesPage> pageSupplier)
        {
            this(icon, title, description, pageSupplier, null);
        }

        PageHolder(@Nonnull ImageIcon icon, @Nonnull ToolDefinition toolDefinition)
        {
            this(icon, toolDefinition.title(), toolDefinition.description(), () -> new ToolPreferencesPage(toolDefinition), toolDefinition);
        }
    }

    /**
     * @since 5.8
     */
    private static class RoutePage extends APreferencesPage
    {
        public RoutePage()
        {
            super(ROUTE);

            addEditor(ROUTE_CHECKPOINT_PREFIX, PreferencesComponentFactory.makeTextEditor());
            addEditor(ROUTE_DRIVING_CHECKPOINT_RADIUS, PreferencesComponentFactory.makeIntSpinnerEditor(0, 10_000, 1));
            addEditor(ROUTE_FLYING_CHECKPOINT_RADIUS, PreferencesComponentFactory.makeIntSpinnerEditor(0, 10_000, 1));
            addEditor(ROUTE_SWIMMING_CHECKPOINT_RADIUS, PreferencesComponentFactory.makeIntSpinnerEditor(0, 10_000, 1));
            addEditor(ROUTE_WALKING_CHECKPOINT_RADIUS, PreferencesComponentFactory.makeIntSpinnerEditor(0, 10_000, 1));
            addEditor(ROUTE_WATERCRAFT_CHECKPOINT_RADIUS, PreferencesComponentFactory.makeIntSpinnerEditor(0, 10_000, 1));
            addCheckboxEditor(ROUTE_ELEVATION_PROFILE_CENTER_ON_SEEKER);
        }
    }

    /**
     * @since 5.6
     */
    private static class BloodhoundPage extends APreferencesPage
    {
        public BloodhoundPage()
        {
            super(BLOODHOUND);

            addCheckboxEditor(BLOODHOUND_DISPLAY_INTERCEPT);
            addCheckboxEditor(BLOODHOUND_AUTO_KEEP_IN_VIEW);
            addCheckboxEditor(BLOODHOUND_AUTO_TARGET_BREADCRUMBS);
            addCheckboxEditor(BLOODHOUND_FLASH);

            addSectionHeader("ETA Preferences");

            var bloodhoundColors = TakColor.getMarkerPalette();

            addTakColorChooserEditor(BLOODHOUND_LOW_SPEED_COLOR, bloodhoundColors);
            addTakColorChooserEditor(BLOODHOUND_OUTER_ETA_COLOR, bloodhoundColors);

            addEditor(BLOODHOUND_MIDDLE_ETA_THRESHOLD, PreferencesComponentFactory.makeIntSpinnerEditor(0, Integer.MAX_VALUE, 1));
            addTakColorChooserEditor(BLOODHOUND_MIDDLE_ETA_COLOR, bloodhoundColors);

            addEditor(BLOODHOUND_INNER_ETA_THRESHOLD, PreferencesComponentFactory.makeIntSpinnerEditor(0, Integer.MAX_VALUE, 1));
            addTakColorChooserEditor(BLOODHOUND_INNER_ETA_COLOR, bloodhoundColors);

            addEditor(BLOODHOUND_FLASH_ETA_THRESHOLD, PreferencesComponentFactory.makeIntSpinnerEditor(0, Integer.MAX_VALUE, 1));
            addTakColorChooserEditor(BLOODHOUND_FLASH_ETA_COLOR, bloodhoundColors);
        }
    }

    private static class MilStd2525Page extends APreferencesPage
    {
        public MilStd2525Page()
        {
            super(MIL_STD_2525);

            addEditor(MIL_STD_2525_PREF, PreferencesComponentFactory.makeComboBox(MilStd2525Variant.getNameList()));
        }
    }

    private static class DataSyncPage extends APreferencesPage
    {
        public DataSyncPage()
        {
            super(DATA_SYNC);

            addCheckboxEditor(DATA_SYNC_AUTO_ACCEPT_FEED_INVITES);
            addCheckboxEditor(DATA_SYNC_AUTO_DOWNLOAD_UPDATES);
            addEditor(DATA_SYNC_AUTO_DELETE_THRESHOLD, PreferencesComponentFactory.makeComboBox(TimeThresholdOption.class));
            addEditor(DATA_SYNC_LARGE_FILE_PROMPT, PreferencesComponentFactory.makeIntSpinnerEditor(0, Integer.MAX_VALUE, 1));
            addEditor(DATA_SYNC_PUBLISH_OPTIONS, PreferencesComponentFactory.makeComboBox(PublishOption.class));
            addEditor(DATA_SYNC_AUTO_DELETE_REMOVED, PreferencesComponentFactory.makeComboBox(DeleteOption.class));
            addEditor(DATA_SYNC_THRESHOLD, PreferencesComponentFactory.makeComboBox(TimeThresholdOption.class));
        }
    }

    private static class DataPackagesPage extends APreferencesPage
    {
        public DataPackagesPage()
        {
            super(DATA_PACKAGES);

            addCheckboxEditor(DATA_PACKAGE_ENABLE_SHARING);
            addEditor(DATA_PACKAGE_SUCCESS_THRESHOLD_LOW_MB, PreferencesComponentFactory.makeIntSpinnerEditor(0, Integer.MAX_VALUE, 1));
            addEditor(DATA_PACKAGE_SUCCESS_THRESHOLD_HIGH_MB, PreferencesComponentFactory.makeIntSpinnerEditor(0, Integer.MAX_VALUE, 1));
            addEditor(DATA_PACKAGE_NO_GO_THRESHOLD_MB, PreferencesComponentFactory.makeIntSpinnerEditor(0, Integer.MAX_VALUE, 1));
        }
    }
}
