package gov.takx.core.preferences;

import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.UserPreference;
import gov.takx.api.annotation.UserPreferenceType;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.preferences.IPreferenceEntry;
import gov.takx.api.preferences.IPreferenceEntryChangeListener;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.api.preferences.IUserPreferenceManager;
import gov.takx.api.preferences.PreferenceEntryChange;
import gov.takx.commons.media.AudioAlert;
import gov.takx.platform.preferences.PreferencesNode;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.utils.InjectionUtils;
import gov.takx.platform.utils.UnitsFormat;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Typed;
import jakarta.enterprise.inject.spi.InjectionPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

/**
 * Implementation of user preference manager. Provides access to user preference node and a method to save the
 * current, in-memory, preferences out to disk.
 * <p>
 * Producer for User Preference values.  To inject a given preference value, annotate your field/argument like this:
 * <pre>
 *     &#064;Inject
 *     &#064;UserPreference(UserPreferenceEntry.DISPLAY_LOCAL_TIME)
 *     boolean displayLocalTime;
 * </pre>
 * <p>
 * Note that any injection of preference values will use the "current" value of the preference.  If a consumer
 * needs to be informed of future changes to the preference, a listener can be registered, or an observer can
 * be created on the {@link PreferenceEntryChange} event and {@link UserPreferenceType} can be used to limit the
 * scope of the event observation to a single preference entry.
 *
 * @see #addPreferenceEntryChangeListener(IPreferenceEntryChangeListener)
 * @see IPreferenceEntry#addPreferenceEntryChangeListener(IPreferenceEntryChangeListener)
 * @see PreferenceEntryChange
 * @see UserPreferenceType
 * @since 2.2 (essentially complete rewrite in 2.35)
 */
@InjectableService(registerAs = IUserPreferenceManager.class)
@ApplicationScoped
@Typed({UserPreferenceManager.class, IUserPreferenceManager.class})
public class UserPreferenceManager extends APreferenceManager implements IUserPreferenceManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String DEFAULT_USER_PREFERENCES_XML = "conf/DefaultUserPreferences.xml";
    private static final String USER_NODE_KEY = "user";

    // Map from preference entry to event qualifier
    private static final Map<IPreferenceEntry, UserPreferenceType> ENTRY_TO_QUALIFIER = new HashMap<>();

    static
    {
        TYPE_CONVERTERS.put(CoordinateSystem.class, v -> CoordinateSystem.getCoordinateSystem(v, CoordinateSystem.DECIMAL_DEGREES));
        TYPE_CONVERTERS.put(UnitsFormat.class, UnitsFormat::toUnits);
        TYPE_CONVERTERS.put(AudioAlert.class, AudioAlert::getAudioAlert);
    }

    UserPreferenceManager()
    {
        super(USER_NODE_KEY, DEFAULT_USER_PREFERENCES_XML);
    }

    @Override
    protected File getPreferencesFile()
    {
        return programEnvironment.getInstanceUserPreferencesFile();
    }

    @Override
    public IPreferenceMonitor<AudioAlert> newAudioAlertMonitor(Consumer<AudioAlert> changeHandler)
    {
        return UserPreferenceEntry.AUDIO_ALERT.newMonitor(AudioAlert.class, changeHandler);
    }

    @Override
    public IPreferenceMonitor<CoordinateSystem> newCoordinateSystemMonitor(Consumer<CoordinateSystem> changeHandler)
    {
        return UserPreferenceEntry.COORDINATE_SYSTEM.newMonitor(CoordinateSystem.class, changeHandler);
    }

    @Override
    public IPreferenceMonitor<UnitsFormat> newUnitsFormatMonitor(Consumer<UnitsFormat> changeHandler)
    {
        return UserPreferenceEntry.DISPLAY_UNIT_OF_MEASURE.newMonitor(UnitsFormat.class, changeHandler);
    }

    @Override
    public void reloadUserPreferences()
    {
        // We cannot just null out userPreferences and reload them- empirical testing confirms that the listeners
        // are apparently orphaned. So, we have to load a new instance of the preferences and push them through
        // the existing instance in order for the listeners to properly trigger. This will generate events for
        // ONLY those nodes that are updated.

        final File prefsFile = getPreferencesFile();

        try (InputStream in = new FileInputStream(prefsFile))
        {
            Preferences.importPreferences(in);
        } catch (Exception e)
        {
            logger.error("Failed to import preferences from file: {}", prefsFile, e);
        }
    }

    @Produces
    @Dependent
    @UserPreference
    public String produceStringValue(InjectionPoint injectionPoint)
    {
        return getValueForAnnotation(injectionPoint);
    }

    @Produces
    @Dependent
    @UserPreference
    public Integer produceIntegerValue(InjectionPoint injectionPoint)
    {
        return getIntegerValue(injectionPoint);
    }

    @Produces
    @Dependent
    @UserPreference
    public Long produceLongValue(InjectionPoint injectionPoint)
    {
        return getLongValue(injectionPoint);
    }

    @Produces
    @Dependent
    @UserPreference
    public Boolean produceBooleanValue(InjectionPoint injectionPoint)
    {
        return getBooleanValue(injectionPoint);
    }

    @Produces
    @Dependent
    @UserPreference
    public Float produceFloatValue(InjectionPoint injectionPoint)
    {
        return getFloatValue(injectionPoint);
    }

    @Produces
    @Dependent
    @UserPreference
    public Double produceDoubleValue(InjectionPoint injectionPoint)
    {
        return getDoubleValue(injectionPoint);
    }

    @Produces
    @Dependent
    @UserPreference
    public PreferencesNode produceNode(InjectionPoint injectionPoint)
    {
        return node(injectionPoint);
    }

    @Override
    protected IPreferenceEntry getPreferenceEntryFromInjection(InjectionPoint injectionPoint)
    {
        return InjectionUtils.getAnnotation(injectionPoint, UserPreference.class)
                .orElseThrow(() -> new IllegalStateException("No @UserPreference on: " + injectionPoint))
                .value();
    }

    /**
     * @return The qualifier to use for the given change event
     * @since 2.35
     */
    @Override
    protected Annotation[] getPreferenceEntryChangeQualifier(PreferenceEntryChange change)
    {
        return new Annotation[]{
                ENTRY_TO_QUALIFIER.computeIfAbsent(change.getPreferenceEntry(),
                        preferenceEntry -> new UserPreferenceType.Literal(UserPreferenceEntry.valueOf(preferenceEntry.name())))
        };
    }
}
