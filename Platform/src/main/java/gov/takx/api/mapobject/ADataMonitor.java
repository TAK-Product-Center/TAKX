package gov.takx.api.mapobject;

import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.plugin.datamonitor.DataMonitorEvent;
import gov.takx.api.plugin.datamonitor.DataMonitorEventType;
import gov.takx.api.plugin.datamonitor.IDataMonitorDelegate;
import gov.takx.commons.constants.State;
import gov.takx.commons.utils.Threshold;
import gov.takx.commons.utils.UUIDUtils;
import gov.takx.platform.KeyUtils;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static gov.takx.api.plugin.datamonitor.DataMonitorEventType.AlarmAcknowledge;
import static gov.takx.api.plugin.datamonitor.DataMonitorEventType.AlarmClear;
import static gov.takx.api.plugin.datamonitor.DataMonitorEventType.ToggleMute;

/**
 * Abstract base for data monitors.  This is a fully functional implementation of {@link IDataMonitor},
 * performing analysis of the configured field value against the configured Thresholds.
 * <p>
 * Subclasses that require custom logic to determine alarm state from field values must override the default
 * implementation of {@link #performAnalysis(IRaptorDataMessage)} or, for more complete control,
 * {@link IDataMonitor#analyze(IRaptorDataMessage)}.
 *
 * @see gov.takx.api.plugin.datamonitor.DataMonitorBuilder
 * @see Threshold
 */
public abstract class ADataMonitor implements IBuildableDataMonitor
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    protected final String family;
    protected final String type;
    protected final String uuid = UUIDUtils.generateType1UUID().toString();
    protected final String uniqueKey;

    protected int unacknowledgedWarningCount;
    protected int unacknowledgedSeriousCount;
    protected int unacknowledgedCriticalCount;

    protected final List<Threshold> thresholdList = new ArrayList<>();
    protected final List<IDataMonitor> children = new ArrayList<>();
    protected IDataMonitorDelegate delegate;
    protected IDataMonitor parent;
    protected URL customIcon;
    protected boolean affectsAudioAndIcons;
    protected String displayName;
    protected boolean showValueOnDisplay;
    protected State defaultState = State.Unknown;
    protected State unclearedAlarmState = State.Unknown;
    protected State currentState = State.Unknown;
    protected boolean clearable = true;
    protected boolean trackHighestAlarmState;
    protected boolean holdHighestAlarmState;
    protected boolean canBeMuted = true;
    protected boolean muted;

    protected Object value;
    protected String fieldName;
    protected FieldType fieldType;
    protected String valueFieldName;
    protected boolean suppressValueLogging = false;
    protected long newestMessageTime = 0;

    protected final Map<String, String> metadata = new HashMap<>();
    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    protected ADataMonitor(@Nonnull String family, @Nonnull String type)
    {
        this.family = family;
        this.type = type;
        uniqueKey = KeyUtils.getUniqueKey(family, type, uuid);
    }

    /**
     * Constructor supporting specification of the display name and icon URL.
     * @since 3.3
     */
    protected ADataMonitor(@Nonnull String family, @Nonnull String type, @Nonnull String displayName, @Nonnull URL customIconUrl)
    {
        this(family, type);
        this.displayName = displayName;
        customIcon = customIconUrl;
    }

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

    @Override
    public boolean isAffectsAudioAndIcons()
    {
        return affectsAudioAndIcons;
    }

    @Override
    public void setAffectsAudioAndIcons(boolean affectsAudioAndIcons)
    {
        this.affectsAudioAndIcons = affectsAudioAndIcons;
    }

    @Nonnull
    @Override
    public State getCurrentState()
    {
        return holdHighestAlarmState ? unclearedAlarmState : currentState;
    }

    @Nonnull
    @Override
    public State getUnclearedAlarmState()
    {
        return unclearedAlarmState;
    }

    @Override
    public void setCustomIcon(@Nonnull URL url)
    {
        customIcon = url;
    }

    @Override
    public URL getCustomIcon()
    {
        return customIcon;
    }

    @Override
    public void setShowValueOnDisplay(boolean showValueOnDisplay)
    {
        this.showValueOnDisplay = showValueOnDisplay;
    }

    @Override
    public boolean getShowValueOnDisplay()
    {
        return showValueOnDisplay;
    }

    @Override
    public void setFieldName(@Nonnull String name)
    {
        fieldName = name;
    }

    @Override
    public String getFieldName()
    {
        return fieldName;
    }

    @Override
    public String getDisplayName()
    {
        if (null != displayName)
        {
            return displayName;
        } else
        {
            logger.warn("Returning an <Empty> display name as displayName in ADataMonitor has not been set");
            return "<Empty>";
        }
    }

    @Override
    public void setDisplayName(@Nonnull String displayName)
    {
        this.displayName = displayName;
    }

    @Override
    public Object getValue()
    {
        return value;
    }

    /**
     * Set the value, firing a {@link IDataMonitor#VALUE_CHANGED} event if the new value is different from
     * the current value.
     *
     * @param newValue New value to install
     * @since 3.0
     */
    protected void setValue(@Nonnull Object newValue)
    {
        logger.debug("setValue = {}", newValue);

        if (!newValue.equals(value))
        {
            Object oldValue = value;
            value = newValue;
            propertyChangeSupport.firePropertyChange(VALUE_CHANGED, oldValue, newValue);
        }
    }

    @Override
    public void setValueFieldName(@Nonnull String name)
    {
        valueFieldName = name;
    }

    @Override
    public void setDefaultState(@Nonnull State defaultState)
    {
        this.defaultState = defaultState;
    }

    @Override
    public boolean canBeMuted()
    {
        return canBeMuted;
    }

    /**
     * Set if this data monitor's audio alarms can be muted.
     *
     * @param canBeMuted Pass {@code true} if this data monitor's audio alarms can be muted, {@code false} if not.
     */
    @Override
    public void setCanBeMuted(boolean canBeMuted)
    {
        this.canBeMuted = canBeMuted;
    }

    @Override
    public boolean isMuted()
    {
        return muted;
    }

    protected void setSuppressValueLogging(boolean suppressValueLogging)
    {
        this.suppressValueLogging = suppressValueLogging;
    }

    @Override
    public String toString()
    {
        return displayName;
    }

    /**
     * Defaults to a comparison of the getDisplayName() methods between this and the other IDataMonitor.  To change
     * the natural ordering of a device plugin's IDataMonitor set, override compareTo() and implement a custom
     * comparison method.
     */
    @Override
    public int compareTo(IDataMonitor dm)
    {
        return getDisplayName().compareTo(dm.getDisplayName());
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof ADataMonitor that)) return false;

        return getUniqueKey().equals(that.getUniqueKey());
    }

    @Override
    public int hashCode()
    {
        return getUniqueKey().hashCode();
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
    public String getUniqueKey()
    {
        return uniqueKey;
    }

    @Override
    public void addThreshold(@Nonnull Comparable<?> maxThreshold, @Nonnull State state, boolean inclusive)
    {
        addThreshold(new Threshold(maxThreshold, state, inclusive));
    }

    @Override
    public void addThreshold(@Nonnull Threshold threshold)
    {
        thresholdList.add(threshold);

        Collections.sort(thresholdList); // Keep it sorted at all times
    }

    @Nonnull
    @Override
    public List<Threshold> thresholds()
    {
        return Collections.unmodifiableList(thresholdList);
    }

    @Override
    public void setDelegate(@Nonnull IDataMonitorDelegate delegate)
    {
        this.delegate = delegate;
    }

    @Override
    public void onChildAdded(@Nonnull IDataMonitor child)
    {
        children.add(child);
    }

    @Nonnull
    @Override
    public List<IDataMonitor> getChildren()
    {
        return Collections.unmodifiableList(children);
    }

    @Override
    public void setParent(@Nonnull IDataMonitor parent)
    {
        this.parent = parent;
        parent.onChildAdded(this);
    }

    @Override
    public IDataMonitor getParent()
    {
        return parent;
    }

    @Nonnull
    @Override
    public Set<String> getMetadataKeys()
    {
        return metadata.keySet();
    }

    @Override
    public String getMetadataValue(String key)
    {
        return metadata.get(key);
    }

    @Override
    public void setFieldType(@Nonnull FieldType fieldType)
    {
        this.fieldType = fieldType;
    }

    protected void setState(@Nonnull State newState)
    {
        logger.debug("Setting State: current {}, new {}", currentState, newState);

        if (currentState != newState)
        {
            State oldState = currentState;
            currentState = newState;

            if (trackHighestAlarmState || holdHighestAlarmState)
            {
                if (newState == State.Warning)
                {
                    unacknowledgedWarningCount++;
                } else if (newState == State.Serious)
                {
                    unacknowledgedSeriousCount++;
                } else if (newState == State.Critical)
                {
                    unacknowledgedCriticalCount++;
                }

                if (unclearedAlarmState.compareTo(currentState) < 0)
                {
                    unclearedAlarmState = currentState;
                }
            } else
            {
                unclearedAlarmState = currentState;
            }

            propertyChangeSupport.firePropertyChange(STATE_CHANGED, oldState, newState);
        }
    }

    protected void clear(@Nonnull State newState)
    {
        resetState(newState);

        delegate.logAlarmEvent("[" + displayName + "] Cleared", displayName + " alarm was cleared by user");
    }

    protected void deactivate()
    {
        clear(State.Unknown);
    }

    private void acknowledge()
    {
        resetState(currentState);

        delegate.logAlarmEvent("[" + displayName + "] Acknowledged", displayName + " alarm acknowledged by user");
    }

    /**
     * Reset our unacknowledged counts and set a new state.  Fire a STATE_CHANGED event.
     */
    protected void resetState(@Nonnull State newState)
    {
        // propertyChangeSupport.firePropertyChange doesn't actually fire an event unless the old and new values differ.
        // The previous event manager would always fire an event, regardless of difference.
        // In the case of an unclearedAlarmState different from the current state, we need to use the uncleared state
        // instead of the current state to ensure the event is fired in that case.
        final State oldState = unclearedAlarmState != currentState ? unclearedAlarmState : currentState;

        currentState = newState;
        unclearedAlarmState = newState;
        unacknowledgedWarningCount = 0;
        unacknowledgedSeriousCount = 0;
        unacknowledgedCriticalCount = 0;

        propertyChangeSupport.firePropertyChange(STATE_CHANGED, oldState, newState);
    }

    @Override
    public void onDataMonitorEvent(@Nonnull DataMonitorEvent event)
    {
        switch (event.getEventType())
        {
            case AlarmClear:
            case DoubleClick:
                if (clearable) clear(defaultState);
                break;

            case Deactivate:
                if (currentState != State.Unknown) deactivate();
                break;

            case AlarmAcknowledge:
                acknowledge();
                break;

            case RightClick:
                showPopupMenu((MouseEvent) event.getProperty(MOUSE_EVENT));
                break;

            case ToggleMute:
                muted = !muted;
                propertyChangeSupport.firePropertyChange(IDataMonitor.MUTE, !muted, muted);
                break;

            case Shutdown:
            case Click:
            case Command:
                break;
            default:
                logger.error("Unknown event type: {}", event.getEventType());
        }
    }

    @Override
    public boolean isAlarming()
    {
        return getCurrentState().compareTo(State.Warning) >= 0;
    }

    @Override
    public void setHoldHighestAlarmState(boolean holdHighestAlarmState)
    {
        this.holdHighestAlarmState = holdHighestAlarmState;
    }

    @Override
    public boolean isHoldHighestAlarmState()
    {
        return holdHighestAlarmState;
    }

    @Override
    public void setTrackHighestAlarmState(boolean trackHighestAlarmState)
    {
        this.trackHighestAlarmState = trackHighestAlarmState;
    }

    @Override
    public boolean isTrackHighestAlarmState()
    {
        return trackHighestAlarmState;
    }

    @Override
    public boolean isClearable()
    {
        return clearable;
    }

    @Override
    public void setClearable(boolean clearable)
    {
        this.clearable = clearable;
    }

    @Override
    public boolean logStateChangeEvent(@Nonnull State previousState, @Nonnull State currentState)
    {
        return true;
    }

    /**
     * Display the popup menu at the location of the given mouse event.
     * If the menu has no items, it is not displayed.
     */
    protected void showPopupMenu(@Nonnull MouseEvent e)
    {
        final JPopupMenu menu = getPopupMenu();
        if (menu.getComponentCount() > 0) menu.show(e.getComponent(), e.getX(), e.getY());
    }

    protected JPopupMenu getPopupMenu()
    {
        return new PopupMenu();
    }

    /**
     * Run thresholds over "fieldName" value and set the state of the dataMonitor.
     * Set the value of dataMonitor for display based on the value field (may differ from state field)
     * Also checks to see if the RDM is imported or older than the newest message... if so, it will not be processed
     *
     * @param rdm Message to analyze
     */
    @Override
    public void analyze(@Nonnull IRaptorDataMessage rdm)
    {
        if (shouldProcessMessage(rdm))
        {
            newestMessageTime = rdm.getTime();

            final State initialState = getCurrentState();
            final Object initialValue = getValue();

            if (performAnalysis(rdm)) logDetailMessage(initialState, initialValue);
        }
    }

    /**
     * Only process new, non-imported messages.
     */
    protected boolean shouldProcessMessage(@Nonnull IRaptorDataMessage rdm)
    {
        return !rdm.isImported() && isNewMessage(rdm);
    }

    /**
     * Perform the actual analysis of the data in the RDM.
     *
     * @param rdm RDM to analyze
     * @return {@code true} if analysis was actually performed, {@code false} if not
     * @since 3.2
     */
    protected boolean performAnalysis(@Nonnull IRaptorDataMessage rdm)
    {
        if (rdm.hasField(fieldName))
        {
            updateValue(rdm);  // Install our value
            analyzeValue(rdm.getField(fieldName)); // Analyze the field value for new state
            return true;
        } else
        {
            logger.debug("Message does not have a field named: {}: {}", fieldName, rdm);
        }
        return false;
    }

    /**
     * Log a detailed message of the state change.
     */
    protected void logDetailMessage(State initialState, Object initialValue)
    {
        final State currentState = getCurrentState();

        if (!suppressValueLogging && initialState != currentState && currentState != State.Inactive)
        {
            final String detail = String.format("%s(%s) went from [%s %s] to [%s %s]", displayName, fieldName,
                    initialState, initialValue, currentState, value);

            delegate.logDataMonitorStateChangeEvent(this, currentState, detail);
        }
    }

    /**
     * Check to see if this is a new message... since some RDMs do not have the time set, for compatibility
     * we have to assume any rdms that return hasTime() == false are new messages, so as not to break any of the
     * devices NOT setting RDM times.
     *
     * @return true if the RDM does not have the most recent timestamp, false otherwise.
     */
    private boolean isNewMessage(IRaptorDataMessage rdm)
    {
        return !rdm.hasTime() || rdm.getTime() >= newestMessageTime;
    }

    /**
     * Compare a value to the thresholds, and set our state to the matched Threshold.
     * If none match, set our state to {@code defaultState}.
     *
     * @param value Value to compare against thresholds
     */
    protected void analyzeValue(@Nonnull Object value)
    {
        logger.debug("Analyze field: {}, VALUE: {}", fieldName, value);

        final State state = thresholdList.stream()
                .filter(threshold -> threshold.matches(value))
                .findFirst()
                .map(Threshold::getState)
                .orElse(defaultState);

        setState(state);
    }

    /**
     * Set the value of this data monitor from the appropriate field in the RDM.  Depending on the setting of
     * {@code valueField} this will be the field named {@code valueFieldName} or {@code fieldName}.
     *
     * @param rdm RDM from which to obtain the field value
     */
    protected void updateValue(@Nonnull IRaptorDataMessage rdm)
    {
        // Pick the right field name to use
        final String fieldNameToUse = valueFieldName != null ? valueFieldName : fieldName;

        if (rdm.hasField(fieldNameToUse)) setValue(rdm.getField(fieldNameToUse));
    }

    protected class PopupMenu extends JPopupMenu
    {
        protected PopupMenu()
        {
            if (clearable) addActionMenuItem("Clear", AlarmClear);

            if (trackHighestAlarmState && unclearedAlarmState != currentState) addActionMenuItem("Acknowledge", AlarmAcknowledge);

            if (canBeMuted) addActionMenuItem(muted ? "Un-mute" : "Mute", ToggleMute);

            addCountMenuItem("Warning: ", unacknowledgedWarningCount);
            addCountMenuItem("Serious: ", unacknowledgedSeriousCount);
            addCountMenuItem("Critical: ", unacknowledgedCriticalCount);
        }

        /**
         * Add a menu item whose action fires a data monitor event of the given type.
         *
         * @param text Text to use in menu item
         * @param eventType Event type to fire
         * @since 3.2
         */
        protected void addActionMenuItem(String text, DataMonitorEventType eventType)
        {
            final JMenuItem menuItem = new JMenuItem(text);
            menuItem.addActionListener(e -> onDataMonitorEvent(new DataMonitorEvent(e.getSource(), eventType)));
            add(menuItem);
        }

        /**
         * Add a disabled menu item that displays a count (if greater than 0) with the given prefix text.
         *
         * @param count Count to test/display
         * @param text Prefix text
         * since 3.2
         */
        protected void addCountMenuItem(String text, int count)
        {
            if (count > 0)
            {
                final JMenuItem menuItem = new JMenuItem(text + count);
                menuItem.setEnabled(false);
                add(menuItem);
            }
        }
    }
}
