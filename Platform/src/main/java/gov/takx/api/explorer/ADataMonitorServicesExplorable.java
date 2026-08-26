package gov.takx.api.explorer;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IDataMonitor;
import gov.takx.api.platform.IDataMonitorServices;
import jakarta.annotation.PostConstruct;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.List;

/**
 * Base implementation for {@link Explorable}s that support data monitors.
 *
 * @since 2.16
 */
@PseudoBean
public abstract class ADataMonitorServicesExplorable extends AExplorable
        implements IDataMonitorServicesExplorable, PropertyChangeListener
{
    /**
     * The data monitor services that provide access to the data monitors, monitoring, etc.
     * Can be {@code null} when the Map Object represented by this explorable has disabled alarms.
     */
    protected IDataMonitorServices dataMonitorServices;

    /**
     * Constructor.
     *
     * @param owner               The owner object.
     * @param parentPath          The ancestor path.
     * @param uniqueKey           This {@link IExplorable}s unique key.
     * @param group               Flag indicating if this {@link IExplorable} will be used to group others together.
     * @param dataMonitorServices The data monitor services, may be {@code null} to disable data monitor handling
     */
    protected ADataMonitorServicesExplorable(Object owner, List<String> parentPath, String uniqueKey, boolean group, IDataMonitorServices dataMonitorServices)
    {
        super(owner, parentPath, uniqueKey, group);
        this.dataMonitorServices = dataMonitorServices;
    }

    @PostConstruct
    private void postConstruct()
    {
        if (dataMonitorServices == null) return; // No DM services, so don't wire up listeners

        // Listen for data monitors being added to and removed from the app
        dataMonitorServices.addPropertyChangeListener(this);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        // Re-fire the event, with its source updated to this object, to this explorer object's listeners.
        firePropertyChange(new PropertyChangeEvent(this, evt.getPropertyName(), evt.getOldValue(), evt.getNewValue()));
    }

    @Override
    public List<IDataMonitor> getDataMonitors()
    {
        return dataMonitorServices != null ? dataMonitorServices.getDataMonitors() : Collections.emptyList();
    }
}
