package gov.takx.api.plugin;

import gov.takx.api.plugin.app.IAppPlugin;
import gov.takx.api.plugin.connection.IConnectionPlugin;
import gov.takx.api.plugin.device.IDevicePlugin;
import gov.takx.api.plugin.io.IImportExportPlugin;

import java.net.URL;
import java.util.List;

/**
 * Provides a global VFS file registration mechanism for a plugin. Automatic registration of the resource manager only
 * occurs when the concrete {@link IRaptorPlugin} implementation implements this interface. In other words, only a class
 * that implements {@link IAppPlugin}, {@link IConnectionPlugin}, {@link IDevicePlugin}, or {@link IImportExportPlugin}
 * is suitable for implementing this interface.
 *
 * @since 1.23
 */
public interface IGlobalResourceManager extends IFileResourceManager
{
    /**
     * Identifies the portion of the resource bundle package name that should be stripped off before the
     * global files (and their corresponding directory structure) are installed in TAKX. For example, if the
     * globals were stored at /com/foo/acme/globals/Servers/ServerListing.xml, the root to remove could be defined
     * as "/com/foo/acme/globals" which means that only a folder named "Servers" would be created in the global VFS
     * and it would contain the file ServerListing.xml.
     *
     * @return Portion of the resource bundle path to strip off.  Paths should always begin with a leading slash (/)
     * since the .jar URL protocol will include this leading slash by default regardless of how the resource URL is
     * created.
     */
    String getGlobalRoot();

    /**
     * Identifies a list of resource URLs that are to be installed into the VFS as global files when the plugin
     * is first loaded. The URL's that are returned are interrogated in the ClassLoader and since TAKX only has
     * one ClassLoader it is important to note that the resource names will be searched for amongst all of the other
     * resources which exist in the one single ClassLoader. All of this to say "make your resource names unique."
     * <p/>
     * <b>NOTE:</b> In RaptorX 1.3, there is no formal plugin installer, so the installation requirement is decided
     * by merely checking for the existence of the first file in the URL listing.  If the file already exists in the
     * VFS, then no files are installed, thus preserving the installed global files and any local changes that
     * may have occurred.
     *
     * @return List of URLs to resource bundled resources that are to be installed into the global VFS
     */
    List<URL> getGlobalFilesForInstallation();
}
