package gov.takx.core.globe.widgets;

import gov.tak.api.commons.graphics.Font;
import gov.tak.api.commons.graphics.TextFormat;
import gov.tak.api.widgets.IMapWidget;
import gov.tak.platform.ui.MotionEvent;
import gov.tak.platform.widgets.TextWidget;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.RemoveObserver;
import gov.takx.api.geofence.event.GeoFenceAlertAdded;
import gov.takx.api.geofence.event.GeoFenceAlertRemoved;
import gov.takx.api.globe.AWidget;
import gov.takx.api.globe.CoreWidgetNames;
import gov.takx.api.globe.IGlobeDelegate;
import gov.takx.core.geofence.AlertManager;
import gov.takx.core.geofence.AlertOverlaysController;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.lang.invoke.MethodHandles;

/**
 * Widget to display information about GeoFence alerts.  Clicking on the widget will open the GeoFence Alerts panel
 * in the Overlays.
 *
 * @since 5.3
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class GeoFenceAlertsWidget extends AWidget
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final Font FONT = new Font("Segoe UI", Font.Style.Normal, 14);
    private static final int RED_RGB = Color.RED.getRGB();
    private static final int[] LINE_COLORS = {RED_RGB, RED_RGB};
    private static final String MESSAGE_FMT_1 = "Geo Fence Breached\n%s, %s";
    private static final String MESSAGE_FMT_N = "%d alerts\nClick here for details";

    @Inject
    private AlertManager alertManager;

    @Inject
    private AlertOverlaysController alertOverlaysController;

    private final TextWidget textWidget = new TextWidget("", new TextFormat(FONT, 0));

    public GeoFenceAlertsWidget(IGlobeDelegate ignored)
    {
        setName(CoreWidgetNames.GEOFENCE_ALERTS);
        setPadding(4);

        textWidget.setBackground(TextWidget.TRANSLUCENT_BLACK);
        textWidget.setColors(LINE_COLORS);
        textWidget.addOnClickListener(new WidgetClickListener());
        addChildWidget(textWidget);
    }

    @PostConstruct
    private void postConstruct()
    {
        updateText();
    }

    private void onAlertAdded(@Observes GeoFenceAlertAdded event)
    {
        updateText();
    }

    private void onAlertRemoved(@Observes GeoFenceAlertRemoved event)
    {
        updateText();
    }

    /**
     * Update our text and visibility based on the number of alerts.  If zero, become invisible. If one, show text
     * indicating the details of the breach.  If more than one, show text indicating the number of alerts.
     */
    private void updateText()
    {
        final var alerts = alertManager.getAllAlerts();
        final int alertCount = alerts.size();

        logger.debug("Update, alert count {}", alertCount);

        if (alertCount > 0)
        {
            final String text;
            if (alertCount == 1)
            {
                final var alert = alerts.get(0);
                text = String.format(MESSAGE_FMT_1, alert.shape().getName(), alert.mapItem().getName());
            } else
            {
                text = String.format(MESSAGE_FMT_N, alertCount);
            }

            textWidget.setText(text);
            setVisible(true);
        } else
        {
            setVisible(false);
        }
    }

    @RemoveObserver
    @Override
    public void releaseResources()
    {
    }

    /**
     * Listener to detect mouse clicks on the text widget and open the {@code Overlays -> Alerts -> Geo Fences} panel.
     */
    private class WidgetClickListener implements IMapWidget.OnClickListener
    {
        @Override
        public void onMapWidgetClick(IMapWidget mapWidget, MotionEvent motionEvent)
        {
            alertOverlaysController.showAlertsInOverlays();
        }
    }
}
