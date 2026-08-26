package gov.takx.core.geofence;

import com.google.common.annotations.VisibleForTesting;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.explorer.Explorable;
import gov.takx.api.explorer.IExplorable;
import gov.takx.api.explorer.IExplorableGroup;
import gov.takx.api.geofence.GeoFence;
import gov.takx.api.geofence.GeoFenceAlert;
import gov.takx.api.geofence.IGeoFenceManager;
import gov.takx.api.geofence.event.GeoFenceAdded;
import gov.takx.api.geofence.event.GeoFenceAlertAdded;
import gov.takx.api.geofence.event.GeoFenceAlertRemoved;
import gov.takx.api.geofence.event.GeoFenceRemoved;
import gov.takx.api.geofence.event.GeoFenceUpdated;
import gov.takx.api.mapobject.IShapeMapObject;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.commons.date.DateTimeParser;
import gov.takx.commons.swing.ActionPopupFactory;
import gov.takx.commons.swing.actionlist.ActionListAction;
import gov.takx.commons.swing.actionlist.ActionListActionEvent;
import gov.takx.core.explorer.IExplorer;
import gov.takx.core.explorer.IExplorerManager;
import gov.takx.core.explorer.common.INonGroupableExplorable;
import gov.takx.core.explorer.common.IPathExplorableGroup;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.core.view.gui.CoreComponentType;
import gov.takx.core.view.gui.IExtendedUISpaceManager;
import gov.takx.core.view.gui.geofence.EditGeoFenceDialog;
import gov.takx.platform.ui.UIResources;
import gov.takx.platform.utils.StandardTimeFormatting;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Action;
import javax.swing.Icon;

import java.awt.Component;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

import static gov.takx.core.explorer.controller.path.ExplorerPathConstants.ALERTS_GEO_FENCES_PATH;
import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;
import static gov.takx.platform.ui.UIResources.ACTION_BUTTON_IMAGE_SIZE;

/**
 * Controller to handle the entries in Overlays for {@link gov.takx.api.geofence.GeoFence geoFences} and
 * {@link gov.takx.api.geofence.GeoFenceAlert alerts} they generate.
 *
 * @since 5.3
 */
@ApplicationScoped
public class AlertOverlaysController
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final BufferedImage GEOFENCE_ALERTS_ICON = UIResources.getSizedImage(PLATFORM_ICONS, "geofence.png", ACTION_BUTTON_IMAGE_SIZE, true);
    private static final BufferedImage GEOFENCE_NO_ALERTS_ICON = UIResources.getSizedImage(PLATFORM_ICONS, "geofence_noalerts.png", ACTION_BUTTON_IMAGE_SIZE, true);
    private static final BufferedImage GEOFENCE_DISABLED_ICON = UIResources.getSizedImage(PLATFORM_ICONS, "geofence_disabled.png", ACTION_BUTTON_IMAGE_SIZE, true);

    private static final Icon DELETE_ICON = UIResources.getSizedIcon(PLATFORM_ICONS, "delete.png", ACTION_BUTTON_IMAGE_SIZE, true);
    private static final Icon EDIT_ICON = UIResources.getSizedIcon(PLATFORM_ICONS, "edit.png", ACTION_BUTTON_IMAGE_SIZE, true);

    private static final DateTimeParser TIME_FORMATTER = new DateTimeParser(StandardTimeFormatting.DEFAULT_TIME_ONLY_FORMAT, DateTimeParser.Style.DateTimeWithZone, ZoneOffset.UTC);

    private final Action dismissAlertAction = new ActionListAction<AlertExplorable>("Click for dismissal options", DELETE_ICON)
            .withEventConsumer(this::dismissAlert);
    private final Action dismissGeoFenceAction = new ActionListAction<GeoFenceExplorableGroup>("Click for dismissal options", DELETE_ICON)
            .withEventConsumer(this::dismissGeofence);
    private final Action editGeoFenceAction = new ActionListAction<GeoFenceExplorableGroup>("Edit this GeoFence", EDIT_ICON)
            .withValueConsumer(this::editGeofence);
    private final List<Action> alertActions = List.of(dismissAlertAction);
    private final List<Action> fenceActions = List.of(dismissGeoFenceAction, editGeoFenceAction);

    private final IExplorerManager explorerManager;
    private final AlertManager alertManager;
    private final IGeoFenceManager geoFenceManager;
    private final IExtendedUISpaceManager uiSpaceManager;
    private final StandardTimeFormatting timeFormatting;

    private IExplorer explorer;
    private IPathExplorableGroup geoFenceAlertsGroup;

    @Inject
    AlertOverlaysController(IExplorerManager explorerManager, AlertManager alertManager, IGeoFenceManager geoFenceManager,
                            IExtendedUISpaceManager uiSpaceManager, StandardTimeFormatting timeFormatting)
    {
        this.explorerManager = explorerManager;
        this.alertManager = alertManager;
        this.geoFenceManager = geoFenceManager;
        this.uiSpaceManager = uiSpaceManager;
        this.timeFormatting = timeFormatting;
    }

    /**
     * Open the Overlays panel and select our root alerts group (showing all GeoFences).
     */
    public void showAlertsInOverlays()
    {
        showAlertsInOverlays(geoFenceAlertsGroup);
    }

    /**
     * Open the Overlays panel and select the given geofence group.
     *
     * @param geoFence GeoFence to show and select in the overlays tree
     */
    public void showAlertsInOverlays(@Nonnull GeoFence geoFence)
    {
        showAlertsInOverlays(getGeoFenceGroup(geoFence));
    }

    /**
     * Open the Overlays panel and select the given group node so its children are shown in the table.
     */
    private void showAlertsInOverlays(@Nullable IExplorable group)
    {
        uiSpaceManager.showComponent(CoreComponentType.OVERLAYS_PANEL);

        if (group != null)
        {
            explorer.expandTreeToNode(group);
            explorer.setTreeSelected(group);
        }
    }

    /**
     * Create an explorer group for the new geofence.
     */
    private void onGeoFenceAdded(@Observes GeoFenceAdded event)
    {
        logger.debug("{}", event);

        geoFenceAlertsGroup.addChild(new GeoFenceExplorableGroup(event.shapeMapObject()));
    }

    /**
     * Update an explorer group for the given geofence.
     */
    private void onGeoFenceUpdated(@Observes GeoFenceUpdated event)
    {
        logger.debug("{}", event);

        final var group = getGeoFenceGroup(event.geoFence());
        if (group != null) group.updateIcon();
    }

    /**
     * Remove the explorer group for the geofence being removed.
     */
    private void onGeoFenceRemoved(@Observes GeoFenceRemoved event)
    {
        logger.debug("{}", event);

        final var group = getGeoFenceGroup(event.geoFence());
        if (group != null)
        {
            group.releaseResources();
            explorer.removeExplorable(group, false);
        }
    }

    /**
     * Add an alert explorable for the new alert.
     */
    private void onAlertAdded(@Observes GeoFenceAlertAdded event)
    {
        logger.debug("{}", event);

        final var alert = event.alert();
        final var group = getGeoFenceGroup(alert.geoFence());
        if (group != null)
        {
            group.addChild(new AlertExplorable(alert));
        } else
        {
            logger.debug("Missing geofence group for: {}", alert.shapeMapObject());
        }
    }

    /**
     * Remove the alert explorable for the alert that was removed.
     */
    private void onAlertRemoved(@Observes GeoFenceAlertRemoved event)
    {
        logger.debug("{}", event);

        final var alert = event.alert();
        final var alertExplorable = explorer.getExplorable(makeAlertUniqueKey(alert));
        if (alertExplorable != null)
        {
            explorer.removeExplorable(alertExplorable, false);
        } else
        {
            logger.debug("Missing alert explorable for: {}", alert);
        }
    }

    /**
     * @return Properly typed geofence group, or {@code null} if not found
     */
    @Nullable
    private GeoFenceExplorableGroup getGeoFenceGroup(GeoFence geoFence)
    {
        return (GeoFenceExplorableGroup) explorer.getExplorable(makeGeoFenceUniqueKey(geoFence));
    }

    /**
     * @return the top level group that holds all geofence alerts
     */
    @VisibleForTesting
    IPathExplorableGroup getGeoFencesGroup()
    {
        return geoFenceAlertsGroup;
    }

    /**
     * @return unique explorable key for a given geofence, combining a prefix and the shape UID
     */
    @VisibleForTesting
    @Nonnull
    static String makeGeoFenceUniqueKey(@Nonnull GeoFence geoFence)
    {
        return String.format("geofence-%s", geoFence.shapeUid());
    }

    /**
     * @return unique key for a given alert, combining the item UID and the time of the alert.
     */
    @VisibleForTesting
    @Nonnull
    static String makeAlertUniqueKey(@Nonnull GeoFenceAlert alert)
    {
        return String.format("alert-%s-%d", alert.mapItem().getUid(), alert.time().getMilliseconds());
    }

    /**
     * @return Display nome for a geofence explorable group
     */
    @VisibleForTesting
    @Nonnull
    static String makeGeoFenceDisplayName(@Nonnull IShapeMapObject<?> shapeMapObject)
    {
        return "'" + shapeMapObject.getName() + "' Alerts";
    }

    /**
     * Update the geofence on the given shape to stop tracking.
     */
    private void stopTracking(IShapeMapObject<?> shapeMapObject)
    {
        final var newGeoFence = GeoFence.builder(shapeMapObject.requireGeoFence()).tracking(false).build();
        geoFenceManager.addOrUpdateGeoFence(newGeoFence);
    }

    /**
     * Must start after ExplorerManager.
     */
    private void startService(@Observes @Priority(ServiceOrdering.START_ExplorerManager + 1) StartProjectServices ignored)
    {
        explorer = explorerManager.getDefaultExplorerDelegate();

        geoFenceAlertsGroup = explorer.getController().getRoot().getOrCreatePath(ALERTS_GEO_FENCES_PATH);
        geoFenceAlertsGroup.setSupportsVisibility(false);
        explorer.addExplorable(geoFenceAlertsGroup);
        explorer.expandTreeToNode(geoFenceAlertsGroup);
    }

    private void stopService(@Observes @Priority(ServiceOrdering.START_GenericCoreService) StopProjectServices ignored)
    {
        explorer = null;
        geoFenceAlertsGroup = null;
    }

    /**
     * Handle the "dismiss" button for a single Alert.  When invoked, it presents the user with a choice of
     * three options: Dismiss alert, stop monitoring, and cancel.
     *
     * @since 5.7
     */
    private void dismissAlert(ActionListActionEvent<AlertExplorable> event)
    {
        //noinspection DataFlowIssue
        var alert = event.getValue().alert;
        final String msg = String.format("%s (%s).  Would you like to dismiss the alert or stop monitoring?",
                alert.message(), timeFormatting.formatApproximateDurationSince(Instant.ofEpochMilli(alert.time().getMilliseconds()), "ago"));

        ActionPopupFactory.builder(msg, (Component) event.getSource())
                .action("Dismiss", "Dismiss this single alert", () -> alertManager.removeAlert(alert))
                .action("Stop Monitoring", "Stop monitoring breaches on this GeoFence", () -> stopTracking(alert.shapeMapObject()))
                .action("Cancel", ActionPopupFactory.DO_NOTHING)
                .build()
                .show();
    }

    /**
     * Handle the "dismiss" button action on a geofence group.  When invoked, it presents the user with a choice of
     * several options: Dismiss alerts, stop monitoring, delete the geofence, and cancel.
     *
     * @since 5.7
     */
    private void dismissGeofence(ActionListActionEvent<GeoFenceExplorableGroup> event)
    {
        //noinspection DataFlowIssue
        var shapeMapObject = event.getValue().shapeMapObject;
        final String shapeUid = shapeMapObject.getUid();
        final int alertCount = alertManager.getAlertCount(shapeUid);
        final String format = alertCount == 1
                ? "'%s' has %d alert. What action would you like to take?"
                : "'%s' has %d alerts. What action would you like to take?";
        final String msg = String.format(format, shapeMapObject.getName(), alertCount);

        ActionPopupFactory.builder(msg, (Component) event.getSource())
                .action("Dismiss Alerts", "Dismiss all alerts for this GeoFence", () -> alertManager.clearAlerts(shapeUid))
                .action("Stop Monitoring", "Stop monitoring breaches on this GeoFence", () -> stopTracking(shapeMapObject))
                .action("Delete", "Delete this GeoFence", () -> geoFenceManager.removeGeoFence(shapeMapObject.requireGeoFence()))
                .action("Cancel", ActionPopupFactory.DO_NOTHING)
                .build()
                .show();
    }

    /**
     * Edit the geofence on a given shape map object.
     *
     * @since 5.7
     */
    private void editGeofence(GeoFenceExplorableGroup explorable)
    {
        new EditGeoFenceDialog(null, explorable.shapeMapObject).setVisible(true);
    }

    /**
     * Explorable Group representing a single {@link GeoFence}.  Children will be individual {@link AlertExplorable alerts}.
     */
    private class GeoFenceExplorableGroup extends Explorable implements IResourceCleaner, IExplorableGroup
    {
        private final IShapeMapObject<?> shapeMapObject;
        private final PropertyChangeListener nameChangeHandler;

        private GeoFenceExplorableGroup(@Nonnull IShapeMapObject<?> shapeMapObject)
        {
            super(null, makeGeoFenceDisplayName(shapeMapObject), makeGeoFenceUniqueKey(shapeMapObject.requireGeoFence()), null, GEOFENCE_NO_ALERTS_ICON, true);

            this.shapeMapObject = shapeMapObject;
            nameChangeHandler = e -> setDisplayName(makeGeoFenceDisplayName(shapeMapObject));
            shapeMapObject.addPropertyChangeListener(nameChangeHandler);
            supportsVisibility = false;
            supportsDeletion = false;
            updateIcon();

            // Sort the alerts based on time, not name
            final var explorerPanel = explorer.getExplorerPanel();
            if (explorerPanel != null) // Can be null during tests
            {
                explorerPanel.registerExplorableComparator(this,
                        Comparator.comparing(node -> ((AlertExplorable) node).alert.time().getMilliseconds()));
            }
        }

        /**
         * Set our icon to one of three states: disabled (not tracking), no alerts, or alerts.
         */
        private void updateIcon()
        {
            final var icon = shapeMapObject.requireGeoFence().tracking()
                    ? getChildrenCount() == 0 ? GEOFENCE_NO_ALERTS_ICON : GEOFENCE_ALERTS_ICON
                    : GEOFENCE_DISABLED_ICON;
            setIconImage(icon);
        }

        @Override
        public String getDetailsTextHtml()
        {
            final int count = getChildrenCount();
            return count == 0 ? "N/A" : (count + (count == 1 ? " Item" : " Items"));
        }

        /**
         * {@inheritDoc}
         * <br>
         * Update our icon and summary text when a child is added.
         */
        @Override
        public boolean addChild(@Nonnull IExplorable child)
        {
            return childrenChanged(() -> super.addChild(child));
        }

        /**
         * {@inheritDoc}
         * <br>
         * Update our icon and summary text when a child is removed.
         */
        @Override
        public boolean removeChild(@Nonnull IExplorable child)
        {
            return childrenChanged(() -> super.removeChild(child));
        }

        /**
         * Perform an action and then update our icon based on the number of children (alerts) present.
         */
        private boolean childrenChanged(Supplier<Boolean> action)
        {
            final boolean result = action.get();
            updateIcon();
            return result;
        }

        @Override
        public void releaseResources()
        {
            shapeMapObject.removePropertyChangeListener(nameChangeHandler);
        }

        @Nonnull
        @Override
        public List<Action> createQuickActions()
        {
            return fenceActions;
        }
    }

    /**
     * Explorable representing a specific {@link GeoFenceAlert}.
     */
    private class AlertExplorable extends Explorable implements INonGroupableExplorable
    {
        private final GeoFenceAlert alert;

        private AlertExplorable(GeoFenceAlert alert)
        {
            super(null, alert.mapItem().getName(), makeAlertUniqueKey(alert), null, alert.mapItem().getIconImage(), false);
            this.alert = alert;
            setSupportsDeletion(false);
        }

        @Override
        public String getDetailsTextHtml()
        {
            return String.format("%s @ %sZ", alert.crossingVerb(), TIME_FORMATTER.format(alert.time().getMilliseconds()));
        }

        @Nonnull
        @Override
        public List<Action> createQuickActions()
        {
            return alertActions;
        }

        @Override
        public boolean supportsVisibility()
        {
            return false;
        }
    }
}
