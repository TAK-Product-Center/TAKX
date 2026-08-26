package gov.takx.commons.swing;

import jakarta.annotation.Nonnull;

import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileSystemView;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A Factory class to simplify creation/management of AlwaysOnTopFileChoosers within TAKX and plugins. If this
 * factory class is instead used to create/get AlwaysOnTopFileChoosers instances, the last path will be remembered.
 *
 */
public class AlwaysOnTopFileChooserFactory
{
    private static final Map<Integer, AlwaysOnTopFileChooser> chooserMap = new ConcurrentHashMap<>();

    /**
     * Create or get a file chooser from the AlwaysOnTopFileChooserFactory.  It's necessary to pass in the owner of
     * file chooser so the factory can return the same instance when the method is called again.
     *
     * @param owner the owner of the returned file chooser
     * @return a file chooser object, remembering the last path used
     */
    public static AlwaysOnTopFileChooser getFileChooser(@Nonnull Object owner)
    {
        int hash = getHashCode(owner);

        AlwaysOnTopFileChooser chooser = chooserMap.get(hash);
        if (chooser != null)
        {
            // force it to update to the current skin, in case it was created while a different one was active.
            SwingUtilities.updateComponentTreeUI(chooser);
        } else
        {
            chooser = new AlwaysOnTopFileChooser();
            chooserMap.put(hash, chooser);
        }
        return chooser;
    }

    /**
     * Create or get a file chooser from the AlwaysOnTopFileChooserFactory where the current directory is physically
     * restricted as the root directoy, ensuring that the user can only drill down, not up.
     *
     * @param owner            the owner of the returned file chooser
     * @param currentDirectory Directory to open the chooser into; also serves as the root directory
     * @param view             Instance of a FileSystemView to enforce the root directory description.
     * @return a file chooser object, remembering the last path used
     */
    public static AlwaysOnTopFileChooser getFileChooser(@Nonnull Object owner, File currentDirectory, FileSystemView view)
    {
        int hash = getHashCode(owner);
        return chooserMap.computeIfAbsent(hash, k -> new AlwaysOnTopFileChooser(currentDirectory, view));
    }

    /**
     * Create or get a file chooser from the AlwaysOnTopFileChooserFactory.  It's necessary to pass in the owner of
     * file chooser so the factory can return the same instance when the method is called again.
     *
     * @param owner the owner of the returned file chooser
     * @param path  current directory path
     * @return a file chooser object, remembering the last path used
     */
    public static AlwaysOnTopFileChooser getFileChooser(@Nonnull Object owner, String path)
    {
        int hash = getHashCode(owner);
        return chooserMap.computeIfAbsent(hash, k -> new AlwaysOnTopFileChooser(path));
    }

    /**
     * Create or get a file chooser from the AlwaysOnTopFileChooserFactory.  It's necessary to pass in the owner of
     * file chooser so the factory can return the same instance when the method is called again.
     *
     * @param owner the owner of the returned file chooser
     * @param path  current directory path
     * @return a file chooser object, remembering the last path used
     */
    public static AlwaysOnTopFileChooser getFileChooser(@Nonnull Object owner, File path)
    {
        int hash = getHashCode(owner);
        return chooserMap.computeIfAbsent(hash, k -> new AlwaysOnTopFileChooser(path));
    }

    /**
     * Remove an owner object from this factory.  If you are certain your class will no longer be using a file chooser,
     * it would be a good idea to call this method to allow the GC to destroy it.
     *
     * @param owner Object which owns this object
     * @return the removed AlwaysOnTopFileChooser object
     */
    public static AlwaysOnTopFileChooser remove(@Nonnull Object owner)
    {
        int hash = getHashCode(owner);
        return chooserMap.remove(hash);
    }

    /**
     * Clear all file chooser objects from this factory
     */
    public static void clearAll()
    {
        chooserMap.clear();
    }

    /**
     * Get the hash of the owner object.  Since some plugins pass in references to "this" and others pass
     * in MyClass.class, we need to make sure to handle both, since a call to AnyClass.class.getClass().hashCode()
     * is always going to return the same value (for all classes that do this).
     *
     * @param owner object to get the hash code for
     * @return the owner hash code
     */
    private static int getHashCode(@Nonnull Object owner)
    {
        if (owner instanceof Class)
        {
            return owner.hashCode();
        } else
        {
            return owner.getClass().hashCode();
        }
    }
}
