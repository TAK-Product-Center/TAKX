package gov.takx.api.explorer;

import gov.takx.platform.KeyUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Utility methods for the explorer.
 *
 * @since 1.48
 */
public class ExplorerUtils
{
    /**
     * A global object for synchronizing explorer tree changes
     */
    private static final Object GLOBAL_EXPLORER_TREE_LOCK = new Object();

    /**
     * Returns an object that, when synchronized on, allows the given explorable to safely alter the explorable hierarchy for its explorer.
     * For now this implementation returns a global lock across all explorer instances. However, it may be desired or required
     * in the future to return a more localized lock for individual explorer instances, at which point this implementation
     * will need to come up with a way to detect which explorer the explorable is part of. It may also be desirable to then
     * allow passing null to this method to return the global lock object.
     *
     * @param explorable the explorable, for context
     * @return the lock object
     * @since 5.3
     */
    @Nonnull
    public static Object getExplorerTreeLock(@Nonnull IExplorable explorable)
    {
        return GLOBAL_EXPLORER_TREE_LOCK;
    }

    /**
     * Create a default explorer key from its display name and parent path/.
     *
     * @param displayName group display name.
     * @param parentPath  group's parent path.
     * @return the group's key.
     */
    public static String createDefaultKey(String displayName, List<String> parentPath)
    {
        List<String> temp = new ArrayList<>();
        if (parentPath != null)
        {
            temp.addAll(parentPath);
        }
        if (displayName != null)
        {
            temp.add(displayName);
        }
        return pathToFlatString(temp);
    }

    /**
     * Convert a path to a flat string.
     *
     * @param path path of interest.
     * @return flat string version of the path.
     */
    public static String pathToFlatString(List<String> path)
    {
        return pathToFlatString(path, KeyUtils.KEY_SEP);
    }

    /**
     * Convert a path to a flat string.
     *
     * @param path      path of interest.
     * @param separator separator string to insert between path nodes.
     * @return flat string version of the path.
     */
    public static String pathToFlatString(List<String> path, String separator)
    {
        if (path == null || path.isEmpty())
        {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(path.get(0));
        for (int i = 1; i < path.size(); i++)
        {
            sb.append(separator).append(path.get(i));
        }
        return sb.toString();
    }

    public static String[] keyToList(String uniqueKey)
    {
        return uniqueKey.split(KeyUtils.KEY_SEP);
    }

    /**
     * Get the parent name from a parent path.  The parent name is the last item in a path list or an empty string if
     * none exist.
     *
     * @param path parent path of interest.
     * @return the last item in the path list or an empty list if none exist.
     */
    public static String getParentName(List<String> path)
    {
        if (path == null || path.isEmpty())
        {
            return "";
        }
        return path.get(path.size() - 1);
    }

    /**
     * A basic utility method to determine whether two objects are different.  It determines whether it is possible
     * to compare the two arguments via the {@link Object#equals(Object)} method (e.g. does {@code null} pointer checks before
     * calling this method), and whether either of them are arrays or collections.  If they are both arrays or
     * collections, the individual objects in both are compared.
     *
     * @param o1 first object to compare.
     * @param o2 second object to compare.
     * @return {@code true} if the objects are different, or {@code false} if they are the same.
     */
    public static boolean valueChanged(Object o1, Object o2)
    {
        // Make sure the values are different unless initial restore
        return ((o1 != null && !equalByType(o1, o2)) || (o1 == null && o2 != null));
    }

    /**
     * Attempts to create a UUID from a string.
     *
     * @param string the string to create a UUID from
     * @return the created UUID or {@code null} if it could not be created
     * @since 5.3
     */
    @Nullable
    public static UUID createUUID(@Nonnull String string)
    {
        try
        {
            return UUID.fromString(string);
        } catch (IllegalArgumentException iae)
        {
            return null;
        }
    }

    /**
     * Tests whether a string is a valid UUID.
     *
     * @param string the string to test
     * @return {@code true} if the string could be turned into a UUID, otherwise {@code false}
     * @since 5.3
     */
    public static boolean isUUID(@Nonnull String string)
    {
        return createUUID(string) != null;
    }

    /**
     * Extend the given {@code basePath} with additional elements.
     *
     * @param basePath Base path to extend
     * @param extensions additional path elements to add
     * @return new path comprised of the base path elements followed by the given {@code extensions}, in order
     * @since 5.3
     */
    public static List<String> extendPath(List<String> basePath, String... extensions)
    {
        final List<String> newPath = new ArrayList<>(basePath);
        newPath.addAll(Arrays.asList(extensions));
        return newPath;
    }

    private static boolean equalByType(Object o1, Object o2)
    {
        if (o1 instanceof Object[])
        {
            return o2 instanceof Object[] && Arrays.equals((Object[]) o1, (Object[]) o2);
        } else if (o1 instanceof byte[])
        {
            return o2 instanceof byte[] && Arrays.equals((byte[]) o1, (byte[]) o2);
        } else if (o1 instanceof short[])
        {
            return o2 instanceof short[] && Arrays.equals((short[]) o1, (short[]) o2);
        } else if (o1 instanceof int[])
        {
            return o2 instanceof int[] && Arrays.equals((int[]) o1, (int[]) o2);
        } else if (o1 instanceof float[])
        {
            return o2 instanceof float[] && Arrays.equals((float[]) o1, (float[]) o2);
        } else if (o1 instanceof double[])
        {
            return o2 instanceof double[] && Arrays.equals((double[]) o1, (double[]) o2);
        } else if (o1 instanceof boolean[])
        {
            return o2 instanceof boolean[] && Arrays.equals((boolean[]) o1, (boolean[]) o2);
        }
        if (o1 instanceof Collection<?> newCollection)
        {
            if (!(o2 instanceof Collection<?> oldCollection)) return false;

            return newCollection.size() == oldCollection.size() && newCollection.containsAll(oldCollection);
        }
        return o1.equals(o2);
    }
}
