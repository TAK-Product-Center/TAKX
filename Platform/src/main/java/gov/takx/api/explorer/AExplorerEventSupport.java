package gov.takx.api.explorer;

import jakarta.annotation.Nonnull;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * Abstract base class for providing basic explorer event support via property change events.
 *
 * @since 1.48
 */
public class AExplorerEventSupport implements IExplorerEventSupport
{
    /**
     * Support for explorable property change events.
     */
    protected final PropertyChangeSupport pcs;

    public AExplorerEventSupport()
    {
        pcs = new PropertyChangeSupport(this);
    }

    @Override
    public void addPropertyChangeListener(@Nonnull PropertyChangeListener listener)
    {
        pcs.addPropertyChangeListener(listener);
    }

    @Override
    public void addPropertyChangeListener(@Nonnull String property, @Nonnull PropertyChangeListener listener)
    {
        pcs.addPropertyChangeListener(property, listener);
    }

    @Override
    public void removePropertyChangeListener(@Nonnull PropertyChangeListener listener)
    {
        pcs.removePropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(@Nonnull String property, @Nonnull PropertyChangeListener listener)
    {
        pcs.removePropertyChangeListener(property, listener);
    }

    /**
     * Fire property change event to listeners.
     *
     * @param propertyName name of property.
     * @param oldValue     old property value.
     * @param newValue     new property value.
     */
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue)
    {
        pcs.firePropertyChange(propertyName, oldValue, newValue);
    }

    /**
     * Fire property change event to listeners.
     *
     * @param evt property change event.
     */
    protected void firePropertyChange(PropertyChangeEvent evt)
    {
        pcs.firePropertyChange(evt);
    }
}
