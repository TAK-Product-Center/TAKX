package gov.takx.core.preferences;

import gov.takx.api.annotation.ExcludedProducerMethod;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.ProgramPreference;
import gov.takx.api.preferences.IPreferenceEntry;
import gov.takx.api.preferences.IProgramPreferenceManager;
import gov.takx.api.ui.IUserProxy;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.preferences.PreferenceUtils;
import gov.takx.core.preferences.migrations.ProgramPropertiesMigrationManager;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.preferences.PreferencesNode;
import gov.takx.platform.utils.InjectionUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.TimeZone;
import java.util.prefs.BackingStoreException;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;

import static gov.takx.core.configuration.BareProgramEnvironment.DEFAULT_PROGRAM_PROPERTIES_XML;

/**
 * Manager for program preferences.  Responsible for initializing and reading in program preferences.
 * <p>
 * Producer for preference values.  To inject a given preference value, annotate your field/argument like this:
 * <pre>
 *     &#064;Inject
 *     &#064;ProgramPreference(PreferenceEntry.ENABLE_EVENT_LOG)
 *     boolean eventLogEnabled;
 * </pre>
 *
 * @see PreferenceEntry
 * @see ProgramPreference
 */
@InjectableService(registerAs = IProgramPreferenceManager.class)
@ApplicationScoped
public class PreferenceManager extends APreferenceManager implements IProgramPreferenceManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String PROGRAM_NODE_KEY = "program";
    private static final String USER_CHANGES_NODE_KEY = "program_user_changes";
    private static final String NODE_PREFIX = "<node name=\"";

    private static final String EMPTY_PREFERENCES = """
            <?xml version="1.0" encoding="UTF-8" standalone="no"?>
            <!DOCTYPE preferences SYSTEM "http://java.sun.com/dtd/preferences.dtd">
            <preferences EXTERNAL_XML_VERSION="1.0">
                <root type="user">
                    <map/>
                    <node name="rx">
                        <map/>
                        <node name="program_user_changes">
                            <map/>
                        </node>
                    </node>
                </root>
            </preferences>
            """;

    private final ProgramPropertiesMigrationManager migrationManager;
    private final IUserProxy userProxy;

    @Inject
    PreferenceManager(ProgramPropertiesMigrationManager migrationManager, IUserProxy userProxy)
    {
        super(PROGRAM_NODE_KEY, DEFAULT_PROGRAM_PROPERTIES_XML, false);
        this.migrationManager = migrationManager;
        this.userProxy = userProxy;
    }

    @Override
    protected File getPreferencesFile()
    {
        return programEnvironment.getInstanceProgramPropertiesFile();
    }

    @Produces
    @Dependent
    @ProgramPreference
    @ExcludedProducerMethod
    public WindowSettings produceWindowSettings(InjectionPoint injectionPoint)
    {
        return getCachedPreferenceEntryFromInjection(injectionPoint).getNodeAs(WindowSettings.class);
    }

    @Produces
    @Dependent
    @ProgramPreference
    public String produceStringValue(InjectionPoint injectionPoint)
    {
        return getValueForAnnotation(injectionPoint);
    }

    @Produces
    @Dependent
    @ProgramPreference
    public Integer produceIntegerValue(InjectionPoint injectionPoint)
    {
        return getIntegerValue(injectionPoint);
    }

    @Produces
    @Dependent
    @ProgramPreference
    public Long produceLongValue(InjectionPoint injectionPoint)
    {
        return getLongValue(injectionPoint);
    }

    @Produces
    @Dependent
    @ProgramPreference
    public Boolean produceBooleanValue(InjectionPoint injectionPoint)
    {
        return getBooleanValue(injectionPoint);
    }

    @Produces
    @Dependent
    @ProgramPreference
    public Float produceFloatValue(InjectionPoint injectionPoint)
    {
        return getFloatValue(injectionPoint);
    }

    @Produces
    @Dependent
    @ProgramPreference
    public Double produceDoubleValue(InjectionPoint injectionPoint)
    {
        return getDoubleValue(injectionPoint);
    }

    @Produces
    @Dependent
    @ProgramPreference
    public URL produceURLValue(InjectionPoint injectionPoint)
    {
        return getURLValue(injectionPoint);
    }

    @Produces
    @Dependent
    @ProgramPreference
    public PreferencesNode produceNode(InjectionPoint injectionPoint)
    {
        return node(injectionPoint);
    }

    /**
     * Grab the {@link ProgramPreference} annotation from the InjectionPoint.
     * It contains the {@link PreferenceEntry} with our key and default value data.
     *
     * @param injectionPoint Injection point to scan
     * @return Associated PreferenceEntry from the ProgramPreference annotation
     */
    @Override
    protected IPreferenceEntry getPreferenceEntryFromInjection(InjectionPoint injectionPoint)
    {
        return InjectionUtils.getAnnotation(injectionPoint, ProgramPreference.class)
                .orElseThrow(() -> new IllegalStateException("No @ProgramPreference on: " + injectionPoint))
                .value();
    }

    /**
     * Load the program property value.
     * <p>
     * The resolution strategy for loading the value is described in {@link IProgramPreferenceManager#getStringValue(IPreferenceEntry)},
     * with the addition of trying our local/default preferences as a final source for the property value.
     *
     * @param preferenceEntry PreferenceEntry to lookup
     * @return the preference value or the defaultValue if not found
     * @since 2.28
     */
    @Override
    protected String loadPreferenceValue(IPreferenceEntry preferenceEntry)
    {
        final String key = preferenceEntry.getKey();
        final String envKey = key.replace('/', '.'); // For env vars and properties, we convert any '/' character to a '.'

        // Try these locations until we find a value:
        //
        // 1. System properties (via -D)
        // 2. Env vars
        // 3. Program preferences

        String value = System.getProperty(envKey); // Try system properties
        if (value == null) value = System.getenv(envKey); // Try environment vars
        if (value == null) value = super.loadPreferenceValue(preferenceEntry);

        return value;
    }

    /**
     * Overridden to add migration handling for the local program properties file, and the creation of an empty
     * {@code ProgramProperties.xml} file if it doesn't exist.  The LPP.xml file is loaded, and then the user changes
     * (stored in {@code ProgramProperties.xml}) are merged onto that.  The preferences in {@code ProgramProperties.xml}
     * are kept in a separate node so we can export just those entries that have been modified by the user via the UI.
     * <p/>
     * Also, install the local time zone for backward compatibility.
     */
    @Override
    protected void loadPreferences()
    {
        loadLocalPreferencesFile();

        // Create an empty preferences file if it doesn't exist
        createEmptyPreferencesFileIfNeeded(getPreferencesFile());

        mergePreferences();

        // Keep this setting for backwards compatibility - value is now available via IProgramEnvironment#getLocalTimeZone()
        getPreferences().put(PreferenceEntry.LOCAL_TIME_ZONE.getKey(), TimeZone.getDefault().getID()); // set up the jvm timezone

        activateUserChangeTracking();
    }

    /**
     * Activate tracking user changes to a separate node tree in the Preferences.
     * <p/>
     * This is way more complicated than you would think it should be, but we can't directly access the
     * {@code MemoryPreferences} class from here.  It is being loaded by the system/app classloader, and we have
     * no direct access to that.  JBoss modules no longer provides a way to add dependencies on elements on that
     * classpath (other than by JDK module name).  You may be wondering why it is being loaded on the system classpath,
     * and that is because of the way that Install4J-generated executables access the Preferences way before our code
     * is invoked, and to install our preferences factory we have to specify it in the {@code .vmoptions} file, so it
     * is on the system classpath, and not handled by JBoss Modules.
     * <p/>
     * So, we're left with doing this operation via reflection.
     * @since 5.1
     */
    private void activateUserChangeTracking()
    {
        try
        {
            Class.forName("gov.takx.preferences.MemoryPreferences", true, ClassLoader.getSystemClassLoader())
                    .getDeclaredMethod("activateUserChangeTracking")
                    .invoke(null);
        } catch (ReflectiveOperationException e)
        {
            throw new RuntimeException("Failed to activate user change tracking in Preferences", e);
        }
    }

    /**
     * Load the {@code LocalProgramProperties.xml}, copying it from {@code DefaultProgramProperties.xml} if it doesn't
     * exist or fails to load.
     *
     * @since 5.1
     */
    private void loadLocalPreferencesFile()
    {
        final File localProgramPropertiesFile = programEnvironment.getLocalProgramPropertiesFile();
        final boolean localPropertiesExists = localProgramPropertiesFile.exists();

        if (!localPropertiesExists)
        {
            restoreDefaults(localProgramPropertiesFile);
        } else
        {
            // First up, before loading our program properties, we need to ensure they are updated
            migrationManager.updateLocalProgramProperties();

            try
            {
                preferences = PreferenceUtils.getPreferencesFromFileChecked(localProgramPropertiesFile, PROGRAM_NODE_KEY);
            } catch (IOException | InvalidPreferencesFormatException e)
            {
                // Try restoring from the defaults since reading failed, likely because someone edited it and the format
                // is now wrong.
                logger.warn("Failed to load '{}'; restoring defaults", localProgramPropertiesFile);
                restoreDefaults(localProgramPropertiesFile);
            }
        }
    }

    /**
     * Copy the {@code DefaultProgramProperties.xml} over {@code LocalProgramProperties.xml} and load it into the
     * Preferences.
     *
     * @param localProgramPropertiesFile Local file to create/overwrite
     * @since 5.1
     */
    private void restoreDefaults(File localProgramPropertiesFile)
    {
        try
        {
            FileUtils.copyFileOrResource(DEFAULT_PROGRAM_PROPERTIES_XML, localProgramPropertiesFile);
            preferences = PreferenceUtils.getPreferencesFromFileChecked(localProgramPropertiesFile, PROGRAM_NODE_KEY);
        } catch (IOException | InvalidPreferencesFormatException e)
        {
            throw new RuntimeException("Failed to restore default preferences to: " + localProgramPropertiesFile, e);
        }
    }

    /**
     * Create an empty {@code ProgramProperties.xml} file if it doesn't already exist.
     * @since 5.1
     */
    private void createEmptyPreferencesFileIfNeeded(File preferencesFile)
    {
        if (!preferencesFile.exists())
        {
            try
            {
                Files.writeString(preferencesFile.toPath(), EMPTY_PREFERENCES);
            } catch (IOException e)
            {
                throw new RuntimeException("Failed to create initial preferences file: " + preferencesFile, e);
            }
        }
    }

    /**
     * Load the contents of {@code ProgramProperties.xml} and then load it again (with a node name tweak) to merge it
     * into the already loaded preferences (from {@code LocalProgramProperties.xml}.
     */
    private void mergePreferences()
    {
        // We need to load the ProgramPreferences.xml file twice.  Once at it's declared node, and then again by
        // changing its node to "program" so it will merge into the local properties that have already been loaded.

        final File preferencesFile = getPreferencesFile();
        logger.debug("Merging preferences from {}", preferencesFile);

        try
        {
            PreferenceUtils.getPreferencesFromFile(preferencesFile, USER_CHANGES_NODE_KEY);
        } catch (FileNotFoundException e)
        {
            // If we failed here, then there's nothing we can do.  So, delete the PP.xml file and it will be
            // recreated when preferences are saved.
            if (!preferencesFile.delete()) logger.error("Failed to delete preferences file: {}", preferencesFile);
            return;
        }

        // Now we need to merge it into our already loaded local properties.  To do that, we need to read in the
        // PP.xml and change the target node to "program".
        try
        {
            final String content = Files.readString(preferencesFile.toPath(), StandardCharsets.UTF_8);
            final String mergeContent = content.replaceFirst(NODE_PREFIX + USER_CHANGES_NODE_KEY,
                    NODE_PREFIX + PROGRAM_NODE_KEY);

            try (InputStream in = new ByteArrayInputStream(mergeContent.getBytes(StandardCharsets.UTF_8)))
            {
                Preferences.importPreferences(in);
            }
        } catch (IOException | InvalidPreferencesFormatException e)
        {
            logger.error("Failed to read preferences: {}", preferencesFile, e);
        }
    }

    @Override
    public boolean savePreferences()
    {
        try
        {
            PreferenceUtils.savePreferences(getUserChangesPreferences(), getPreferencesFile());
            return true;
        } catch (IOException | BackingStoreException e)
        {
            userProxy.reportPreferencesError(e);
        }
        return false;
    }

    /**
     * @return The "user changes" Preferences node
     */
    private Preferences getUserChangesPreferences()
    {
        return Preferences.userRoot().node(PreferenceUtils.RX_NODE_KEY).node(USER_CHANGES_NODE_KEY);
    }
}
