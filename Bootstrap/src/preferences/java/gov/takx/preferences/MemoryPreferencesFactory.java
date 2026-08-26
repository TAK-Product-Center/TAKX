package gov.takx.preferences;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

/**
 * Preferences factory for constructing Preferences objects that live only in memory. The values maintained
 * in the Preferences are transient, as there is no persistent backing store.  If the values are to be maintained
 * across JVM invocations, then the preferences content must be written to an external store of some kind by the
 * users of this class.
 *
 * @see MemoryPreferences
 * @since 2.20 (moved from Commons to a special stand-alone jar for use with Install4J generated executables)
 */
public class MemoryPreferencesFactory implements PreferencesFactory
{
    private Preferences systemRoot;
    private Preferences userRoot;

    @Override
    public Preferences systemRoot()
    {
        if (systemRoot == null)
        {
            systemRoot = new MemoryPreferences();
        }
        return systemRoot;
    }

    @Override
    public Preferences userRoot()
    {
        if (userRoot == null)
        {
            userRoot = new MemoryPreferences();
        }
        return userRoot;
    }

    /**
     * Simple testing of factory and preferences.
     *
     * @param args arguments
     * @throws BackingStoreException never
     */
    public static void main(String[] args) throws BackingStoreException
    {
        System.setProperty("java.util.prefs.PreferencesFactory", MemoryPreferencesFactory.class.getName());

        Preferences p = Preferences.userNodeForPackage(MemoryPreferencesFactory.class);

        System.out.println("Class node is: " + p.absolutePath());

        for (String s : p.keys())
        {
            System.out.println("p[" + s + "]=" + p.get(s, null));
        }

        p.putBoolean("hi", true);
        p.put("Number", String.valueOf(System.currentTimeMillis()));

        System.out.println("hi = " + p.getBoolean("hi", false));
        System.out.println("Number = " + p.get("Number", "crap"));

        // Export it
        try
        {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            p.exportSubtree(os);
            System.out.println("XML: " + os);
        } catch (IOException e)
        {
            System.err.println("Failed to export Preferences");
            e.printStackTrace();
        }
    }
}
