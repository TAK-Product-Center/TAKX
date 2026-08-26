package gov.takx.core.view.gui.routes;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.ctc.wstx.stax.WstxInputFactory;
import com.ctc.wstx.stax.WstxOutputFactory;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import gov.tak.api.comms.takserver.IExtendedDataPackageManager;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.core.comms.cot.CotExporterManager;
import gov.takx.api.annotation.UiEventBus;
import gov.takx.api.event.EditorDisarmedEvent;
import gov.takx.api.event.FinalizeShapeEvent;
import gov.takx.api.event.InitializeDrawToolEvent;
import gov.takx.api.event.mapobject.MapObjectAdded;
import gov.takx.api.event.mapobject.MapObjectRemoved;
import gov.takx.api.event.project.ProjectClosing;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.globe.IGlobeMapObjectEditor;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.IRouteMapObject;
import gov.takx.api.mapobject.IShapeMapObject;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.mapobject.MapToolType;
import gov.takx.api.navigation.IRouteNavigationManager;
import gov.takx.api.notification.INotificationService;
import gov.takx.api.notification.Notification;
import gov.takx.api.persist.IMapObjectFinder;
import gov.takx.api.routes.IRouteEditListener;
import gov.takx.api.routes.IRouteListener;
import gov.takx.api.routes.IRoutesManager;
import gov.takx.api.shapes.IRoute;
import gov.takx.api.ui.ComponentPosition;
import gov.takx.commons.lang.ExceptionUtils;
import gov.takx.commons.swing.AlwaysOnTopFileChooserFactory;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.swing.EscapeKeyListener;
import gov.takx.core.globe.GlobeRouteMapObjectEditor;
import gov.takx.core.mapobject.gui.SendAsCotDialog;
import gov.takx.core.mapobject.gui.SendAsDataPackage;
import gov.takx.core.mapobject.gui.widgets.ContextMenuIcon;
import gov.takx.core.navigation.NavigationConstants;
import gov.takx.core.routes.RouteExportService;
import gov.takx.core.view.gui.CoreComponentType;
import gov.takx.core.view.gui.IExtendedUISpaceManager;
import gov.takx.platform.KeyUtils;
import gov.takx.platform.ui.ComponentDescriptor;
import gov.takx.platform.ui.ComponentDescriptorBuilder;
import gov.takx.platform.ui.ResourceDirectory;
import gov.takx.platform.ui.UIResources;
import gov.takx.platform.utils.UnitIdHelper;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import java.awt.event.ActionEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Manager handling creation, editing, and deletion of TAK routes.
 *
 * @since 3.3 (renamed from RoutesController in 3.4)
 */
@ApplicationScoped
public class RoutesManager implements IRoutesManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final ImageIcon EDIT_ROUTE_MENU_ICON = UIResources.getStandardSizedIcon(ResourceDirectory.TAK_ASSETS_PNG, "Edit_36x36.png");
    private static final ImageIcon EDIT_ROUTE_MENU_ICON_BIG = UIResources.getFeatureIcon(ResourceDirectory.TAK_ASSETS_PNG, "Edit_36x36.png");

    private static final String OVERLAY_STRING = "Click map to add point.  Double click to add a check point.  Ctrl+click to remove point. Shift+click to promote an existing point. ESC to cancel";
    private static final String EDIT_ROUTE_STRING = "Edit Route";
    private static final String NAVIGATE_ROUTE_STRING = "Navigate Route";

    private static final XmlMapper XML_MAPPER = new XmlMapper(new WstxInputFactory(), new WstxOutputFactory());

    private final KeyListener escapeListener = new EscapeKeyListener(this::cancelRouteEditing);
    private final List<IRouteEditListener> routeEditListeners = new CopyOnWriteArrayList<>();
    private final List<IRouteListener> routeListeners = new CopyOnWriteArrayList<>();

    private final IExtendedGlobeManager globeManager;
    private final IMapObjectManager mapObjectManager;
    private final IMapObjectFinder mapObjectFinder;
    private final EventBus uiEventBus;
    private final IExtendedUISpaceManager uiSpaceManager;
    private final CotExporterManager exporterManager;
    private final UnitIdHelper unitIdHelper;
    private final IRouteNavigationManager navigationManager;
    private final INotificationService notificationService;
    private final IExtendedDataPackageManager dataPackageManager;

    private final ComponentDescriptor detailsPanelDescriptor;

    private RoutesPanel routesPanel;
    private RouteDetailsPanel detailsPanel;

    private IRoute newRouteProperties;
    private IRoute currentEditingRoute;

    static
    {
        XML_MAPPER.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY).enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Inject
    RoutesManager(IExtendedGlobeManager globeManager, IMapObjectManager mapObjectManager, IMapObjectFinder mapObjectFinder,
                  @UiEventBus EventBus uiEventBus, IExtendedUISpaceManager uiSpaceManager, CotExporterManager exporterManager,
                  UnitIdHelper unitIdHelper, IRouteNavigationManager navigationManager, INotificationService notificationService,
                  IExtendedDataPackageManager dataPackageManager)
    {
        this.globeManager = globeManager;
        this.mapObjectManager = mapObjectManager;
        this.mapObjectFinder = mapObjectFinder;
        this.uiEventBus = uiEventBus;
        this.uiSpaceManager = uiSpaceManager;
        this.exporterManager = exporterManager;
        this.unitIdHelper = unitIdHelper;
        this.navigationManager = navigationManager;
        this.notificationService = notificationService;
        this.dataPackageManager = dataPackageManager;

        detailsPanelDescriptor = new ComponentDescriptorBuilder(RouteDetailsPanel.class, RouteDetailsPanel.PANEL_TITLE)
                .setComponentPosition(ComponentPosition.RIGHT)
                .setIcon(UIResources.getUrl(PLATFORM_ICONS, "route.png"))
                .setToolbarButtonVisible(false)
                .setPinned(true)
                .setHidePin(false)
                .build();
    }

    /**
     * @param routesPanel The {@link RoutesPanel} that UI updates will be passed to
     * @since 3.4
     */
    public void setRoutesPanel(@Nonnull RoutesPanel routesPanel)
    {
        this.routesPanel = routesPanel;
    }

    /**
     * Notified by {@link IMapObjectManager} when a new map object is added.
     *
     * @param event The event providing the added map object
     */
    @SuppressWarnings("unused")
    private void onMapObjectAdded(@Observes MapObjectAdded event)
    {
        if (event.getMapObject() instanceof IRouteMapObject routeMapObject)
        {
            ExceptionUtils.safeDoOperation(routeListeners, l -> l.onRouteAdded(routeMapObject));
            addMenuItem(routeMapObject);
        }
    }

    /**
     * Adds a menu item to an {@link IRouteMapObject} that will open the {@link RouteDetailsPanel} for the specified route
     *
     * @param routeMapObject The map object to add a menu item for.
     */
    private void addMenuItem(@Nonnull IRouteMapObject routeMapObject)
    {
        logger.trace("Adding menu item for route map object {}", routeMapObject);

        final JMenuItem editMenuItem = new JMenuItem(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                openRouteDetailsPanel(routeMapObject);
                // Only start editing if the route is marked editable
                if (routeMapObject.isEditable()) startRouteEditing(routeMapObject);
            }
        });
        editMenuItem.setIcon(new ContextMenuIcon(EDIT_ROUTE_MENU_ICON.getImage(), EDIT_ROUTE_MENU_ICON_BIG.getImage()));
        editMenuItem.setToolTipText(EDIT_ROUTE_STRING);
        editMenuItem.setName(EDIT_ROUTE_STRING);
        editMenuItem.setText(EDIT_ROUTE_STRING);
        editMenuItem.getAction().putValue(Action.NAME, EDIT_ROUTE_STRING);
        routeMapObject.getController().getMenuSpaceManager().addDevicePopupMenuComponent(editMenuItem);

        final JMenuItem navigationMenuItem = new JMenuItem(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                navigateRoute(routeMapObject);
            }
        });
        navigationMenuItem.setIcon(new ContextMenuIcon(NavigationConstants.NAVIGATE_TO_ICON.getImage(), NavigationConstants.NAVIGATE_TO_ACTION_BUTTON_ICON.getImage()));
        navigationMenuItem.setToolTipText(NAVIGATE_ROUTE_STRING);
        navigationMenuItem.setName(NAVIGATE_ROUTE_STRING);
        navigationMenuItem.setText(NAVIGATE_ROUTE_STRING);
        navigationMenuItem.getAction().putValue(Action.NAME, NAVIGATE_ROUTE_STRING);
        routeMapObject.getController().getMenuSpaceManager().addDevicePopupMenuComponent(navigationMenuItem);
    }

    /**
     * Notified by {@link IMapObjectManager} when a new map object is added.
     *
     * @param event The event providing the added map object
     */
    @SuppressWarnings("unused")
    private void onMapObjectRemoved(@Observes MapObjectRemoved event)
    {
        logger.trace("Saw {}", event);

        if (event.getMapObject() instanceof IRouteMapObject mapObject)
        {
            ExceptionUtils.safeDoOperation(routeListeners, l -> l.onRouteRemoved(mapObject));

            if (detailsPanel != null && mapObject.getKey().equals(detailsPanel.getRoute().getKey()))
            {
                detailsPanel.releaseResources();
                uiSpaceManager.removeComponent(detailsPanel);
            }
        }
    }

    /**
     * Notified when the current project is closing.
     *
     * @param event The event providing the added map object
     */
    @SuppressWarnings("unused")
    private void onProjectClosing(@Observes ProjectClosing event)
    {
        if (detailsPanel != null) detailsPanel.finishEditing();
    }

    @Nullable
    @Override
    public IRoute findRouteByKey(@Nonnull String key)
    {
        return mapObjectFinder.findMapObject(key) instanceof IRouteMapObject route ? route : null;
    }

    @Override
    public @Nonnull List<IRoute> getRoutes()
    {
        return mapObjectFinder.findMapObjectsOfType(IRouteMapObject.class).stream()
                .map(IRoute.class::cast)
                .toList();
    }

    @Override
    public void startNewRouteEditing(IRoute routeProperties)
    {
        newRouteProperties = routeProperties;

        final var globe = getLastFocusedGlobe();

        // Clear any active tool before calling configureUisForEditing(); otherwise, deactivation of a previously
        // selected tool will clear the route UI
        globe.setMapTool(MapToolType.NONE);
        configureUisForEditing(false);
        globe.setMapTool(MapToolType.DRAWING_ROUTE);
    }

    @Override
    public void startRouteEditing(@Nonnull IRoute route)
    {
        logger.debug("Start editing an existing route: {}", route);

        if (!route.isEditable())
        {
            logger.error("Unable to edit route {} as it is not currently editable.", route);
            throw new IllegalStateException("Route " + route.getName() + " is not currently editable.");
        }
        // First need to clear selection, edit won't work if we try to set selection to something that was already selected
        mapObjectManager.setSelected(null);
        mapObjectManager.setSelected(route.getKey());

        notifyRouteEdit(route, false);

        if (getCurrentEditor() instanceof GlobeRouteMapObjectEditor editor) // Can be null during tests
        {
            configureUisForEditing(true);
            editor.enableEditing();
        }
    }

    @Override
    public void cancelRouteEditing()
    {
        completeEditing(true);
    }

    @Override
    public void finishRouteEditing()
    {
        completeEditing(false);
    }

    /**
     * Finish/cancel the current route editing operation.
     *
     * @param canceled {@code true} to cancel the edit, {@code false} to complete it normally
     * @since 3.4
     */
    private void completeEditing(boolean canceled)
    {
        if (getCurrentEditor() instanceof GlobeRouteMapObjectEditor routeEditor && routeEditor.isArmed())
        {
            if (canceled)
            {
                routeEditor.cancelCreation(); // Explicitly cancel so we restore previous state
            } else
            {
                // If editing an existing shape, and we no longer have sufficient control points, warn the user.
                if (currentEditingRoute != null && currentEditingRoute.getShapeMapObject() != null && currentEditingRoute.getControlPoints().size() < 2)
                {
                    AlwaysOnTopJOptionPane.showErrorDialog("Routes must have at least 2 control points; cannot apply edit changes.",
                            "Invalid Route Edits");
                    return;
                }
            }

            getLastFocusedGlobe().setMapTool(MapToolType.NONE);
            // Need to wait until after the map tool is cleared to fire listeners because the above line triggers the MapObjectAdded event
            notifyRouteCompletion(canceled);

            // If we don't deselect at this point, if you click on the object without selecting something else then
            // the GlobeMapObjectManager won't receive a MapObjectSelectedEvent and enable the drawing tool correctly
            mapObjectManager.setSelected(null);
            globeManager.setSelectListenersActive(true);

        }
    }

    @Override
    @Nullable
    public IRoute getCurrentlyEditingRoute()
    {
        return currentEditingRoute;
    }

    /**
     * Notifies the UI that route creation was finished.
     */
    private void onRouteCreationEnded()
    {
        uiEventBus.unregister(this);
        globeManager.hideGlobeMessage();
        getLastFocusedGlobe().getWorldWindow().getInputHandler().removeKeyListener(escapeListener);
        if (routesPanel != null) routesPanel.finishRouteDrawing();
        if (detailsPanel != null) detailsPanel.finishEditing();
    }

    @Override
    @Nonnull
    public IRouteMapObject addRoute(@Nonnull IRoute route)
    {
        if (route.getShapeMapObject() != null) throw new IllegalArgumentException("Route must not already be associated with a Map Object: " + route.getShapeMapObject());
        if (route.getKey() != null) throw new IllegalArgumentException("Route must not have a pre-assigned unique key; found " + route.getKey());
        if (route.getControlPoints().size() < 2) throw new IllegalArgumentException("Routes must have at least 2 control points");

        setRouteNameIfNeeded(route);

        final String uid = UUID.randomUUID().toString();
        final MapObjectSpec spec = MapObjectSpec.builder(KeyUtils.TOOL_FAMILY, IRouteMapObject.ROUTE_TYPE, uid)
                .uid(uid)
                .location(route.getLocation())
                .name(route.getName())
                .initializer(mo -> ((IShapeMapObject<?>) mo).setAssociatedShape(route))
                .build();

        return (IRouteMapObject) mapObjectManager.getOrCreateMapObject(spec);
    }

    /**
     * If the given route doesn't have a name, assign a user-friendly name, of the form "Route n".
     * @since 3.4
     */
    private void setRouteNameIfNeeded(IRoute route)
    {
        if (route.getName().isBlank())
        {
            route.setName("Route " + unitIdHelper.getNextFriendlyUnitIdNoUpdate(KeyUtils.TOOL_FAMILY, IRouteMapObject.ROUTE_TYPE));
        }
    }

    @Override
    public void deleteRoute(@Nonnull String routeKey) throws IllegalArgumentException
    {
        logger.debug("Requesting deletion of route : {}", routeKey);

        final IMapObject mapObject = mapObjectFinder.findMapObject(routeKey);

        if (mapObject == null)
        {
            throw new IllegalArgumentException("No route with key: " + routeKey);
        } else if (!(mapObject instanceof IRouteMapObject))
        {
            throw new IllegalArgumentException("Map Object with that key is not a route, has key" + mapObject.getKey());
        }

        mapObjectManager.deleteMapObject(mapObject);
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void finalizeShape(FinalizeShapeEvent event)
    {
        onRouteCreationEnded();
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void editorDisarmed(EditorDisarmedEvent event)
    {
        onRouteCreationEnded();
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void initializeToolMapObject(InitializeDrawToolEvent event)
    {
        // Check for a newly created route (spawned when user creates the first route point on the map)
        if (event.mapObject() instanceof IRoute route)
        {
            copyNewRouteProperties(route);
            setRouteNameIfNeeded(route);
            notifyRouteEdit(route, true);
        }
    }

    /**
     * Initialize the given model with our new route model properties.
     * @since 3.4
     */
    private void copyNewRouteProperties(IRoute routeModel)
    {
        routeModel.setLineColor(newRouteProperties.getLineColor());
        routeModel.setLineWidth(newRouteProperties.getLineWidth());
        routeModel.setDirection(newRouteProperties.getDirection());
        routeModel.setMethod(newRouteProperties.getMethod());
        routeModel.setRouteType(newRouteProperties.getRouteType());
        routeModel.setRouteOrder(newRouteProperties.getRouteOrder());
        routeModel.setPrefix(newRouteProperties.getPrefix());

        final String name = newRouteProperties.getName();
        if (!name.isBlank()) routeModel.setName(name);
    }

    /**
     * Configure UI elements when we are creating or editing a route.
     *
     * @param editingExisting {@code true} if we are editing an existing route, {@code false} if we are creating a new one
     */
    private void configureUisForEditing(boolean editingExisting)
    {
        globeManager.setSelectListenersActive(false);
        globeManager.showGlobeMessage(OVERLAY_STRING);
        getLastFocusedGlobe().getWorldWindow().getInputHandler().addKeyListener(escapeListener);
        uiEventBus.register(this);

        if (routesPanel != null) routesPanel.startRouteDrawing();

        if (detailsPanel != null)
        {
            if (editingExisting)
            {
                detailsPanel.startRouteEditing();
            } else
            {
                detailsPanel.disableEditing();
            }
        }
    }

    /**
     * Sets the {@link IRoute} for the {@link RouteDetailsPanel} and then opens it.
     *
     * @param routeModel The route to display
     */
    protected void openRouteDetailsPanel(@Nonnull IRoute routeModel)
    {
        if (detailsPanel == null) detailsPanel = new RouteDetailsPanel();
        detailsPanel.setRoute(routeModel);
        uiSpaceManager.addComponent(detailsPanelDescriptor, detailsPanel);
        uiSpaceManager.showComponent(detailsPanel);
    }

    /**
     * Sends the provided {@link IRoute}'s {@link IShapeMapObject} as a {@link CotEvent} directly to contacts or as a
     * broadcast.
     *
     * @param routeModel The route to send
     */
    public void publishRouteAsCot(@Nonnull IRoute routeModel)
    {
        final String routeName = routeModel.getName();

        new SendAsCotDialog(
                routeName,
                () -> Collections.singletonList(exporterManager.export(routeModel.getShapeMapObject())),
                () -> logger.warn("Unable to send CoT for route : {}", routeName))
                .setVisible(true);
    }

    /**
     * Makes the {@link RoutesPanel} visible.
     */
    public void openRoutesPanel()
    {
        uiSpaceManager.showComponent(CoreComponentType.ROUTES_PANEL);
    }

    @Override
    public void addRouteListener(@Nonnull IRouteListener listener)
    {
        routeListeners.add(listener);
    }

    @Override
    public void removeRouteListener(@Nonnull IRouteListener listener)
    {
        routeListeners.remove(listener);
    }

    @Override
    public void addRouteEditListener(@Nonnull IRouteEditListener listener)
    {
        routeEditListeners.add(listener);
    }

    @Override
    public void removeRouteEditListener(@Nonnull IRouteEditListener listener)
    {
        routeEditListeners.remove(listener);
    }

    private IApplicationGlobe getLastFocusedGlobe()
    {
        return globeManager.getLastFocusedGlobe();
    }

    private IGlobeMapObjectEditor getCurrentEditor()
    {
        return getLastFocusedGlobe().getGlobeMapObjectManager().getCurrentEditor();
    }

    /**
     * Notify all {@link IRouteEditListener route edit listeners} that an edit operation has started.
     *
     * @param route Route being edited (may be incomplete if {@code newRoute} is {@code true}
     * @param newRoute {@code true} if this is a new route, {@code false} if editing an existing route
     * @since 3.4
     */
    private void notifyRouteEdit(IRoute route, boolean newRoute)
    {
        currentEditingRoute = route;
        final Consumer<IRouteEditListener> notifier = newRoute
                ? l -> l.onEditNewRoute(route)
                : l -> l.onEditExistingRoute(route);

        ExceptionUtils.safeDoOperation(routeEditListeners, notifier);
    }

    /**
     * Notify all {@link IRouteEditListener route edit listeners} that an edit operation has completed.
     *
     * @param canceled {@code true} if the edit was canceled, {@code false} if editing was completed normally
     * @since 3.4
     */
    private void notifyRouteCompletion(boolean canceled)
    {
        ExceptionUtils.safeDoOperation(routeEditListeners, l -> l.onEditComplete(currentEditingRoute, canceled));
        currentEditingRoute = null;
    }

    @Override
    public void navigateRoute(@Nonnull IRoute route)
    {
        navigationManager.navigateToRoute(route, null);
    }

    /**
     * Export a route in a given format. Prompts the user to select a save folder location.
     * The file name is determined by the route name and export time.
     * @param route the {@link IRoute}
     * @param type the file export type. Accepted types are limited to {@link RouteExportService.FileType}.
     *
     * @since 5.8
     */
    public void exportRoute(@Nonnull IRoute route, @Nonnull RouteExportService.FileType type)
    {
        JFileChooser fc = AlwaysOnTopFileChooserFactory.getFileChooser(this);
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setMultiSelectionEnabled(false);
        fc.setAcceptAllFileFilterUsed(false);
        if (fc.showSaveDialog(JFrame.getFrames()[0]) != JFileChooser.APPROVE_OPTION) return;
        exportRoute(route, type, fc.getSelectedFile());
    }

    /**
     * Export a route in a given format to the provided save file. The file name is determined by the route name and export time.
     * @param route the {@link IRoute}
     * @param type the file export type. Accepted types are limited to {@link RouteExportService.FileType}
     * @param saveFolder the folder to save the route to
     *
     * @since 5.8
     */
    public void exportRoute(@Nonnull IRoute route, @Nonnull RouteExportService.FileType type, @Nonnull File saveFolder)
    {
        LocalDateTime utc = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
        String fileName = route.getName().replace(' ', '_') + "." + utc.getDayOfMonth() + "." + utc.format(RouteExportService.TIME_FORMAT);
        final File saveFile = new File(saveFolder, fileName + "." + type.name().toLowerCase(Locale.ROOT));
        Consumer<Exception> errorNotification = e -> {
            String errorMessage = "Exception exporting the Route to %s".formatted(type);
            notificationService.show(Notification.builder(errorMessage).setType(Notification.Type.Error).build());
            logger.error(errorMessage, e);
        };

        switch (type)
        {
            case GPX ->
            {
                try
                {
                    XML_MAPPER.writeValue(saveFile, RouteExportService.buildRouteGpx(route, utc));
                } catch (IOException e)
                {
                    errorNotification.accept(e);
                    return;
                }
            }
            case KML, KMZ ->
            {
                Kml kml = RouteExportService.buildRouteKml(route, fileName, utc);
                if (type == RouteExportService.FileType.KML)
                {
                    try
                    {
                        kml.marshal(saveFile);
                    } catch (IOException e)
                    {
                        errorNotification.accept(e);
                        return;
                    }
                } else
                {
                    // Add KML file to KMZ zip
                    try (ZipOutputStream zos = FileSystemUtils.getZipOutputStream(saveFile))
                    {
                        zos.putNextEntry(new ZipEntry("doc.kml")); // default fileName for inner file
                        if (!kml.marshal(zos))
                        {
                            // The KML file couldn't be marshalled, so we need to delete the empty zip file we ended up creating.
                            zos.close();
                            saveFile.delete();
                        }
                        zos.closeEntry();
                    } catch (IOException e)
                    {
                        errorNotification.accept(e);
                        return;
                    }
                }
            }
        }

        int response = AlwaysOnTopJOptionPane.showOptionDialog("Route exported as %s to %s".formatted(type, saveFile.getAbsolutePath()),
                "Route Exported as " + type, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION, null,
                new String[]{"Send to Contacts", "Done"}, JOptionPane.YES_OPTION);
        if (response != JOptionPane.YES_OPTION) return; // response will always be CLOSED_OPTION, so no need for headless check here

        Runnable errorHandler = () -> notificationService.show(Notification.builder("Error in sending the attachment DataPackage for " + fileName, Notification.Type.Error).build());
        new SendAsDataPackage(fileName,
                () -> {
                    try
                    {
                        File dataPackage = dataPackageManager.createNewTempDataPackage(fileName + "_" + UUID.randomUUID() + ".zip");
                        dataPackageManager.addFileToDataPackage(dataPackage, saveFile);
                        return dataPackage;
                    } catch (IOException e)
                    {
                        return null;
                    }
                },
                errorHandler, errorHandler).setVisible(true);
    }
}
