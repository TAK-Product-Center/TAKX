package gov.takx.core.view.gui.menus;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.IGlobeDelegate.SnapshotFormat;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.client.platform.ApplicationController;
import gov.takx.core.toolbar.ToolbarButtonBuilder;
import gov.takx.core.toolbar.ToolbarConfigurationManager;
import gov.takx.core.view.gui.CoreComponentType;
import gov.takx.core.view.gui.IExtendedUISpaceManager;
import gov.takx.core.view.gui.PreferenceSpaceManager;
import gov.takx.core.view.gui.contentcleaner.ContentCleanerDialog;
import gov.takx.core.view.gui.preferences.PluginPreferencesDialog;
import gov.takx.core.view.gui.projectadmin.ExportProjectGUI;
import gov.takx.core.view.gui.projectadmin.ProjectCleaner;
import gov.takx.core.view.gui.projectadmin.ProjectExplorer;
import gov.takx.core.view.gui.properties.ProjectSettingsDialog;
import gov.takx.core.view.gui.screenimageutils.ApplicationImageCaptureTool;
import gov.takx.core.view.gui.screenimageutils.SnapshotUtils;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * The main menu bar for the application.
 *
 * @since 3.3
 */
@PseudoBean
public class ApplicationMenu extends JMenuBar
{
    private static final Icon MENU_ICON = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "menu.png");

    @Inject
    private SnapshotUtils snapshotUtils;

    @Inject
    private ApplicationController applicationController;

    @Inject
    protected ToolbarConfigurationManager toolbarConfigManager;

    @Inject
    private IExtendedUISpaceManager uiSpaceManager;

    @Inject
    private PreferenceSpaceManager preferenceSpaceManager;

    private final JMenu menu = new JMenu();

    private final List<String> hiddenEntries;

    private JMenuItem pluginPreferencesItem;

    public ApplicationMenu()
    {
        setName("this");

        menu.setIcon(MENU_ICON);
        menu.setName("menu");
        menu.getPopupMenu().addPopupMenuListener(new PluginPreferencesMenuEnabler());

        add(menu);

        hiddenEntries = Arrays.stream(PreferenceEntry.APPLICATION_MENU_HIDDEN_ITEMS.getStringValue().split(","))
                .map(s -> s.trim().toUpperCase())
                .toList();
    }

    @PostConstruct
    private void postConstruct()
    {
        // This has to happen in an @PC instead of in the constructor to avoid a race condition where the EDT
        // picks up the event *before* the injection processing happens at the end of the constructor.
        SwingUtilities.invokeLater(this::lateInit);
    }

    private void lateInit()
    {
        addMenuItemAndButton(ButtonType.NEW_PROJECT, e -> applicationController.openProject(ProjectExplorer.StartupMode.NEW), menu);
        addMenuItemAndButton(ButtonType.OPEN_PROJECT, e -> applicationController.openProject(ProjectExplorer.StartupMode.EXISTING), menu);
        addMenuItemAndButton(ButtonType.SAVE_PROJECT, e -> applicationController.saveMenuItemActionPerformed(), menu);
        addMenuItemAndButton(ButtonType.SAVE_PROJECT_AS, e -> applicationController.saveAsMenuItemActionPerformed(), menu);
        addMenuItemAndButton(ButtonType.PACKAGE_PROJECT, e -> new ExportProjectGUI().setVisible(true), menu);
        addMenuItemAndButton(ButtonType.PROJECT_CLEANER, e -> new ProjectCleaner().setVisible(true), menu);
        addMenuItemAndButton(ButtonType.CLEAR_CONTENT, e -> new ContentCleanerDialog().setVisible(true), menu);
        menu.addSeparator();

        final var screenShotMenu = addMenu(ButtonType.SCREEN_SHOT_MENU, menu);
        if (screenShotMenu != null)
        {
            addMenuItemAndButton(ButtonType.SCREEN_SHOT_APPLICATION, e -> new ApplicationImageCaptureTool().startCapture(), screenShotMenu);
            addMenuItemAndButton(ButtonType.SCREEN_SHOT_GLOBE, e -> snapshotUtils.createSnapShotLocationPrompt(SnapshotFormat.JPEG), screenShotMenu);
            addMenuItemAndButton(ButtonType.SCREEN_SHOT_GEOTIFF, e -> snapshotUtils.createSnapShotLocationPrompt(SnapshotFormat.GEOTIFF), screenShotMenu);
            menu.addSeparator();
        }

        addMenuItemAndButton(ButtonType.PROJECT_SETTINGS, e -> new ProjectSettingsDialog().setVisible(true), menu);
        pluginPreferencesItem = addMenuItemAndButton(ButtonType.PLUGIN_PREFERENCES, e -> new PluginPreferencesDialog().setVisible(true), menu);
        addMenuItemAndButton(ButtonType.SETTINGS, e -> uiSpaceManager.toggleComponent(CoreComponentType.PREFERENCES_PANEL), menu);
        addMenuItemAndButton(ButtonType.QUIT, e -> applicationController.shutdown(true), menu);
    }

    /**
     * Creates a new menu item and adds it to the provided menu, using the button type as the source of the text and 
     * icon. Also creates a toolbar button and makes it available to be added to the toolbar (though it is not added by
     * default).
     *
     * @param buttonType The button type associated with the menu item (used for the display text and icon)
     * @param actionListener The listener to notify when the menu item is selected
     * @param menu The menu to which to add the new menu item
     * @return the created menu item or {@code null} if the item should not be displayed
     * @since 3.4 (return value added in 5.4)
     */
    @Nullable
    private JMenuItem addMenuItemAndButton(@Nonnull ButtonType buttonType, @Nonnull ActionListener actionListener, @Nonnull JMenu menu)
    {
        if (hiddenEntries.contains(buttonType.name())) return null;

        final JMenuItem menuItem = new JMenuItem(buttonType.getDisplayText());
        menuItem.setIcon(UIResources.getStandardSizedIcon(buttonType.getImageUrl()));
        menuItem.addActionListener(actionListener);
        menu.add(menuItem);

        // Make a toolbar button available to be included in configurations if desired
        toolbarConfigManager.addReferenceButton(ToolbarButtonBuilder.createCoreButtonBuilder(buttonType, buttonType.requireDisplayText())
                .addActionListener(actionListener)
                .build());

        return menuItem;
    }

    /**
     * Creates a new menu and adds it to the provided menu.
     *
     * @param buttonType The button type associated with the menu (used for the display text and icon)
     * @param menu The menu to which to add the new menu item
     * @return The newly created menu or {@code null} if the menu should not be displayed
     * @since 5.7 (changed signature)
     */
    @SuppressWarnings("SameParameterValue")
    @Nullable
    private JMenu addMenu(@Nonnull ButtonType buttonType, @Nonnull JMenu menu)
    {
        if (hiddenEntries.contains(buttonType.name())) return null;

        final var newMenu = new JMenu(buttonType.getDisplayText());
        newMenu.setIcon(UIResources.getStandardSizedIcon(buttonType.getImageUrl()));
        menu.add(newMenu);
        return newMenu;
    }

    /**
     * Popup listener to enable/disable the plugin preferences menu item based on whether any custom panels have
     * been registered with {@link gov.takx.api.ui.IPreferenceSpaceManager}.
     * 
     * @since 5.4
     */
    private class PluginPreferencesMenuEnabler implements PopupMenuListener
    {
        @Override
        public void popupMenuWillBecomeVisible(PopupMenuEvent e)
        {
            if (pluginPreferencesItem == null) return;

            final boolean enabled = preferenceSpaceManager.hasCustomPanels();
            pluginPreferencesItem.setEnabled(enabled);
            pluginPreferencesItem.setToolTipText(enabled ? "" : "No plugins have provided preferences");
        }

        @Override
        public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
        {
            // don't care
        }

        @Override
        public void popupMenuCanceled(PopupMenuEvent e)
        {
            // don't care
        }
    }
}
