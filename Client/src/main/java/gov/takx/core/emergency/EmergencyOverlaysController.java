package gov.takx.core.emergency;

import com.google.common.annotations.VisibleForTesting;
import gov.tak.api.maps.PointMapItem;
import gov.takx.api.emergency.EmergencyAlert;
import gov.takx.api.emergency.event.EmergencyAlertAdded;
import gov.takx.api.emergency.event.EmergencyAlertRemoved;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.explorer.Explorable;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.commons.swing.ActionPopupFactory;
import gov.takx.commons.swing.actionlist.ActionListAction;
import gov.takx.core.explorer.IExplorer;
import gov.takx.core.explorer.IExplorerManager;
import gov.takx.core.explorer.common.INonGroupableExplorable;
import gov.takx.core.explorer.common.IPathExplorableGroup;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.core.view.gui.CoreComponentType;
import gov.takx.core.view.gui.IExtendedUISpaceManager;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import java.util.List;

import static gov.takx.core.explorer.controller.path.ExplorerPathConstants.ALERTS_EMERGENCY_PATH;
import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;
import static gov.takx.platform.ui.ResourceDirectory.TAK_ASSETS_ICONS;
import static gov.takx.platform.ui.UIResources.ACTION_BUTTON_IMAGE_SIZE;

/**
 * Controller to handle the entries in Overlays for {@link EmergencyAlert Emergency Alerts}.
 *
 * @since 5.7
 */
@ApplicationScoped
public class EmergencyOverlaysController
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final BufferedImage ALERT_ICON = UIResources.getSizedImage(TAK_ASSETS_ICONS, "alarm-trouble.png", ACTION_BUTTON_IMAGE_SIZE, true);
    private static final Icon DELETE_ICON = UIResources.getSizedIcon(PLATFORM_ICONS, "delete.png", ACTION_BUTTON_IMAGE_SIZE, true);

    private final IExplorerManager explorerManager;
    private final IExtendedUISpaceManager uiSpaceManager;
    private final EmergencyAlertManager alertManager;

    private final Action deleteAction = new ActionListAction<AlertExplorable>("Delete", DELETE_ICON,
            e -> ActionPopupFactory
                    .yesNo("Remove this alert?", (Component) e.getSource(), () -> removeAlertExplorable(e.requireValue()))
                    .show());
    private final List<Action> alertActions = List.of(deleteAction);

    private IExplorer explorer;
    private IPathExplorableGroup emergencyAlertsGroup;

    @Inject
    EmergencyOverlaysController(IExplorerManager explorerManager, IExtendedUISpaceManager uiSpaceManager, EmergencyAlertManager alertManager)
    {
        this.explorerManager = explorerManager;
        this.uiSpaceManager = uiSpaceManager;
        this.alertManager = alertManager;
    }

    /**
     * Open the Overlays panel and select our root alerts group (showing all emergency alerts).
     */
    public void showAlertsInOverlays()
    {
        uiSpaceManager.showComponent(CoreComponentType.OVERLAYS_PANEL);

        if (emergencyAlertsGroup != null)
        {
            explorer.expandTreeToNode(emergencyAlertsGroup);
            explorer.setTreeSelected(emergencyAlertsGroup);
        }
    }

    private void onAlertAdded(@Observes EmergencyAlertAdded event)
    {
        logger.debug("{}", event);

        emergencyAlertsGroup.addChild(new AlertExplorable(event.alert()));
    }

    private void onAlertRemoved(@Observes EmergencyAlertRemoved event)
    {
        //noinspection DuplicatedCode
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

    private void removeAlertExplorable(AlertExplorable explorable)
    {
        alertManager.removeAlert(explorable.alert);
    }

    /**
     * Must start after ExplorerManager.
     */
    private void startService(@Observes @Priority(ServiceOrdering.START_ExplorerManager + 1) StartProjectServices ignored)
    {
        explorer = explorerManager.getDefaultExplorerDelegate();

        emergencyAlertsGroup = explorer.getController().getRoot().getOrCreatePath(ALERTS_EMERGENCY_PATH);
        emergencyAlertsGroup.setSupportsVisibility(false);
        explorer.addExplorable(emergencyAlertsGroup);
        explorer.expandTreeToNode(emergencyAlertsGroup);
        emergencyAlertsGroup.setIconImage(ALERT_ICON);
    }

    private void stopService(@Observes @Priority(ServiceOrdering.START_GenericCoreService) StopProjectServices ignored)
    {
        explorer = null;
        emergencyAlertsGroup = null;
    }

    /**
     * @return unique key for a given alert, combining the item UID and the type of the alert.
     */
    @VisibleForTesting
    @Nonnull
    static String makeAlertUniqueKey(@Nonnull EmergencyAlert alert)
    {
        return String.format("emergency-%s-%s", alert.mapItem().getUid(), alert.type());
    }

    /**
     * Explorable representing a specific {@link EmergencyAlert}.
     */
    private class AlertExplorable extends Explorable implements INonGroupableExplorable, PropertyChangeListener
    {
        private final EmergencyAlert alert;
        private final String detailsText;

        private AlertExplorable(EmergencyAlert alert)
        {
            super(null, alert.callsign(), makeAlertUniqueKey(alert), null, alert.mapItem().getIconImage(), false);
            this.alert = alert;
            detailsText = String.format("%s<br>%s", alert.type(), alertManager.getRangeAndBearing(alert));

            setSupportsDeletion(false);

            // The icon is not set properly at the time we are created, so we need to monitor for changes.
            alert.mapItem().addPropertyChangeListener(IMapObject.ICON, this);
        }

        @Override
        public String getDetailsTextHtml()
        {
            return detailsText;
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            final var mapItem = (PointMapItem) evt.getSource();
            setIconImage(mapItem.getIconImage());
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
