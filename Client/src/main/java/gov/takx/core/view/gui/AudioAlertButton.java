package gov.takx.core.view.gui;

import gov.takx.commons.media.AudioAlert;
import gov.takx.core.view.gui.preferences.APreferencesButton;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.ui.UIResources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Icon;

import java.lang.invoke.MethodHandles;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Audio alert button.
 */
public class AudioAlertButton extends APreferencesButton
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private Icon alertsOnIcon;
    private Icon alertsOffIcon;
    private Icon criticalOnlyIcon;

    public AudioAlertButton()
    {
        super("Alarm Audio Alerts", UserPreferenceEntry.AUDIO_ALERT);

        //Load our icons.
        loadIcons();

        //Set all of our icons and tooltips.
        for (AudioAlert alarmAlert : AudioAlert.values())
        {
            setIcon(alarmAlert.toString(), getIcon(alarmAlert));
            setToolTipText(alarmAlert.toString(), getToolTipText(alarmAlert));
        }

        // Only want the icon to display on the button. Both the text and icon will appear in the pop-up menu.
        setButtonDecoration(ButtonDecoration.Icon_Only);
    }

    public void setDefaultSelection(AudioAlert audioAlert)
    {
        setSelectedMenuItem(audioAlert.getName());
    }

    /**
     * Load the icons for alerts off, on and critical alerts only.
     */
    private void loadIcons()
    {
        alertsOffIcon = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "notifications_off.png");
        alertsOnIcon = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "notifications_active.png");
        criticalOnlyIcon = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "notifications_important.png");
    }

    /**
     * Get the proper icon for a given audio alert level.
     *
     * @param audioAlert audio alert value
     * @return icon for the audio alert value, or {@code null} for an unexpected audio alert
     */
    private Icon getIcon(AudioAlert audioAlert)
    {
        switch (audioAlert)
        {
            case ALERTS_ON:
                return alertsOnIcon;
            case ALERTS_OFF:
                return alertsOffIcon;
            case CRITICAL_ONLY:
                return criticalOnlyIcon;
            default:
                logger.warn("Unexpected audio alert: {}", audioAlert);
                return null;
        }
    }

    /**
     * Get tool tip text for the five audio alert.
     *
     * @param audioAlert audio alert value
     * @return tool tip text for the audio alert
     */
    private String getToolTipText(AudioAlert audioAlert)
    {
        switch (audioAlert)
        {
            case ALERTS_ON:
                return "All Audio Alerts Playing";
            case ALERTS_OFF:
                return "All Audio Alerts Muted";
            case CRITICAL_ONLY:
                return "Only Playing Critical Alarm Alerts";
            default:
                logger.warn("Unexpected audio alert: {}", audioAlert);
                return "";
        }
    }
}
