package gov.takx.core.globe;

import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.event.project.ProjectClosing;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.pointdropper.CotIcon;
import gov.takx.api.pointdropper.IPointDropperService;
import gov.takx.api.pointdropper.PointDropperConfiguration;
import gov.takx.core.globe.radial.RadialMenuHelper;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Provider of a popup menu offering user actions on terrain (when no other object is under the mouse).
 * This includes quick actions to drop CoT icons, copy the location to the clipboard, and any actions added
 * by plugins.
 *
 * @see #createMenu
 * @since 5.3 (renamed from {@code TerrainPopupMenu} and converted to an app scoped bean
 */
@ApplicationScoped
public class TerrainPopupMenuProvider
{
    // Menu items added via plugin API.
    private static final List<JMenuItem> EXTRA_MENU_ITEMS = new ArrayList<>();

    private final IPointDropperService pointDropperService;

    @Inject
    TerrainPopupMenuProvider(IPointDropperService pointDropperService)
    {
        this.pointDropperService = pointDropperService;
    }

    /**
     * Add a menu item to the terrain popup menu.  This menu is displayed when the user right-clicks on globe terrain.
     *
     * @param menuItem Menu item to add
     * @see #removeTerrainPopupMenuItem
     * @since 1.49
     */
    public void addTerrainPopupMenuItem(@Nonnull JMenuItem menuItem)
    {
        EXTRA_MENU_ITEMS.add(menuItem);
    }

    /**
     * Remove a previously added menu item from the terrain popup menu.
     *
     * @param menuItem Menu item to remove
     * @see #addTerrainPopupMenuItem
     * @since 1.49
     */
    public void removeTerrainPopupMenuItem(@Nonnull JMenuItem menuItem)
    {
        EXTRA_MENU_ITEMS.remove(menuItem);
    }

    /**
     * Clear any registered extra menu items when the project closes.
     * @since 5.3
     */
    private void onProjectClosing(@Observes ProjectClosing ignored)
    {
        EXTRA_MENU_ITEMS.clear();
    }

    /**
     * Construct the popup menu on the given globe.
     *
     * @param globe Globe to support copying the location to the clipboard
     * @param location Location at which the user event initiated this menu
     * @return new popup menu
     */
    public JPopupMenu createMenu(IApplicationGlobe globe, ILocation location)
    {
        final var copyLocationAction = new AbstractAction("Copy Location")
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                globe.copyLocationToClipboard(location);
            }
        };

        final var menu = new JPopupMenu();

        menu.add(RadialMenuHelper.createRadialMenuItem(copyLocationAction));

        // CoT Markers
        Arrays.stream(CotIcon.values())
                .map(cotIcon -> new PlaceMarkerAction(cotIcon, location))
                .map(RadialMenuHelper::createRadialMenuItem)
                .forEach(menu::add);

        // Plugin provided menu items
        EXTRA_MENU_ITEMS.stream()
                .map(RadialMenuHelper::createRadialMenuItem)
                .forEach(menu::add);

        return menu;
    }

    /**
     * Action to place a CoT marker on the globe.
     * @since 5.3
     */
    private class PlaceMarkerAction extends AbstractAction
    {
        private final ILocation location;
        private final PointDropperConfiguration configuration;

        PlaceMarkerAction(CotIcon cotIcon, ILocation location)
        {
            super(cotIcon.getShortName());

            this.location = location;
            configuration = PointDropperConfiguration.builder(cotIcon).useCotNamer().build();

            //noinspection DataFlowIssue - definition can't be null or an exception would have been thrown above
            putValue(Action.SMALL_ICON, new ImageIcon(configuration.pluginDefinition().getIcon()));
        }

        @Override
        public void actionPerformed(ActionEvent ignored)
        {
            pointDropperService.dropPoint(configuration, location);
        }
    }
}
