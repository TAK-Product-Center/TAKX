package gov.takx.test.mock.explorer;

import gov.takx.api.explorer.ExplorerUtils;
import gov.takx.api.explorer.IExplorerEventSupport;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 */
public class TestExplorerPropertyChangeListener implements PropertyChangeListener
{
    private final String eventName;
    private final IExplorerEventSupport eventSupport;
    private boolean eventOccurred;
    private final Object expectedOldValue;
    private final Object expectedNewValue;
    private Object oldValue;
    private Object newValue;

    /**
     * Constructor.
     *
     * @param eventName Name of the event for which to listen
     * @param expectedOldValue The expected old value for the event
     * @param expectedNewValue The expected new value for the event
     * @param eventSupport Interface for registering listener
     */
    public TestExplorerPropertyChangeListener(String eventName,
                                              Object expectedOldValue,
                                              Object expectedNewValue,
                                              IExplorerEventSupport eventSupport)
    {
        this.eventName = eventName;
        this.expectedNewValue = expectedNewValue;
        this.expectedOldValue = expectedOldValue;
        eventOccurred = false;
        oldValue = null;
        newValue = null;
        this.eventSupport = eventSupport;
        this.eventSupport.addPropertyChangeListener(this);
    }

    public boolean hasOccurred()
    {
        return eventOccurred;
    }

    public boolean isExpectedNewValue()
    {
        return !ExplorerUtils.valueChanged(expectedNewValue, newValue);
    }

    public boolean isExpectedOldValue()
    {
        return !ExplorerUtils.valueChanged(expectedOldValue, oldValue);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getPropertyName().equals(eventName))
        {
            oldValue = evt.getOldValue();
            newValue = evt.getNewValue();
            eventOccurred = true;
            // Don't detach unless we have the exact event we're looking for
            if (isExpectedNewValue() && isExpectedOldValue())
            {
                detach();
            }
        }
    }

    private void detach()
    {
        eventSupport.removePropertyChangeListener(this);
    }
}
