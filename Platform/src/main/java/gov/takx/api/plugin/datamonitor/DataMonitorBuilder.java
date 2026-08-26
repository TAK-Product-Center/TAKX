package gov.takx.api.plugin.datamonitor;

import gov.takx.api.mapobject.ADataMonitor;
import gov.takx.api.mapobject.FieldType;
import gov.takx.api.mapobject.IBuildableDataMonitor;
import gov.takx.api.mapobject.IDataMonitor;
import gov.takx.commons.constants.State;
import gov.takx.commons.utils.Threshold;
import jakarta.annotation.Nonnull;

import java.net.URL;

/**
 * Constructs an {@link IDataMonitor} based on the parameters specified by the user.
 *
 * @since 1.10
 */
public class DataMonitorBuilder
{
    /**
     * The data monitor that will be progressively set up by this builder.
     *
     * @since 1.11
     */
    protected IBuildableDataMonitor dataMonitor;

    /**
     * Constructor.
     *
     * @param family       The data monitor's family.
     * @param type         The data monitor's type.
     * @param standardType The predefined type of data monitor.
     */
    public DataMonitorBuilder(@Nonnull String family, @Nonnull String type, @Nonnull StandardDataMonitorType standardType)
    {
        dataMonitor = createStandardDataMonitor(family, type, standardType);
    }

    /**
     * Constructor.
     *
     * @param family The data monitor's family.
     * @param type   The data monitor's type.
     */
    public DataMonitorBuilder(@Nonnull String family, @Nonnull String type)
    {
        dataMonitor = new BasicDataMonitor(family, type);
    }

    /**
     * @see IBuildableDataMonitor#setAffectsAudioAndIcons(boolean)
     * @return {@code this} for method chaining
     */
    public DataMonitorBuilder setAffectsAudioAndIcons(boolean affectsAudioAndIcons)
    {
        dataMonitor.setAffectsAudioAndIcons(affectsAudioAndIcons);
        return this;
    }

    /**
     * @see IBuildableDataMonitor#setFieldName(String)
     * @return {@code this} for method chaining
     */
    public DataMonitorBuilder setFieldName(@Nonnull String fieldName)
    {
        dataMonitor.setFieldName(fieldName);
        return this;
    }

    /**
     * @see IBuildableDataMonitor#setFieldType(FieldType)
     * @return {@code this} for method chaining
     */
    public DataMonitorBuilder setFieldType(@Nonnull FieldType fieldType)
    {
        dataMonitor.setFieldType(fieldType);
        return this;
    }

    /**
     * @see IBuildableDataMonitor#setValueFieldName(String)
     * @return {@code this} for method chaining
     */
    public DataMonitorBuilder setValueFieldName(@Nonnull String fieldName)
    {
        dataMonitor.setValueFieldName(fieldName);
        return this;
    }

    /**
     * @see IBuildableDataMonitor#setShowValueOnDisplay(boolean)
     * @return {@code this} for method chaining
     */
    public DataMonitorBuilder setShowValueOnDisplay(boolean flag)
    {
        dataMonitor.setShowValueOnDisplay(flag);
        return this;
    }

    /**
     * @see IBuildableDataMonitor#setDefaultState(State)
     * @return {@code this} for method chaining
     */
    public DataMonitorBuilder setDefaultState(@Nonnull State defaultState)
    {
        dataMonitor.setDefaultState(defaultState);
        return this;
    }

    /**
     * @see IBuildableDataMonitor#addThreshold(Comparable, State, boolean)
     * @return {@code this} for method chaining
     */
    public DataMonitorBuilder addThreshold(@Nonnull Comparable<?> maxThreshold, @Nonnull State state, boolean inclusive)
    {
        dataMonitor.addThreshold(maxThreshold, state, inclusive);
        return this;
    }

    /**
     * @see IBuildableDataMonitor#addThreshold(Threshold)
     * @return {@code this} for method chaining
     */
    public DataMonitorBuilder addThreshold(@Nonnull Threshold threshold)
    {
        dataMonitor.addThreshold(threshold);
        return this;
    }

    /**
     * @see IBuildableDataMonitor#setClearable(boolean)
     * @return {@code this} for method chaining
     */
    public DataMonitorBuilder setClearable(boolean flag)
    {
        dataMonitor.setClearable(flag);
        return this;
    }

    /**
     * @see IBuildableDataMonitor#setDisplayName(String)
     * @return {@code this} for method chaining
     */
    public DataMonitorBuilder setDisplayName(@Nonnull String name)
    {
        dataMonitor.setDisplayName(name);
        return this;
    }

    /**
     * @see IBuildableDataMonitor#setParent(IDataMonitor)
     * @return {@code this} for method chaining
     */
    public DataMonitorBuilder setParent(@Nonnull IDataMonitor monitor)
    {
        dataMonitor.setParent(monitor);
        return this;
    }

    /**
     * @see IBuildableDataMonitor#setCustomIcon(URL)
     * @return {@code this} for method chaining
     */
    public DataMonitorBuilder setCustomIcon(@Nonnull URL imagePathURL)
    {
        dataMonitor.setCustomIcon(imagePathURL);
        return this;
    }

    /**
     * @see IBuildableDataMonitor#setTrackHighestAlarmState(boolean)
     * @return {@code this} for method chaining
     */
    public DataMonitorBuilder setTrackHighestAlarmState(boolean trackHighestAlarmState)
    {
        dataMonitor.setTrackHighestAlarmState(trackHighestAlarmState);
        return this;
    }

    /**
     * @see IBuildableDataMonitor#setHoldHighestAlarmState(boolean)
     * @return {@code this} for method chaining
     */
    public DataMonitorBuilder setHoldHighestAlarmState(boolean holdHighestAlarmState)
    {
        dataMonitor.setHoldHighestAlarmState(holdHighestAlarmState);
        return this;
    }

    /**
     * @see IBuildableDataMonitor#setCanBeMuted(boolean)
     * @return {@code this} for method chaining
     */
    public DataMonitorBuilder setCanBeMuted(boolean canBeMuted)
    {
        dataMonitor.setCanBeMuted(canBeMuted);
        return this;
    }

    /**
     * Builds a data monitor based on the settings applied to this {@link DataMonitorBuilder}.
     *
     * @return A new data monitor based on the settings applied to this {@link DataMonitorBuilder}.
     */
    public IDataMonitor build()
    {
        return dataMonitor;
    }

    /**
     * Build a pre-defined data monitor based on the given {@code standardType}.
     *
     * @param family       The data monitor's family.
     * @param type         The data monitor's type.
     * @param standardType The pre-defined data monitor type being requested.
     * @return A standard {@link IDataMonitor} of the specified type.
     */
    private IBuildableDataMonitor createStandardDataMonitor(String family, String type, StandardDataMonitorType standardType)
    {
        if (StandardDataMonitorType.Comms == standardType)
        {
            return new CommsDataMonitor(family, type, false);
        } else if (StandardDataMonitorType.Battery == standardType)
        {
            return new BatteryDataMonitor(family, type);
        } else if (StandardDataMonitorType.Power == standardType)
        {
            return new PowerDataMonitor(family, type);
        } else
        {
            throw new IllegalArgumentException("The type " + standardType + " is not supported.");
        }
    }

    /**
     * Concrete implementation of {@link ADataMonitor}.
     * @since 3.2
     */
    private static final class BasicDataMonitor extends ADataMonitor
    {
        private BasicDataMonitor(String family, String type)
        {
            super(family, type);
        }
    }
}
