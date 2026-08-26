package gov.takx.core.propertymanager;

import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.event.project.ProjectClosing;
import gov.takx.api.globe.ICameraEye;
import gov.takx.api.globe.IGlobeManager;
import gov.takx.api.preferences.IPreferenceEntry;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.api.propertymanager.ISystemPropertyChangeListener;
import gov.takx.api.propertymanager.ISystemPropertyManager;
import gov.takx.api.propertymanager.SystemPropertyChangeEvent;
import gov.takx.api.propertymanager.SystemPropertyType;
import gov.takx.commons.media.AudioAlert;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.utils.UnitsFormat;
import gov.takx.platform.utils.units.RangeUnits;
import gov.takx.platform.utils.units.UnitsFormatUtils;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static gov.takx.platform.preferences.ProjectPreferenceEntry.WMS_OFFLINE_MODE;
import static gov.takx.platform.preferences.UserPreferenceEntry.AUDIO_ALERT;
import static gov.takx.platform.preferences.UserPreferenceEntry.COORDINATE_SYSTEM;

/**
 * Implementation of the ISystemPropertyManager interface.
 *
 * @see ISystemPropertyManager
 * @since 1.6
 */
@ApplicationScoped
public class SystemPropertyManager implements ISystemPropertyManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final List<ISystemPropertyChangeListener> systemPropertyChangeListeners = new CopyOnWriteArrayList<>();

    private final IPreferenceMonitor<CoordinateSystem> coordinateSystemMonitor = COORDINATE_SYSTEM.newMonitor(CoordinateSystem.class, this::coordinateSystemChanged);
    private final IPreferenceMonitor<AudioAlert> audioAlertMonitor = AUDIO_ALERT.newMonitor(AudioAlert.class, this::audioAlertChanged);
    private final IPreferenceMonitor<Boolean> wmsOfflineMonitor = WMS_OFFLINE_MODE.newBooleanMonitor(this::wmsOfflineChanged);

    private final IGlobeManager globeManager;

    @Inject
    SystemPropertyManager(IGlobeManager globeManager)
    {
        this.globeManager = globeManager;
    }

    /**
     * Clear our listener list on project boundaries.
     * @since 3.1
     */
    private void onProjectClosing(@Observes ProjectClosing ignore)
    {
        systemPropertyChangeListeners.clear();
    }

    private void coordinateSystemChanged(CoordinateSystem coordinateSystem)
    {
        fireChangeEvent(COORDINATE_SYSTEM, coordinateSystemMonitor, false, SystemPropertyType.CoordinateSystem);
    }

    private void audioAlertChanged(AudioAlert audioAlert)
    {
        fireChangeEvent(AUDIO_ALERT, audioAlertMonitor, false, SystemPropertyType.GlobalAudioThreshold);
    }

    private void wmsOfflineChanged(boolean wmsOffline)
    {
        fireChangeEvent(WMS_OFFLINE_MODE, wmsOfflineMonitor, false, SystemPropertyType.WMSNetworkStatus);
    }

    /**
     * Fire {@link ISystemPropertyChangeListener#onSystemPropertyChange(SystemPropertyChangeEvent)}
     * for all registered listeners.
     *
     * @param pref The preference that changed
     * @param monitor The preference monitor, holding old and new values
     * @param useRawValue Pass {@code true} to use the raw (String) monitor value, {@code false} for typed value
     * @param propertyType The property type that changed
     * @since 3.1
     */
    @InvokeOnEDT
    private void fireChangeEvent(IPreferenceEntry pref, IPreferenceMonitor<?> monitor, boolean useRawValue, SystemPropertyType propertyType)
    {
        final Object oldValue = useRawValue ? monitor.getPreviousStringValue() : monitor.getPreviousValue();
        final Object newValue = useRawValue ? monitor.getCurrentStringValue() : monitor.getCurrentValue();

        final var event = new SystemPropertyChangeEvent(this, pref.getKey(), oldValue, newValue, propertyType);

        logger.debug("Fire: {}", event);

        systemPropertyChangeListeners.forEach(listener -> listener.onSystemPropertyChange(event));
    }

    @Override
    public boolean isWMSNetworkOnline()
    {
        return !wmsOfflineMonitor.getCurrentValue();
    }

    @Override
    public CoordinateSystem getCoordinateSystem()
    {
        return coordinateSystemMonitor.getCurrentValue();
    }

    @Override
    public AudioAlert getAudioAlertLevel()
    {
        return audioAlertMonitor.getCurrentValue();
    }

    @Override
    public String getUnitsOfMeasure()
    {
        final var rangeUnits = UserPreferenceEntry.DISPLAY_RANGE_UNITS.getValueAs(RangeUnits.class);
        final var unitsString = UnitsFormatUtils.unitsFormatStringFromRangeUnits(rangeUnits);

        logger.warn("UnitsFormat is no longer supported. Converted RangeUnits {} to UnitsFormat String {}", rangeUnits.name(), unitsString);

        return unitsString;
    }

    @Override
    public UnitsFormat getUnitsFormat()
    {
        final var rangeUnits = UserPreferenceEntry.DISPLAY_RANGE_UNITS.getValueAs(RangeUnits.class);
        final var unitsFormat = UnitsFormatUtils.toUnitsFormat(rangeUnits);

        logger.warn("UnitsFormat is no longer supported. Converting RangeUnits {} to UnitsFormat {}\\{}\\{}",
                rangeUnits.name(), unitsFormat.getLengthUnits(), unitsFormat.getAreaUnits(), unitsFormat.getSpeedUnits());

        return unitsFormat;
    }

    @Override
    public void addSystemPropertyChangeListener(@Nonnull ISystemPropertyChangeListener listener)
    {
        systemPropertyChangeListeners.add(listener);
    }

    @Override
    public boolean removeSystemPropertyChangeListener(@Nonnull ISystemPropertyChangeListener listener)
    {
        return systemPropertyChangeListeners.remove(listener);
    }

    @Override
    public ICameraEye getCurrentCameraEye()
    {
        return globeManager.getPrimaryGlobeDelegate().getGlobeViewController().getCurrentCameraEye();
    }
}
