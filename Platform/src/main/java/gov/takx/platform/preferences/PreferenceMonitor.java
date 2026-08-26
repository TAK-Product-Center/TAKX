package gov.takx.platform.preferences;

import gov.takx.api.annotation.Internal;
import gov.takx.api.preferences.IPreferenceEntry;
import gov.takx.api.preferences.IPreferenceEntryChangeListener;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.api.preferences.ITypedPreferenceValue;
import gov.takx.api.preferences.PreferenceEntryChange;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Concrete implementation of {@link IPreferenceMonitor}.  For internal use only.  Plugins should use appropriate
 * factory methods on {@link IPreferenceEntry}.
 *
 * @see IPreferenceEntry#newBooleanMonitor(Consumer)
 * @see IPreferenceEntry#newDoubleMonitor(Consumer)
 * @see IPreferenceEntry#newFloatMonitor(Consumer)
 * @see IPreferenceEntry#newIntegerMonitor(Consumer)
 * @see IPreferenceEntry#newLongMonitor(Consumer)
 * @see IPreferenceEntry#newStringMonitor(Consumer)
 * @see IPreferenceEntry#newMonitor(Class, Consumer)
 * @since 2.35
 */
@Internal
public class PreferenceMonitor<T> implements IPreferenceEntryChangeListener, IPreferenceMonitor<T>
{
    private final IPreferenceEntry preferenceEntry;
    private final Function<ITypedPreferenceValue, T> typedValueAccessor;
    private final Consumer<T> changeHandler;
    private boolean started = false;

    private String currentRawValue;
    private String previousRawValue;
    private T currentValue;
    private T previousValue;

    /**
     * Internal constructor for use by factory methods.
     *
     * @param preferenceEntry    Preference entry of interest
     * @param changeHandler      Consumer to invoke with new (converted) preference value
     * @param typedValueAccessor Function to convert a {@link ITypedPreferenceValue} object into a properly typed value
     * @param start Pass {@code true} to have the monitor start on construction
     * @since 3.5 (signature change)
     */
    @Internal
    public PreferenceMonitor(IPreferenceEntry preferenceEntry, Consumer<T> changeHandler, Function<ITypedPreferenceValue, T> typedValueAccessor, boolean start)
    {
        this.preferenceEntry = preferenceEntry;
        this.typedValueAccessor = typedValueAccessor;
        this.changeHandler = changeHandler;

        initValues();

        if (start) start();
    }

    @Override
    public T getCurrentValue()
    {
        return currentValue;
    }

    @Override
    public T getPreviousValue()
    {
        return previousValue;
    }

    @Override
    public String getCurrentStringValue()
    {
        return currentRawValue;
    }

    @Override
    public String getPreviousStringValue()
    {
        return previousRawValue;
    }

    @Override
    public void preferenceEntryChanged(PreferenceEntryChange change)
    {
        previousRawValue = currentRawValue;
        currentRawValue = change.getStringValue();

        previousValue = currentValue;
        currentValue = typedValueAccessor.apply(change);

        changeHandler.accept(currentValue);
    }

    @Override
    public void dispose()
    {
        stop();
    }

    @Override
    public void start()
    {
        if (!started)
        {
            initValues();
            preferenceEntry.addPreferenceEntryChangeListener(this);
            started = true;
        }
    }

    @Override
    public void stop()
    {
        if (started)
        {
            preferenceEntry.removePreferenceEntryChangeListener(this);
            started = false;
        }
    }

    /**
     * Initialize the current and previous values from the preference entry.
     * @since 3.5
     */
    private void initValues()
    {
        previousRawValue = currentRawValue = preferenceEntry.getStringValue();
        previousValue = currentValue = typedValueAccessor.apply(preferenceEntry);
    }
}
