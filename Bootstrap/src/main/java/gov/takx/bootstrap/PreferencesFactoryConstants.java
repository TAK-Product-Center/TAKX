package gov.takx.bootstrap;

import gov.takx.preferences.MemoryPreferencesFactory;

/**
 * Constants related to our {@code MemoryPreferencesFactory} and {@code MemoryPreferences} classes.  This is necessary
 * since these classes are not contained within the bootstrap jar, they are in a special jar all of its own, and there's
 * no "published" artifact for it.  The bootstrap build script has a special tweak to allow this compile time reference
 * to work, and then this class is generally available to all other code via the standard compile dependency on bootstrap.
 *
 * @since 2.20
 */
public class PreferencesFactoryConstants
{
    /**
     * The {@link MemoryPreferencesFactory} class name.
     * <p/>
     * Note: This can only be used during test execution, accessing this during normal execution will lead to CNFEs
     */
    public static final String PREFERENCES_FACTORY_CLASS = MemoryPreferencesFactory.class.getName();
}
