package gov.takx.preferences;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Preferences implementation that has no permanent backing store.  All values are maintained in memory
 * and no cross-JVM contamination is possible (and no values will be stored in the Windows registry).
 * <p/>
 * As of 5.1, we now also write any changes made to the program preferences to a second node that holds just
 * the user changes (as opposed to the whole preferences tree).
 *
 * @see MemoryPreferencesFactory
 * @since 2.20 (moved from Commons to a special stand-alone jar for use with Install4J generated executables)
 */
public class MemoryPreferences extends AbstractPreferences
{
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final String PROGRAM_PATH_PREFIX = "/rx/program";
    private static final String USER_CHANGES_PATH_PREFIX = "/rx/program_user_changes";

    private static volatile boolean trackUserChanges = false;

    private final Map<String, String> values = new HashMap<>();

    /**
     * Activate the custom handling for tracking user changes.  This needs to be delayed until after the
     * preferences files are loaded, otherwise, since loading preferences calls our {@link #putSpi(String, String)}
     * method (and that's where we would copy changes into the user changes node tree) and that would lead to ALL the
     * initial properties being copied there, and not just the ones modified after the properties are loaded.
     *
     * @since 5.1
     */
    @SuppressWarnings("unused") // called via reflection
    public static void activateUserChangeTracking()
    {
        trackUserChanges = true;
    }

    /**
     * Construct a root Preferences node.
     */
    public MemoryPreferences()
    {
        super(null, "");
    }

    /**
     * Construct a preferences node with the given parent and name.  This method is private to guarantee that
     * instances are only created via this pluggable implementation.
     *
     * @param parent Parent node, may be {@code null} if this is a root node
     * @param name   Name of node to create
     * @see #childSpi(String)
     */
    private MemoryPreferences(MemoryPreferences parent, String name)
    {
        super(parent, name);
    }

    @Override
    protected String[] childrenNamesSpi()
    {
        // We manage no child nodes that aren't already known by the Abstract base class
        return EMPTY_STRING_ARRAY;
    }

    @Override
    protected void putSpi(String key, String value)
    {
        values.put(key, value);
        updateUserChanges(node -> node.put(key, value));
    }

    /**
     * Support path-ed names. If {@code "foo/bar/baz"} is requested, we should locate node {@code foo/bar} and then
     * retrieve value {@code baz}.
     * <p>
     * We also have to support keys that contain slashes for backward compatibility reasons.  So, first test to
     * see if the given key exists on this node, and if so, return the associated value.  If it doesn't exist,
     * then process it as a path, as described above.
     * <p>
     * {@inheritDoc}
     */
    @Override
    protected String getSpi(String key)
    {
        if (values.containsKey(key))
        {
            return values.get(key);
        } else if (key.indexOf('/') >= 0)
        {
            final int lastSlash = key.lastIndexOf('/');
            final String relativePath = key.substring(0, lastSlash);
            final Preferences node = node(relativePath);

            final String newKey = key.substring(lastSlash + 1);
            return ((MemoryPreferences) node).values.get(newKey);
        }

        return null; // Doesn't exist and not a path
    }

    @Override
    protected void removeSpi(String key)
    {
        values.remove(key);
        updateUserChanges(node -> node.remove(key));
    }

    @Override
    protected void removeNodeSpi()
    {
        updateUserChanges(node -> {
            try
            {
                node.removeNode();
            } catch (BackingStoreException ignore)
            {
                // can't happen
            }
        });
    }

    /**
     * Apply an update operation to the user changes node, if enabled.
     *
     * @param updater Update operation to apply
     * @since 5.1
     */
    private void updateUserChanges(Consumer<Preferences> updater)
    {
        final String absolutePath = absolutePath();
        if (trackUserChanges && absolutePath.startsWith(PROGRAM_PATH_PREFIX) && !absolutePath.startsWith(USER_CHANGES_PATH_PREFIX))
        {
            final String changeNodeKey = absolutePath.replaceFirst(PROGRAM_PATH_PREFIX, USER_CHANGES_PATH_PREFIX);
            updater.accept(node(changeNodeKey));
        }
    }

    @Override
    protected String[] keysSpi()
    {
        return values.keySet().toArray(new String[0]);
    }

    @Override
    protected AbstractPreferences childSpi(String name)
    {
        return new MemoryPreferences(this, name);
    }

    @Override
    protected void syncSpi()
    {
        // Nothing to do, no backing store
    }

    @Override
    protected void flushSpi()
    {
        // Nothing to do, no backing store
    }
}
