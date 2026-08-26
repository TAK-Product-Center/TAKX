package gov.takx.core.view.gui.eventlog;

import gov.takx.api.eventlog.EventLogType;
import gov.takx.commons.swing.CheckBoxItem;

/**
 */
public class EventLogCheckListItem extends CheckBoxItem
{
    private final EventLogType eventType;

    public EventLogCheckListItem(EventLogType eventType)
    {
        super(eventType.toString());
        this.eventType = eventType;
    }

    public EventLogCheckListItem(EventLogType eventType, boolean selected)
    {
        super(eventType.toString(), selected);
        this.eventType = eventType;
    }

    public EventLogType getEventType()
    {
        return eventType;
    }
}
