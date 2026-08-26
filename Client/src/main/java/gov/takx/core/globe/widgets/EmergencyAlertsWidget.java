package gov.takx.core.globe.widgets;

import gov.tak.api.commons.graphics.Font;
import gov.tak.api.commons.graphics.TextFormat;
import gov.tak.platform.widgets.TextWidget;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.RemoveObserver;
import gov.takx.api.emergency.event.EmergencyAlertAdded;
import gov.takx.api.emergency.event.EmergencyAlertRemoved;
import gov.takx.api.globe.AWidget;
import gov.takx.api.globe.CoreWidgetNames;
import gov.takx.api.globe.IGlobeDelegate;
import gov.takx.core.emergency.EmergencyAlertManager;
import gov.takx.core.emergency.EmergencyOverlaysController;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.lang.invoke.MethodHandles;

/**
 * Widget to display information about Emergency alerts.  Clicking on the widget will open the Emergency Alerts node
 * in the Overlays.
 *
 * @since 5.7
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class EmergencyAlertsWidget extends AWidget
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final Font FONT = new Font("Segoe UI", Font.Style.Bold, 16);
    private static final int RED_RGB = Color.RED.getRGB();
    private static final int[] LINE_COLORS = {RED_RGB, RED_RGB};
    private static final String MESSAGE_FMT_1 = "%s\n%s %s";
    private static final String MESSAGE_FMT_N = "%d alerts\nClick here for details";

    @Inject
    private EmergencyAlertManager alertManager;

    @Inject
    private EmergencyOverlaysController emergencyOverlaysController;

    private final TextWidget textWidget = new TextWidget("", new TextFormat(FONT, 0), TextWidget.TRANSLUCENT_BLACK);

    public EmergencyAlertsWidget(IGlobeDelegate ignored)
    {
        setName(CoreWidgetNames.EMERGENCY_ALERTS);
        setPadding(4);

        textWidget.setColors(LINE_COLORS);
        textWidget.addOnClickListener((widget, event) -> emergencyOverlaysController.showAlertsInOverlays());
        addChildWidget(textWidget);
    }

    @PostConstruct
    private void postConstruct()
    {
        updateText();
    }

    private void onAlertAdded(@Observes EmergencyAlertAdded event)
    {
        updateText();
    }

    private void onAlertRemoved(@Observes EmergencyAlertRemoved event)
    {
        updateText();
    }

    /**
     * Update our text and visibility based on the number of alerts.  If zero, become invisible. If one, show text
     * indicating the details of the alert.  If more than one, show text indicating the number of alerts.
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
                text = String.format(MESSAGE_FMT_1, alert.type(), alert.callsign(), alertManager.getRangeAndBearing(alert));
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
}
