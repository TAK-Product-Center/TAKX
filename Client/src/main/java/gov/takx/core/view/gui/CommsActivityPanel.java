package gov.takx.core.view.gui;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.event.project.ProjectClosed;
import gov.takx.api.ui.event.CommunicationsEvent;
import gov.takx.api.ui.event.CommunicationsEvent.CommDirection;
import gov.takx.platform.ui.UIResources;
import gov.takx.platform.utils.TimeBoundedTask;
import jakarta.enterprise.event.Observes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import java.lang.invoke.MethodHandles;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Communications Activity Feedback Panel.  Two LEDs are shown, one for data received by takx and one for data
 * sent from takx.  When communications activity is reported (via an event dispatch), the appropriate LED is
 * turned on for .3 seconds.
 *
 * @since 3.1 (near complete rework, was originally in 1.3)
 */
@PseudoBean(singletonObserver = true)
public class CommsActivityPanel extends JPanel
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final Icon OFF_ICON = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "led_gray.png");
    private static final Icon ON_ICON = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "led_blue.png");

    private static final int INITIAL_DELAY = 300; // When a light is turned on, it will stay on at least this long

    private final JLabel txLight = makeLight("tx");
    private final JLabel rxLight = makeLight("rx");

    // These tasks turn out the lights once comms activity stops
    private final TimeBoundedTask rxLightTask = new TimeBoundedTask("RX Comms Monitor",
            INITIAL_DELAY, Long.MAX_VALUE, () -> switchLight(rxLight, OFF_ICON));

    private final TimeBoundedTask txLightTask = new TimeBoundedTask("TX Comms Monitor",
            INITIAL_DELAY, Long.MAX_VALUE, () -> switchLight(txLight, OFF_ICON));

    public CommsActivityPanel()
    {
        setName("CommsActivityPanel");
        setLayout(new FormLayout("default, $lcgap, default", "default"));

        final CellConstraints cc = new CellConstraints();

        add(txLight, cc.xy(1, 1));
        add(rxLight, cc.xy(3, 1));
    }

    /**
     * Create a "light" with the given text.  Icon is initially "off" and there is no gap between icon and text.
     */
    private JLabel makeLight(String text)
    {
        final JLabel light = new JLabel(text, OFF_ICON, SwingConstants.LEADING);
        light.setIconTextGap(0);
        return light;
    }

    /**
     * Handle a communication event, update our lights.
     */
    public void onCommunicationsEvent(@Observes CommunicationsEvent event)
    {
        logger.debug("Event received: {}", event);

        if (event.getCommDirection() == CommDirection.CommIn)
        {
            turnLightOn(rxLight, rxLightTask);

        } else
        {
            turnLightOn(txLight, txLightTask);
        }
    }

    /**
     * Turn on the given light and queue the task to turn it off.
     *
     * @param light light to turn on
     * @param task Task to notify
     */
    private void turnLightOn(JLabel light, TimeBoundedTask task)
    {
        if (light.getIcon() != ON_ICON) switchLight(light, ON_ICON);

        task.request(); // This will turn the light back off
    }

    /**
     * Set the icon on the given light, on the EDT.
     */
    @InvokeOnEDT
    private void switchLight(JLabel light, Icon icon)
    {
        light.setIcon(icon);
        light.repaint();
    }

    /**
     * Turn out the lights as you leave.
     */
    private void onProjectClosed(@SuppressWarnings("unused") @Observes ProjectClosed event)
    {
        rxLightTask.processNow(true);
        txLightTask.processNow(true);
    }
}
