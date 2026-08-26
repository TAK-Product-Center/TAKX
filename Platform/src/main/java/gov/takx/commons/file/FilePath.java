package gov.takx.commons.file;

import jakarta.annotation.Nonnull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Abstract representation of a file path. A file path is represented as a list of path elements. These elements
 * typically represent directories on disk. The abstract path is a relative path. It can be converted to an absolute
 * system path by interpreting the abstract path relative to a root directory. For example:
 * <p/>
 * <pre>
 * FilePath path = new FilePath(IVirtualFileSystem.DEVICE_NAMESPACE, "com.acme", "AcmeDevice", "1");
 * path.toString();   // Returns "Devices/com.acme/AcmeDevice/1"
 * path.toSystemPath(new File("C:\\Users\\AcmeUser")); // Returns "C:\Users\AcmeUser\Devices\com.acme\AcmeDevice\1"
 * </pre>
 *
 */
public class FilePath implements Cloneable
{
    public static final String SEPARATORS = "\\/";

    private final List<String> path = new ArrayList<>();

    /**
     * Create an abstract path from a path string. Any character in the {@link #SEPARATORS} string will be interpreted
     * as a path separator. The path may optionally begin with a separator. For example,
     * {@code new FilePath("/Devices/com.acme/AcmeDevice/1")} results in the abstract path components "Devices",
     * "com.acme", "AcmeDevice", "1".
     *
     * @param path Path string.
     */
    public FilePath(String path)
    {
        add(path);
    }

    /**
     * Create an abstract path from a list of path components. Any character in the {@link #SEPARATORS} string will
     * be interpreted as a path separator.
     *
     * @param path Path elements.
     */
    public FilePath(List<String> path)
    {
        addAll(path);
    }

    /**
     * Create an abstract path from a list of path components. Any character in the {@link #SEPARATORS} string will
     * be interpreted as a path separator. For example {@code new FilePath("Devices", "com.acme", "AcmeDevice/1")}
     * results in the abstract path elements "Devices", "com.acme", "AcmeDevice", "1".
     *
     * @param path Path elements.
     */
    public FilePath(String... path)
    {
        if (null != path) addAll(Arrays.asList(path));
    }

    /**
     * Append an element to the path.
     *
     * @param name New element.
     */
    public void append(@Nonnull String name)
    {
        add(name);
    }

    /**
     * Get the parent of the file or directory specified by this path. The parent is the second to last path
     * element in the list. For example, in the path "Devices/com.acme/AcmeDevice/1/acmeFile.txt",
     * "Devices/com.acme/AcmeDevice/1" is the parent.
     *
     * @return File or directory name.
     */
    public FilePath getParent()
    {
        return path.size() > 1
                ? new FilePath(path.subList(0, path.size() - 1))
                : new FilePath(SEPARATORS);
    }

    /**
     * Get the file (or directory) name specified by this path. The name is the last path element in the list.
     * For example, in the path "Devices/com.acme/AcmeDevice/1/acmeFile.txt", "acmeFile.txt" is the name.
     *
     * @return File or directory name.
     */
    public String getName()
    {
        return !path.isEmpty() ? path.get(path.size() - 1) : "";
    }

    /**
     * Clone the abstract path.
     *
     * @return A deep copy of the path.
     */
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public FilePath clone() throws CloneNotSupportedException
    {
        return new FilePath(path);
    }

    /**
     * Convert the abstract path to an array of path elements. For example, the abstract path
     * "/Devices/com.acme/AcmeDevice" will convert to {@code ["Devices", "com.acme", "AcmeDevice"]}.
     *
     * @return Array representation of the file path.
     */
    public String[] toArray()
    {
        return path.toArray(new String[0]);
    }

    /**
     * Convert the abstract path to a system path.
     *
     * @param root The root directory of the Virtual File System.
     * @return System path for the abstract VFS path.
     */
    public String toSystemPath(File root)
    {
        return (new File(root, toString())).getAbsolutePath();
    }

    /**
     * Convert the path to a String. The path elements will be joined with "/" characters. For example, the path
     * elements "Devices", "com.acme", "AcmeDevice", "1" yield the string representation "Devices/com.acme/AcmeDevice/1".
     *
     * @return A string representation of the file path.
     */
    @Override
    public String toString()
    {
        if (!path.isEmpty())
        {
            StringBuilder sb = new StringBuilder();
            boolean first = true;

            for (String el : path)
            {
                if (first)
                {
                    first = false;
                } else
                {
                    sb.append("/");
                }
                sb.append(el);
            }
            return sb.toString();
        } else
        {
            return "/";
        }
    }

    @Override
    public boolean equals(Object o)
    {
        return o instanceof FilePath && path.equals(((FilePath) o).path);
    }

    @Override
    public int hashCode()
    {
        return path.hashCode();
    }

    private void addAll(Collection<String> values)
    {
        for (String value : values) add(value);
    }

    /**
     * Append a string to the path. If the string contains separator characters it will be broken into multiple path elements.
     *
     * @param value String to append.
     */
    private void add(String value)
    {
        if (null != value)
        {
            final var st = new StringTokenizer(value, SEPARATORS);
            while (st.hasMoreTokens()) path.add(FileUtils.sanitizeWindowsSpecialNames(st.nextToken()));
        }
    }
}
