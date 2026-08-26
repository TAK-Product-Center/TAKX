package gov.takx.core.explorer.common;

import gov.takx.api.explorer.Explorable;
import gov.takx.api.explorer.IExplorable;
import gov.takx.commons.constants.State;
import jakarta.annotation.Nonnull;

import javax.swing.Icon;
import javax.swing.JPopupMenu;

import java.awt.event.MouseEvent;

/**
 * Basic implementation of {@link IAlarmExplorable}.
 *
 * @since 5.3
 */
public class AlarmExplorable extends Explorable implements IAlarmExplorable
{
    private final IExplorable explorable;
    private final Icon explorableIcon;
    private State alarmState;

    /**
     * Construct a new instance.
     *
     * @param owner the owner of the alarm
     * @param uniqueKey the explorable unique key
     * @param explorable the explorable this alarm is for
     * @param alarmState the alarm state
     */
    public AlarmExplorable(@Nonnull Object owner, @Nonnull String uniqueKey, @Nonnull IExplorable explorable, @Nonnull State alarmState)
    {
        super(owner, explorable.getDisplayName(), uniqueKey, null, explorable.getIconImage(), false);
        this.explorable = explorable;
        this.alarmState = alarmState;
        supportsVisibility = false;
        explorableIcon = CoreExplorerUtils.iconForExplorable(explorable);
    }

    @Override
    public IExplorable getExplorable()
    {
        return explorable;
    }

    @Override
    public String getAlarmName()
    {
        return "";
    }

    @Override
    public State getAlarmLevel()
    {
        return alarmState;
    }

    @Override
    public Icon getExplorableIcon()
    {
        return explorableIcon;
    }

    @Nonnull
    @Override
    public String getDisplayName()
    {
        return explorable.getDisplayName();
    }

    @Override
    public String getDisplayNameHtml()
    {
        return explorable.getDisplayNameHtml();
    }

    @Override
    public JPopupMenu createPopupMenu(@Nonnull MouseEvent mouseEvent)
    {
        return explorable.createPopupMenu(mouseEvent);
    }

    /**
     * {@inheritDoc}
     * This implementation just clears the alarm.<br/>
     * TODO DCW TAKX-3282 clearing the alarm from the showDialog method may be confusing, but this is the method that hooks
     *  into a double click. It would probably be cleaner to do this from a quick action, but that would break WinTak parity
     */
    @Override
    public void showDialog()
    {
        clearAlarm();
    }

    /**
     * Sets the alarm state.<br/>
     * Note: This is intentionally not exposed in {@link IAlarmExplorable}.
     *
     * @param newState the new alarm state.
     */
    public void setAlarmState(@Nonnull State newState)
    {
        if (alarmState != newState)
        {
            State oldState = alarmState;
            alarmState = newState;
            firePropertyChange(ALARM_STATE, oldState, newState);
        }
    }
}