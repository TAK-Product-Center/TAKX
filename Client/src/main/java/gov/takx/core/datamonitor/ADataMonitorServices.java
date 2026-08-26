package gov.takx.core.datamonitor;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.eventlog.IRaptorEventLogManager;
import gov.takx.api.globe.IStateIconManager;
import gov.takx.api.mapobject.IDataMonitor;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.platform.IDataMonitorServices;
import gov.takx.api.plugin.AForwardingPluginDelegate;
import gov.takx.api.plugin.IPluginDelegate;
import gov.takx.api.plugin.IRaptorObject;
import gov.takx.api.plugin.datamonitor.DataMonitorBuilder;
import gov.takx.api.plugin.datamonitor.DataMonitorEvent;
import gov.takx.api.plugin.datamonitor.DataMonitorEventType;
import gov.takx.api.plugin.datamonitor.IDataMonitorDelegate;
import gov.takx.api.plugin.datamonitor.StandardDataMonitorType;
import gov.takx.api.query.IQueryService;
import gov.takx.commons.constants.State;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * Abstract base for Data Monitor Services.
 *
 * @param <DELEGATE> Type of plugin delegate of our owner
 * @since 2.29
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public abstract class ADataMonitorServices<DELEGATE extends IPluginDelegate> implements IDataMonitorServices
{
    @Inject
    protected IRaptorEventLogManager eventLogManager;

    protected final String family;
    protected final String type;
    protected final String name;
    protected final String key;
    protected final DELEGATE servicesDelegate;

    /**
     * Delegate passed to data monitors so they can access core services.
     */
    protected IDataMonitorDelegate dataMonitorDelegate;

    /**
     * The app icon. May be {@code null}.
     */
    protected final BufferedImage iconImage;

    /**
     * The set of data monitors associated with this app.
     */
    protected final List<IDataMonitor> dataMonitors;

    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    // Re-fire all DM PCEs to our listeners
    private final PropertyChangeListener dataMonitorPropertyChangeHandler = propertyChangeSupport::firePropertyChange;

    /**
     * Constructor.
     *
     * @param delegate Our plugin's delegate
     * @param family   The plugin's family
     * @param type     The plugin's type
     * @param name     The display name to use
     * @param key      The plugin's unique key
     */
    protected ADataMonitorServices(DELEGATE delegate, String family, String type, String name, String key, BufferedImage iconImage)
    {
        servicesDelegate = delegate;
        this.family = family;
        this.type = type;
        this.name = name;
        this.key = key;
        this.iconImage = iconImage;

        // No reason to create the list if we have no delegate (see MapObjectController.NoopDataMonitorServices)
        dataMonitors = delegate != null ? new CopyOnWriteArrayList<>() : Collections.emptyList();
    }

    /**
     * Construct the appropriate data monitor delegate.
     *
     * @param delegate Our plugin's delegate
     * @return DM delegate
     */
    protected abstract ADataMonitorDelegate makeDataMonitorDelegate(DELEGATE delegate);

    @Override
    public void addPropertyChangeListener(@Nonnull PropertyChangeListener listener)
    {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(@Nonnull PropertyChangeListener listener)
    {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    /**
     * Fire a {@link java.beans.PropertyChangeEvent} with the given name, old, and new values.
     * @since 3.4
     */
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue)
    {
        propertyChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }

    @Override
    public void addDataMonitor(@Nonnull IDataMonitor dataMonitor)
    {
        // No duplicate display names
        final String displayName = dataMonitor.getDisplayName();
        if (dataMonitors.stream().anyMatch(dm -> dm.getDisplayName().equals(displayName)))
        {
            throw new IllegalArgumentException("cannot add multiple data monitors with the same display name." +
                    "\nkey=" + getKey() + ", name='" + displayName + "'");
        }

        // Create delegate on demand
        if (dataMonitorDelegate == null) dataMonitorDelegate = makeDataMonitorDelegate(servicesDelegate);

        dataMonitor.setDelegate(dataMonitorDelegate);

        dataMonitors.add(dataMonitor);
        dataMonitor.addPropertyChangeListener(dataMonitorPropertyChangeHandler);

        propertyChangeSupport.firePropertyChange(DATA_MONITOR_ADDED, null, dataMonitor);
    }

    @Override
    public void removeDataMonitor(@Nonnull IDataMonitor dataMonitor)
    {
        dataMonitors.remove(dataMonitor);
        dataMonitor.removePropertyChangeListener(dataMonitorPropertyChangeHandler);

        propertyChangeSupport.firePropertyChange(DATA_MONITOR_REMOVED, null, dataMonitor);
    }

    /**
     * Shutdown and remove all registered data monitors.
     */
    public void shutdownDataMonitors()
    {
        if (!dataMonitors.isEmpty())
        {
            dataMonitors.forEach(monitor -> monitor.onDataMonitorEvent(new DataMonitorEvent(monitor, DataMonitorEventType.Shutdown)));

            // Remove the data monitors in case the plugin developer does not.
            new ArrayList<>(dataMonitors).forEach(this::removeDataMonitor);

            dataMonitors.clear();
        }
    }

    @Override
    public void sortDataMonitors()
    {
        // Do nothing by default
    }

    @Override
    public List<IDataMonitor> getDataMonitors()
    {
        return Collections.unmodifiableList(dataMonitors);
    }

    @Override
    public DataMonitorBuilder createDataMonitorBuilder()
    {
        return new DataMonitorBuilder(family, type);
    }

    @Override
    public DataMonitorBuilder createStandardDataMonitorBuilder(StandardDataMonitorType standardDataMonitorType)
    {
        return new DataMonitorBuilder(family, type, standardDataMonitorType);
    }

    @Override
    public DataMonitorEvent sendDataMonitorCommand(IRaptorObject target, String command, Map<String, Object> properties)
    {
        final DataMonitorEvent event = new DataMonitorEvent(target, command, DataMonitorEventType.Command, properties);
        final String key = target.getUniqueKey();

        dataMonitors.stream()
                .filter(dm -> dm.getUniqueKey().equals(key))
                .findFirst()
                .ifPresent(dm -> dm.onDataMonitorEvent(event));

        return event;
    }

    @Override
    public void delegateSort(@Nonnull Comparator<IDataMonitor> comparator)
    {
        // Do nothing by default
    }

    @Override
    public State getHighestState()
    {
        Optional<IDataMonitor> dataMonitor = dataMonitors.stream()
                .max(Comparator.comparing(d -> d.getCurrentState().ordinal()));

        return dataMonitor.map(IDataMonitor::getCurrentState).orElse(State.Unknown);
    }

    @Override
    public String getKey()
    {
        return key;
    }

    @Override
    public String getFamily()
    {
        return family;
    }

    @Override
    public String getType()
    {
        return type;
    }

    @Override
    public String getDisplayName()
    {
        return name;
    }

    @Override
    public BufferedImage getIconImage()
    {
        return iconImage;
    }

    protected abstract class ADataMonitorDelegate extends AForwardingPluginDelegate implements IDataMonitorDelegate
    {
        protected final Supplier<IQueryService> queryServiceSupplier;

        protected ADataMonitorDelegate(IPluginDelegate targetDelegate, Supplier<IQueryService> queryServiceSupplier)
        {
            super(targetDelegate);
            this.queryServiceSupplier = queryServiceSupplier;
        }

        @Override
        public IQueryService getQueryService()
        {
            return queryServiceSupplier.get();
        }

        @Override
        public void setCustomIcon(URL imagePathURL)
        {
            throw new UnsupportedOperationException("State icons are not supported: " + getKey());
        }

        @Override
        public abstract void logAlarmEvent(String message, String details);

        @Override
        public void logStateChangeEvent(Date logTime, IMapObject mapObject, State curState, String sDetails)
        {
            eventLogManager.logStateChange(logTime, mapObject, curState, sDetails);
        }

        @Override
        public abstract void logDataMonitorStateChangeEvent(IDataMonitor dataMonitor, State curState, String sDetails);

        @Override
        public IStateIconManager getStateIconManager()
        {
            throw new UnsupportedOperationException("State icons are not supported: " + getKey());
        }
    }
}
