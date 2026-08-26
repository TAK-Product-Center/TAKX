package gov.takx.core.view.gui.menus;

import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.event.project.ProjectClosing;
import gov.takx.api.event.project.ProjectOpened;
import gov.takx.api.globe.IFavorite;
import gov.takx.api.globe.IFavoritesListener;
import gov.takx.api.globe.IFavoritesManager;
import gov.takx.api.globe.IProjectFavoritesManager;
import gov.takx.api.globe.IUserFavoritesManager;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.api.toolbar.TabType;
import gov.takx.commons.swing.ButtonAction;
import gov.takx.commons.swing.ErrorReportingSwingWorker;
import gov.takx.commons.swing.IPopupMenuProvider;
import gov.takx.core.globe.layers.worldwind.HTTPPasswordManager;
import gov.takx.core.kml.KmlExporter;
import gov.takx.core.placemarkoverlay.PlacemarkOverlay;
import gov.takx.core.placemarkoverlay.PlacemarkOverlayManager;
import gov.takx.core.toolbar.ToolbarButton;
import gov.takx.core.toolbar.ToolbarSeparator;
import gov.takx.core.view.gui.StandardDialogManager;
import gov.takx.core.view.gui.cachecleaner.CacheCleanerDialog;
import gov.takx.core.view.gui.favorites.AddFavoriteDialog;
import gov.takx.core.view.gui.favorites.ManageFavoritesDialog;
import gov.takx.core.view.gui.maps.AddImageryOverlayDialog;
import gov.takx.core.view.gui.maps.AddKMLMapObjectsDialog;
import gov.takx.core.view.gui.maps.AddKMLOverlayDialog;
import gov.takx.core.view.gui.maps.AddShapeOverlayDialog;
import gov.takx.core.view.gui.maps.ConnectToImageryServerDialog;
import gov.takx.core.view.gui.maps.LoadElevationFilesDialog;
import gov.takx.core.view.gui.maps.LoadImageryFilesDialog;
import gov.takx.core.view.gui.maps.LoadModelFilesDialog;
import gov.takx.core.view.gui.maps.exporters.dataset.MapSetExportDialog;
import gov.takx.core.view.gui.maps.terraindiscovery.controller.TerrainDiscoveryController;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 */
@PseudoBean
public class MapsToolbarTabWrapper extends AToolbarTabWrapper
{
    private static final String ADD_FAVORITE_TOOLTIP = "Save your current view and location as a favorite place. Favorites can be saved with the current project or associated with this computer for use across all projects.";
//    private static final String CACHE_CLEANER_TOOLTIP = "Clean the directory of stored imagery on this computer to create more available space. Warning: Map data that is removed from the Map Set (store location) cannot be retrieved unless the original source is reconnected or the map data is reinstalled.";
    private static final String CONNECT_TO_IMAGERY_SERVER_TOOLTIP = "Connect to an imagery server to stream imagery and maps from that location. Note: Whatever imagery you view will be stored on this computer for future use. If you disconnect from the imagery server, only this previously viewed imagery will be available. Use the Map Set Builder to download all of the imagery of an area for offline use.";
    private static final String LOAD_IMAGERY_FILES_TOOLTIP = "Load source imagery files to be displayed on the globe.";
    private static final String LOAD_MODEL_FILES_TOOLTIP = "Load source 3D model files to be displayed on the globe.";
    private static final String LOAD_ELEVATION_FILES_TOOLTIP = "Load source elevation files to be displayed on the globe.";
    private static final String IMAGE_OVERLAY_TOOLTIP = "Overlay an Image onto the surface of the globe. Image overlays are not geographically anchored and can be stretched or modified to your preferences.";
    private static final String KML_KMZ_IMPORT_TOOLTIP = "Overlay a KML or KMZ file onto the surface of the globe either as an overlay or as map objects. KML/KMZ overlay data can be turned on or off as one layer.";
    private static final String KML_KMZ_EXPORT_TOOLTIP = "Export everything possible on the globe to KML/KMZ";
    private static final String MANAGE_FAVORITES_TOOLTIP = "Edit a favorite place or change it between project and user favorites.";
    private static final String MAP_SET_BUILDER_TOOLTIP = "Download Imagery or Elevation data, from an Imagery Server, for a user-defined Area of Interest. This will build a Map Set, in a user-specified location, which can then be used in disconnected operations.";
    private static final String PLACEMARKS_TOOLTIP = "Overlay a file and use placemarks to represent the location of data on the globe. Placemark overlays can be turned on or off together as one layer.";
    private static final String SERVER_PASSWORDS_TOOLTIP = "Cache user names and passwords in memory for password-protected WMS servers. Note: The user names and passwords are NOT encrypted in memory.";
    private static final String SHP_TOOLTIP = "Overlay an SHP file onto the surface of the globe. An SHP overlay can be turned on or off as one layer.";
    private static final String TERRAIN_DISCOVERY_TOOLTIP = "Visualize the resolution of the terrain data in a user-defined area of interest.";

    @Inject
    private KmlExporter kmlExporter;

    @Inject
    private PlacemarkOverlayManager placemarkOverlayManager;

    @Inject
    private IProjectFavoritesManager projectFavoritesManager;

    @Inject
    private TerrainDiscoveryController terrainDiscoveryController;

    @Inject
    private IUserFavoritesManager userFavoritesManager;

    @Inject
    private StandardDialogManager standardDialogManager;

    private CacheCleanerDialog cacheCleanerDialog;
    private FavoritesHandler projectFavoritesHandler;
    private FavoritesHandler userFavoritesHandler;
    private final ActionListener showAddPlacemarkDialogActionListener = e -> placemarkOverlayManager.showAddNewPlacemarkOverlayDialog();

    @PostConstruct
    private void postConstruct()
    {
        userFavoritesHandler = new FavoritesHandler(userFavoritesManager, ButtonType.USER_FAVORITE);
        projectFavoritesHandler = new FavoritesHandler(projectFavoritesManager, ButtonType.PROJECT_FAVORITE);

        createTab(TabType.MAPS, true);
    }

    @Override
    protected void addComponents()
    {
        // Lookup and reuse the Maps & Favorites button added by the Home tab
        tab.addComponent(configurationManager.getReferenceButton(ButtonType.MAPS.requireSingularId()).copy());
        tab.addComponent(new ToolbarSeparator(TabType.MAPS, tab.getComponents().size()));

        tab.addComponent(createButton(ButtonType.CONNECT_TO_IMAGERY_SERVER, e -> standardDialogManager.showDialog(ConnectToImageryServerDialog.class), CONNECT_TO_IMAGERY_SERVER_TOOLTIP));
        tab.addComponent(createButton(ButtonType.LOAD_IMAGERY_FILES, e -> standardDialogManager.showDialog(LoadImageryFilesDialog.class), LOAD_IMAGERY_FILES_TOOLTIP));
        tab.addComponent(createButton(ButtonType.LOAD_MODEL_FILES, e -> standardDialogManager.showDialog(LoadModelFilesDialog.class), LOAD_MODEL_FILES_TOOLTIP));
        tab.addComponent(new ToolbarSeparator(TabType.MAPS, tab.getComponents().size()));

        tab.addComponent(createButton(ButtonType.LOAD_ELEVATION_FILES, e -> standardDialogManager.showDialog(LoadElevationFilesDialog.class), LOAD_ELEVATION_FILES_TOOLTIP));
        tab.addComponent(new ToolbarSeparator(TabType.MAPS, tab.getComponents().size()));

        tab.addComponent(createButton(ButtonType.IMAGE_OVERLAY, e -> AddImageryOverlayDialog.showInstance(), IMAGE_OVERLAY_TOOLTIP));
        tab.addComponent(createButtonWithPopupMenu(ButtonType.KML_KMZ_IMPORT, e -> AddKMLOverlayDialog.showInstance(), new KmlKmzImportPopupMenuProvider(), KML_KMZ_IMPORT_TOOLTIP));
        tab.addComponent(createButton(ButtonType.KML_KMZ_EXPORT, getKMLExportListener(), KML_KMZ_EXPORT_TOOLTIP));
        tab.addComponent(createButton(ButtonType.SHP, e -> AddShapeOverlayDialog.showInstance(), SHP_TOOLTIP));

        tab.addComponent(createButton(ButtonType.MAP_SET_BUILDER, e -> standardDialogManager.showDialog(MapSetExportDialog.class), MAP_SET_BUILDER_TOOLTIP));

        // TODO KMB: Map sets are no longer used in TAK Map Engine. We need to determine if any of the functions provided
        //  by cache cleaner should be retained.
//        tab.addComponent(createButton(ButtonType.CACHE_CLEANER, e -> getCacheCleanerDialog().setVisible(true), CACHE_CLEANER_TOOLTIP));
        tab.addComponent(createButton(ButtonType.TERRAIN_DISCOVERY, e -> terrainDiscoveryController.show(), TERRAIN_DISCOVERY_TOOLTIP));
        tab.addComponent(createButton(ButtonType.SERVER_PASSWORDS, e -> HTTPPasswordManager.getInstance().displayEditor(), SERVER_PASSWORDS_TOOLTIP));
        tab.addComponent(new ToolbarSeparator(TabType.MAPS, tab.getComponents().size()));

        tab.addComponent(createButton(ButtonType.ADD_FAVORITE, e -> new AddFavoriteDialog().setVisible(true), ADD_FAVORITE_TOOLTIP));
        tab.addComponent(createButton(ButtonType.MANAGE_FAVORITES, e -> standardDialogManager.showDialog(ManageFavoritesDialog.class), MANAGE_FAVORITES_TOOLTIP));

        // The following button will be available for custom configs but not added to the default toolbar configuration
        configurationManager.addReferenceButton(createButtonWithPopupMenu(ButtonType.PLACEMARKS,
                showAddPlacemarkDialogActionListener, new PointPlacemarkPopupMenuProvider(), PLACEMARKS_TOOLTIP));
    }

    /**
     * Create an action listener for the KML export button. Exports to KML on a swing worker thread.
     *
     * @return the Action Listener
     * @since 3.4 (moved from AttachmentsToolbarTabWrapper)
     */
    @Nonnull
    private ActionListener getKMLExportListener()
    {
        return actionEvent -> {
            ErrorReportingSwingWorker<Void, Long> worker = new ErrorReportingSwingWorker<>("KML/KMZ Export", "Failed to properly export to KML/KMZ")
            {
                @Override
                protected Void doInBackground()
                {
                    kmlExporter.exportKML();
                    return null;
                }
            };
            worker.execute();
        };
    }

    /**
     * Get and return the cache cleaner dialog, lazily initializing it on first use. Using the same dialog each time it
     * opens allows us to retain the state of the selected directories.
     * <p/>
     * This should be called on the EDT, and as long as it is, no synchronization is needed.
     *
     * @return The cache cleaner dialog
     */
    @Nonnull
    private CacheCleanerDialog getCacheCleanerDialog()
    {
        if (cacheCleanerDialog == null)
        {
            cacheCleanerDialog = new CacheCleanerDialog();
        }

        return cacheCleanerDialog;
    }

    @InvokeOnEDT
    private void onProjectOpened(@Observes ProjectOpened ignored)
    {
        userFavoritesHandler.loadState();
        projectFavoritesHandler.loadState();
    }

    private void onProjectClosing(@Observes ProjectClosing ignored)
    {
        projectFavoritesHandler.clearState();
        userFavoritesHandler.clearState();
    }

    /**
     * Handler for a set of favorites, user or project.
     *
     * @since 3.0 moved from View tab
     */
    private final class FavoritesHandler implements IFavoritesListener
    {
        private final IFavoritesManager favoritesManager;
        private final ButtonType buttonType;
        private final List<IFavorite> favorites = new ArrayList<>();
        private final List<ToolbarButton> favoriteButtons = new ArrayList<>();

        /**
         * @param buttonType Button type to use for constructing buttons
         */
        FavoritesHandler(@Nonnull IFavoritesManager favoritesManager, @Nonnull ButtonType buttonType)
        {
            this.favoritesManager = favoritesManager;
            this.buttonType = buttonType;
        }

        /**
         * Load the given favorites and create buttons for them.  Listen for changes from the manager.
         */
        private void loadState()
        {
            favoritesManager.getBookmarks().forEach(this::addFavorite);
            favoritesManager.addListener(this);
        }

        /**
         * Remove all of the favorites and their buttons. Stop listening to events.
         */
        private void clearState()
        {
            favoritesManager.removeListener(this);
            favoriteButtons.forEach(button -> configurationManager.removeReferenceButton(button, false));
            favoriteButtons.clear();
            favorites.clear();
        }

        /**
         * Add the given favorite and create a toolbar button for it.
         */
        private void addFavorite(IFavorite favorite)
        {
            final String description = favorite.getDescription();
            final ToolbarButton button = createButton(buttonType, description,
                    e -> getLastFocusedGlobe().goTo(favorite), "Navigate globe to " + description);

            // Make the button available to be added to toolbar configurations. Since the MAPS tab includes this
            // button type as a dynamic button, it will also be added to any MAPS tab instances automatically.
            configurationManager.addReferenceButton(button);

            favorites.add(favorite);
            favoriteButtons.add(button);
        }

        /**
         * Remove the given favorite and its associated button.
         */
        private void removeFavorite(IFavorite favorite)
        {
            final int index = favorites.indexOf(favorite);
            final ToolbarButton button = favoriteButtons.remove(index);
            favorites.remove(favorite);

            configurationManager.removeReferenceButton(button, false);
        }

        /**
         * Update the indicated favorite, this can move a button (change the order), or it can replace the
         * button with new values (like name, tooltip, etc.).
         *
         * @param favorite      Favorite being updated
         * @param favoriteIndex Index of favorite, to determine if it is being moved
         */
        private void updateFavorites(IFavorite favorite, int favoriteIndex)
        {
            final int currentIndex = favorites.indexOf(favorite);

            if (favoriteIndex != currentIndex)
            {
                // Move the favorite and its button
                favorites.add(favoriteIndex, favorites.remove(currentIndex));
                final ToolbarButton buttonToMove = favoriteButtons.remove(currentIndex);
                favoriteButtons.add(favoriteIndex, buttonToMove);

                // TODO KMB: ideally, config manager would handle this, but as it stands right now, there is no good way
                //  to manage component index order on a toolbar tab. This will be addressed in TAKX-1487.
                configurationManager.removeComponentFromTab(buttonToMove, TabType.MAPS);
                configurationManager.addButtonToTab(TabType.MAPS, buttonToMove, favoriteIndex);
            } else
            {
                favoriteButtons.get(favoriteIndex).setDisplayText(favorite.getDescription());
            }
        }

        @Override
        public void onFavoriteAdded(IFavorite favorite)
        {
            addFavorite(favorite);
        }

        @Override
        public void onFavoriteRemoved(IFavorite favorite)
        {
            removeFavorite(favorite);
        }

        @Override
        public void onFavoriteChanged(IFavorite bookmark, int index)
        {
            updateFavorites(bookmark, index);
        }
    }

    /**
     * Provider of the popup menu for the KML/KMZ import button.
     * @since 3.4
     */
    @PseudoBean
    private static class KmlKmzImportPopupMenuProvider implements IPopupMenuProvider
    {
        private final JPopupMenu popupMenu = new JPopupMenu();

        public KmlKmzImportPopupMenuProvider()
        {
            popupMenu.add(new ButtonAction("Import KML/KMZ Overlay", e -> AddKMLOverlayDialog.showInstance()));
            popupMenu.add(new ButtonAction("Import KML/KMZ Map Objects", e -> AddKMLMapObjectsDialog.showInstance()));
        }

        @Override
        public JPopupMenu getPopupMenu()
        {
            return popupMenu;
        }
    }

    /**
     * Producer for the popup menu for the placemark button.
     * @since 3.3 (moved from AttachmentsToolbarTabWrapper in 3.4)
     */
    private class PointPlacemarkPopupMenuProvider implements IPopupMenuProvider
    {
        @Nonnull
        @Override
        public JPopupMenu getPopupMenu()
        {
            JMenuItem newLayerMenuItem = new JMenuItem("Add Placemark Overlay", null);
            newLayerMenuItem.addActionListener(showAddPlacemarkDialogActionListener);

            JMenuItem newPlacemarkMenuItem = new JMenuItem("Add Placemark", null);
            newPlacemarkMenuItem.addActionListener(e -> {
                // TODO KMB: This logic is completely busted. It doesn't include any reference to the layer in the
                //  menu text, nor in the action itself. I wrote a ticket against it previously: TAKX-909.
                // Show instructional popup
                JMenuItem menuItem = new JMenuItem("Choose the placemark's location by clicking on the map.");
                JPopupMenu popup = new JPopupMenu();
                popup.add(menuItem);
                Component globeComponent = getLastFocusedGlobe().getWorldWindow().getComponent();
                Point globeLocation = globeComponent.getLocation();
                popup.show(globeComponent, globeLocation.x + 5, globeLocation.y + 5);

                // Listen for placemark location click
                placemarkOverlayManager.armNewPlacemarkLocationListener();
            });

            JMenuItem allOnMenuItem = new JMenuItem("Show All Placemarks", null);
            allOnMenuItem.addActionListener(e -> placemarkOverlayManager.showAllPlacemarks());

            JMenuItem allOffMenuItem = new JMenuItem("Hide All Placemarks", null);
            allOffMenuItem.addActionListener(e -> placemarkOverlayManager.hideAllPlacemarks());

            JPopupMenu popupMenu = new JPopupMenu();
            popupMenu.add(newLayerMenuItem);

            for (PlacemarkOverlay overlay : placemarkOverlayManager.getLayers())
            {
                if (overlay.isEditable())
                {
                    popupMenu.add(newPlacemarkMenuItem);
                    break;
                }
            }

            popupMenu.addSeparator();
            popupMenu.add(allOnMenuItem);
            popupMenu.add(allOffMenuItem);

            return popupMenu;
        }
    }
}
