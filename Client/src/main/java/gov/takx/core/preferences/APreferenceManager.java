package gov.takx.core.preferences;

import gov.tak.api.icons.TakColor;
import gov.takx.api.preferences.IPreferenceEntry;
import gov.takx.api.preferences.IPreferenceEntryChangeListener;
import gov.takx.api.preferences.IPreferenceManager;
import gov.takx.api.preferences.PreferenceEntryChange;
import gov.takx.api.ui.IUserProxy;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.preferences.PreferenceUtils;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.configuration.ProgramEnvironment;
import gov.takx.core.contentcleaner.ContentCleaner;
import gov.takx.platform.preferences.PreferencesNode;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.prefs.BackingStoreException;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;

/**
 * Abstract base for preference managers providing common functionality for accessing and converting preference values.
 *
 * @since 2.35
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
abstract class APreferenceManager implements IPreferenceManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Preference values that will be interpreted as a boolean true.
     */
    private static final List<String> TRUE_VALUES = Arrays.asList("true", "yes", "1");

    /**
     * Function to convert string values to booleans.  Handles more "true" values than {@link Boolean#valueOf(String)}.
     *
     * @see #TRUE_VALUES
     */
    private static final Function<String, Boolean> BOOLEAN_CONVERTER = value -> TRUE_VALUES.contains(value.toLowerCase());

    protected static final Annotation[] EMPTY_ANNOTATIONS = new Annotation[0];

    // Map from type to registered string value converter functions
    protected static final Map<Class<?>, Function<String, ?>> TYPE_CONVERTERS = new ConcurrentHashMap<>();

    // Map from type to registered node converter functions
    protected static final Map<Class<?>, Function<PreferencesNode, ?>> NODE_TYPE_CONVERTERS = new ConcurrentHashMap<>();

    static
    {
        // Pre-register our primitive/wrapper converters
        TYPE_CONVERTERS.put(Boolean.class, BOOLEAN_CONVERTER);
        TYPE_CONVERTERS.put(Integer.class, Integer::parseInt);
        TYPE_CONVERTERS.put(Long.class, Long::parseLong);
        TYPE_CONVERTERS.put(Float.class, Float::parseFloat);
        TYPE_CONVERTERS.put(Double.class, Double::parseDouble);
        TYPE_CONVERTERS.put(TakColor.class, TakColor::parseColor); // Necessary to handle preference values stored using the color name instead of the enum name
    }

    @Inject
    protected ProgramEnvironment programEnvironment;

    @Inject
    private Event<PreferenceEntryChange> preferenceEntryChangeEvent;

    @Inject
    protected IUserProxy userProxy;

    // Cache of the IPreferenceEntry referenced by a given injection point.
    private final Map<InjectionPoint, IPreferenceEntry> preferenceEntryForInjectionPoint = new ConcurrentHashMap<>(128);

    // List of all "global" change listeners (listening to any preference change)
    private final List<IPreferenceEntryChangeListener> globalChangeListeners = new CopyOnWriteArrayList<>();

    // Cache of property values.
    private final Map<IPreferenceEntry, String> preferenceValues = new ConcurrentHashMap<>(512);

    private final String nodeKey;
    private final String defaultPreferencesFile;
    private final boolean autoRestoreDefaults;

    protected Preferences preferences;

    /**
     * Constructor enabling automatic restoration of the default preferences file if the primary preferences file
     * is missing or fails to load.
     *
     * @param nodeKey                The preferences node key into which our preferences will be read and saved from/to
     * @param defaultPreferencesFile Location of the "default" preferences file for this manager
     */
    protected APreferenceManager(String nodeKey, String defaultPreferencesFile)
    {
        this(nodeKey, defaultPreferencesFile, true);
    }

    /**
     * @param nodeKey                The preferences node key into which our preferences will be read and saved from/to
     * @param defaultPreferencesFile Location of the "default" preferences file for this manager
     * @param autoRestoreDefaults    Pass {@code true} to copy the {@code defaultPreferencesFile} if the primary
     *                               preferences file is missing or fails to load
     * @since 5.1
     */
    protected APreferenceManager(String nodeKey, String defaultPreferencesFile, boolean autoRestoreDefaults)
    {
        this.nodeKey = nodeKey;
        this.defaultPreferencesFile = defaultPreferencesFile;
        this.autoRestoreDefaults = autoRestoreDefaults;
    }

    @PostConstruct
    private void postConstruct()
    {
        loadPreferences();
    }

    /**
     * @return The preferences file from which our values are obtained
     */
    protected abstract File getPreferencesFile();

    /**
     * Load the preferences from disk.  If an error occurs loading the preferences, then optionally restore the default
     * preferences and try again.  If that fails, error out.
     */
    protected void loadPreferences()
    {
        final File preferencesFile = getPreferencesFile();
        logger.debug("loading preferences from {}", preferencesFile);

        try
        {
            preferences = PreferenceUtils.getPreferencesFromFileChecked(preferencesFile, nodeKey);
        } catch (IOException | InvalidPreferencesFormatException e)
        {
            // Decrease noise when running plugin integration tests
            final boolean shouldReport = !programEnvironment.isPlatformUnderTest();

            if (e instanceof FileNotFoundException)
            {
                if (shouldReport) logger.error("Preferences file missing: {}", preferencesFile);
            } else
            {
                logger.error("Failed to load preferences: {} from {}", nodeKey, preferencesFile, e);
            }

            // So, we were unable to load the preferences file, most likely because it was corrupted and failed
            // to parse.  Regardless of the failure, we will try to copy in the default preferences file and then
            // try to load them again.  If it still fails, we're pretty hosed.

            if (autoRestoreDefaults)
            {
                if (shouldReport) logger.warn("Restoring preferences from: {}", defaultPreferencesFile);

                restoreDefaultPreferences(preferencesFile);
            }
        }
    }

    /**
     * Copy the default preferences file to the given {@code preferencesFile}, and attempt to read them in,
     * throwing an exception if that fails.
     *
     * @throws RuntimeException if the copy or read fails
     * @since 5.1
     */
    private void restoreDefaultPreferences(File preferencesFile)
    {
        try
        {
            FileUtils.copyFileOrResource(defaultPreferencesFile, getPreferencesFile());
            preferences = PreferenceUtils.getPreferencesFromFileChecked(preferencesFile, nodeKey);
        } catch (Exception e)
        {
            // Failed even after a restore
            throw new RuntimeException("Still unable to load preferences: " + preferencesFile, e);
        }
    }

    /**
     * Returns the preferences managed by this instance. These preferences should generally not be modified directly.
     * They are only exposed to allow for node/entry deletion.
     *
     * @return The preferences
     * @since 5.7 (changed access modifier to public)
     * @see ContentCleaner#cleanProgramProperties()
     */
    public Preferences getPreferences()
    {
        if (preferences == null) loadPreferences();

        return preferences;
    }

    /**
     * Null out our preferences reference.  Subsequently, any request for a preference entry value will cause the
     * preferences to be reloaded from the configured file.
     *
     * @see #loadPreferences()
     * @see #getPreferencesFile()
     */
    protected void unloadPreferences()
    {
        preferences = null;
        preferenceValues.clear();
    }

    @Override
    public String getStringValue(IPreferenceEntry preferenceEntry)
    {
        return preferenceValues.computeIfAbsent(preferenceEntry, this::loadPreferenceValue);
    }

    /**
     * Get the value of the given preferences entry according to the resolution rules of this preference manager.
     * The default implementation just looks up the preference by key.
     *
     * @param preferenceEntry Preference entry for which to obtain the value
     * @return Value of preference entry
     */
    protected String loadPreferenceValue(IPreferenceEntry preferenceEntry)
    {
        return getPreferences().get(preferenceEntry.getKey(), preferenceEntry.getDefaultValue());
    }

    @Override
    public void addPreferenceEntryChangeListener(IPreferenceEntryChangeListener listener)
    {
        globalChangeListeners.add(listener);
    }

    @Override
    public void removePreferenceEntryChangeListener(IPreferenceEntryChangeListener listener)
    {
        globalChangeListeners.remove(listener);
    }

    @Override
    public boolean nodeExists(IPreferenceEntry entry)
    {
        try
        {
            return getPreferences().nodeExists(entry.getKey());
        } catch (BackingStoreException e)
        {
            logger.error("Failed to test node existence: {}", entry.getKey(), e);
        }
        return false;
    }

    @Override
    public PreferencesNode node(IPreferenceEntry entry)
    {
        return new PreferencesNode(getPreferences().node(entry.getKey()));
    }

    /**
     * @return Get the Preferences node referenced by the given injection point
     */
    protected PreferencesNode node(InjectionPoint injectionPoint)
    {
        return node(getCachedPreferenceEntryFromInjection(injectionPoint));
    }

    @Override
    public Integer getIntegerValue(IPreferenceEntry entry)
    {
        return convertValue(entry, Integer.class);
    }

    /**
     * @return Integer value converted from the string value of the preference entry referenced by the given injection point
     */
    protected Integer getIntegerValue(InjectionPoint injectionPoint)
    {
        return convertValue(injectionPoint, Integer.class);
    }

    @Override
    public Long getLongValue(IPreferenceEntry entry)
    {
        return convertValue(entry, Long.class);
    }

    /**
     * @return Long value converted from the string value of the preference entry referenced by the given injection point
     */
    protected Long getLongValue(InjectionPoint injectionPoint)
    {
        return convertValue(injectionPoint, Long.class);
    }

    @Override
    public Boolean getBooleanValue(IPreferenceEntry entry)
    {
        return convertValue(entry, Boolean.class);
    }

    /**
     * @return Boolean value converted from the string value of the preference entry referenced by the given injection point
     */
    protected Boolean getBooleanValue(InjectionPoint injectionPoint)
    {
        return convertValue(injectionPoint, Boolean.class);
    }

    @Override
    public Float getFloatValue(IPreferenceEntry entry)
    {
        return convertValue(entry, Float.class);
    }

    /**
     * @return Float value converted from the string value of the preference entry referenced by the given injection point
     */
    protected Float getFloatValue(InjectionPoint injectionPoint)
    {
        return convertValue(injectionPoint, Float.class);
    }

    @Override
    public Double getDoubleValue(IPreferenceEntry entry)
    {
        return convertValue(entry, Double.class);
    }

    /**
     * @return Double value converted from the string value of the preference entry referenced by the given injection point
     */
    protected Double getDoubleValue(InjectionPoint injectionPoint)
    {
        return convertValue(injectionPoint, Double.class);
    }

    @Override
    public URL getURLValue(IPreferenceEntry entry)
    {
        return convertValue(entry, URL.class);
    }

    /**
     * @return URL value converted from the string value of the preference entry referenced by the given injection point
     */
    protected URL getURLValue(InjectionPoint injectionPoint)
    {
        return convertValue(injectionPoint, URL.class);
    }

    @Override
    public <T> T getValueAs(IPreferenceEntry entry, Class<T> type)
    {
        return convertValue(entry, type);
    }

    @Override
    public <T> T getNodeAs(IPreferenceEntry entry, Class<T> type)
    {
        return getNodeAs(entry, type, false);
    }

    @Override
    public <T> T getNodeAs(IPreferenceEntry entry, Class<T> type, boolean create)
    {
        return convertNode(entry, type, create, e -> nodeConversionFailure(entry, type, e));
    }

    @Override
    public void put(IPreferenceEntry entry, String value)
    {
        put(getPreferences(), entry, value, true);
    }

    /**
     * Internal method to allow subclasses to specify whether a {@link PreferenceEntryChange} should be fired.
     *
     * @param preferences Preferences node to modify
     * @param entry IPreferenceEntry to update
     * @param value Value to set
     * @param fireEvent Pass {@code true} if an event should be fired if the new value differs from the current value
     * @since 5.1
     */
    protected void put(Preferences preferences, IPreferenceEntry entry, String value, boolean fireEvent)
    {
        preferenceValues.put(entry, value); // Update our cached value

        // Support for entry keys that contain '/' characters.  Get the sub-node and set the preference value in it.
        final String key = entry.getKey();

        final Preferences node;
        final String keyInNode;

        int lastSlash = key.lastIndexOf('/');
        if (lastSlash > 0)
        {
            final String relativePath = key.substring(0, lastSlash);
            node = preferences.node(relativePath);
            keyInNode = key.substring(lastSlash + 1);
        } else
        {
            node = preferences;
            keyInNode = key;
        }

        // Ony update and fire events if the value actually changed
        final String currentValue = node.get(keyInNode, null);

        if (!Objects.equals(currentValue, value))
        {
            node.put(keyInNode, value);
            if (fireEvent) firePreferenceEntryChangeEvent(entry); // Notify all listeners
        }
    }

    @Override
    public void put(IPreferenceEntry entry, Number value)
    {
        put(entry, String.valueOf(value));
    }

    @Override
    public void put(IPreferenceEntry entry, boolean value)
    {
        put(entry, Boolean.toString(value));
    }

    @Override
    public boolean savePreferences()
    {
        try
        {
            PreferenceUtils.savePreferences(getPreferences(), getPreferencesFile());
            return true;
        } catch (IOException | BackingStoreException e)
        {
            userProxy.reportPreferencesError(e);
        }
        return false;
    }

    /**
     * Get the preference value for the entry contained in the annotation.
     *
     * @param injectionPoint current injection point
     * @return the extracted preference value for the given InjectionPoint
     */
    protected String getValueForAnnotation(InjectionPoint injectionPoint)
    {
        return getStringValue(getCachedPreferenceEntryFromInjection(injectionPoint));
    }

    /**
     * Construct a new instance of a type from the given value.  Exceptions are reported, but not propagated.
     *
     * @param constructor Constructor to invoke
     * @param value       Value to pass to constructor
     * @param <T>         Inferred type
     * @return new instance
     */
    private <T> T newInstance(Constructor<T> constructor, Object value)
    {
        try
        {
            return constructor.newInstance(value);
        } catch (ReflectiveOperationException e)
        {
            logger.error("Failed to convert '{}' to {}", value, constructor.getDeclaringClass(), e);
        }
        return null;
    }

    /**
     * Get the cached {@link IPreferenceEntry} for the given injection point.
     *
     * @param injectionPoint Injection point to scan
     * @return Associated PreferenceEntry from the ProgramPreference annotation
     */
    protected IPreferenceEntry getCachedPreferenceEntryFromInjection(InjectionPoint injectionPoint)
    {
        return preferenceEntryForInjectionPoint.computeIfAbsent(injectionPoint, k -> getPreferenceEntryFromInjection(injectionPoint));
    }

    /**
     * Get the {@link IPreferenceEntry} from the preference type annotation from the InjectionPoint.
     *
     * @param injectionPoint Injection point to scan
     * @return Associated IPreferenceEntry from the appropriate annotation
     */
    protected abstract IPreferenceEntry getPreferenceEntryFromInjection(InjectionPoint injectionPoint);

    /**
     * Convert a preference value from its string form to a specified type.
     *
     * @param injectionPoint The injection point specifying the preference value to process
     * @param type           The target class (type to which we are converting)
     * @param <T>            target type
     * @return The converted value
     * @throws RuntimeException if the conversion throws an exception
     */
    protected <T> T convertValue(InjectionPoint injectionPoint, Class<T> type)
    {
        final IPreferenceEntry entry = getCachedPreferenceEntryFromInjection(injectionPoint);
        return convertValue(entry, type, e -> conversionFailure(injectionPoint, type, getStringValue(entry), e));
    }

    /**
     * Convert a preference value from its string form to a specified type.
     *
     * @param entry Preference entry to process
     * @param type  The target class (type to which we are converting)
     * @param <T>   target type
     * @return The converted value
     * @throws RuntimeException if the conversion throws an exception
     */
    protected <T> T convertValue(IPreferenceEntry entry, Class<T> type)
    {
        return convertValue(entry, type, e -> conversionFailure(entry, type, getStringValue(entry), e));
    }

    /**
     * Convert the preference value for the given entry to an object of the given type.
     *
     * @param entry              Preference entry to process
     * @param type               The target class (type to which we are converting)
     * @param exceptionGenerator Function to translate caught exceptions into a {@link RuntimeException}
     * @param <T>                Inferred target type
     * @return The converted value
     */
    @SuppressWarnings("unchecked")
    protected <T> T convertValue(IPreferenceEntry entry, Class<T> type, Function<Exception, RuntimeException> exceptionGenerator)
    {
        final String value = getStringValue(entry);
        if (StringUtils.isEmpty(value, true)) return null;

        if (TYPE_CONVERTERS.containsKey(type)) // Try registered converters first
        {
            return (T) TYPE_CONVERTERS.get(type).apply(value);
        } else if (type.isEnum()) // Then try Enums
        {
            try
            {
                //noinspection RedundantCast - IJ is wrong, without the cast this won't compile
                return (T) Enum.valueOf(type.asSubclass(Enum.class), value);
            } catch (IllegalArgumentException e)
            {
                // This means that the value in preferences is not the name of one of the constants for this enum class.
                // We're going to avoid letting this exception reach the caller since it often happens during class
                // initialization where classes are obtaining preference values or creating monitors on them.
                // An exception thrown at that time causes the class to fail to load, which leads to a cascade of
                // other failures.  Better to trap it and use the default value for the preference.

                final String defaultValue = entry.getDefaultValue();

                logger.error("Failed to load preference {}: {}", entry.name(), e.getMessage());
                logger.error("Will use value: {}", defaultValue);

                entry.put(defaultValue); // Change the preference value so subsequent access to it doesn't continue to error out

                //noinspection TailRecursion
                return convertValue(entry, type, exceptionGenerator);
            }
        } else // Try to find a constructor taking a String
        {
            try
            {
                final Constructor<T> constructor = type.getDeclaredConstructor(String.class);
                if (!constructor.canAccess(null)) constructor.setAccessible(true);

                TYPE_CONVERTERS.put(type, v -> newInstance(constructor, v)); // Add this constructor as a type converter

                return constructor.newInstance(value);
            } catch (ReflectiveOperationException e)
            {
                throw exceptionGenerator.apply(e);
            }
        }
    }

    /**
     * Convert the preference node for the given entry to an object of the given type.
     *
     * @param entry              Preference entry to process
     * @param type               The target class (type to which we are converting)
     * @param create             Pass {@code true} to create the node if it doesn't already exist
     * @param exceptionGenerator Function to translate caught exceptions into a {@link RuntimeException}
     * @param <T>                Inferred target type
     * @return The converted value
     */
    @SuppressWarnings("unchecked")
    protected <T> T convertNode(IPreferenceEntry entry, Class<T> type, boolean create, Function<Exception, RuntimeException> exceptionGenerator)
    {
        final PreferencesNode preferencesNode = nodeExists(entry) || create ? node(entry) : null;

        if (NODE_TYPE_CONVERTERS.containsKey(type)) // Try registered converters first
        {
            return (T) NODE_TYPE_CONVERTERS.get(type).apply(preferencesNode);
        } else // Try to find a constructor taking a Preferences
        {
            try
            {
                final Constructor<T> constructor = type.getDeclaredConstructor(PreferencesNode.class);
                if (!constructor.canAccess(null)) constructor.setAccessible(true);

                NODE_TYPE_CONVERTERS.put(type, v -> newInstance(constructor, v)); // Add this constructor as a type converter

                return constructor.newInstance(preferencesNode);
            } catch (ReflectiveOperationException e)
            {
                throw exceptionGenerator.apply(e);
            }
        }
    }

    /**
     * Construct a new RuntimeException for a conversion failure.
     *
     * @param injectionPoint Injection point being processed
     * @param type           Type we're trying to produce
     * @param value          The value that we couldn't parse
     * @param e              The exception we caught
     * @return New, informative runtime exception
     */
    protected RuntimeException conversionFailure(InjectionPoint injectionPoint, Class<?> type, String value, Exception e)
    {
        final IPreferenceEntry preferenceEntry = getCachedPreferenceEntryFromInjection(injectionPoint);

        final String msg = String.format("Error while converting %s property [%s] with value [%s] on class %s",
                type.getSimpleName(), preferenceEntry.getKey(), value, injectionPoint.getBean());

        return new RuntimeException(msg, e);
    }

    /**
     * Construct a new RuntimeException for a conversion failure.
     *
     * @param preferenceEntry Preference entry being processed
     * @param type            Type we're trying to produce
     * @param value           The value that we couldn't parse
     * @param e               The exception we caught
     * @return New, informative runtime exception
     */
    protected RuntimeException conversionFailure(IPreferenceEntry preferenceEntry, Class<?> type, String value, Exception e)
    {
        final String msg = String.format("Error while converting %s property [%s] with value [%s]",
                type.getSimpleName(), preferenceEntry.getKey(), value);

        return new RuntimeException(msg, e);
    }

    /**
     * Construct a new RuntimeException for a conversion failure.
     *
     * @param preferenceEntry Preference entry being processed
     * @param type            Type we're trying to produce
     * @param e               The exception we caught
     * @return New, informative runtime exception
     */
    protected RuntimeException nodeConversionFailure(IPreferenceEntry preferenceEntry, Class<?> type, Exception e)
    {
        final String msg = String.format("Error while converting %s node [%s] to type %s",
                preferenceEntry.getClass().getSimpleName(), preferenceEntry.getKey(), type.getSimpleName());

        return new RuntimeException(msg, e);
    }

    /**
     * Fire a preference entry change event to all listeners.
     *
     * @param preferenceEntry Preference entry that changed
     */
    protected void firePreferenceEntryChangeEvent(IPreferenceEntry preferenceEntry)
    {
        final PreferenceEntryChange change = new PreferenceEntryChange(this, preferenceEntry);

        preferenceEntryChangeEvent.select(getPreferenceEntryChangeQualifier(change)).fire(change);

        globalChangeListeners.forEach(l -> l.preferenceEntryChanged(change));

        preferenceEntry.firePreferenceEntryChangeEvent(change);
    }

    /**
     * @return The qualifier to be used for the given change event.  Default is {@code null}.
     */
    protected Annotation[] getPreferenceEntryChangeQualifier(PreferenceEntryChange change)
    {
        return EMPTY_ANNOTATIONS;
    }
}
